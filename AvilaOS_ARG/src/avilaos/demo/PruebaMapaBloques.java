package avilaos.demo;

import avilaos.fs.MapaBloques;

/**
 * @author Gianm
 * Pruebas del mapa de bloques: asignacion encadenada, huecos, falta de espacio,
 * liberacion, area de swap, carga contigua y renombrado.
 */
public class PruebaMapaBloques {

    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        probarAsignacionSimple();
        probarHuecoNoContiguo();
        probarSinEspacio();
        probarLiberar();
        probarSwap();
        probarAsignarEn();
        probarCambiarOcupante();
        probarValidaciones();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static void probarAsignacionSimple() {
        System.out.println("--- Asignar 5 bloques en un disco de 20 ---");
        MapaBloques m = new MapaBloques(20);
        int primero = m.asignar(5, "/a.txt");
        verificar(primero == 0, "el archivo empieza en el bloque 0");
        verificar(iguales(m.cadena(primero), new int[]{0, 1, 2, 3, 4}), "la cadena es 0 -> 1 -> 2 -> 3 -> 4");
        verificar(m.getSiguiente(4) == -1, "el ultimo bloque apunta a -1");
        verificar(m.libres() == 15, "quedan 15 bloques libres");
        verificar("/a.txt".equals(m.getOcupante(2)), "los bloques guardan la ruta del archivo");
        verificar(m.primerLibre() == 5, "el primer libre es el 5");
    }

    private static void probarHuecoNoContiguo() {
        System.out.println("--- Hueco en el medio: cadena no contigua ---");
        MapaBloques m = new MapaBloques(20);
        m.asignar(3, "/a");                 // 0, 1, 2
        int b = m.asignar(4, "/b");         // 3, 4, 5, 6
        m.asignar(2, "/c");                 // 7, 8
        verificar(b == 3, "/b ocupa del bloque 3 al 6");
        verificar(m.liberar(b) == 4, "al liberar /b se liberan 4 bloques");
        int d = m.asignar(6, "/d");
        verificar(iguales(m.cadena(d), new int[]{3, 4, 5, 6, 9, 10}),
                "el nuevo archivo usa el hueco y sigue al final: 3 4 5 6 9 10");
        verificar(m.getSiguiente(6) == 9, "el bloque 6 apunta al 9: la cadena no es contigua");
        verificar("/c".equals(m.getOcupante(7)), "el archivo /c no se movio");
    }

    private static void probarSinEspacio() {
        System.out.println("--- No hay bloques suficientes ---");
        MapaBloques m = new MapaBloques(10);
        m.asignar(8, "/grande");
        verificar(m.asignar(5, "/x") == -1, "pedir 5 bloques con 2 libres devuelve -1");
        verificar(m.libres() == 2, "no se asigno nada a medias");
        verificar(m.estaLibre(8) && m.estaLibre(9), "los bloques 8 y 9 siguen libres");
        verificar(m.asignar(2, "/justo") == 8, "pedir exactamente los 2 libres si funciona");
        verificar(m.primerLibre() == -1 && m.libres() == 0, "el disco queda lleno");
    }

    private static void probarLiberar() {
        System.out.println("--- Liberar ---");
        MapaBloques m = new MapaBloques(20);
        int a = m.asignar(5, "/a");
        verificar(m.liberar(2) == 0, "liberar desde la mitad de una cadena no libera nada");
        verificar(m.liberar(a) == 5, "liberar el archivo devuelve 5");
        verificar(m.libres() == 20, "todos los bloques quedan libres");
        verificar(m.getOcupante(0) == null && m.getSiguiente(0) == -1,
                "el bloque liberado queda sin ocupante y sin siguiente");
        verificar(m.liberar(a) == 0, "liberar un bloque libre devuelve 0");
        verificar(m.cadena(a).length == 0, "la cadena de un bloque libre esta vacia");
    }

    private static void probarSwap() {
        System.out.println("--- Area de swap ---");
        MapaBloques m = new MapaBloques(20);
        m.configurarSwap(10);
        verificar(m.getTotalBloques() == 30 && m.getBloquesSwap() == 10, "el disco tiene 20 + 10 = 30 bloques");
        verificar(!m.esSwap(19) && m.esSwap(20), "el swap empieza justo despues del area de archivos (bloque 20)");
        verificar(m.libres() == 20 && m.libresSwap() == 10, "libres() solo cuenta el area de archivos");
        int s = m.asignarSwap(4, "swap:P7");
        verificar(s == 20, "el proceso suspendido ocupa el swap desde el bloque 20");
        verificar("swap:P7".equals(m.getOcupante(23)) && m.libres() == 20, "asignarSwap no toca el area de archivos");
        m.asignar(20, "/lleno");
        verificar(m.asignar(1, "/otro") == -1, "con el area de archivos llena no se usa el swap para archivos");
        verificar(m.liberar(s) == 0, "liberar no borra bloques de swap");
        verificar(lanza(() -> m.configurarSwap(5)), "no se cambia el swap mientras tiene procesos");
        verificar(m.liberarSwap(s) == 4 && m.libresSwap() == 10, "liberarSwap libera los 4 bloques del proceso");
        m.asignarSwap(3, "swap:P8");
        m.asignarSwap(2, "swap:P9");
        m.liberarTodoSwap();
        verificar(m.libresSwap() == 10, "liberarTodoSwap deja el swap vacio");
        m.configurarSwap(5);
        verificar(m.getTotalBloques() == 25 && m.libres() == 0 && "/lleno".equals(m.getOcupante(0)),
                "con el swap vacio se puede reconfigurar y los archivos se conservan");
    }

    private static void probarAsignarEn() {
        System.out.println("--- asignarEn (carga del JSON) ---");
        MapaBloques m = new MapaBloques(20);
        verificar(m.asignarEn(5, 3, "/j") == 5, "asignarEn coloca el archivo desde el bloque 5");
        verificar(iguales(m.cadena(5), new int[]{5, 6, 7}), "los bloques quedan contiguos: 5 -> 6 -> 7");
        verificar(m.asignarEn(6, 2, "/k") == -1, "rechaza bloques ya ocupados");
        verificar(m.libres() == 17 && m.estaLibre(8), "el rechazo no deja nada a medias");
        verificar(m.asignarEn(18, 5, "/l") == -1, "rechaza un archivo que se sale del area de archivos");
        verificar(m.asignarEn(-1, 2, "/m") == -1, "rechaza un inicio negativo");
    }

    private static void probarCambiarOcupante() {
        System.out.println("--- Cambiar ocupante (renombrar) ---");
        MapaBloques m = new MapaBloques(20);
        m.asignar(2, "/x");                     // 0, 1
        m.asignar(1, "/y");                     // 2
        m.liberar(0);                           // 0 y 1 quedan libres
        int p = m.asignar(3, "/a/doc.txt");     // 0, 1, 3
        verificar(iguales(m.cadena(p), new int[]{0, 1, 3}), "el archivo quedo repartido: 0 -> 1 -> 3");
        m.cambiarOcupante(p, "/b/doc.txt");
        verificar("/b/doc.txt".equals(m.getOcupante(0)) && "/b/doc.txt".equals(m.getOcupante(3)),
                "renombrar cambia el ocupante de toda la cadena");
        verificar("/y".equals(m.getOcupante(2)), "los bloques de otro archivo no cambian");
    }

    private static void probarValidaciones() {
        System.out.println("--- Validaciones ---");
        verificar(lanza(() -> new MapaBloques(0)), "un disco de 0 bloques es rechazado");
        verificar(lanza(() -> new MapaBloques(501)), "un disco de mas de 500 bloques es rechazado");
        MapaBloques m = new MapaBloques(10);
        verificar(lanza(() -> m.configurarSwap(-1)), "un swap negativo es rechazado");
        verificar(lanza(() -> m.configurarSwap(501)), "un swap de mas de 500 bloques es rechazado");
        verificar(lanza(() -> m.asignar(0, "/a")), "asignar 0 bloques es rechazado");
        verificar(lanza(() -> m.asignar(1, "  ")), "un ocupante vacio es rechazado");
        verificar(lanza(() -> m.estaLibre(10)), "un bloque fuera del disco es rechazado");
    }

    // ---- auxiliares ----

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

    private static boolean lanza(Runnable accion) {
        try {
            accion.run();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return true;
        }
        return false;
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
