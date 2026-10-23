package avilaos.model;

/**
 * Motivo por el que un proceso esta en estado BLOQUEADO. La interfaz lo muestra
 * junto a cada proceso bloqueado. NINGUNO se usa cuando el proceso no esta bloqueado.
 */
public enum MotivoBloqueo {

    NINGUNO("Ninguno"),
    /** Espera que termine una operacion de E/S (procesos I/O bound). */
    ESPERA_ES("Esperando E/S"),
    /** Semaforo s (mutex) del buffer: otro proceso esta en la region critica. */
    REGION_CRITICA_OCUPADA("Regi\u00f3n cr\u00edtica ocupada"),
    /** Semaforo e (espacios libres): un productor no encuentra espacio. */
    BUFFER_LLENO("Buffer lleno"),
    /** Semaforo n (elementos): un consumidor no encuentra elementos. */
    BUFFER_VACIO("Buffer vac\u00edo"),
    /** Latencia de red al acceder a un buffer alojado en otro computador. */
    ACCESO_REMOTO("Acceso remoto al buffer");

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