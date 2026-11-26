/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.demo;

import avilaos.cluster.Cluster;
import avilaos.disco.Disco;
import avilaos.disco.PoliticaFifo;
import avilaos.disco.SolicitudDisco;
import avilaos.fs.MapaBloques;
import avilaos.kernel.Nucleo;
import avilaos.kernel.SwapDisco;
import avilaos.metrics.MetricasComputador;
import avilaos.metrics.RecolectorMetricas;
import avilaos.model.EstadoProceso;
import avilaos.model.GeneradorId;
import avilaos.model.Pcb;
import avilaos.model.TipoProceso;
import avilaos.scheduler.PlanificadorFcfs;

/**
 *
 * @author richard
 * 
 * Pruebas de las metricas de suspension: cuenta suspensiones y reactivaciones, y no
 * pierde de vista a los procesos mientras estan en el swap.
 */
public class PruebaMetricasSwap {

    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        GeneradorId.reiniciar();
        SolicitudDisco.reiniciarIds();
        Cluster cluster = new Cluster(1000, 2000);
        Nucleo nucleo = cluster.agregarComputador(100, new PlanificadorFcfs()).getNucleo();
        RecolectorMetricas recolector = new RecolectorMetricas(cluster, 1, 100);
        MapaBloques mapa = new MapaBloques(200);
        mapa.configurarSwap(100);
        Disco disco = new Disco(300, new PoliticaFifo(), 0, 50);
        nucleo.setPuertoSwap(new SwapDisco(mapa, disco));
        disco.agregarOyente(nucleo);
        cluster.avanzarCiclo();
        cluster.getReloj().registrar(disco);

        MetricasComputador inicio = recolector.generarReporte().getPorComputador()[0];
        verificar(inicio.getSuspensiones() == 0 && inicio.getReactivaciones() == 0, "empiezan en 0");

        Pcb a = cluster.crearProcesoEs(1, "A", 1, 60, 5, 5000, 1, 5);
        avanzarHasta(cluster, a, EstadoProceso.BLOQUEADO, 20);
        Pcb c = cluster.crearProceso(1, "C", TipoProceso.CPU_BOUND, 1, 50, 20, 5000);

        MetricasComputador suspendido = recolector.generarReporte().getPorComputador()[0];
        verificar(a.getEstado().estaSuspendido(), "A esta suspendido");
        verificar(suspendido.getSuspensiones() == 1, "cuenta la suspension");
        verificar(suspendido.getReactivaciones() == 0, "todavia no hay reactivaciones");
        verificar(suspendido.getProcesosTotales() == 2, "el reporte incluye al proceso suspendido");

        avanzarHasta(cluster, c, EstadoProceso.TERMINADO, 150);
        avanzarHasta(cluster, a, EstadoProceso.TERMINADO, 300);

        MetricasComputador fin = recolector.generarReporte().getPorComputador()[0];
        MetricasComputador sistema = recolector.generarReporte().getSistema();
        verificar(fin.getReactivaciones() == 1, "cuenta la reactivacion");
        verificar(fin.getCompletados() == 2, "los dos procesos terminan");
        verificar(sistema.getSuspensiones() == 1 && sistema.getReactivaciones() == 1,
                "la fila del sistema suma los mismos valores");

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static void avanzarHasta(Cluster cluster, Pcb p, EstadoProceso estado, int limite) {
        for (int i = 0; i < limite && p.getEstado() != estado; i++) {
            cluster.avanzarCiclo();
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