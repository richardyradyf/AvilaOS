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
 * Pruebas de la reactivacion: un proceso listo/suspendido vuelve a la RAM cuando hay
 * espacio, pasando por la cola del disco, y al terminar la lectura libera su swap.
 */
public class PruebaReactivacion {

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

        Sistema() {
            GeneradorId.reiniciar();
            SolicitudDisco.reiniciarIds();
            mapa.configurarSwap(100);
            disco = new Disco(300, new PoliticaFifo(), 0, 50);
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

        boolean avanzarHastaLeer(Pcb p, int limite) {
            for (int i = 0; i < limite && !p.isLeyendoSwap(); i++) {
                reloj.avanzarCiclo();
            }
            return p.isLeyendoSwap();
        }

        boolean avanzarHastaFinLectura(Pcb p, int limite) {
            for (int i = 0; i < limite && p.isLeyendoSwap(); i++) {
                reloj.avanzarCiclo();
            }
            return !p.isLeyendoSwap();
        }

        void avanzar(int ciclos) {
            for (int i = 0; i < ciclos; i++) {
                reloj.avanzarCiclo();
            }
        }

        Pcb bloqueado(String nombre, int memoria, int ciclosEs, int deadline) {
            Pcb p = new Pcb(nombre, 1, TipoProceso.IO_BOUND, 1, memoria, 5, deadline, 0);
            p.configurarEs(1, ciclosEs);
            nucleo.agregarProceso(p);
            avanzarHasta(p, EstadoProceso.BLOQUEADO, 20);
            return p;
        }

        Pcb nuevo(String nombre, int memoria, int instrucciones) {
            Pcb p = new Pcb(nombre, 1, TipoProceso.CPU_BOUND, 1, memoria, instrucciones, 5000, 0);
            nucleo.agregarProceso(p);
            return p;
        }
    }

    public static void main(String[] args) {
        probarSolicitudDeLectura();
        probarReactivaAlLiberarRam();
        probarNoLeSeQuitaLaRamAlNuevo();
        probarBloqueadoSuspendidoEspera();
        probarReactivarAMano();
        probarDeadlineDuranteLaLectura();
        probarProcesoTerminaCompleto();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static void probarSolicitudDeLectura() {
        System.out.println("--- Solicitud de lectura ---");
        Sistema s = new Sistema();
        Pcb p = new Pcb("A", 1, TipoProceso.IO_BOUND, 1, 30, 5, 1000, 0);
        s.swap.escribirEnSwap(p, 1);
        SolicitudDisco lectura = s.swap.leerDeSwap(p, 2);
        verificar(lectura.getOperacion() == TipoOperacionES.REACTIVAR, "la solicitud es REACTIVAR");
        verificar(lectura.getBloqueDestino() == 200, "va al primer bloque del proceso en el swap");
        verificar(lectura.getBloquesATransferir() == 30, "lee un bloque por unidad de memoria");
        verificar(s.disco.getCantidadEnCola() == 2, "pasa por la cola del disco");
        verificar(s.mapa.libresSwap() == 70, "leer no libera el swap todavia");
    }

    private static void probarReactivaAlLiberarRam() {
        System.out.println("--- Reactiva al liberarse la RAM ---");
        Sistema s = new Sistema();
        Pcb a = s.bloqueado("A", 60, 5, 5000);
        Pcb c = s.nuevo("C", 50, 20);
        verificar(s.avanzarHasta(a, EstadoProceso.LISTO_SUSPENDIDO, 20), "A recibe su evento en el swap");
        verificar(s.avanzarHasta(c, EstadoProceso.TERMINADO, 150), "el proceso nuevo termina");
        verificar(s.avanzarHastaLeer(a, 5), "al liberarse la RAM, A se empieza a leer");
        verificar(a.getEstado() == EstadoProceso.LISTO_SUSPENDIDO, "sigue suspendido mientras se lee");
        verificar(s.ram.getUsada() == 60, "su RAM queda reservada durante la lectura");
        verificar(s.mapa.libresSwap() == 40, "su swap sigue ocupado durante la lectura");
        verificar(s.avanzarHastaFinLectura(a, 90), "el disco termina de leer");
        verificar(a.getEstado() == EstadoProceso.LISTO || a.getEstado() == EstadoProceso.EJECUCION
                || a.getEstado() == EstadoProceso.BLOQUEADO, "A vuelve a la RAM");
        verificar(s.mapa.libresSwap() == 100 && a.getBloqueSwap() == -1, "al terminar la lectura libera el swap");
    }

    private static void probarNoLeSeQuitaLaRamAlNuevo() {
        System.out.println("--- El nuevo entra primero ---");
        Sistema s = new Sistema();
        Pcb a = s.bloqueado("A", 60, 5, 5000);
        Pcb c = s.nuevo("C", 50, 500);
        s.avanzarHasta(a, EstadoProceso.LISTO_SUSPENDIDO, 20);
        s.avanzar(90);
        verificar(c.getEstado() != EstadoProceso.NUEVO, "la RAM liberada es para el proceso nuevo");
        verificar(!a.isLeyendoSwap() && a.getEstado() == EstadoProceso.LISTO_SUSPENDIDO,
                "A espera en el swap porque no cabe");
    }

    private static void probarBloqueadoSuspendidoEspera() {
        System.out.println("--- Bloqueado/Suspendido espera su evento ---");
        Sistema s = new Sistema();
        Pcb a = s.bloqueado("A", 60, 1000, 5000);
        Pcb c = s.nuevo("C", 50, 20);
        s.avanzarHasta(c, EstadoProceso.TERMINADO, 150);
        s.avanzar(10);
        verificar(s.ram.getUsada() == 0, "hay RAM libre");
        verificar(a.getEstado() == EstadoProceso.BLOQUEADO_SUSPENDIDO && !a.isLeyendoSwap(),
                "no se reactiva solo mientras espera su evento");
    }

    private static void probarReactivarAMano() {
        System.out.println("--- Reactivar a mano ---");
        Sistema s = new Sistema();
        Pcb a = s.bloqueado("A", 60, 1000, 5000);
        Pcb c = s.nuevo("C", 50, 20);
        verificar(!s.nucleo.reactivar(a), "no se reactiva mientras se escribe en el swap");
        s.avanzarHasta(c, EstadoProceso.TERMINADO, 150);
        s.avanzar(2);
        verificar(s.nucleo.reactivar(a) && a.isLeyendoSwap(), "con RAM libre, se pide la lectura");
        verificar(!s.nucleo.reactivar(a), "no se pide dos veces");
        verificar(s.avanzarHastaFinLectura(a, 90) && a.getEstado() == EstadoProceso.BLOQUEADO,
                "un bloqueado/suspendido vuelve a bloqueados");
        verificar(s.mapa.libresSwap() == 100 && s.ram.getUsada() == 60, "queda en la RAM y sin swap");
    }

    private static void probarDeadlineDuranteLaLectura() {
        System.out.println("--- Deadline durante la lectura ---");
        Sistema s = new Sistema();
        Pcb a = s.bloqueado("A", 60, 5, 120);
        Pcb c = s.nuevo("C", 50, 20);
        s.avanzarHasta(c, EstadoProceso.TERMINADO, 150);
        verificar(s.avanzarHastaLeer(a, 5), "A se esta leyendo");
        verificar(s.avanzarHasta(a, EstadoProceso.TERMINADO, 60) && a.isTerminadoPorDeadline() && a.isLeyendoSwap(),
                "vence mientras el disco lo lee");
        verificar(s.ram.getUsada() == 60 && s.mapa.libresSwap() == 40, "conserva RAM y swap hasta que el disco termine");
        verificar(s.avanzarHastaFinLectura(a, 90), "el disco termina");
        verificar(s.ram.getUsada() == 0 && s.mapa.libresSwap() == 100, "al terminar se liberan los dos");
    }

    private static void probarProcesoTerminaCompleto() {
        System.out.println("--- Recorrido completo ---");
        Sistema s = new Sistema();
        Pcb a = s.bloqueado("A", 60, 5, 5000);
        Pcb c = s.nuevo("C", 50, 20);
        verificar(s.avanzarHasta(a, EstadoProceso.TERMINADO, 800) && !a.isTerminadoPorDeadline(),
                "A vuelve del swap y termina sus instrucciones");
        verificar(c.getEstado() == EstadoProceso.TERMINADO, "C tambien termina");
        verificar(s.ram.getUsada() == 0, "la RAM queda vacia");
        verificar(s.mapa.libresSwap() == 100, "el swap queda vacio");
    }

    private static void verificar(boolean condicion, String descripcion) {
        total++;
        if (!condicion) {
            fallos++;
        }
        System.out.println((condicion ? "OK    - " : "FALLO - ") + descripcion);
    }
}
