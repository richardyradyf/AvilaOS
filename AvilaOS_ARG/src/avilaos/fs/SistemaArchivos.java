package avilaos.fs;

import avilaos.model.TipoOperacionES;
import avilaos.util.Iterador;
import avilaos.util.Lista;
import java.util.concurrent.Semaphore;

/**
 * @author Gianm
 * Sistema de archivos jerarquico de AvilaOS: un arbol de NodoFs que empieza en la
 * raiz "/" y un MapaBloques donde los archivos ocupan sus bloques.
 *
 * Las operaciones las pide el nucleo cuando el disco termina una solicitud de un
 * proceso de E/S. Nunca lanzan excepciones hacia el reloj: devuelven un ResultadoFs
 * con el mensaje para la bitacora.
 *
 * Lo usan a la vez el hilo del reloj (operaciones) y el hilo de Swing (el arbol y la
 * tabla), por eso todo se protege con un mutex. Los metodos privados asumen que el
 * mutex ya esta tomado. "version" aumenta con cada cambio para que el JTree sepa si
 * tiene que reconstruirse.
 */
public class SistemaArchivos {

    /** Largo maximo del nombre de un archivo o directorio. */
    public static final int MAX_NOMBRE = 30;
    /** Dueno de la raiz y usuario del modo administrador. */
    public static final String ADMIN = "admin";

    /** Datos de un nodo que se va a crear, ya validados (o el error si algo fallo). */
    private static class Destino {
        NodoFs padre;
        String nombre;
        String ruta;
        String error;
    }

    private final MapaBloques mapa;
    private final NodoFs raiz;
    private final Semaphore mutex = new Semaphore(1);
    private volatile int version = 0;

    /** Crea el sistema de archivos con solo la raiz "/" (duena admin, publica). */
    public SistemaArchivos(MapaBloques mapa) {
        if (mapa == null) {
            throw new IllegalArgumentException("El mapa de bloques es obligatorio");
        }
        this.mapa = mapa;
        this.raiz = NodoFs.nuevoDirectorio("/", null, ADMIN, Permiso.PUBLICO, NodoFs.CREADOR_SISTEMA);
    }

    // ================= consulta =================

    /** Nodo con esa ruta absoluta (por ejemplo "/usuarios/notas.txt"), o null si no existe. */
    public NodoFs buscar(String ruta) {
        mutex.acquireUninterruptibly();
        try {
            return buscarSinMutex(ruta);
        } finally {
            mutex.release();
        }
    }

    public NodoFs getRaiz() {
        return raiz;
    }

    public MapaBloques getMapa() {
        return mapa;
    }

    /** Aumenta con cada cambio del arbol; la interfaz lo compara para saber si redibujar. */
    public int getVersion() {
        return version;
    }

    /** Todos los archivos del arbol (sin directorios), recorriendo desde la raiz. Para la tabla. */
    public NodoFs[] archivos() {
        mutex.acquireUninterruptibly();
        try {
            Lista<NodoFs> lista = new Lista<>();
            juntarArchivos(raiz, lista);
            NodoFs[] copia = new NodoFs[lista.tamano()];
            Iterador<NodoFs> it = lista.iterador();
            int i = 0;
            while (it.tieneSiguiente()) {
                copia[i++] = it.siguiente();
            }
            return copia;
        } finally {
            mutex.release();
        }
    }

    // ================= crear =================

    /** Crea un directorio vacio. No ocupa bloques. */
    public ResultadoFs crearDirectorio(String ruta, String dueno, Permiso permiso) {
        mutex.acquireUninterruptibly();
        try {
            Destino d = prepararCreacion(ruta, dueno, permiso);
            if (d.error != null) {
                return ResultadoFs.error(d.error);
            }
            NodoFs nuevo = NodoFs.nuevoDirectorio(d.nombre, d.padre, dueno.trim(), permiso,
                    NodoFs.CREADOR_SISTEMA);
            d.padre.agregarHijo(nuevo);
            version++;
            return ResultadoFs.ok("Directorio " + d.ruta + " creado");
        } finally {
            mutex.release();
        }
    }

    /**
     * Crea un archivo y le asigna "bloques" bloques en el mapa (asignacion encadenada).
     * Primero valida todo y solo al final pide los bloques, asi nada queda a medias.
     */
    public ResultadoFs crearArchivo(String ruta, int bloques, String dueno, Permiso permiso,
                                    String procesoCreador) {
        mutex.acquireUninterruptibly();
        try {
            Destino d = prepararCreacion(ruta, dueno, permiso);
            if (d.error != null) {
                return ResultadoFs.error(d.error);
            }
            if (bloques < 1) {
                return ResultadoFs.error("Un archivo necesita al menos 1 bloque (se pidieron " + bloques + ")");
            }
            if (procesoCreador == null || procesoCreador.isBlank()) {
                return ResultadoFs.error("Falta el proceso que crea " + d.ruta);
            }
            int primero = mapa.asignar(bloques, d.ruta);
            if (primero < 0) {
                return ResultadoFs.error("No hay bloques libres suficientes: se piden " + bloques
                        + " y quedan " + mapa.libres());
            }
            NodoFs nuevo = NodoFs.nuevoArchivo(d.nombre, d.padre, dueno.trim(), permiso, primero,
                    bloques, procesoCreador.trim());
            d.padre.agregarHijo(nuevo);
            version++;
            return ResultadoFs.ok("Archivo " + d.ruta + " creado: " + bloques
                    + " bloques desde el bloque " + primero);
        } finally {
            mutex.release();
        }
    }

    // ================= leer, renombrar, eliminar =================

    /** Comprueba que el archivo o directorio exista (los archivos no tienen contenido). */
    public ResultadoFs leer(String ruta) {
        mutex.acquireUninterruptibly();
        try {
            NodoFs nodo = buscarSinMutex(ruta);
            if (nodo == null) {
                return ResultadoFs.error("No existe " + textoRuta(ruta));
            }
            if (nodo.esArchivo()) {
                return ResultadoFs.ok("Archivo " + nodo.getRuta() + " leído: "
                        + nodo.getCantidadBloques() + " bloques");
            }
            return ResultadoFs.ok("Directorio " + nodo.getRuta() + " leído: "
                    + nodo.cantidadHijos() + " elementos");
        } finally {
            mutex.release();
        }
    }

    /**
     * Cambia el nombre de un archivo o directorio. Como el ocupante de cada bloque es
     * la ruta del archivo, se actualiza el mapa; si es un directorio, se actualizan
     * recursivamente todos los archivos de adentro.
     */
    public ResultadoFs renombrar(String ruta, String nuevoNombre) {
        mutex.acquireUninterruptibly();
        try {
            NodoFs nodo = buscarSinMutex(ruta);
            if (nodo == null) {
                return ResultadoFs.error("No existe " + textoRuta(ruta));
            }
            if (nodo.esRaiz()) {
                return ResultadoFs.error("No se puede renombrar la raíz /");
            }
            String nuevo = nuevoNombre == null ? null : nuevoNombre.trim();
            String errorNombre = validarNombre(nuevo);
            if (errorNombre != null) {
                return ResultadoFs.error(errorNombre);
            }
            NodoFs padre = nodo.getPadre();
            if (padre.buscarHijo(nuevo) != null) {
                return ResultadoFs.error("Ya existe " + rutaHija(padre, nuevo));
            }
            String rutaVieja = nodo.getRuta();
            nodo.cambiarNombre(nuevo);
            actualizarOcupantes(nodo);
            version++;
            return ResultadoFs.ok(rutaVieja + " renombrado a " + nodo.getRuta());
        } finally {
            mutex.release();
        }
    }

    /**
     * Elimina un archivo (libera sus bloques) o un directorio completo: primero todos
     * sus hijos, archivos y subdirectorios, y luego el directorio. La raiz no se elimina.
     */
    public ResultadoFs eliminar(String ruta) {
        mutex.acquireUninterruptibly();
        try {
            NodoFs nodo = buscarSinMutex(ruta);
            if (nodo == null) {
                return ResultadoFs.error("No existe " + textoRuta(ruta));
            }
            if (nodo.esRaiz()) {
                return ResultadoFs.error("No se puede eliminar la raíz /");
            }
            String rutaNodo = nodo.getRuta();
            int archivosBorrados = contarArchivos(nodo);
            int liberados = eliminarRecursivo(nodo);
            nodo.getPadre().quitarHijo(nodo);
            version++;
            if (nodo.esArchivo()) {
                return ResultadoFs.ok("Archivo " + rutaNodo + " eliminado: " + liberados + " bloques liberados");
            }
            return ResultadoFs.ok("Directorio " + rutaNodo + " eliminado con " + archivosBorrados
                    + " archivos: " + liberados + " bloques liberados");
        } finally {
            mutex.release();
        }
    }

    // ================= lo que pide el nucleo =================

    /**
     * Bloque al que debe ir el cabezal del disco para esta operacion.
     * - LEER, ACTUALIZAR y ELIMINAR de un archivo: su primer bloque.
     * - CREAR: el primer bloque libre, porque ahi empezara la asignacion.
     * - Un directorio: el primer bloque del primer archivo que tenga dentro.
     * Devuelve -1 si no hay bloque (directorio vacio, ruta inexistente, disco lleno):
     * con -1 el disco no mueve el cabezal.
     */
    public int bloqueDestino(TipoOperacionES op, String ruta) {
        if (op == null) {
            return -1;
        }
        mutex.acquireUninterruptibly();
        try {
            switch (op) {
                case CREAR:
                    return mapa.primerLibre();
                case LEER:
                case ACTUALIZAR:
                case ELIMINAR: {
                    NodoFs nodo = buscarSinMutex(ruta);
                    if (nodo == null) {
                        return -1;
                    }
                    return nodo.esArchivo() ? nodo.getPrimerBloque() : primerBloqueDentro(nodo);
                }
                default:
                    return -1;   // SUSPENDER y REACTIVAR no son operaciones del sistema de archivos
            }
        } finally {
            mutex.release();
        }
    }

    /**
     * Cantidad de bloques que transfiere el disco para esta operacion.
     * - LEER de un archivo: todos sus bloques (de un directorio, 1).
     * - CREAR: los bloques nuevos del archivo.
     * - ACTUALIZAR y ELIMINAR: 1, porque solo se escriben metadatos.
     * SUSPENDER y REACTIVAR devuelven 0: no son operaciones del sistema de archivos.
     */
    public int bloquesATransferir(TipoOperacionES op, String ruta, int bloquesNuevos) {
        if (op == null) {
            return 0;
        }
        mutex.acquireUninterruptibly();
        try {
            switch (op) {
                case LEER: {
                    NodoFs nodo = buscarSinMutex(ruta);
                    return (nodo != null && nodo.esArchivo()) ? nodo.getCantidadBloques() : 1;
                }
                case CREAR:
                    return Math.max(1, bloquesNuevos);
                case ACTUALIZAR:
                case ELIMINAR:
                    return 1;
                default:
                    return 0;
            }
        } finally {
            mutex.release();
        }
    }

    
        // ================= carga desde JSON (solo CargadorSistemaArchivos) =================

    /**
     * Crea un archivo que ya trae sus bloques exactos (del JSON). Valida igual que
     * crearArchivo; el proceso creador queda como "sistema".
     */
    ResultadoFs cargarArchivo(String ruta, int[] cadena, String dueno, Permiso permiso) {
        mutex.acquireUninterruptibly();
        try {
            Destino d = prepararCreacion(ruta, dueno, permiso);
            if (d.error != null) {
                return ResultadoFs.error(d.error);
            }
            if (cadena == null || cadena.length == 0) {
                return ResultadoFs.error("El archivo " + d.ruta + " necesita al menos 1 bloque");
            }
            int primero = mapa.asignarCadena(cadena, d.ruta);
            if (primero < 0) {
                return ResultadoFs.error("Los bloques de " + d.ruta
                        + " se solapan con otro archivo o se salen del área de archivos");
            }
            NodoFs nuevo = NodoFs.nuevoArchivo(d.nombre, d.padre, dueno.trim(), permiso, primero,
                    cadena.length, NodoFs.CREADOR_SISTEMA);
            d.padre.agregarHijo(nuevo);
            version++;
            return ResultadoFs.ok("Archivo " + d.ruta + " cargado: " + cadena.length
                    + " bloques desde el bloque " + primero);
        } finally {
            mutex.release();
        }
    }

    /** Todos los nodos (sin la raiz), cada directorio antes que su contenido. Para guardar. */
    NodoFs[] todosLosNodos() {
        mutex.acquireUninterruptibly();
        try {
            Lista<NodoFs> lista = new Lista<>();
            juntarNodos(raiz, lista);
            NodoFs[] copia = new NodoFs[lista.tamano()];
            Iterador<NodoFs> it = lista.iterador();
            int i = 0;
            while (it.tieneSiguiente()) {
                copia[i++] = it.siguiente();
            }
            return copia;
        } finally {
            mutex.release();
        }
    }

    private void juntarNodos(NodoFs nodo, Lista<NodoFs> destino) {
        for (NodoFs hijo : nodo.getHijos()) {
            destino.agregarAlFinal(hijo);
            if (hijo.esDirectorio()) {
                juntarNodos(hijo, destino);
            }
        }
    }
    // ================= privados (el mutex ya esta tomado) =================

    /** Recorre el arbol desde la raiz siguiendo cada parte de la ruta. */
    private NodoFs buscarSinMutex(String ruta) {
        if (ruta == null) {
            return null;
        }
        String limpia = ruta.trim();
        if (!limpia.startsWith("/")) {
            return null;
        }
        if (limpia.equals("/")) {
            return raiz;
        }
        String[] partes = limpia.substring(1).split("/", -1);
        NodoFs actual = raiz;
        for (String parte : partes) {
            if (parte.isEmpty() || actual.esArchivo()) {
                return null;
            }
            actual = actual.buscarHijo(parte);
            if (actual == null) {
                return null;
            }
        }
        return actual;
    }

    /**
     * Valida todo lo necesario para crear un nodo: ruta, padre, nombre, que no este
     * repetido, dueno y permiso. Si algo falla, el Destino trae el mensaje de error.
     */
    private Destino prepararCreacion(String ruta, String dueno, Permiso permiso) {
        Destino d = new Destino();
        if (ruta == null || ruta.isBlank()) {
            d.error = "La ruta no puede estar vacía";
            return d;
        }
        String limpia = ruta.trim();
        if (!limpia.startsWith("/")) {
            d.error = "La ruta debe empezar con /: " + limpia;
            return d;
        }
        if (limpia.equals("/")) {
            d.error = "La raíz / ya existe";
            return d;
        }
        if (limpia.endsWith("/")) {
            d.error = "La ruta no puede terminar en /: " + limpia;
            return d;
        }
        int corte = limpia.lastIndexOf('/');
        String rutaPadre = corte == 0 ? "/" : limpia.substring(0, corte);
        String nombre = limpia.substring(corte + 1);
        String errorNombre = validarNombre(nombre);
        if (errorNombre != null) {
            d.error = errorNombre;
            return d;
        }
        NodoFs padre = buscarSinMutex(rutaPadre);
        if (padre == null) {
            d.error = "No existe el directorio " + rutaPadre;
            return d;
        }
        if (padre.esArchivo()) {
            d.error = rutaPadre + " es un archivo, no un directorio";
            return d;
        }
        if (padre.buscarHijo(nombre) != null) {
            d.error = "Ya existe " + limpia;
            return d;
        }
        if (dueno == null || dueno.isBlank()) {
            d.error = "El dueño de " + limpia + " no puede estar vacío";
            return d;
        }
        if (permiso == null) {
            d.error = "Falta el permiso (público o privado) de " + limpia;
            return d;
        }
        d.padre = padre;
        d.nombre = nombre;
        d.ruta = limpia;
        return d;
    }

    /** Pone la ruta actual como ocupante de los bloques de cada archivo (al renombrar). */
    private void actualizarOcupantes(NodoFs nodo) {
        if (nodo.esArchivo()) {
            mapa.cambiarOcupante(nodo.getPrimerBloque(), nodo.getRuta());
            return;
        }
        for (NodoFs hijo : nodo.getHijos()) {
            actualizarOcupantes(hijo);
        }
    }

    /** Libera los bloques de un archivo o, si es directorio, de todo lo que tiene dentro. */
    private int eliminarRecursivo(NodoFs nodo) {
        if (nodo.esArchivo()) {
            return mapa.liberar(nodo.getPrimerBloque());
        }
        int liberados = 0;
        for (NodoFs hijo : nodo.getHijos()) {
            liberados += eliminarRecursivo(hijo);
            nodo.quitarHijo(hijo);
        }
        return liberados;
    }

    private int contarArchivos(NodoFs nodo) {
        if (nodo.esArchivo()) {
            return 1;
        }
        int cantidad = 0;
        for (NodoFs hijo : nodo.getHijos()) {
            cantidad += contarArchivos(hijo);
        }
        return cantidad;
    }

    /** Primer bloque del primer archivo que haya dentro del directorio, o -1 si no hay. */
    private int primerBloqueDentro(NodoFs directorio) {
        for (NodoFs hijo : directorio.getHijos()) {
            if (hijo.esArchivo()) {
                return hijo.getPrimerBloque();
            }
            int dentro = primerBloqueDentro(hijo);
            if (dentro >= 0) {
                return dentro;
            }
        }
        return -1;
    }

    private void juntarArchivos(NodoFs nodo, Lista<NodoFs> destino) {
        for (NodoFs hijo : nodo.getHijos()) {
            if (hijo.esArchivo()) {
                destino.agregarAlFinal(hijo);
            } else {
                juntarArchivos(hijo, destino);
            }
        }
    }

    private static String rutaHija(NodoFs padre, String nombre) {
        return padre.esRaiz() ? "/" + nombre : padre.getRuta() + "/" + nombre;
    }

    private static String textoRuta(String ruta) {
        return ruta == null || ruta.isBlank() ? "(ruta vacía)" : ruta.trim();
    }

    /** Devuelve null si el nombre es valido, o el mensaje que explica por que no. */
    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "El nombre no puede estar vacío";
        }
        if (nombre.contains("/")) {
            return "El nombre no puede contener /: " + nombre;
        }
        if (nombre.length() > MAX_NOMBRE) {
            return "El nombre \"" + nombre + "\" tiene " + nombre.length()
                    + " caracteres (máximo " + MAX_NOMBRE + ")";
        }
        return null;
    }
}
