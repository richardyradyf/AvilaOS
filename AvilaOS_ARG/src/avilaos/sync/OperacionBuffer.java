package avilaos.sync;

import avilaos.model.ModoEjecucion;

/**
 * Cada instruccion del programa de un productor o consumidor. Las operaciones sobre
 * semaforos y buffer se ejecutan en modo sistema operativo; producir y consumir son
 * trabajo del programa de usuario.
 *
 * Semaforos del buffer (Stallings): s = mutex, n = elementos, e = espacios libres.
 */
public enum OperacionBuffer {

    PRODUCIR("producir", false),
    SEM_WAIT_ESPACIOS("semWait(e)", true),
    SEM_WAIT_MUTEX("semWait(s)", true),
    ANADIR("añadir al buffer", true),
    SEM_SIGNAL_MUTEX("semSignal(s)", true),
    SEM_SIGNAL_ELEMENTOS("semSignal(n)", true),
    SEM_WAIT_ELEMENTOS("semWait(n)", true),
    EXTRAER("extraer del buffer", true),
    SEM_SIGNAL_ESPACIOS("semSignal(e)", true),
    CONSUMIR("consumir", false);

    private final String descripcion;
    private final boolean modoSistemaOperativo;

    OperacionBuffer(String descripcion, boolean modoSistemaOperativo) {
        this.descripcion = descripcion;
        this.modoSistemaOperativo = modoSistemaOperativo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean esModoSistemaOperativo() {
        return modoSistemaOperativo;
    }

    public ModoEjecucion getModo() {
        return modoSistemaOperativo ? ModoEjecucion.SISTEMA_OPERATIVO : ModoEjecucion.USUARIO;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}

