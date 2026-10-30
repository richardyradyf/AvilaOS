package avilaos.metrics;

/**
 * @author Gianm
 * Metricas de un computador (o del sistema completo si idComputador vale 0). Es una
 * foto inmutable tomada en un ciclo. Los valores que no se pueden calcular todavia
 * (por ejemplo, el tiempo de respuesta sin procesos que hayan empezado) valen NaN.
 */
public final class MetricasComputador {

    private final int idComputador;
    private final long ciclos;
    private final int procesosTotales;
    private final int completados;
    private final int vencidos;
    private final double throughput;
    private final double utilizacion;
    private final double tiempoRespuestaPromedio;
    private final double cumplimientoDeadlines;
    private final double equidad;

    public MetricasComputador(int idComputador, long ciclos, int procesosTotales, int completados,
                              int vencidos, double throughput, double utilizacion,
                              double tiempoRespuestaPromedio, double cumplimientoDeadlines,
                              double equidad) {
        this.idComputador = idComputador;
        this.ciclos = ciclos;
        this.procesosTotales = procesosTotales;
        this.completados = completados;
        this.vencidos = vencidos;
        this.throughput = throughput;
        this.utilizacion = utilizacion;
        this.tiempoRespuestaPromedio = tiempoRespuestaPromedio;
        this.cumplimientoDeadlines = cumplimientoDeadlines;
        this.equidad = equidad;
    }

    /** 0 si son las metricas del sistema completo. */
    public int getIdComputador() {
        return idComputador;
    }

    public long getCiclos() {
        return ciclos;
    }

    public int getProcesosTotales() {
        return procesosTotales;
    }

    /** Procesos que terminaron todas sus instrucciones (sin morir por deadline). */
    public int getCompletados() {
        return completados;
    }

    /** Procesos terminados porque su deadline llego a 0. */
    public int getVencidos() {
        return vencidos;
    }

    /** Procesos completados por ciclo de reloj. */
    public double getThroughput() {
        return throughput;
    }

    /** Fraccion de ciclos con la CPU ejecutando instrucciones, de 0.0 a 1.0. */
    public double getUtilizacion() {
        return utilizacion;
    }

    /** Promedio de (primera ejecucion - creacion), en ciclos. */
    public double getTiempoRespuestaPromedio() {
        return tiempoRespuestaPromedio;
    }

    /** Completados / (completados + vencidos), de 0.0 a 1.0. */
    public double getCumplimientoDeadlines() {
        return cumplimientoDeadlines;
    }

    /** Indice de equidad de Jain sobre la fraccion de su vida que cada proceso uso la CPU. */
    public double getEquidad() {
        return equidad;
    }
}