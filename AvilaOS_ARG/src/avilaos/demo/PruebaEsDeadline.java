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
 *
 * @author richard
 * 
 * Pruebas de la E/S de los procesos I/O bound y de la terminacion por deadline. Reloj manual.
 */

public class PruebaEsDeadline {
 
    private static int total = 0;
    private static int fallos = 0;
 
    private static class Sistema {
        final Bitacora bitacora = new Bitacora(500);
        final Ram ram;
        final Cpu cpu = new Cpu(1);
        final Nucleo nucleo;
        final RelojGlobal reloj = new RelojGlobal(1000);
 
        Sistema(int memoria) {
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
 
        Pcb cpuBound(String nombre, int memoria, int instrucciones, int deadline) {
            Pcb p = new Pcb(nombre, 1, TipoProceso.CPU_BOUND, 1, memoria, instrucciones, deadline,
                    reloj.getCicloActual());
            nucleo.agregarProceso(p);
            return p;
        }
 
        Pcb ioBound(String nombre, int memoria, int instrucciones, int deadline, int cada, int ciclosEs) {
            Pcb p = new Pcb(nombre, 1, TipoProceso.IO_BOUND, 1, memoria, instrucciones, deadline,
                    reloj.getCicloActual());
            p.configurarEs(cada, ciclosEs);
            nucleo.agregarProceso(p);
            return p;
        }
    }
 
    public static void main(String[] args) {
        probarEs();
        probarEsConOtroProceso();
        probarDeadline();
        probarDeadlineEnTodasLasColas();
        probarValidaciones();
 
        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }
 
    private static void probarEs() {
        System.out.println("--- E/S de un proceso I/O bound (6 instrucciones, E/S cada 2, dura 3 ciclos) ---");
        GeneradorId.reiniciar();
        Sistema s = new Sistema(100);
        Pcb p = s.ioBound("Io", 10, 6, 1000, 2, 3);
        s.avanzar(3);   // ciclo 1 despacha; ciclos 2 y 3 ejecutan 2 instrucciones
        verificar(p.getEstado() == EstadoProceso.BLOQUEADO && p.getMotivoBloqueo() == MotivoBloqueo.ESPERA_ES,
                "tras 2 instrucciones pide E/S y queda bloqueado por E/S");
        verificar(s.cpu.estaOciosa(), "la CPU queda libre mientras espera la E/S");
        s.avanzar(2);   // ciclos 4 y 5
        verificar(p.getEstado() == EstadoProceso.BLOQUEADO, "sigue bloqueado durante la E/S (ciclos 4 y 5)");
        s.avanzar(1);   // ciclo 6: termina la E/S y se despacha
        verificar(p.getEstado() == EstadoProceso.EJECUCION && p.getPc() == 2,
                "la E/S termina en el ciclo 6 (3 ciclos) y el proceso vuelve a la CPU con PC=2");
        s.avanzar(7);   // hasta el ciclo 13
        verificar(p.getEstado() == EstadoProceso.TERMINADO && p.getCicloFinalizacion() == 13,
                "tras una segunda E/S termina en el ciclo 13");
        verificar(!p.isTerminadoPorDeadline(), "termino normalmente");
        verificar(s.bitacora.contiene("pide E/S"), "la bitacora registra la E/S");
    }
 
    private static void probarEsConOtroProceso() {
        System.out.println("--- Mientras uno espera E/S, otro usa la CPU ---");
        GeneradorId.reiniciar();
        Sistema s = new Sistema(100);
        Pcb io = s.ioBound("Io", 10, 4, 1000, 1, 5);
        Pcb cpu = s.cpuBound("Calculo", 10, 3, 1000);
        s.avanzar(2);   // ciclo 1 despacha Io; ciclo 2 ejecuta, pide E/S y se despacha Calculo
        verificar(io.getEstado() == EstadoProceso.BLOQUEADO, "Io esta en E/S");
        verificar(s.cpu.getProceso() == cpu, "Calculo aprovecha la CPU mientras Io espera");
        s.avanzar(3);   // ciclos 3, 4 y 5: Calculo ejecuta sus 3 instrucciones
        verificar(cpu.getEstado() == EstadoProceso.TERMINADO && cpu.getCicloFinalizacion() == 5,
                "Calculo termina en el ciclo 5, sin esperar a que acabe la E/S de Io");
    }
 
    private static void probarDeadline() {
        System.out.println("--- Deadline ---");
        GeneradorId.reiniciar();
        Sistema s = new Sistema(100);
        Pcb largo = s.cpuBound("Largo", 30, 100, 10);
        s.avanzar(10);
        verificar(largo.getEstado() == EstadoProceso.TERMINADO && largo.isTerminadoPorDeadline(),
                "un proceso que no alcanza a terminar muere por deadline");
        verificar(largo.getCicloFinalizacion() == 10, "muere en el ciclo 10, cuando su deadline llega a 0");
        verificar(s.ram.getUsada() == 0, "libera su memoria");
        verificar(s.cpu.estaOciosa(), "la CPU queda libre");
        verificar(s.bitacora.contiene("excede su deadline"), "la bitacora registra el deadline vencido");
 
        GeneradorId.reiniciar();
        Sistema justo = new Sistema(100);
        Pcb exacto = justo.cpuBound("Justo", 10, 9, 10);
        justo.avanzar(10);
        verificar(exacto.getEstado() == EstadoProceso.TERMINADO && !exacto.isTerminadoPorDeadline(),
                "si termina exactamente en el ciclo de su deadline, cuenta como terminado normal");
    }
 
    private static void probarDeadlineEnTodasLasColas() {
        System.out.println("--- Deadline en listos, bloqueados y nuevos ---");
        GeneradorId.reiniciar();
        Sistema s = new Sistema(30);
        Pcb a = s.cpuBound("A", 10, 100, 1000);
        Pcb enListos = s.cpuBound("EnListos", 10, 50, 5);
        Pcb io = s.ioBound("EnEs", 10, 50, 6, 1, 100);
        Pcb nuevo = s.cpuBound("Nuevo", 10, 5, 4);   // la RAM (30) ya esta llena: queda en nuevos
        verificar(nuevo.getEstado() == EstadoProceso.NUEVO, "el cuarto proceso espera en nuevos por falta de memoria");
        s.avanzar(4);
        verificar(nuevo.getEstado() == EstadoProceso.TERMINADO && nuevo.isTerminadoPorDeadline(),
                "un proceso en nuevos tambien muere por deadline (ciclo 4)");
        verificar(s.ram.getUsada() == 30, "como nunca tuvo memoria, no libera nada");
        s.avanzar(1);
        verificar(enListos.getEstado() == EstadoProceso.TERMINADO && enListos.isTerminadoPorDeadline()
                && enListos.getCicloFinalizacion() == 5, "uno que espera en listos muere por deadline (ciclo 5)");
        verificar(s.ram.getUsada() == 20, "y libera su memoria");
        s.avanzar(1);
        verificar(io.getEstado() == EstadoProceso.TERMINADO && io.isTerminadoPorDeadline(),
                "uno bloqueado en E/S tambien muere por deadline (ciclo 6)");
        verificar(s.nucleo.getBloqueados().length == 0, "ya no queda nadie en bloqueados");
        verificar(a.getEstado() != EstadoProceso.TERMINADO, "A, con deadline de 1000, sigue vivo");
        verificar(s.nucleo.getTerminados().length == 3, "hay 3 procesos terminados");
    }
 
    private static void probarValidaciones() {
        System.out.println("--- Validaciones ---");
        GeneradorId.reiniciar();
        Pcb cpuBound = new Pcb("C", 1, TipoProceso.CPU_BOUND, 1, 10, 5, 100, 0);
        verificar(lanza(IllegalStateException.class, () -> cpuBound.configurarEs(2, 3)),
                "solo un proceso I/O bound puede configurar E/S");
        Pcb io = new Pcb("I", 1, TipoProceso.IO_BOUND, 1, 10, 5, 100, 0);
        verificar(lanza(IllegalArgumentException.class, () -> io.configurarEs(0, 3)),
                "la frecuencia de E/S debe ser 1 o mas");
        verificar(lanza(IllegalArgumentException.class, () -> io.configurarEs(2, 0)),
                "la duracion de la E/S debe ser 1 o mas");
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