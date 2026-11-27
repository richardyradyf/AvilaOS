/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.demo;

import avilaos.cluster.Cluster;
import avilaos.cluster.Computador;
import avilaos.model.EstadoProceso;
import avilaos.model.GeneradorId;
import avilaos.model.Pcb;
import avilaos.scheduler.PlanificadorRoundRobin;
import avilaos.model.PoliticaPlanificacion;
import avilaos.model.TipoProceso;
import avilaos.scheduler.PlanificadorFcfs;

/** 
 * @author richard
 * 
 * Pruebas del cluster: dos computadores bajo un mismo reloj global. */
public class PruebaCluster {

    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) throws InterruptedException {
        probarUnComputador();
        probarValidaciones();
        probarConHiloDelReloj();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static void probarUnComputador() {
        System.out.println("--- Un computador, un reloj ---");
        GeneradorId.reiniciar();
        Cluster cluster = new Cluster(1000, 500);
        Computador c1 = cluster.agregarComputador(100, new PlanificadorFcfs());
        verificar(c1.getId() == 1 && cluster.cantidadComputadores() == 1, "el sistema tiene el computador 1");
        verificar(lanza(IllegalStateException.class, () -> cluster.agregarComputador(50, new PlanificadorFcfs())),
                "no se agrega un segundo computador");

        Pcb a = cluster.crearProceso(1, "A", TipoProceso.CPU_BOUND, 1, 30, 4, 100);
        Pcb b = cluster.crearProceso(1, "B", TipoProceso.CPU_BOUND, 1, 20, 2, 100);
        verificar(a.getIdComputador() == 1 && b.getIdComputador() == 1, "los procesos quedan en el computador 1");
        verificar(c1.getRam().getUsada() == 50, "la RAM reserva la memoria de los dos");

        for (int i = 0; i < 5; i++) {
            cluster.avanzarCiclo();
        }
        verificar(a.getEstado() == EstadoProceso.TERMINADO && a.getCicloFinalizacion() == 5,
                "A termina en el ciclo 5 (1 de despacho + 4 instrucciones)");
        verificar(b.getEstado() != EstadoProceso.TERMINADO, "B espera a que A termine (FCFS)");
        for (int i = 0; i < 10; i++) {
            cluster.avanzarCiclo();
        }
        verificar(b.getEstado() == EstadoProceso.TERMINADO && b.getCicloFinalizacion() > 5,
                "B termina despues de A");
        verificar(cluster.getCicloActual() == 15, "el reloj global va por el ciclo 15");
        verificar(cluster.contarTerminados() == 2, "hay 2 procesos terminados");
        verificar(c1.getRam().getUsada() == 0, "la memoria quedo libre");

        cluster.cambiarPlanificador(1, new PlanificadorRoundRobin(3));
        verificar(c1.getNucleo().getPlanificador().getTipo() == PoliticaPlanificacion.ROUND_ROBIN,
                "se puede cambiar la politica del computador");
        verificar(cluster.getBitacora().contiene("Computador 1 agregado"), "la bitacora registra el alta");
    }

    private static void probarValidaciones() {
        System.out.println("--- Validaciones ---");
        GeneradorId.reiniciar();
        Cluster vacio = new Cluster(1000, 100);
        verificar(lanza(IllegalStateException.class, vacio::iniciar),
                "no se inicia sin computador");
        Cluster cluster = new Cluster(1000, 100);
        cluster.agregarComputador(100, new PlanificadorFcfs());
        verificar(lanza(IllegalArgumentException.class, () -> cluster.getComputador(3)),
                "pedir un computador inexistente lanza excepcion");
        verificar(lanza(IllegalArgumentException.class,
                () -> cluster.crearProceso(5, "X", TipoProceso.CPU_BOUND, 1, 10, 5, 50)),
                "crear un proceso en un computador inexistente lanza excepcion");
        verificar(lanza(IllegalArgumentException.class,
                () -> vacio.agregarComputador(0, new PlanificadorFcfs())),
                "una RAM de 0 MB es rechazada");
    }

    private static void probarConHiloDelReloj() throws InterruptedException {
        System.out.println("--- Reloj en su propio hilo ---");
        GeneradorId.reiniciar();
        Cluster cluster = new Cluster(2, 100);
        cluster.agregarComputador(100, new PlanificadorFcfs());
        cluster.crearProceso(1, "H1", TipoProceso.CPU_BOUND, 1, 10, 20, 500);
        cluster.crearProceso(1, "H2", TipoProceso.CPU_BOUND, 1, 10, 20, 500);
        cluster.iniciar();
        int espera = 0;
        while (cluster.contarTerminados() < 2 && espera < 100) {
            Thread.sleep(50);
            espera++;
        }
        verificar(lanza(IllegalStateException.class, () -> cluster.agregarComputador(100, new PlanificadorFcfs())),
                "con el reloj en marcha no se agregan computadores");
        cluster.detener();
        verificar(cluster.contarTerminados() == 2, "con el hilo del reloj, los 2 procesos terminan solos");
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