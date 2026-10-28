package avilaos.model;

/**
 * Modo en el que trabaja el procesador en un ciclo dado: ejecutando un programa
 * de usuario o ejecutando codigo del sistema operativo (semWait, semSignal,
 * insertar o extraer del buffer, planificacion).
 */
public enum ModoEjecucion {

    USUARIO("Usuario"),
    SISTEMA_OPERATIVO("Sistema operativo");

    private final String nombreVisible;

    ModoEjecucion(String nombreVisible) {
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