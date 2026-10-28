/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.demo;

import avilaos.model.EstadoProceso;
import avilaos.model.GeneradorId;
import avilaos.model.MotivoBloqueo;
import avilaos.model.Pcb;
import avilaos.model.TipoProceso;

/** Pruebas del PCB: creacion, ciclo de vida, transiciones invalidas y deadline. */
public class PruebaPcb {

    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        GeneradorId.reiniciar();
        probarCreacion();
        probarCicloDeVida();
        probarTransicionesInvalidas();
        probarDeadline();
        probarValidaciones();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static Pcb nuevo(String nombre, int instrucciones) {
        return new Pcb(nombre, 1, TipoProceso.CPU_BOUND, 2, 64, instrucciones, 100, 0);
    }

    private static void probarCreacion() {
        System.out.println("--- Creacion ---");
        Pcb p = nuevo("Navegador", 10);
        verificar(p.getId() == 1, "el primer proceso recibe el ID 1");
        verificar(p.getEtiqueta().equals("P1"), "la etiqueta es P1");
        verificar(p.getEstado() == EstadoProceso.NUEVO, "un proceso recien creado esta en NUEVO");
        verificar(p.getMotivoBloqueo() == MotivoBloqueo.NINGUNO, "sin motivo de bloqueo al crearse");
        verificar(p.getInstruccionesRestantes() == 10, "restantes = totales al crearse");
        verificar(p.getPc() == 0 && p.getMar() == 0, "PC y MAR empiezan en 0");
        verificar(p.getCicloLimite() == 100, "cicloLimite = cicloCreacion (0) + deadline (100)");
        verificar(p.getCicloPrimeraEjecucion() == -1, "aun no tiene primera ejecucion");

        Pcb q = nuevo("Editor", 5);
        verificar(q.getId() == 2, "el segundo proceso recibe el ID 2");
    }

    private static void probarCicloDeVida() {
        System.out.println("--- Ciclo de vida ---");
        Pcb p = nuevo("Ciclo", 5);

        p.admitir();
        verificar(p.getEstado() == EstadoProceso.LISTO, "admitir: NUEVO -> LISTO");

        p.despachar(7);
        verificar(p.getEstado() == EstadoProceso.EJECUCION, "despachar: LISTO -> EJECUCION");
        verificar(p.getCicloPrimeraEjecucion() == 7, "se registra el ciclo de la primera ejecucion");

        p.ejecutarInstruccion();
        p.ejecutarInstruccion();
        p.ejecutarInstruccion();
        verificar(p.getPc() == 3 && p.getMar() == 3, "tras 3 instrucciones, PC y MAR valen 3");
        verificar(p.getInstruccionesRestantes() == 2, "quedan 2 instrucciones");

        p.bloquear(MotivoBloqueo.BUFFER_LLENO);
        verificar(p.getEstado() == EstadoProceso.BLOQUEADO, "bloquear: EJECUCION -> BLOQUEADO");
        verificar(p.getMotivoBloqueo() == MotivoBloqueo.BUFFER_LLENO, "se guarda el motivo del bloqueo");

        p.desbloquear();
        verificar(p.getEstado() == EstadoProceso.LISTO, "desbloquear: BLOQUEADO -> LISTO");
        verificar(p.getMotivoBloqueo() == MotivoBloqueo.NINGUNO, "el motivo se limpia al desbloquear");

        p.despachar(12);
        verificar(p.getCicloPrimeraEjecucion() == 7, "la primera ejecucion no se sobrescribe");
        p.expulsar();
        verificar(p.getEstado() == EstadoProceso.LISTO, "expulsar: EJECUCION -> LISTO");

        p.despachar(15);
        p.ejecutarInstruccion();
        p.ejecutarInstruccion();
        verificar(p.completoSusInstrucciones(), "ejecuto todas sus instrucciones");

        p.terminar(17, false);
        verificar(p.getEstado() == EstadoProceso.TERMINADO, "terminar: -> TERMINADO");
        verificar(p.getCicloFinalizacion() == 17, "se registra el ciclo de finalizacion");
        verificar(!p.isTerminadoPorDeadline(), "termino de forma normal, no por deadline");
    }

    private static void probarTransicionesInvalidas() {
        System.out.println("--- Transiciones invalidas ---");
        Pcb p = nuevo("Invalido", 3);
        verificar(lanza(IllegalStateException.class, () -> p.despachar(1)),
                "NUEVO -> EJECUCION no esta permitido");
        verificar(lanza(IllegalStateException.class, () -> p.ejecutarInstruccion()),
                "no se ejecuta una instruccion fuera de EJECUCION");

        p.admitir();
        verificar(lanza(IllegalStateException.class, () -> p.admitir()),
                "admitir dos veces no esta permitido");
        verificar(lanza(IllegalStateException.class, () -> p.desbloquear()),
                "desbloquear un proceso LISTO no esta permitido");

        p.despachar(2);
        verificar(lanza(IllegalArgumentException.class, () -> p.bloquear(MotivoBloqueo.NINGUNO)),
                "bloquear exige un motivo real");

        p.terminar(5, true);
        verificar(p.isTerminadoPorDeadline(), "terminado por deadline queda registrado");
        verificar(lanza(IllegalStateException.class, () -> p.terminar(6, false)),
                "un proceso terminado no puede terminar otra vez");
        verificar(lanza(IllegalStateException.class, () -> p.admitir()),
                "TERMINADO no tiene transiciones de salida");
    }

    private static void probarDeadline() {
        System.out.println("--- Deadline ---");
        Pcb p = new Pcb("Deadline", 1, TipoProceso.IO_BOUND, 1, 32, 10, 20, 10);
        verificar(p.getCicloLimite() == 30, "cicloLimite = 10 + 20 = 30");
        verificar(p.deadlineRestante(25) == 5, "en el ciclo 25 quedan 5 ciclos");
        verificar(!p.deadlineVencido(29), "en el ciclo 29 aun no esta vencido");
        verificar(p.deadlineVencido(30), "en el ciclo 30 el deadline llega a 0");
        verificar(p.deadlineVencido(31), "despues del ciclo limite sigue vencido");
    }

    private static void probarValidaciones() {
        System.out.println("--- Validaciones del constructor ---");
        verificar(lanza(IllegalArgumentException.class,
                () -> new Pcb("  ", 1, TipoProceso.CPU_BOUND, 1, 32, 10, 20, 0)), "nombre vacio");
        verificar(lanza(IllegalArgumentException.class,
                () -> new Pcb("X", 0, TipoProceso.CPU_BOUND, 1, 32, 10, 20, 0)), "computador 0");
        verificar(lanza(IllegalArgumentException.class,
                () -> new Pcb("X", 1, null, 1, 32, 10, 20, 0)), "tipo nulo");
        verificar(lanza(IllegalArgumentException.class,
                () -> new Pcb("X", 1, TipoProceso.CPU_BOUND, 0, 32, 10, 20, 0)), "prioridad 0");
        verificar(lanza(IllegalArgumentException.class,
                () -> new Pcb("X", 1, TipoProceso.CPU_BOUND, 1, 0, 10, 20, 0)), "memoria 0");
        verificar(lanza(IllegalArgumentException.class,
                () -> new Pcb("X", 1, TipoProceso.CPU_BOUND, 1, 32, 0, 20, 0)), "instrucciones 0");
        verificar(lanza(IllegalArgumentException.class,
                () -> new Pcb("X", 1, TipoProceso.CPU_BOUND, 1, 32, 10, 0, 0)), "deadline 0");

        Pcb a = nuevo("A", 1);
        int idA = a.getId();
        lanza(IllegalArgumentException.class,
                () -> new Pcb("", 1, TipoProceso.CPU_BOUND, 1, 32, 10, 20, 0));
        Pcb b = nuevo("B", 1);
        verificar(b.getId() == idA + 1, "un constructor que falla no consume IDs");
    }

    // ---- ayudas ----

    private static boolean lanza(Class<? extends RuntimeException> tipo, Runnable accion) {
        try {
            accion.run();
        } catch (RuntimeException e) {
            return tipo.isInstance(e);
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
