package avilaos.fs;

import avilaos.config.EntradaInvalidaException;
import avilaos.config.JsonObjeto;
import avilaos.config.LectorJson;
import avilaos.util.Lista;

/**
 * @author Gianm
 * Carga y guarda el sistema de archivos en el formato JSON del enunciado:
 *
 * { "diskConfig": { "totalBlocks": 200, "initialHead": 50 },
 *   "fileSystem": [ { "path": "/sistema", "type": "DIRECTORY", "owner": "admin", "permissions": "public" },
 *                   { "path": "/sistema/boot.bin", "type": "FILE", "startBlock": 11, "blocks": 23, ... } ] }
 *
 * Al cargar valida cada campo y, si algo esta mal, lanza EntradaInvalidaException con
 * un mensaje que dice que entrada fallo. Crea primero los directorios (de menos a mas
 * profundos) y despues los archivos, asi no importa el orden del JSON. Los archivos
 * se cargan contiguos (startBlock, startBlock+1...), salvo que traigan "chain".
 *
 * Al guardar escribe el mismo formato. Un archivo que quedo repartido en el disco
 * lleva ademas "chain" con su cadena exacta, para poder reconstruirlo igual.
 */
public final class CargadorSistemaArchivos {

    public static final int MIN_BLOQUES = 100;
    public static final int MAX_BLOQUES = 500;

    /** Una entrada de "fileSystem" ya validada. */
    private static final class Entrada {
        String campo;
        String ruta;
        TipoNodo tipo;
        String dueno;
        Permiso permiso;
        int[] cadena;
    }

    private CargadorSistemaArchivos() {
    }

    // ================= cargar =================

    /** Lee y valida el JSON y arma el sistema de archivos. */
    public static EstadoDiscoInicial desdeJson(String texto) {
        JsonObjeto o = objeto(LectorJson.leer(texto), "El JSON del sistema de archivos");
        JsonObjeto disco = objeto(requerido(o, "diskConfig", "diskConfig"), "diskConfig");
        int total = entero(disco, "totalBlocks", "diskConfig.totalBlocks", MIN_BLOQUES, MAX_BLOQUES);
        int cabezal = entero(disco, "initialHead", "diskConfig.initialHead", 0, total - 1);
        Lista<Object> lista = lista(o, "fileSystem");

        Entrada[] entradas = new Entrada[lista.tamano()];
        Lista<String> rutas = new Lista<>();
        int profundidadMaxima = 0;
        for (int i = 0; i < entradas.length; i++) {
            Entrada e = leerEntrada(lista.obtener(i), i + 1, total);
            if (rutas.contiene(e.ruta)) {
                throw new EntradaInvalidaException(e.campo + ": ruta repetida");
            }
            rutas.agregarAlFinal(e.ruta);
            entradas[i] = e;
            profundidadMaxima = Math.max(profundidadMaxima, profundidad(e.ruta));
        }

        MapaBloques mapa = new MapaBloques(total);
        SistemaArchivos fs = new SistemaArchivos(mapa);
        // 1) directorios, de menos a mas profundos: el padre siempre existe antes que el hijo
        for (int p = 1; p <= profundidadMaxima; p++) {
            for (Entrada e : entradas) {
                if (e.tipo == TipoNodo.DIRECTORIO && profundidad(e.ruta) == p) {
                    exigir(fs.crearDirectorio(e.ruta, e.dueno, e.permiso), e.campo);
                }
            }
        }
        // 2) archivos, con sus bloques exactos
        for (Entrada e : entradas) {
            if (e.tipo == TipoNodo.ARCHIVO) {
                exigir(fs.cargarArchivo(e.ruta, e.cadena, e.dueno, e.permiso), e.campo);
            }
        }
        return new EstadoDiscoInicial(fs, mapa, total, cabezal);
    }

    private static Entrada leerEntrada(Object valor, int numero, int total) {
        String base = "fileSystem[" + numero + "]";
        JsonObjeto j = objeto(valor, base);
        Entrada e = new Entrada();
        e.ruta = texto(j, "path", base + ".path").trim();
        if (!e.ruta.startsWith("/")) {
            throw new EntradaInvalidaException(base + ".path: debe ser una ruta absoluta que empiece con / (\""
                    + e.ruta + "\")");
        }
        e.campo = base + " (" + e.ruta + ")";

        String textoTipo = texto(j, "type", e.campo + ".type");
        try {
            e.tipo = TipoNodo.desdeJson(textoTipo);
        } catch (IllegalArgumentException ex) {
            throw new EntradaInvalidaException(e.campo + ".type: debe ser FILE o DIRECTORY (vale \"" + textoTipo + "\")");
        }

        e.dueno = texto(j, "owner", e.campo + ".owner").trim();
        if (e.dueno.isEmpty()) {
            throw new EntradaInvalidaException(e.campo + ".owner: no puede estar vacío");
        }

        String textoPermiso = texto(j, "permissions", e.campo + ".permissions");
        try {
            e.permiso = Permiso.desdeJson(textoPermiso);
        } catch (IllegalArgumentException ex) {
            throw new EntradaInvalidaException(e.campo + ".permissions: debe ser public o private (vale \""
                    + textoPermiso + "\")");
        }

        if (e.tipo == TipoNodo.ARCHIVO) {
            int inicio = entero(j, "startBlock", e.campo + ".startBlock", 0, total - 1);
            int bloques = entero(j, "blocks", e.campo + ".blocks", 1, total);
            if (j.tiene("chain")) {
                e.cadena = leerCadena(j, e.campo, inicio, bloques, total);
            } else {
                if (inicio + bloques > total) {
                    throw new EntradaInvalidaException(e.campo + ": startBlock + blocks = " + (inicio + bloques)
                            + " se sale del disco (" + total + " bloques)");
                }
                e.cadena = new int[bloques];
                for (int i = 0; i < bloques; i++) {
                    e.cadena[i] = inicio + i;
                }
            }
        }
        return e;
    }

    /** Lee "chain": la lista exacta de bloques de un archivo que quedo repartido. */
    private static int[] leerCadena(JsonObjeto j, String campo, int inicio, int bloques, int total) {
        Lista<Object> lista = lista(j, "chain");
        if (lista.tamano() != bloques) {
            throw new EntradaInvalidaException(campo + ".chain: tiene " + lista.tamano()
                    + " bloques y blocks dice " + bloques);
        }
        int[] cadena = new int[bloques];
        for (int i = 0; i < bloques; i++) {
            Object v = lista.obtener(i);
            if (!(v instanceof Long) || (Long) v < 0 || (Long) v >= total) {
                throw new EntradaInvalidaException(campo + ".chain[" + (i + 1)
                        + "]: debe ser un número entero entre 0 y " + (total - 1));
            }
            cadena[i] = (int) (long) (Long) v;
        }
        if (cadena[0] != inicio) {
            throw new EntradaInvalidaException(campo + ".chain: debe empezar en startBlock (" + inicio + ")");
        }
        return cadena;
    }

    // ================= guardar =================

    /** Escribe el estado del sistema de archivos en el mismo formato del JSON. */
    public static String aJson(SistemaArchivos fs, int cabezalActual) {
        if (fs == null) {
            throw new IllegalArgumentException("El sistema de archivos es obligatorio");
        }
        MapaBloques mapa = fs.getMapa();
        int total = mapa.getBloquesArchivos();
        int totalDisco = mapa.getTotalBloques();   // archivos + swap
        if (cabezalActual < 0 || cabezalActual >= totalDisco) {
            throw new IllegalArgumentException("El cabezal debe estar entre 0 y " + (totalDisco - 1));
        }
        // El formato solo describe el area de archivos (initialHead de 0 a totalBlocks - 1).
        // Si el cabezal esta en el swap, se guarda en el ultimo bloque del area de archivos.
        int cabezalGuardado = Math.min(cabezalActual, total - 1);
        NodoFs[] nodos = fs.todosLosNodos();
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"diskConfig\": {\n");
        sb.append("    \"totalBlocks\": ").append(total).append(",\n");
        sb.append("    \"initialHead\": ").append(cabezalGuardado).append("\n");
        sb.append("  },\n");
        sb.append("  \"fileSystem\": [\n");
        for (int i = 0; i < nodos.length; i++) {
            NodoFs n = nodos[i];
            sb.append("    {\"path\": \"").append(escapar(n.getRuta())).append("\"")
              .append(", \"type\": \"").append(n.getTipo().aJson()).append("\"");
            if (n.esArchivo()) {
                int[] cadena = mapa.cadena(n.getPrimerBloque());
                sb.append(", \"startBlock\": ").append(n.getPrimerBloque())
                  .append(", \"blocks\": ").append(n.getCantidadBloques());
                if (!esContigua(cadena, n.getCantidadBloques())) {
                    sb.append(", \"chain\": [");
                    for (int k = 0; k < cadena.length; k++) {
                        sb.append(k > 0 ? ", " : "").append(cadena[k]);
                    }
                    sb.append("]");
                }
            }
            sb.append(", \"owner\": \"").append(escapar(n.getDueno())).append("\"")
              .append(", \"permissions\": \"").append(n.getPermiso().aJson()).append("\"}")
              .append(i < nodos.length - 1 ? "," : "").append("\n");
        }
        sb.append("  ]\n}\n");
        return sb.toString();
    }

    // ================= ayudas =================

    private static boolean esContigua(int[] cadena, int bloques) {
        if (cadena.length != bloques) {
            return false;
        }
        for (int i = 1; i < cadena.length; i++) {
            if (cadena[i] != cadena[i - 1] + 1) {
                return false;
            }
        }
        return true;
    }

    /** Cantidad de "/" de la ruta: /sistema es 1, /sistema/boot.bin es 2. */
    private static int profundidad(String ruta) {
        int barras = 0;
        for (int i = 0; i < ruta.length(); i++) {
            if (ruta.charAt(i) == '/') {
                barras++;
            }
        }
        return barras;
    }

    private static void exigir(ResultadoFs r, String campo) {
        if (!r.isExito()) {
            throw new EntradaInvalidaException(campo + ": " + r.getMensaje());
        }
    }

    private static String escapar(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static Object requerido(JsonObjeto o, String clave, String campo) {
        if (!o.tiene(clave)) {
            throw new EntradaInvalidaException(campo + ": falta este campo");
        }
        return o.obtener(clave);
    }

    private static int entero(JsonObjeto o, String clave, String campo, int min, int max) {
        Object v = requerido(o, clave, campo);
        if (!(v instanceof Long)) {
            throw new EntradaInvalidaException(campo + ": debe ser un número entero entre " + min + " y " + max);
        }
        long n = (Long) v;
        if (n < min || n > max) {
            throw new EntradaInvalidaException(campo + ": debe ser un número entero entre " + min + " y " + max
                    + " (vale " + n + ")");
        }
        return (int) n;
    }

    private static String texto(JsonObjeto o, String clave, String campo) {
        Object v = requerido(o, clave, campo);
        if (!(v instanceof String)) {
            throw new EntradaInvalidaException(campo + ": debe ser un texto entre comillas");
        }
        return (String) v;
    }

    @SuppressWarnings("unchecked")
    private static Lista<Object> lista(JsonObjeto o, String clave) {
        Object v = requerido(o, clave, clave);
        if (!(v instanceof Lista)) {
            throw new EntradaInvalidaException(clave + ": debe ser una lista [ ... ]");
        }
        return (Lista<Object>) v;
    }

    private static JsonObjeto objeto(Object v, String campo) {
        if (!(v instanceof JsonObjeto)) {
            throw new EntradaInvalidaException(campo + ": debe ser un objeto { ... }");
        }
        return (JsonObjeto) v;
    }
}
