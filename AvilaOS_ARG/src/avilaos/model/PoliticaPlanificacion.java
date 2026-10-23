package avilaos.model;

/**
 * Politicas de planificacion disponibles. Este enum es la lista que ve el usuario;
 * el comportamiento de cada politica se define con una interfaz (Fase 5).
 */
public enum PoliticaPlanificacion {

    FCFS("FCFS"),
    EDF("EDF"),
    ROUND_ROBIN("Round Robin"),
    PRIORIDADES("Prioridades");

    private final String nombreVisible;

    PoliticaPlanificacion(String nombreVisible) {
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