package avilaos.model;

/**
 * Estados por los que pasa un proceso de AvilaOS.
 * NUEVO -> LISTO -> EJECUCION -> TERMINADO, con EJECUCION <-> BLOQUEADO -> LISTO.
 */
public enum EstadoProceso {

    NUEVO("Nuevo"),
    LISTO("Listo"),
    EJECUCION("Ejecuci\u00f3n"),
    BLOQUEADO("Bloqueado"),
    TERMINADO("Terminado");

    private final String nombreVisible;

    EstadoProceso(String nombreVisible) {
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