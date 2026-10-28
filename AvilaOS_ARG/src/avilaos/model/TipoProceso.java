package avilaos.model;

/** Tipos de proceso que el usuario puede crear. */
public enum TipoProceso {

    CPU_BOUND("CPU bound"),
    IO_BOUND("I/O bound"),
    PRODUCTOR("Productor"),
    CONSUMIDOR("Consumidor");

    private final String nombreVisible;

    TipoProceso(String nombreVisible) {
        this.nombreVisible = nombreVisible;
    }

    public String getNombreVisible() {
        return nombreVisible;
    }

    @Override
    public String toString() {
        return nombreVisible;
    }
}