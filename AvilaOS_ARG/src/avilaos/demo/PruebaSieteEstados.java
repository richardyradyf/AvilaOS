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
import avilaos.model.MotivoBloqueo;
import avilaos.model.Pcb;
import avilaos.model.TipoProceso;
import avilaos.scheduler.PlanificadorFcfs;
import avilaos.util.Bitacora;

/**
 * @author richard
 *
 * Pruebas del modelo de siete estados en el nucleo: colas de suspendidos,
 * suspension y reactivacion con la RAM, E/S y deadline de procesos en el swap.
 */
public class PruebaSieteEstados {

    private static int total = 0;
    private static int fallos = 0;

    private static class Sistema {
        final Bitacora bitacora = new Bitacora(500);
        final Ram ram;
        final Cpu cpu = new Cpu(1);
        final Nucleo nucleo;
        final RelojGlobal reloj = new RelojGlobal(1000);

        Sistema(int memoria) {
            GeneradorId.reiniciar();
            ram = new Ram(memoria);
            nucleo = new Nucleo(1, ram, cpu, new PlanificadorFcfs(), bitacora);
            reloj.registrar(cpu);
            reloj.registrar(nucleo);
        }

        void avanzar(int ciclos) {
            for (int i = 0; i < ciclos; i++) {
                reloj.avanzarCiclo();
            }
        }

        /** Avanza hasta que el proceso llegue al estado, como maximo "limite" ciclos. */
        boolean avanzarHasta(Pcb p, EstadoProceso estado, int limite) {
            for (int i = 0; i < limite && p.getEstado() != estado; i++) {
                reloj.avanzarCiclo();
            }
            return p.getEstado() == estado;
        }
    }

    public static void main(String[] args) {
        probarSuspenderYReactivarListo();
        probarBloqueadoSuspendidoConEs();
        probarDesbloquearSuspendido();
        probarDeadlineEnSwap();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static void probarSuspenderYReactivarListo() {
        System.out.println("--- Listo <-> Listo/Suspendido ---");
        Sistema s = new Sistema(100);
        Pcb a = cpu("A", 60, 10, 1000);
        Pcb b = cpu("B", 30, 10, 1000);
        s.nucleo.agregarProceso(a);
        s.nucleo.agregarProceso(b);
        verificar(s.ram.getUsada() == 90, "A y B admitidos: 90 MB usados");

        s.nucleo.suspender(b);
        verificar(b.getEstado() == EstadoProceso.LISTO_SUSPENDIDO, "B pasa a Listo/Suspendido");
        verificar(s.nucleo.getListosSuspendidos().length == 1 && s.nucleo.getListos().length == 1,
                "B sale de listos y entra a listos/suspendidos");
        verificar(s.ram.getUsada() == 60, "suspender libera los 30 MB de B");

        Pcb c = cpu("C", 30, 10, 1000);
        s.nucleo.agregarProceso(c);
        verificar(s.ram.getUsada() == 90, "C ocupa el espacio que dejo B");
        verificar(!s.nucleo.reactivar(b), "B no se reactiva si no cabe en la RAM");
        verificar(b.getEstado() == EstadoProceso.LISTO_SUSPENDIDO, "B sigue suspendido");

        s.nucleo.suspender(c);
        verificar(s.nucleo.reactivar(b), "con espacio, B se reactiva");
        verificar(b.getEstado() == EstadoProceso.LISTO && s.ram.getUsada() == 90,
                "B vuelve a listos y reserva su memoria");
        verificar(s.nucleo.getListosSuspendidos().length == 1, "ahora el suspendido es C");

        verificar(lanza(() -> s.nucleo.reactivar(a)), "no se reactiva un proceso que no esta suspendido");
        verificar(lanza(() -> s.nucleo.suspender(c)), "no se suspende un proceso que ya esta suspendido");
        verificar(s.bitacora.contiene("se suspende") && s.bitacora.contiene("se reactiva"),
                "la bitacora registra la suspension y la reactivacion");
    }

    private static void probarBloqueadoSuspendidoConEs() {
        System.out.println("--- Bloqueado/Suspendido con E/S ---");
        Sistema s = new Sistema(100);
        Pcb io = new Pcb("Lector", 1, TipoProceso.IO_BOUND, 1, 40, 6, 1000, 0);
        io.configurarEs(2, 5);
        s.nucleo.agregarProceso(io);
        verificar(s.avanzarHasta(io, EstadoProceso.BLOQUEADO, 20), "el lector pide E/S y se bloquea");

        s.nucleo.suspender(io);
        verificar(io.getEstado() == EstadoProceso.BLOQUEADO_SUSPENDIDO, "pasa a Bloqueado/Suspendido");
        verificar(s.nucleo.getBloqueadosSuspendidos().length == 1 && s.nucleo.getBloqueados().length == 0,
                "sale de bloqueados y entra a bloqueados/suspendidos");
        verificar(s.ram.getUsada() == 0, "su memoria queda libre");

        verificar(s.avanzarHasta(io, EstadoProceso.LISTO_SUSPENDIDO, 20),
                "la E/S termina en el swap: pasa a Listo/Suspendido");
        verificar(s.nucleo.getListosSuspendidos().length == 1 && s.ram.getUsada() == 0,
                "sigue en el swap, sin ocupar RAM");

        verificar(s.nucleo.reactivar(io), "se reactiva");
        verificar(io.getEstado() == EstadoProceso.LISTO && s.ram.getUsada() == 40, "vuelve a listos con sus 40 MB");
        verificar(s.avanzarHasta(io, EstadoProceso.TERMINADO, 100) && !io.isTerminadoPorDeadline(),
                "termina normalmente");
        verificar(s.ram.getUsada() == 0, "al terminar libera la memoria");
    }

    private static void probarDesbloquearSuspendido() {
        System.out.println("--- Evento de un proceso suspendido ---");
        Sistema s = new Sistema(100);
        Pcb p = cpu("Espera", 20, 10, 1000);
        s.nucleo.agregarProceso(p);
        s.avanzar(2);
        s.nucleo.bloquearProcesoEnCpu(MotivoBloqueo.REGION_CRITICA_OCUPADA);
        s.nucleo.suspender(p);
        s.nucleo.desbloquear(p);
        verificar(p.getEstado() == EstadoProceso.LISTO_SUSPENDIDO,
                "desbloquear a un bloqueado/suspendido lo deja en listos/suspendidos");
        verificar(s.nucleo.getListosSuspendidos().length == 1 && s.nucleo.getBloqueadosSuspendidos().length == 0,
                "cambia de cola");
        verificar(lanza(() -> s.nucleo.desbloquear(p)), "un listo/suspendido no se desbloquea");
    }

    private static void probarDeadlineEnSwap() {
        System.out.println("--- Deadline vencido en el swap ---");
        Sistema s = new Sistema(100);
        Pcb largo = cpu("Largo", 30, 50, 1000);
        Pcb corto = cpu("Corto", 20, 10, 5);
        s.nucleo.agregarProceso(largo);
        s.nucleo.agregarProceso(corto);
        s.nucleo.suspender(corto);
        s.avanzar(10);
        verificar(corto.getEstado() == EstadoProceso.TERMINADO && corto.isTerminadoPorDeadline(),
                "el suspendido termina por deadline");
        verificar(s.nucleo.getListosSuspendidos().length == 0 && s.nucleo.getTerminados().length == 1,
                "sale de listos/suspendidos y entra a terminados");
        verificar(s.ram.getUsada() == 30, "no libera memoria que no tenia (solo queda la del largo)");
    }

    private static Pcb cpu(String nombre, int memoria, int instrucciones, int deadline) {
        return new Pcb(nombre, 1, TipoProceso.CPU_BOUND, 1, memoria, instrucciones, deadline, 0);
    }

    private static boolean lanza(Runnable accion) {
        try {
            accion.run();
            return false;
        } catch (IllegalArgumentException | IllegalStateException e) {
            return true;
        }
    }

    private static void verificar(boolean condicion, String descripcion) {
        total++;
        if (!condicion) {
            fallos++;
        }
        System.out.println((condicion ? "OK    - " : "FALLO - ") + descripcion);
    }
}
