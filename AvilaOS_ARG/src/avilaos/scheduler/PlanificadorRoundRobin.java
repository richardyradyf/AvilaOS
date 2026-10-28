package avilaos.scheduler;

import avilaos.model.Pcb;
import avilaos.model.PoliticaPlanificacion;
import avilaos.util.Cola;

/**
 * Round Robin: cada proceso usa la CPU como maximo un quantum de ciclos y, si
 * hay otro esperando, vuelve al final de la cola de listos.
 * 
 * @author andre
 */
public class PlanificadorRoundRobin implements Planificador {

    private final int quantum;

    /** @param quantum ciclos consecutivos permitidos en la CPU (1 o mas) */
    public PlanificadorRoundRobin(int quantum) {
        if (quantum < 1) {
            throw new IllegalArgumentException("El quantum debe ser de 1 ciclo o mas");
        }
        this.quantum = quantum;
    }

    public int getQuantum() {
        return quantum;
    }

    @Override
    public PoliticaPlanificacion getTipo() {
        return PoliticaPlanificacion.ROUND_ROBIN;
    }

    @Override
    public void alEntrarAListos(Cola<Pcb> listos, Pcb proceso) {
        listos.encolar(proceso);
    }

    @Override
    public Pcb seleccionar(Cola<Pcb> listos) {
        return listos.desencolar();
    }

    @Override
    public boolean debeExpulsar(Pcb enCpu, int ciclosEnCpu, Cola<Pcb> listos) {
        return ciclosEnCpu >= quantum && !listos.estaVacia();
    }

    @Override
    public void reordenar(Cola<Pcb> listos) {
        // Round Robin conserva el orden actual de la cola
    }
}