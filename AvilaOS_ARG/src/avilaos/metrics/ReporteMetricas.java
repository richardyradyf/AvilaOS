package avilaos.metrics;


/**
 * @author Gianm
/** Todas las metricas del sistema en un ciclo: por computador, del sistema completo y de los buffers. */

public final class ReporteMetricas {

    private final long ciclo;
    private final MetricasComputador[] porComputador;
    private final MetricasComputador sistema;

    public ReporteMetricas(long ciclo, MetricasComputador[] porComputador, MetricasComputador sistema) {
        this.ciclo = ciclo;
        this.porComputador = porComputador;
        this.sistema = sistema;
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
}