/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.demo;

import avilaos.disco.Disco;
import avilaos.disco.PoliticaFifo;
import avilaos.disco.SolicitudDisco;
import avilaos.fs.MapaBloques;
import avilaos.hardware.Cpu;
import avilaos.hardware.Ram;
import avilaos.hardware.RelojGlobal;
import avilaos.kernel.Nucleo;
import avilaos.kernel.SwapDisco;
import avilaos.model.EstadoProceso;
import avilaos.model.GeneradorId;
import avilaos.model.Pcb;
import avilaos.model.TipoOperacionES;
import avilaos.model.TipoProceso;
import avilaos.scheduler.PlanificadorFcfs;
import avilaos.util.Bitacora;

/**
 *
 * @author richard
 * 
 * Pruebas del planificador a mediano plazo con el swap real: cuando un proceso nuevo
 * no cabe, se suspende un bloqueado, se escribe en el swap por la cola del disco y
 * su memoria se libera cuando el disco termina.
 */
public class PruebaSwap {

    private static int total = 0;
    private static int fallos = 0;

    private static class Sistema {
        final Bitacora bitacora = new Bitacora(500);
        final Ram ram = new Ram(100);
        final Cpu cpu = new Cpu(1);
        final Nucleo nucleo;
        final RelojGlobal reloj = new RelojGlobal(1000);
        final MapaBloques mapa = new MapaBloques(200);
        final Disco disco;
        final SwapDisco swap;

        Sistema(int bloquesSwap) {
            GeneradorId.reiniciar();
            SolicitudDisco.reiniciarIds();
            mapa.configurarSwap(bloquesSwap);
            disco = new Disco(200 + bloquesSwap, new PoliticaFifo(), 0, 50);
            swap = new SwapDisco(mapa, disco);
            nucleo = new Nucleo(1, ram, cpu, new PlanificadorFcfs(), bitacora);
            nucleo.setPuertoSwap(swap);
            disco.agregarOyente(nucleo);
            reloj.registrar(cpu);
            reloj.registrar(nucleo);
            reloj.registrar(disco);
        }

        boolean avanzarHasta(Pcb p, EstadoProceso estado, int limite) {
            for (int i = 0; i < limite && p.getEstado() != estado; i++) {
                reloj.avanzarCiclo();
            }
            return p.getEstado() == estado;
        }

        void avanzar(int ciclos) {
            for (int i = 0; i < ciclos; i++) {
                reloj.avanzarCiclo();
            }
        }

        /** Crea un proceso que queda bloqueado esperando una E/S de "ciclosEs" ciclos. */
        Pcb bloqueado(String nombre, int prioridad, int memoria, int ciclosEs, int deadline) {
            Pcb p = new Pcb(nombre, 1, TipoProceso.IO_BOUND, prioridad, memoria, 5, deadline, 0);
            p.configurarEs(1, ciclosEs);
            nucleo.agregarProceso(p);
            avanzarHasta(p, EstadoProceso.BLOQUEADO, 20);
            return p;
        }

        Pcb nuevo(String nombre, int memoria) {
            Pcb p = new Pcb(nombre, 1, TipoProceso.CPU_BOUND, 1, memoria, 50, 5000, 0);
            nucleo.agregarProceso(p);
            return p;
        }
    }

    public static void main(String[] args) {
        probarEscrituraEnSwap();
        probarSuspendeParaAdmitir();
        probarCriterio();
        probarSinSuspensionInutil();
        probarSinEspacioEnSwap();
        probarEventoDuranteLaEscritura();
        probarDeadlineEnElSwap();
        probarDeadlineDuranteLaEscritura();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static void probarEscrituraEnSwap() {
        System.out.println("--- Escritura en el swap ---");
        Sistema s = new Sistema(100);
        Pcb p = new Pcb("A", 1, TipoProceso.IO_BOUND, 1, 30, 5, 1000, 0);
        SolicitudDisco sol = s.swap.escribirEnSwap(p, 4);
        verificar(sol.getOperacion() == TipoOperacionES.SUSPENDER, "la solicitud es SUSPENDER");
        verificar(sol.getBloqueDestino() == 200 && p.getBloqueSwap() == 200, "empieza en el primer bloque del swap");
        verificar(sol.getBloquesATransferir() == 30, "transfiere un bloque por unidad de memoria");
        verificar(s.mapa.libresSwap() == 70, "ocupa 30 bloques del swap");
        verificar("swap:P1".equals(s.mapa.getOcupante(200)), "el mapa muestra que proceso lo ocupa");
        verificar(s.disco.getCantidadEnCola() == 1, "la solicitud pasa por la cola del disco");
        s.swap.liberarSwap(p);
        verificar(s.mapa.libresSwap() == 100 && p.getBloqueSwap() == -1, "liberar devuelve los bloques");
        verificar(lanza(() -> new SwapDisco(null, s.disco)), "el swap necesita el mapa");
    }

    private static void probarSuspendeParaAdmitir() {
        System.out.println("--- Suspende a un bloqueado para admitir a un nuevo ---");
        Sistema s = new Sistema(100);
        Pcb a = s.bloqueado("A", 1, 60, 1000, 5000);
        Pcb c = s.nuevo("C", 50);
        verificar(a.getEstado() == EstadoProceso.BLOQUEADO_SUSPENDIDO, "el bloqueado pasa a Bloqueado/Suspendido");
        verificar(c.getEstado() == EstadoProceso.NUEVO, "el nuevo espera a que se libere la RAM");
        verificar(s.ram.getUsada() == 60 && a.isEscribiendoSwap(), "la RAM sigue ocupada mientras se escribe");
        verificar(s.mapa.libresSwap() == 40, "reserva 60 bloques de swap");
        verificar(s.bitacora.contiene("Se suspende al proceso P1 para admitir a P2"), "la bitacora explica la decision");
        s.avanzar(70);
        verificar(!a.isEscribiendoSwap() && c.getEstado() != EstadoProceso.NUEVO,
                "al terminar la escritura se libera la RAM y entra el nuevo");
        verificar(s.ram.getUsada() == 50, "en la RAM solo queda el nuevo");
        verificar(s.mapa.libresSwap() == 40, "el suspendido sigue en el swap");
    }

    private static void probarCriterio() {
        System.out.println("--- Criterio de seleccion ---");
        Sistema s = new Sistema(100);
        Pcb alta = s.bloqueado("Alta", 1, 30, 1000, 5000);
        Pcb baja = s.bloqueado("Baja", 5, 30, 1000, 5000);
        Pcb baja2 = s.bloqueado("Baja2", 5, 30, 1000, 5000);
        s.nuevo("Nuevo", 30);
        verificar(baja.getEstado() == EstadoProceso.BLOQUEADO_SUSPENDIDO, "se elige al de menor prioridad");
        verificar(baja2.getEstado() == EstadoProceso.BLOQUEADO && alta.getEstado() == EstadoProceso.BLOQUEADO,
                "con igual prioridad, el que lleva mas tiempo bloqueado; los demas no se tocan");
    }

    private static void probarSinSuspensionInutil() {
        System.out.println("--- No suspende si no serviria ---");
        Sistema s = new Sistema(100);
        Pcb a = s.bloqueado("A", 1, 20, 1000, 5000);
        Pcb enCpu = new Pcb("Cpu", 1, TipoProceso.CPU_BOUND, 1, 70, 500, 5000, 0);
        s.nucleo.agregarProceso(enCpu);
        s.avanzarHasta(enCpu, EstadoProceso.EJECUCION, 10);
        Pcb c = s.nuevo("C", 80);
        verificar(a.getEstado() == EstadoProceso.BLOQUEADO && c.getEstado() == EstadoProceso.NUEVO,
                "si ni con todos los bloqueados alcanza, no suspende a nadie");
    }

    private static void probarSinEspacioEnSwap() {
        System.out.println("--- Swap sin espacio ---");
        Sistema s = new Sistema(10);
        Pcb a = s.bloqueado("A", 1, 60, 1000, 5000);
        Pcb c = s.nuevo("C", 50);
        s.avanzar(5);
        verificar(a.getEstado() == EstadoProceso.BLOQUEADO && s.ram.getUsada() == 60,
                "si no cabe en el swap, sigue bloqueado en la RAM");
        verificar(c.getEstado() == EstadoProceso.NUEVO && s.mapa.libresSwap() == 10, "el nuevo espera y el swap no cambia");
    }

    private static void probarEventoDuranteLaEscritura() {
        System.out.println("--- Evento mientras se escribe ---");
        Sistema s = new Sistema(100);
        Pcb a = s.bloqueado("A", 1, 60, 5, 5000);
        s.nuevo("C", 50);
        verificar(s.avanzarHasta(a, EstadoProceso.LISTO_SUSPENDIDO, 20) && a.isEscribiendoSwap(),
                "su evento llega durante la escritura: pasa a Listo/Suspendido");
        verificar(s.ram.getUsada() == 60, "la RAM no se libera hasta terminar la escritura");
        s.avanzar(70);
        verificar(!a.isEscribiendoSwap() && s.ram.getUsada() == 50, "al terminar la escritura entra el nuevo");
    }

    private static void probarDeadlineEnElSwap() {
        System.out.println("--- Deadline ya en el swap ---");
        Sistema s = new Sistema(100);
        Pcb a = s.bloqueado("A", 1, 60, 1000, 100);
        s.nuevo("C", 50);
        s.avanzar(70);
        verificar(s.mapa.libresSwap() == 40, "esta en el swap");
        verificar(s.avanzarHasta(a, EstadoProceso.TERMINADO, 60) && a.isTerminadoPorDeadline(), "vence su deadline");
        verificar(s.mapa.libresSwap() == 100 && s.ram.getUsada() == 50, "libera el swap sin tocar la RAM del otro");
    }

    private static void probarDeadlineDuranteLaEscritura() {
        System.out.println("--- Deadline durante la escritura ---");
        Sistema s = new Sistema(100);
        Pcb a = s.bloqueado("A", 1, 60, 1000, 15);
        s.nuevo("C", 50);
        verificar(s.avanzarHasta(a, EstadoProceso.TERMINADO, 20) && a.isEscribiendoSwap(),
                "vence mientras el disco lo escribe");
        verificar(s.ram.getUsada() == 60 && s.mapa.libresSwap() == 40, "conserva RAM y swap hasta que el disco termine");
        s.avanzar(70);
        verificar(s.mapa.libresSwap() == 100 && s.ram.getUsada() == 50, "al terminar se liberan los dos");
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

