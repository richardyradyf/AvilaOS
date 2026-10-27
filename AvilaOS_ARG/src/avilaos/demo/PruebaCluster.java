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
        probarDosComputadoresIndependientes();
        probarValidaciones();
        probarConHiloDelReloj();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static void probarDosComputadoresIndependientes() {
        System.out.println("--- Dos computadores, un reloj ---");
        GeneradorId.reiniciar();
        Cluster cluster = new Cluster(1000, 500);
        Computador c1 = cluster.agregarComputador(100, new PlanificadorFcfs());
        Computador c2 = cluster.agregarComputador(50, new PlanificadorFcfs());
        verificar(c1.getId() == 1 && c2.getId() == 2, "los identificadores son 1 y 2");
        verificar(cluster.cantidadComputadores() == 2, "el cluster tiene 2 computadores");

        Pcb a = cluster.crearProceso(1, "A", TipoProceso.CPU_BOUND, 1, 30, 4, 100);
        Pcb b = cluster.crearProceso(2, "B", TipoProceso.CPU_BOUND, 1, 20, 6, 100);
        verificar(a.getIdComputador() == 1 && b.getIdComputador() == 2,
                "cada proceso queda en su computador");
        verificar(c1.getRam().getUsada() == 30 && c2.getRam().getUsada() == 20,
                "cada computador reserva su propia memoria");

        for (int i = 0; i < 5; i++) {
            cluster.avanzarCiclo();
        }
        verificar(a.getEstado() == EstadoProceso.TERMINADO && a.getCicloFinalizacion() == 5,
                "A termina en el ciclo 5 (1 de despacho + 4 instrucciones)");
        verificar(b.getEstado() == EstadoProceso.EJECUCION, "B sigue ejecutando en el otro computador");
        cluster.avanzarCiclo();
        cluster.avanzarCiclo();
        verificar(b.getEstado() == EstadoProceso.TERMINADO && b.getCicloFinalizacion() == 7,
                "B termina en el ciclo 7");
        verificar(cluster.getCicloActual() == 7, "el reloj global va por el ciclo 7");
        verificar(cluster.contarTerminados() == 2, "hay 2 procesos terminados en total");
        verificar(c1.getRam().getUsada() == 0 && c2.getRam().getUsada() == 0, "ambas memorias quedaron libres");

        cluster.cambiarPlanificador(2, new PlanificadorFcfs());
        verificar(c2.getNucleo().getPlanificador().getTipo() == PoliticaPlanificacion.FCFS,
                "se puede cambiar la politica de un computador");
        verificar(cluster.getBitacora().contiene("Computador 2 agregado"), "la bitacora registra el alta");
    }

    private static void probarValidaciones() {
        System.out.println("--- Validaciones ---");
        GeneradorId.reiniciar();
        Cluster cluster = new Cluster(1000, 100);
        cluster.agregarComputador(100, new PlanificadorFcfs());
        verificar(lanza(IllegalStateException.class, cluster::iniciar),
                "no se inicia con menos de 2 computadores");
        verificar(lanza(IllegalArgumentException.class, () -> cluster.getComputador(3)),
                "pedir un computador inexistente lanza excepcion");
        verificar(lanza(IllegalArgumentException.class,
                () -> cluster.crearProceso(5, "X", TipoProceso.CPU_BOUND, 1, 10, 5, 50)),
                "crear un proceso en un computador inexistente lanza excepcion");
        verificar(lanza(IllegalArgumentException.class,
                () -> cluster.agregarComputador(0, new PlanificadorFcfs())),
                "una RAM de 0 MB es rechazada");
    }

    private static void probarConHiloDelReloj() throws InterruptedException {
        System.out.println("--- Reloj en su propio hilo ---");
        GeneradorId.reiniciar();
        Cluster cluster = new Cluster(2, 100);
        cluster.agregarComputador(100, new PlanificadorFcfs());
        cluster.agregarComputador(100, new PlanificadorFcfs());
        cluster.crearProceso(1, "H1", TipoProceso.CPU_BOUND, 1, 10, 20, 500);
        cluster.crearProceso(2, "H2", TipoProceso.CPU_BOUND, 1, 10, 20, 500);
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