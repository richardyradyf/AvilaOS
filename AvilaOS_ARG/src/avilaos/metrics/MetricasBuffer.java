package avilaos.metrics;

/**
 * @author Gianm
/** Foto de las metricas de un buffer: cuanto se produjo y se consumio en el. */
public final class MetricasBuffer {

    private final int id;
    private final int idAnfitrion;
    private final int capacidad;
    private final int elementos;
    private final long producidos;
    private final long consumidos;

    public MetricasBuffer(int id, int idAnfitrion, int capacidad, int elementos,
                          long producidos, long consumidos) {
        this.id = id;
        this.idAnfitrion = idAnfitrion;
        this.capacidad = capacidad;
        this.elementos = elementos;
        this.producidos = producidos;
        this.consumidos = consumidos;
    }

    public int getId() {
        return id;
    }

    public String getEtiqueta() {
        return "B" + id;
    }

    public int getIdAnfitrion() {
        return idAnfitrion;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public int getElementos() {
        return elementos;
    }

    public long getProducidos() {
        return producidos;
    }

    public long getConsumidos() {
        return consumidos;
    }
}
