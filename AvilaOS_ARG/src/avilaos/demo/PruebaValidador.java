package avilaos.demo;

import avilaos.config.EntradaInvalidaException;
import avilaos.config.ValidadorEntradas;
import avilaos.model.SolicitudProceso;
import avilaos.model.TipoProceso;

/** Pruebas de las validaciones de entrada (sin ventanas). */
public class PruebaValidador {

    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        probarEnteros();
        probarNombres();
        probarProcesosValidos();
        probarProcesosInvalidos();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static void probarEnteros() {
        System.out.println("--- Enteros ---");
        verificar(ValidadorEntradas.entero("42", "X", 1, 100) == 42, "42 es válido");
        verificar(ValidadorEntradas.entero("  12 ", "X", 1, 100) == 12, "los espacios alrededor se ignoran");
        verificar(ValidadorEntradas.entero("1", "X", 1, 100) == 1 && ValidadorEntradas.entero("100", "X", 1, 100) == 100,
                "los extremos del rango son válidos");
        verificar(invalida(() -> ValidadorEntradas.entero("", "X", 1, 100)), "vacío es inválido");
        verificar(invalida(() -> ValidadorEntradas.entero("   ", "X", 1, 100)), "solo espacios es inválido");
        verificar(invalida(() -> ValidadorEntradas.entero(null, "X", 1, 100)), "null es inválido");
        verificar(invalida(() -> ValidadorEntradas.entero("abc", "X", 1, 100)), "texto es inválido");
        verificar(invalida(() -> ValidadorEntradas.entero("1.5", "X", 1, 100)), "un decimal es inválido");
        verificar(invalida(() -> ValidadorEntradas.entero("12abc", "X", 1, 100)), "número con letras es inválido");
        verificar(invalida(() -> ValidadorEntradas.entero("0", "X", 1, 100)), "0 está por debajo del rango");
        verificar(invalida(() -> ValidadorEntradas.entero("101", "X", 1, 100)), "101 está por encima del rango");
        verificar(invalida(() -> ValidadorEntradas.entero("-5", "X", 1, 100)), "un negativo es inválido");
        verificar(invalida(() -> ValidadorEntradas.entero("99999999999999999999", "X", 1, 100)),
                "un número gigante se rechaza sin romper el programa");
    }

    private static void probarNombres() {
        System.out.println("--- Nombres ---");
        verificar("Navegador web".equals(ValidadorEntradas.nombre("  Navegador web ")), "se recortan los espacios");
        verificar("Proceso_1-a.b".equals(ValidadorEntradas.nombre("Proceso_1-a.b")), "guiones, punto y número son válidos");
        verificar(invalida(() -> ValidadorEntradas.nombre("")), "nombre vacío es inválido");
        verificar(invalida(() -> ValidadorEntradas.nombre("a@b")), "una arroba no está permitida");
        verificar(invalida(() -> ValidadorEntradas.nombre("x".repeat(31))), "31 caracteres es demasiado largo");
        verificar(ValidadorEntradas.nombre("x".repeat(30)).length() == 30, "30 caracteres es válido");
    }

    private static void probarProcesosValidos() {
        System.out.println("--- Procesos válidos ---");
        SolicitudProceso cpu = ValidadorEntradas.validarProceso("Navegador", TipoProceso.CPU_BOUND,
                "100", "64", "2", "500", 0, "", "", "", 0);
        verificar(cpu.getInstrucciones() == 100 && cpu.getMemoria() == 64 && cpu.getPrioridad() == 2
                && cpu.getDeadline() == 500 && cpu.esAutomatico(), "CPU bound: datos y computador automático");

        SolicitudProceso io = ValidadorEntradas.validarProceso("Disco", TipoProceso.IO_BOUND,
                "50", "32", "1", "300", 2, "10", "5", "", 0);
        verificar(io.getCiclosEs() == 10 && io.getCadaCiclos() == 5 && io.getIdComputador() == 2,
                "I/O bound: ciclos de E/S y computador 2");

        SolicitudProceso prod = ValidadorEntradas.validarProceso("Productor", TipoProceso.PRODUCTOR,
                "", "16", "3", "1000", 1, "", "2", "20", 1);
        verificar(prod.getInstrucciones() == 0 && prod.getElementos() == 20 && prod.getIdBuffer() == 1,
                "productor: sin instrucciones (las calcula el sistema), con buffer y elementos");

        SolicitudProceso cons = ValidadorEntradas.validarProceso("Consumidor", TipoProceso.CONSUMIDOR,
                "texto basura", "16", "3", "1000", 2, "", "3", "20", 1);
        verificar(cons.getCadaCiclos() == 3, "los campos que no aplican al tipo se ignoran");
    }

    private static void probarProcesosInvalidos() {
        System.out.println("--- Procesos inválidos ---");
        verificar(invalida(() -> ValidadorEntradas.validarProceso("", TipoProceso.CPU_BOUND,
                "100", "64", "2", "500", 0, "", "", "", 0)), "sin nombre");
        verificar(invalida(() -> ValidadorEntradas.validarProceso("X", null,
                "100", "64", "2", "500", 0, "", "", "", 0)), "sin tipo");
        verificar(invalida(() -> ValidadorEntradas.validarProceso("X", TipoProceso.CPU_BOUND,
                "abc", "64", "2", "500", 0, "", "", "", 0)), "instrucciones con texto");
        verificar(invalida(() -> ValidadorEntradas.validarProceso("X", TipoProceso.CPU_BOUND,
                "100", "65537", "2", "500", 0, "", "", "", 0)), "memoria sobre el máximo");
        verificar(invalida(() -> ValidadorEntradas.validarProceso("X", TipoProceso.CPU_BOUND,
                "100", "64", "11", "500", 0, "", "", "", 0)), "prioridad 11");
        verificar(invalida(() -> ValidadorEntradas.validarProceso("X", TipoProceso.CPU_BOUND,
                "100", "64", "2", "0", 0, "", "", "", 0)), "deadline 0");
        verificar(invalida(() -> ValidadorEntradas.validarProceso("X", TipoProceso.IO_BOUND,
                "100", "64", "2", "500", 0, "", "5", "", 0)), "I/O bound sin ciclos de E/S");
        verificar(invalida(() -> ValidadorEntradas.validarProceso("X", TipoProceso.PRODUCTOR,
                "", "16", "3", "1000", 1, "", "2", "20", 0)), "productor sin buffer");
        verificar(invalida(() -> ValidadorEntradas.validarProceso("X", TipoProceso.CONSUMIDOR,
                "", "16", "3", "1000", 1, "", "2", "", 1)), "consumidor sin elementos");
        verificar(invalida(() -> ValidadorEntradas.validarProceso("X", TipoProceso.CPU_BOUND,
                "100", "64", "2", "500", -1, "", "", "", 0)), "computador inválido");
    }

    private static boolean invalida(Runnable accion) {
        try {
            accion.run();
        } catch (EntradaInvalidaException e) {
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
