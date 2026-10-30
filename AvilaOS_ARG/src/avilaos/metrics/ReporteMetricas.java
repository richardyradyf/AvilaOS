package avilaos.metrics;
/**
 * @author Gianm
/** Todas las metricas del sistema en un ciclo: por computador, del sistema completo y de los buffers. */
public final class ReporteMetricas {

    private final long ciclo;
    private final MetricasComputador[] porComputador;
    private final MetricasComputador sistema;
    private final MetricasBuffer[] buffers;
    private final int procesosConBuffer;
    private final double bloqueoSemaforosPromedio;
    private final double bloqueoRemotoPromedio;

    public ReporteMetricas(long ciclo, MetricasComputador[] porComputador, MetricasComputador sistema,
                           MetricasBuffer[] buffers, int procesosConBuffer,
                           double bloqueoSemaforosPromedio, double bloqueoRemotoPromedio) {
        this.ciclo = ciclo;
        this.porComputador = porComputador;
        this.sistema = sistema;
        this.buffers = buffers;
        this.procesosConBuffer = procesosConBuffer;
        this.bloqueoSemaforosPromedio = bloqueoSemaforosPromedio;
        this.bloqueoRemotoPromedio = bloqueoRemotoPromedio;
    }

    public long getCiclo() {
        return ciclo;
    }

    public MetricasComputador[] getPorComputador() {
        return porComputador;
    }

    public MetricasComputador getSistema() {
        return sistema;
    }

    public MetricasBuffer[] getBuffers() {
        return buffers;
    }

    /** Cantidad de productores y consumidores del sistema. */
    public int getProcesosConBuffer() {
        return procesosConBuffer;
    }

    /** Ciclos promedio que un productor o consumidor paso bloqueado en un semaforo (NaN si no hay). */
    public double getBloqueoSemaforosPromedio() {
        return bloqueoSemaforosPromedio;
    }

    /** Ciclos promedio que un productor o consumidor paso esperando un acceso remoto (NaN si no hay). */
    public double getBloqueoRemotoPromedio() {
        return bloqueoRemotoPromedio;
    }
}