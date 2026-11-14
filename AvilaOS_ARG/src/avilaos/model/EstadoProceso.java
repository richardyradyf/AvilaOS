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
    LISTO_SUSPENDIDO("Listo/Suspendido"),
    BLOQUEADO_SUSPENDIDO("Bloqueado/Suspendido"),
    TERMINADO("Terminado");

    private final String nombreVisible;

    EstadoProceso(String nombreVisible) {
        this.nombreVisible = nombreVisible;
    }

    public String getNombreVisible() {
        return nombreVisible;
    }

    /** true si el proceso esta en el area de swap (no ocupa RAM). */
    public boolean estaSuspendido() {
        return this == LISTO_SUSPENDIDO || this == BLOQUEADO_SUSPENDIDO;
    }

    @Override
    public String toString() {
        return nombreVisible;
    }
}