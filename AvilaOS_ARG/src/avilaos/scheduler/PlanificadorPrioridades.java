package avilaos.scheduler;

import avilaos.model.Pcb;
import avilaos.model.PoliticaPlanificacion;
import avilaos.util.Cola;
import avilaos.util.Comparador;

/**
 * Planificacion por prioridades: el numero mas bajo es la prioridad mas alta
 * (1 es la maxima). Con igual prioridad se respeta el orden de llegada.
 * Puede ser apropiativa: un proceso de prioridad estrictamente mayor expulsa al
 * que esta en la CPU.
 * 
 * @author andre
 */
public class PlanificadorPrioridades implements Planificador {

    private static final Comparador<Pcb> POR_PRIORIDAD =
            (a, b) -> Integer.compare(a.getPrioridad(), b.getPrioridad());

    private final boolean apropiativo;

    public PlanificadorPrioridades() {
        this(true);
    }

    public PlanificadorPrioridades(boolean apropiativo) {
        this.apropiativo = apropiativo;
    }

    @Override
    public PoliticaPlanificacion getTipo() {
        return PoliticaPlanificacion.PRIORIDADES;
    }

    @Override
    public void alEntrarAListos(Cola<Pcb> listos, Pcb proceso) {
        listos.encolarOrdenado(proceso, POR_PRIORIDAD);
    }

    @Override
    public Pcb seleccionar(Cola<Pcb> listos) {
        return listos.desencolar();
    }

    @Override
    public boolean debeExpulsar(Pcb enCpu, int ciclosEnCpu, Cola<Pcb> listos) {
        if (!apropiativo) {
            return false;
        }
        Pcb primero = listos.verPrimero();
        return primero != null && primero.getPrioridad() < enCpu.getPrioridad();
    }

    @Override
    public void reordenar(Cola<Pcb> listos) {
        listos.ordenar(POR_PRIORIDAD);
    }
}