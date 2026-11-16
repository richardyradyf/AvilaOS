package avilaos.model;

/**
 * Motivo por el que un proceso esta en estado BLOQUEADO. La interfaz lo muestra
 * junto a cada proceso bloqueado. NINGUNO se usa cuando el proceso no esta bloqueado.
 */
public enum MotivoBloqueo {

    NINGUNO("Ninguno"),
    /** Espera que termine una operacion de E/S. */
    ESPERA_ES("Esperando E/S"),
    /** Espera en un semaforo: otro proceso esta en la region critica. */
    REGION_CRITICA_OCUPADA("Regi\u00f3n cr\u00edtica ocupada");

    private final String nombreVisible;

    MotivoBloqueo(String nombreVisible) {
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