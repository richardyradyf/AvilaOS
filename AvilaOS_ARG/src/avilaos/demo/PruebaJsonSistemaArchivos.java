package avilaos.demo;

import avilaos.config.EntradaInvalidaException;
import avilaos.fs.CargadorSistemaArchivos;
import avilaos.fs.EstadoDiscoInicial;
import avilaos.fs.NodoFs;
import avilaos.fs.Permiso;
import avilaos.fs.ResultadoFs;
import avilaos.fs.SistemaArchivos;

/**
 * @author Gianm
 * Pruebas del JSON del sistema de archivos: carga del JSON del enunciado, orden de
 * las entradas, JSON invalidos y la ida y vuelta (guardar y volver a cargar) con un
 * archivo repartido en el disco.
 */
public class PruebaJsonSistemaArchivos {

    /** JSON del sistema de archivos del enunciado. */
    private static final String ENUNCIADO = """
            {
              "diskConfig": { "totalBlocks": 200, "initialHead": 50 },
              "fileSystem": [
                { "path": "/mbr.bin", "type": "FILE", "startBlock": 0, "blocks": 11, "owner": "admin", "permissions": "public" },
                { "path": "/sistema", "type": "DIRECTORY", "owner": "admin", "permissions": "public" },
                { "path": "/usuarios", "type": "DIRECTORY", "owner": "admin", "permissions": "public" },
                { "path": "/sistema/boot.bin", "type": "FILE", "startBlock": 11, "blocks": 23, "owner": "admin", "permissions": "public" },
                { "path": "/usuarios/notas.txt", "type": "FILE", "startBlock": 34, "blocks": 28, "owner": "user1", "permissions": "private" },
                { "path": "/sistema/script.py", "type": "FILE", "startBlock": 62, "blocks": 2, "owner": "admin", "permissions": "public" },
                { "path": "/sistema/core.dll", "type": "FILE", "startBlock": 64, "blocks": 31, "owner": "admin", "permissions": "public" },
                { "path": "/sistema/config.sys", "type": "FILE", "startBlock": 95, "blocks": 24, "owner": "admin", "permissions": "public" },
                { "path": "/usuarios/img1.png", "type": "FILE", "startBlock": 119, "blocks": 4, "owner": "user1", "permissions": "private" },
                { "path": "/usuarios/data.csv", "type": "FILE", "startBlock": 123, "blocks": 57, "owner": "user2", "permissions": "public" },
                { "path": "/usuarios/video.mp4", "type": "FILE", "startBlock": 180, "blocks": 20, "owner": "user1", "permissions": "public" }
              ]
            }
            """;

    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        probarEnunciado();
        probarOrdenDeLasEntradas();
        probarJsonInvalidos();
        probarIdaYVuelta();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static void probarEnunciado() {
        System.out.println("--- JSON del enunciado ---");
        EstadoDiscoInicial e = CargadorSistemaArchivos.desdeJson(ENUNCIADO);
        SistemaArchivos fs = e.getSistemaArchivos();
        verificar(e.getTotalBloques() == 200 && e.getCabezalInicial() == 50, "disco de 200 bloques con el cabezal en 50");
        verificar(fs.archivos().length == 9, "se cargan 9 archivos");
        verificar(contarDirectorios(fs.getRaiz()) == 2, "se cargan 2 directorios");
        verificar(fs.getMapa().libres() == 0, "el disco queda lleno (libres = 0)");
        NodoFs notas = fs.buscar("/usuarios/notas.txt");
        verificar(notas != null && notas.getPrimerBloque() == 34 && notas.getCantidadBloques() == 28,
                "/usuarios/notas.txt empieza en 34 con 28 bloques");
        int[] cadena = fs.getMapa().cadena(34);
        verificar(cadena.length == 28 && cadena[0] == 34 && cadena[27] == 61, "su cadena va contigua del 34 al 61");
        NodoFs mbr = fs.buscar("/mbr.bin");
        verificar(mbr != null && "admin".equals(mbr.getDueno()), "/mbr.bin queda en la raiz, de admin");
        NodoFs datos = fs.buscar("/usuarios/data.csv");
        verificar(notas != null && notas.getPermiso() == Permiso.PRIVADO && "user1".equals(notas.getDueno())
                && datos != null && datos.getPermiso() == Permiso.PUBLICO && "user2".equals(datos.getDueno()),
                "se respetan dueno y permiso de cada archivo");
        verificar(notas != null && "sistema".equals(notas.getProcesoCreador()), "lo que viene del JSON lo creo el sistema");
    }

    private static void probarOrdenDeLasEntradas() {
        System.out.println("--- El orden del JSON no importa ---");
        String json = disco(100, 0, archivo("/a/b/c.txt", 0, 3), dir("/a/b"), dir("/a"));
        boolean cargo;
        try {
            SistemaArchivos fs = CargadorSistemaArchivos.desdeJson(json).getSistemaArchivos();
            cargo = fs.buscar("/a/b/c.txt") != null;
        } catch (EntradaInvalidaException e) {
            cargo = false;
        }
        verificar(cargo, "un archivo que aparece antes que sus directorios se carga igual");
    }

    private static void probarJsonInvalidos() {
        System.out.println("--- JSON invalidos ---");
        rechaza(disco(100, 0, archivo("/a", 0, 10), archivo("/b", 5, 10)), "se solapan",
                "rechaza archivos con bloques solapados");
        rechaza(disco(100, 0, archivo("/a", 98, 5)), "se sale del disco",
                "rechaza un archivo que se sale del disco");
        rechaza(disco(100, 0, archivo("/nada/x.txt", 0, 3)), "No existe el directorio /nada",
                "rechaza un archivo cuyo directorio padre no existe");
        rechaza(disco(100, 0, "{\"path\": \"/a\", \"type\": \"LINK\", \"owner\": \"admin\", \"permissions\": \"public\"}"),
                ".type", "rechaza un type desconocido");
        rechaza(disco(100, 0, dir("/a"), dir("/a")), "ruta repetida", "rechaza una ruta repetida");
        rechaza(disco(5, 0), "totalBlocks", "rechaza totalBlocks 5 (debe ser de 100 a 500)");
        rechaza(disco(100, 100), "initialHead", "rechaza un initialHead fuera del disco");
        rechaza(disco(100, 0, "{\"path\": \"/a\", \"type\": \"DIRECTORY\", \"owner\": \"admin\", \"permissions\": \"secret\"}"),
                "permissions", "rechaza un permiso que no es public ni private");
        rechaza(disco(100, 0, "{\"path\": \"/a\", \"type\": \"DIRECTORY\", \"permissions\": \"public\"}"),
                "owner", "rechaza una entrada sin owner");
        rechaza("{", "JSON", "rechaza un texto que no es JSON valido");
    }

    private static void probarIdaYVuelta() {
        System.out.println("--- Guardar y volver a cargar ---");
        SistemaArchivos recien = CargadorSistemaArchivos.desdeJson(ENUNCIADO).getSistemaArchivos();
        verificar(!CargadorSistemaArchivos.aJson(recien, 50).contains("chain"),
                "un disco recien cargado se guarda sin chain (todo contiguo)");

        SistemaArchivos fs1 = CargadorSistemaArchivos.desdeJson(ENUNCIADO).getSistemaArchivos();
        fs1.eliminar("/usuarios/img1.png");   // libera 119 a 122
        fs1.eliminar("/sistema/script.py");   // libera 62 y 63
        ResultadoFs r = fs1.crearArchivo("/usuarios/nuevo.txt", 6, "user2", Permiso.PRIVADO, "P9");
        verificar(r.isExito(), "despues de borrar dos archivos se crea uno nuevo de 6 bloques");
        int[] cadenaNueva = fs1.getMapa().cadena(fs1.buscar("/usuarios/nuevo.txt").getPrimerBloque());
        verificar(iguales(cadenaNueva, new int[]{62, 63, 119, 120, 121, 122}),
                "el archivo nuevo queda repartido: 62 63 119 120 121 122");

        String json = CargadorSistemaArchivos.aJson(fs1, 77);
        verificar(json.contains("\"chain\""), "al guardar, el archivo repartido lleva su chain");

        EstadoDiscoInicial e2 = CargadorSistemaArchivos.desdeJson(json);
        SistemaArchivos fs2 = e2.getSistemaArchivos();
        verificar(e2.getCabezalInicial() == 77 && e2.getTotalBloques() == 200, "se conservan el cabezal y el total de bloques");
        verificar(fs2.archivos().length == 8 && fs2.getMapa().libres() == 0, "vuelve con 8 archivos y el disco lleno");
        boolean igual = true;
        for (NodoFs a : fs1.archivos()) {
            NodoFs b = fs2.buscar(a.getRuta());
            igual = igual && b != null
                    && iguales(fs1.getMapa().cadena(a.getPrimerBloque()), fs2.getMapa().cadena(b.getPrimerBloque()))
                    && a.getDueno().equals(b.getDueno()) && a.getPermiso() == b.getPermiso();
        }
        verificar(igual, "cada archivo vuelve con la misma cadena, dueno y permiso");
        
        SistemaArchivos conSwap = CargadorSistemaArchivos.desdeJson(ENUNCIADO).getSistemaArchivos();
        conSwap.getMapa().configurarSwap(100);   // disco de 300 bloques: swap del 200 al 299
        EstadoDiscoInicial e3 = CargadorSistemaArchivos.desdeJson(CargadorSistemaArchivos.aJson(conSwap, 250));
        verificar(e3.getCabezalInicial() == 199,
                "con el cabezal en el swap (250) se guarda en el ultimo bloque de archivos (199) y vuelve a cargar");
    }

    // ---- auxiliares ----

    private static String disco(int totalBloques, int cabezal, String... entradas) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"diskConfig\": {\"totalBlocks\": ").append(totalBloques)
          .append(", \"initialHead\": ").append(cabezal).append("}, \"fileSystem\": [");
        for (int i = 0; i < entradas.length; i++) {
            sb.append(i > 0 ? ", " : "").append(entradas[i]);
        }
        return sb.append("]}").toString();
    }

    private static String dir(String ruta) {
        return "{\"path\": \"" + ruta + "\", \"type\": \"DIRECTORY\", \"owner\": \"admin\", \"permissions\": \"public\"}";
    }

    private static String archivo(String ruta, int inicio, int bloques) {
        return "{\"path\": \"" + ruta + "\", \"type\": \"FILE\", \"startBlock\": " + inicio
                + ", \"blocks\": " + bloques + ", \"owner\": \"admin\", \"permissions\": \"public\"}";
    }

    /** Comprueba que el JSON se rechace con un mensaje que contenga "debeDecir", y lo muestra. */
    private static void rechaza(String json, String debeDecir, String descripcion) {
        String mensaje = null;
        try {
            CargadorSistemaArchivos.desdeJson(json);
        } catch (EntradaInvalidaException e) {
            mensaje = e.getMessage();
        }
        verificar(mensaje != null && mensaje.contains(debeDecir), descripcion);
        if (mensaje != null) {
            System.out.println("        mensaje: " + mensaje);
        }
    }

    private static int contarDirectorios(NodoFs nodo) {
        int cantidad = 0;
        for (NodoFs hijo : nodo.getHijos()) {
            if (hijo.esDirectorio()) {
                cantidad += 1 + contarDirectorios(hijo);
            }
        }
        return cantidad;
    }

    private static boolean iguales(int[] a, int[] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    private static void verificar(boolean condicion, String descripcion) {
        total++;
        if (condicion) {
            System.out.println("OK    - " + descripcion);
        } else {
            fallos++;
            System.out.println("FALLO - " + descripcion);
        }
    }
}
