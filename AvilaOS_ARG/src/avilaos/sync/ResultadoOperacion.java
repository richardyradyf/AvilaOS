package avilaos.sync;

import avilaos.model.MotivoBloqueo;
import avilaos.model.Pcb;

/**
 * Resultado de ejecutar una operacion sobre un buffer: el proceso puede continuar
 * (quizas despertando a otro) o se bloqueo por un motivo concreto.
 */
public final class ResultadoOperacion {

    private static final ResultadoOperacion CONTINUA = new ResultadoOperacion(true, null, null);

    private final boolean continua;
    private final Pcb despertado;
    private final MotivoBloqueo motivo;

    private ResultadoOperacion(boolean continua, Pcb despertado, MotivoBloqueo motivo) {
        this.continua = continua;
        this.despertado = despertado;
        this.motivo = motivo;
    }

    static ResultadoOperacion continuar() {
        return CONTINUA;
    }

    static ResultadoOperacion continuarDespertando(Pcb despertado) {
        return despertado == null ? CONTINUA : new ResultadoOperacion(true, despertado, null);
    }

    static ResultadoOperacion bloquear(MotivoBloqueo motivo) {
        return new ResultadoOperacion(false, null, motivo);
    }

    /** El proceso puede seguir con su siguiente instruccion. */
    public boolean continua() {
        return continua;
    }

    /** El proceso quedo en la cola de un semaforo y debe pasar a BLOQUEADO. */
    public boolean bloqueado() {
        return !continua;
    }

    /** Proceso a desbloquear como consecuencia de un semSignal, o null. */
    public Pcb getDespertado() {
        return despertado;
    }

    /** Motivo del bloqueo; solo tiene valor si bloqueado() es true. */
    public MotivoBloqueo getMotivo() {
        return motivo;
    }
}

