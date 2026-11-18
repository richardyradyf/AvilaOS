/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.demo;

import avilaos.cluster.Cluster;
import avilaos.disco.PuertoEs;
import avilaos.disco.SolicitudDisco;
import avilaos.hardware.Cpu;
import avilaos.hardware.Ram;
import avilaos.hardware.RelojGlobal;
import avilaos.kernel.Nucleo;
import avilaos.model.EstadoProceso;
import avilaos.model.GeneradorId;
import avilaos.model.MotivoBloqueo;
import avilaos.model.Pcb;
import avilaos.model.SolicitudProceso;
import avilaos.model.TipoOperacionES;
import avilaos.model.TipoProceso;
import avilaos.scheduler.PlanificadorFcfs;
import avilaos.util.Bitacora;

/**
 *
 * @author richard
 * 
 * Pruebas de los procesos de E/S en el nucleo: piden el disco una sola vez, se
 * bloquean esperando el disco y el aviso del disco (OyenteDisco) los despierta.
 * El disco se reemplaza por un puerto falso que solo guarda las solicitudes.
 * 
 */
public class PruebaProcesosEs {

    private static int total = 0;
    private static int fallos = 0;

    /** Puerto falso: guarda la ultima solicitud y cuenta cuantas recibio. */
    private static class PuertoFalso implements PuertoEs {
        int pedidos = 0;
        SolicitudDisco ultima;

        @Override
        public SolicitudDisco pedirDisco(Pcb proceso, long ciclo) {
            pedidos++;
            int bloques = proceso.getOperacion() == TipoOperacionES.CREAR ? proceso.getBloquesNuevos() : 1;
            ultima = new SolicitudDisco(proceso, proceso.getOperacion(), proceso.getRuta(), 87, bloques, ciclo);
            return ultima;
        }
    }

    private static class Sistema {
        final Bitacora bitacora = new Bitacora(500);
        final Ram ram = new Ram(100);
        final Cpu cpu = new Cpu(1);
        final Nucleo nucleo;
        final RelojGlobal reloj = new RelojGlobal(1000);
        final PuertoFalso puerto = new PuertoFalso();

        Sistema(boolean conPuerto) {
            GeneradorId.reiniciar();
            SolicitudDisco.reiniciarIds();
            nucleo = new Nucleo(1, ram, cpu, new PlanificadorFcfs(), bitacora);
            if (conPuerto) {
                nucleo.setPuertoEs(puerto);
            }
            reloj.registrar(cpu);
            reloj.registrar(nucleo);
        }

        void avanzar(int ciclos) {
            for (int i = 0; i < ciclos; i++) {
                reloj.avanzarCiclo();
            }
        }

        boolean avanzarHasta(Pcb p, EstadoProceso estado, int limite) {
            for (int i = 0; i < limite && p.getEstado() != estado; i++) {
                reloj.avanzarCiclo();
            }
            return p.getEstado() == estado;
        }
    }

    public static void main(String[] args) {
        probarCreacionDesdeDeEs();
        probarPideElDiscoYDespierta();
        probarUnaSolaInstruccion();
        probarSinPuerto();
        probarAvisosQueNoAplican();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static void probarCreacionDesdeDeEs() {
        System.out.println("--- Proceso creado con deEs ---");
        GeneradorId.reiniciar();
        Cluster cluster = new Cluster(1000, 100);
        cluster.agregarComputador(100, new PlanificadorFcfs());
        Pcb p = cluster.crearProcesoArchivo(SolicitudProceso.deEs("Crear notas", 3, 10, 2, 500,
                TipoOperacionES.CREAR, "/usuarios/notas.txt", 4, null, "maria"));
        verificar(p.esDeArchivo() && p.getTipo() == TipoProceso.IO_BOUND, "es un proceso de E/S I/O bound");
        verificar(p.getOperacion() == TipoOperacionES.CREAR && p.getRuta().equals("/usuarios/notas.txt"),
                "guarda la operacion y la ruta");
        verificar(p.getBloquesNuevos() == 4 && p.getUsuario().equals("maria"), "guarda los bloques y el usuario");
        verificar(!p.yaPidioDisco(), "al crearse todavia no pidio el disco");
        Pcb normal = cluster.crearProceso(1, "Calculo", TipoProceso.CPU_BOUND, 1, 10, 5, 500);
        verificar(!normal.esDeArchivo(), "un proceso CPU bound no es de E/S de archivos");
        verificar(lanza(() -> normal.configurarArchivo(TipoOperacionES.SUSPENDER, "/a", 0, null, null)),
                "SUSPENDER no es una operacion de archivo");
    }

    private static void probarPideElDiscoYDespierta() {
        System.out.println("--- Pide el disco y el aviso lo despierta ---");
        Sistema s = new Sistema(true);
        Pcb p = archivo("Leer boot", TipoOperacionES.LEER, "/sistema/boot.bin", 3);
        s.nucleo.agregarProceso(p);
        verificar(s.avanzarHasta(p, EstadoProceso.BLOQUEADO, 10), "tras su primera instruccion se bloquea");
        verificar(p.getMotivoBloqueo() == MotivoBloqueo.ESPERA_DISCO, "el motivo es esperar el disco");
        verificar(s.puerto.pedidos == 1 && s.puerto.ultima.getProceso() == p, "manda una solicitud al disco");
        verificar(s.puerto.ultima.getOperacion() == TipoOperacionES.LEER
                && s.puerto.ultima.getRuta().equals("/sistema/boot.bin"), "la solicitud lleva su operacion y ruta");
        verificar(s.cpu.estaOciosa(), "la CPU queda libre mientras espera");

        s.avanzar(5);
        verificar(p.getEstado() == EstadoProceso.BLOQUEADO, "sin aviso del disco sigue bloqueado");

        s.nucleo.solicitudAtendida(s.puerto.ultima, 8);
        verificar(p.getEstado() == EstadoProceso.LISTO && p.getMotivoBloqueo() == MotivoBloqueo.NINGUNO,
                "el aviso del disco lo pasa a listos");
        verificar(s.avanzarHasta(p, EstadoProceso.TERMINADO, 20) && !p.isTerminadoPorDeadline(),
                "termina sus instrucciones");
        verificar(s.puerto.pedidos == 1, "pide el disco una sola vez");
        verificar(s.ram.getUsada() == 0, "al terminar libera la memoria");
        verificar(s.bitacora.contiene("pide el disco") && s.bitacora.contiene("el disco atendio"),
                "la bitacora registra el pedido y el aviso");
    }

    private static void probarUnaSolaInstruccion() {
        System.out.println("--- Proceso de una sola instruccion ---");
        Sistema s = new Sistema(true);
        Pcb p = archivo("Borrar", TipoOperacionES.ELIMINAR, "/tmp/viejo.txt", 1);
        s.nucleo.agregarProceso(p);
        verificar(s.avanzarHasta(p, EstadoProceso.BLOQUEADO, 10), "pide el disco y se bloquea");
        s.nucleo.solicitudAtendida(s.puerto.ultima, 4);
        verificar(p.getEstado() == EstadoProceso.TERMINADO && p.getCicloFinalizacion() == 4,
                "sin instrucciones pendientes, el aviso lo termina en ese ciclo");
        verificar(s.ram.getUsada() == 0 && s.nucleo.getTerminados().length == 1, "libera la RAM y queda en terminados");
    }

    private static void probarSinPuerto() {
        System.out.println("--- Sin puerto de E/S ---");
        Sistema s = new Sistema(false);
        Pcb p = archivo("Leer", TipoOperacionES.LEER, "/a.txt", 3);
        s.nucleo.agregarProceso(p);
        verificar(s.avanzarHasta(p, EstadoProceso.TERMINADO, 20), "sin disco conectado corre como un proceso normal");
        verificar(!p.yaPidioDisco(), "no pidio el disco");
    }

    private static void probarAvisosQueNoAplican() {
        System.out.println("--- Avisos que no aplican ---");
        Sistema s = new Sistema(true);
        Pcb cpu = new Pcb("Calculo", 1, TipoProceso.CPU_BOUND, 1, 10, 3, 500, 0);
        s.nucleo.agregarProceso(cpu);
        verificar(s.avanzarHasta(cpu, EstadoProceso.TERMINADO, 20) && s.puerto.pedidos == 0,
                "un proceso CPU bound no pide el disco");

        Pcb p = archivo("Suspendido", TipoOperacionES.LEER, "/b.txt", 3);
        s.nucleo.agregarProceso(p);
        s.avanzarHasta(p, EstadoProceso.BLOQUEADO, 10);
        s.nucleo.suspender(p);
        s.nucleo.solicitudAtendida(s.puerto.ultima, 20);
        verificar(p.getEstado() == EstadoProceso.LISTO_SUSPENDIDO,
                "si esperaba suspendido, el aviso lo pasa a listos/suspendidos");

        SolicitudDisco vieja = s.puerto.ultima;
        verificar(!lanza(() -> s.nucleo.solicitudAtendida(vieja, 21)),
                "un aviso repetido se ignora sin error");
        verificar(!lanza(() -> s.nucleo.solicitudAtendida(
                new SolicitudDisco(null, TipoOperacionES.LEER, "/c", 3, 1, 0), 22)),
                "un aviso sin proceso (del sistema) se ignora");
    }

    private static Pcb archivo(String nombre, TipoOperacionES op, String ruta, int instrucciones) {
        Pcb p = new Pcb(nombre, 1, TipoProceso.IO_BOUND, 1, 20, instrucciones, 1000, 0);
        p.configurarArchivo(op, ruta, 0, null, null);
        return p;
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
