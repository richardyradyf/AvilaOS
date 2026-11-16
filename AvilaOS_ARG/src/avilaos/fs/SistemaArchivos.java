package avilaos.fs;

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
