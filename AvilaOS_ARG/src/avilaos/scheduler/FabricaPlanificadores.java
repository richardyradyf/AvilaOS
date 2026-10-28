package avilaos.scheduler;

import avilaos.model.PoliticaPlanificacion;

/**
 * @author andre
 * 
 * Crea el planificador que corresponde a cada valor del enum. La interfaz grafica
 * y el cargador de configuracion usan esta clase en vez de conocer cada politica.
 */
public final class FabricaPlanificadores {

    /** Quantum que se usa si la configuracion no indica uno. */
    public static final int QUANTUM_POR_DEFECTO = 4;

    private FabricaPlanificadores() {
    }

    public static Planificador crear(PoliticaPlanificacion politica, int quantum) {
        if (politica == null) {
            throw new IllegalArgumentException("La politica no puede ser nula");
        }
        switch (politica) {
            case FCFS:
                return new PlanificadorFcfs();
            case EDF:
                return new PlanificadorEdf();
            case ROUND_ROBIN:
                return new PlanificadorRoundRobin(quantum);
            case PRIORIDADES:
                return new PlanificadorPrioridades();
            default:
                throw new IllegalArgumentException("Politica no soportada: " + politica);
        }
    }

    public static Planificador crear(PoliticaPlanificacion politica) {
        return crear(politica, QUANTUM_POR_DEFECTO);
    }
}