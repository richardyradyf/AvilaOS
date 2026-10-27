/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.demo;

import avilaos.hardware.Cpu;
import avilaos.hardware.Ram;
import avilaos.hardware.RelojGlobal;
import avilaos.kernel.Nucleo;
import avilaos.model.EstadoProceso;
import avilaos.model.GeneradorId;
import avilaos.model.ModoEjecucion;
import avilaos.model.MotivoBloqueo;
import avilaos.model.Pcb;
import avilaos.model.PoliticaPlanificacion;
import avilaos.model.TipoProceso;
import avilaos.scheduler.Planificador;
import avilaos.scheduler.PlanificadorFcfs;
import avilaos.util.Bitacora;
import avilaos.util.Cola;

/**
 * @author richard
 * 
 * Pruebas del nucleo con el reloj en modo manual (un ciclo por llamada).
 * Ejecutar con Shift+F6; el log del sistema se imprime mientras corre.
 */
public class PruebaNucleo {

    private static int total = 0;
    private static int fallos = 0;

    private static class Sistema {
        final Bitacora bitacora = new Bitacora(500);
        final Ram ram;
        final Cpu cpu = new Cpu(1);
        final Nucleo nucleo;
        final RelojGlobal reloj = new RelojGlobal(1000);

        Sistema(int memoria, Planificador planificador) {
            ram = new Ram(memoria);
            nucleo = new Nucleo(1, ram, cpu, planificador, bitacora);
            reloj.registrar(cpu);
            reloj.registrar(nucleo);
            bitacora.setEco(true);
        }

        void avanzar(int ciclos) {
            for (int i = 0; i < ciclos; i++) {
                reloj.avanzarCiclo();
            }
        }
    }

    private static class PlanificadorQuantum implements Planificador {
        private final int quantum;

        PlanificadorQuantum(int quantum) {
            this.quantum = quantum;
        }

        @Override
        public PoliticaPlanificacion getTipo() {
            return PoliticaPlanificacion.ROUND_ROBIN;
        }

        @Override
        public void alEntrarAListos(Cola<Pcb> listos, Pcb proceso) {
            listos.encolar(proceso);
        }

        @Override
        public Pcb seleccionar(Cola<Pcb> listos) {
            return listos.desencolar();
        }

        @Override
        public boolean debeExpulsar(Pcb enCpu, int ciclosEnCpu, Cola<Pcb> listos) {
            return ciclosEnCpu >= quantum && !listos.estaVacia();
        }

        @Override
        public void reordenar(Cola<Pcb> listos) {
        }
    }

    public static void main(String[] args) {
        probarFcfsConMemoriaLimitada();
        probarBloqueoYDesbloqueo();
        probarExpulsionConPlanificadorIntercambiable();
        probarValidaciones();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static void probarFcfsConMemoriaLimitada() {
        System.out.println("--- FCFS y admision por memoria ---");
        GeneradorId.reiniciar();
        Sistema s = new Sistema(100, new PlanificadorFcfs());
        Pcb p1 = pcb("Uno", 10, 40);
        Pcb p2 = pcb("Dos", 5, 40);
        Pcb p3 = pcb("Tres", 3, 40);
        s.nucleo.agregarProceso(p1);
        s.nucleo.agregarProceso(p2);
        s.nucleo.agregarProceso(p3);

        verificar(s.nucleo.getListos().length == 2, "P1 y P2 caben en la RAM y pasan a listos");
        verificar(s.nucleo.getNuevos().length == 1 && s.nucleo.getNuevos()[0] == p3,
                "P3 no cabe (quedan 20 MB) y espera en nuevos");
        verificar(s.ram.getUsada() == 80, "la RAM tiene 80 MB usados");

        s.avanzar(1);
        verificar(s.cpu.getProceso() == p1 && p1.getEstado() == EstadoProceso.EJECUCION,
                "ciclo 1: el nucleo despacha a P1 (FCFS)");
        verificar(s.nucleo.getListos().length == 1, "P2 sigue esperando en listos");

        s.avanzar(10);
        verificar(p1.getEstado() == EstadoProceso.TERMINADO && p1.getCicloFinalizacion() == 11,
                "P1 termina en el ciclo 11, el de su ultima instruccion");
        verificar(p1.getCicloPrimeraEjecucion() == 2, "P1 empezo a ejecutar en el ciclo 2");
        verificar(p3.getEstado() == EstadoProceso.LISTO, "al liberar memoria, P3 se admite");
        verificar(s.cpu.getProceso() == p2, "el nucleo despacha a P2 sin dejar la CPU ociosa");

        s.avanzar(8);
        verificar(p2.getCicloFinalizacion() == 16, "P2 termina en el ciclo 16");
        verificar(p3.getCicloFinalizacion() == 19, "P3 termina en el ciclo 19");
        verificar(s.nucleo.getTerminados().length == 3, "los 3 procesos estan en terminados");
        verificar(s.ram.getUsada() == 0, "toda la memoria quedo liberada");
        verificar(s.cpu.estaOciosa(), "la CPU queda ociosa");
        verificar(s.cpu.getCiclosTotales() == 19 && s.cpu.getCiclosOcupada() == 18,
                "19 ciclos en total, 18 con la CPU ocupada (el primero fue el despacho)");
        verificar(s.bitacora.contiene("selecciona el proceso P1"), "el log registra la seleccion de P1");
    }

    private static void probarBloqueoYDesbloqueo() {
        System.out.println("--- Bloqueo y desbloqueo ---");
        GeneradorId.reiniciar();
        Sistema s = new Sistema(100, new PlanificadorFcfs());
        Pcb p4 = pcb("Cuatro", 6, 10);
        Pcb p5 = pcb("Cinco", 2, 10);
        s.nucleo.agregarProceso(p4);
        s.nucleo.agregarProceso(p5);

        s.avanzar(3);
        verificar(p4.getPc() == 2, "P4 ejecuto 2 instrucciones");

        s.nucleo.bloquearProcesoEnCpu(MotivoBloqueo.BUFFER_VACIO);
        verificar(p4.getEstado() == EstadoProceso.BLOQUEADO
                && p4.getMotivoBloqueo() == MotivoBloqueo.BUFFER_VACIO, "P4 queda bloqueado con su motivo");
        verificar(s.nucleo.getBloqueados().length == 1, "P4 esta en la cola de bloqueados");
        verificar(s.cpu.estaOciosa() && s.cpu.getModo() == ModoEjecucion.SISTEMA_OPERATIVO,
                "la CPU quedo libre y en modo sistema operativo");

        s.avanzar(1);
        verificar(s.cpu.getProceso() == p5, "el nucleo despacha a P5 mientras P4 esta bloqueado");

        s.avanzar(2);
        verificar(p5.getEstado() == EstadoProceso.TERMINADO && p5.getCicloFinalizacion() == 6,
                "P5 termina en el ciclo 6");
        verificar(s.cpu.estaOciosa(), "con P4 bloqueado y nadie en listos, la CPU queda ociosa");

        s.nucleo.desbloquear(p4);
        verificar(p4.getEstado() == EstadoProceso.LISTO && p4.getMotivoBloqueo() == MotivoBloqueo.NINGUNO,
                "P4 vuelve a LISTO y pierde el motivo de bloqueo");
        verificar(s.nucleo.getBloqueados().length == 0 && s.nucleo.getListos().length == 1,
                "P4 paso de bloqueados a listos");

        s.avanzar(5);
        verificar(p4.getEstado() == EstadoProceso.TERMINADO && p4.getCicloFinalizacion() == 11 && p4.getPc() == 6,
                "P4 retoma donde iba (PC=2) y termina en el ciclo 11 con PC=6");
        verificar(s.ram.getUsada() == 0, "memoria liberada");
    }

    private static void probarExpulsionConPlanificadorIntercambiable() {
        System.out.println("--- Expulsion por quantum y cambio de politica ---");
        GeneradorId.reiniciar();
        Sistema s = new Sistema(100, new PlanificadorQuantum(2));
        Pcb a = pcb("A", 4, 10);
        Pcb b = pcb("B", 4, 10);
        s.nucleo.agregarProceso(a);
        s.nucleo.agregarProceso(b);

        s.avanzar(1);
        verificar(s.cpu.getProceso() == a, "ciclo 1: despacha a A");
        s.avanzar(2);
        verificar(s.cpu.getProceso() == b && a.getEstado() == EstadoProceso.LISTO && a.getPc() == 2,
                "tras 2 ciclos A es expulsado (PC=2) y entra B");
        s.avanzar(2);
        verificar(s.cpu.getProceso() == a && b.getPc() == 2, "tras 2 ciclos B es expulsado y vuelve A");
        s.avanzar(2);
        verificar(a.getEstado() == EstadoProceso.TERMINADO && a.getCicloFinalizacion() == 7,
                "A termina en el ciclo 7 (terminar tiene prioridad sobre expulsar)");
        verificar(s.cpu.getProceso() == b, "B recibe la CPU");
        s.avanzar(2);
        verificar(b.getEstado() == EstadoProceso.TERMINADO && b.getCicloFinalizacion() == 9,
                "B termina en el ciclo 9");

        verificar(s.nucleo.getPlanificador().getTipo() == PoliticaPlanificacion.ROUND_ROBIN,
                "el nucleo usa la politica con quantum");
        s.nucleo.cambiarPlanificador(new PlanificadorFcfs());
        verificar(s.nucleo.getPlanificador().getTipo() == PoliticaPlanificacion.FCFS,
                "la politica se cambia en tiempo de ejecucion");
        verificar(s.bitacora.contiene("cambia su politica de planificacion a FCFS"),
                "el cambio de politica queda en el log");
    }

    private static void probarValidaciones() {
        System.out.println("--- Validaciones ---");
        GeneradorId.reiniciar();
        Sistema s = new Sistema(100, new PlanificadorFcfs());
        s.bitacora.setEco(false);

        verificar(lanza(IllegalArgumentException.class, () -> s.nucleo.agregarProceso(null)),
                "agregar null lanza excepcion");
        verificar(lanza(IllegalArgumentException.class, () -> s.nucleo.agregarProceso(
                new Pcb("Otro", 2, TipoProceso.CPU_BOUND, 1, 10, 5, 100, 0))),
                "un proceso de otro computador es rechazado");
        verificar(lanza(IllegalArgumentException.class, () -> s.nucleo.agregarProceso(pcb("Enorme", 5, 500))),
                "un proceso mas grande que la RAM total es rechazado");
        Pcb admitido = pcb("Admitido", 5, 10);
        admitido.admitir();
        verificar(lanza(IllegalArgumentException.class, () -> s.nucleo.agregarProceso(admitido)),
                "solo se agregan procesos en estado NUEVO");
        verificar(lanza(IllegalStateException.class, () -> s.nucleo.bloquearProcesoEnCpu(MotivoBloqueo.ESPERA_ES)),
                "bloquear con la CPU ociosa lanza excepcion");
        verificar(lanza(IllegalArgumentException.class, () -> s.nucleo.desbloquear(pcb("Suelto", 5, 10))),
                "desbloquear un proceso que no esta bloqueado lanza excepcion");
    }

    // ---- ayudas ----

    private static Pcb pcb(String nombre, int instrucciones, int memoria) {
        return new Pcb(nombre, 1, TipoProceso.CPU_BOUND, 1, memoria, instrucciones, 1000, 0);
    }

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