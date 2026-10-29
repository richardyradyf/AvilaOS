package avilaos.demo;

import avilaos.cluster.Cluster;
import avilaos.gui.PanelGraficaUtilizacion;
import avilaos.gui.VistaMetricas;
import avilaos.metrics.RecolectorMetricas;
import avilaos.metrics.ReporteMetricas;
import avilaos.metrics.SerieTemporal;
import avilaos.model.GeneradorId;
import avilaos.model.Pcb;
import avilaos.model.TipoProceso;
import avilaos.scheduler.PlanificadorFcfs;
import java.awt.image.BufferedImage;

/**
 * @author Gianm
/** Pruebas de las metricas: serie temporal, calculos del recolector y dibujo de la grafica. */
public class PruebaMetricas {

    private static int total = 0;
    private static int fallos = 0;

    private static void verificar(String descripcion, boolean condicion) {
        total++;
        if (condicion) {
            System.out.println("OK    " + descripcion);
        } else {
            fallos++;
            System.out.println("FALLO " + descripcion);
        }
    }

    private static boolean cerca(double a, double b) {
        return Math.abs(a - b) < 1e-9;
    }

    public static void main(String[] args) {
        probarSerie();
        probarMetricasBasicas();
        probarDeadlinesYBuffers();
        probarGrafica();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
        System.exit(fallos == 0 ? 0 : 1);
    }

    private static void probarSerie() {
        System.out.println("--- SerieTemporal ---");
        SerieTemporal s = new SerieTemporal(3);
        for (int i = 1; i <= 5; i++) {
            s.agregar(i, i / 10.0);
        }
        verificar("conserva solo los 3 ultimos puntos", s.tamano() == 3);
        verificar("el mas antiguo es el ciclo 3", s.getCiclos()[0] == 3);
        verificar("el valor mas reciente es 0.5", cerca(s.getValores()[2], 0.5));
        boolean fallo = false;
        try {
            new SerieTemporal(0);
        } catch (IllegalArgumentException e) {
            fallo = true;
        }
        verificar("rechaza una serie sin capacidad", fallo);
    }

    private static Cluster nuevoCluster() {
        GeneradorId.reiniciar();
        Cluster c = new Cluster(1000, 2000, 5);
        c.agregarComputador(100, new PlanificadorFcfs());
        c.agregarComputador(100, new PlanificadorFcfs());
        return c;
    }

    private static void probarMetricasBasicas() {
        System.out.println("--- Metricas basicas ---");
        Cluster c = nuevoCluster();
        RecolectorMetricas rec = new RecolectorMetricas(c, 1, 100);
        c.getReloj().registrar(rec);
        // Dos procesos CPU bound de 10 instrucciones en el computador 1; el computador 2 queda ocioso
        c.crearProceso(1, "A", TipoProceso.CPU_BOUND, 1, 10, 10, 1000);
        c.crearProceso(1, "B", TipoProceso.CPU_BOUND, 1, 10, 10, 1000);
        for (int i = 0; i < 40; i++) {
            c.avanzarCiclo();
        }
        ReporteMetricas r = rec.generarReporte();
        verificar("computador 1: 2 procesos completados", r.getPorComputador()[0].getCompletados() == 2);
        verificar("computador 1: ningun vencido", r.getPorComputador()[0].getVencidos() == 0);
        verificar("cumplimiento de deadlines 100 %", cerca(r.getPorComputador()[0].getCumplimientoDeadlines(), 1.0));
        verificar("computador 2 ocioso: utilizacion 0", cerca(r.getPorComputador()[1].getUtilizacion(), 0.0));
        double u1 = r.getPorComputador()[0].getUtilizacion();
        verificar("computador 1: utilizacion entre 0 y 1 (" + u1 + ")", u1 > 0.0 && u1 <= 1.0);
        verificar("sistema: 2 completados", r.getSistema().getCompletados() == 2);
        verificar("throughput = completados / ciclos",
                cerca(r.getSistema().getThroughput(), 2.0 / r.getCiclo()));
        verificar("tiempo de respuesta definido", !Double.isNaN(r.getPorComputador()[0].getTiempoRespuestaPromedio()));
        double eq = r.getPorComputador()[0].getEquidad();
        verificar("equidad de Jain entre 0 y 1", eq > 0.0 && eq <= 1.0 + 1e-9);
        verificar("sin buffers: bloqueo promedio indefinido", Double.isNaN(r.getBloqueoSemaforosPromedio()));
        verificar("la serie del computador 1 tiene puntos", rec.getSerie(1).tamano() > 0);
    }

    private static void probarDeadlinesYBuffers() {
        System.out.println("--- Deadlines vencidos y buffers ---");
        Cluster c = nuevoCluster();
        RecolectorMetricas rec = new RecolectorMetricas(c, 5, 100);
        c.getReloj().registrar(rec);
        // El proceso largo no alcanza su deadline de 5 ciclos
        c.crearProceso(1, "Largo", TipoProceso.CPU_BOUND, 1, 10, 50, 5);
        int buffer = c.crearBuffer(2, 2).getId();   // buffer alojado en el computador 2
        c.crearProcesoBuffer(1, "Prod", TipoProceso.PRODUCTOR, 1, 10, 1000, buffer, 2, 6);
        c.crearProcesoBuffer(2, "Cons", TipoProceso.CONSUMIDOR, 1, 10, 1000, buffer, 2, 6);
        for (int i = 0; i < 400; i++) {
            c.avanzarCiclo();
        }
        ReporteMetricas r = rec.generarReporte();
        verificar("un proceso vencido en el computador 1", r.getPorComputador()[0].getVencidos() == 1);
        verificar("cumplimiento menor a 100 %", r.getPorComputador()[0].getCumplimientoDeadlines() < 1.0);
        verificar("hay un buffer en el reporte", r.getBuffers().length == 1);
        verificar("se produjeron 6 elementos", r.getBuffers()[0].getProducidos() == 6);
        verificar("se consumieron 6 elementos", r.getBuffers()[0].getConsumidos() == 6);
        verificar("2 procesos con buffer", r.getProcesosConBuffer() == 2);
        verificar("el acceso remoto produjo espera (" + r.getBloqueoRemotoPromedio() + ")",
                r.getBloqueoRemotoPromedio() > 0);
        verificar("la serie se limita por el intervalo de muestreo", rec.getSerie(1).tamano() <= 400 / 5);
    }

    private static void probarGrafica() {
        System.out.println("--- Grafica ---");
        Cluster c = nuevoCluster();
        RecolectorMetricas rec = new RecolectorMetricas(c, 1, 100);
        c.getReloj().registrar(rec);
        c.crearProceso(1, "A", TipoProceso.CPU_BOUND, 1, 10, 30, 1000);
        for (int i = 0; i < 20; i++) {
            c.avanzarCiclo();
        }
        PanelGraficaUtilizacion panel = new PanelGraficaUtilizacion(rec);
        panel.setSize(600, 300);
        BufferedImage img = new BufferedImage(600, 300, BufferedImage.TYPE_INT_RGB);
        boolean ok = true;
        try {
            panel.paint(img.getGraphics());
        } catch (RuntimeException e) {
            ok = false;
        }
        verificar("la grafica se dibuja sin errores", ok);
        boolean hayColor = false;
        for (int x = 60; x < 580 && !hayColor; x++) {
            for (int y = 30; y < 266 && !hayColor; y++) {
                int rgb = img.getRGB(x, y) & 0xFFFFFF;
                hayColor = rgb == 0x1F77B4;
            }
        }
        verificar("la linea del computador 1 aparece en la imagen", hayColor);
        VistaMetricas vista = new VistaMetricas(rec);
        vista.refrescar();
        vista.detener();
        verificar("la vista de metricas se refresca", true);
    }
}
