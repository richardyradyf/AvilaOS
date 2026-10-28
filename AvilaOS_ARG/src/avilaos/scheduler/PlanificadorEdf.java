package avilaos.scheduler;

import avilaos.model.Pcb;
import avilaos.model.PoliticaPlanificacion;
import avilaos.util.Cola;
import avilaos.util.Comparador;

/**
 * Earliest Deadline First: se ejecuta primero el proceso cuyo limite (ciclo de
 * creacion + deadline) esta mas cerca. Puede ser apropiativa: si llega a listos un
 * proceso con un limite estrictamente menor que el del proceso en CPU, lo expulsa.
 * 
 * @author andre
 */
public class PlanificadorEdf implements Planificador {

    private static final Comparador<Pcb> POR_LIMITE =
            (a, b) -> Long.compare(a.getCicloLimite(), b.getCicloLimite());

    private final boolean apropiativo;

    public PlanificadorEdf() {
        this(true);
    }

    public PlanificadorEdf(boolean apropiativo) {
        this.apropiativo = apropiativo;
    }

    @Override
    public PoliticaPlanificacion getTipo() {
        return PoliticaPlanificacion.EDF;
    }

    @Override
    public void alEntrarAListos(Cola<Pcb> listos, Pcb proceso) {
        listos.encolarOrdenado(proceso, POR_LIMITE);
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
        return primero != null && primero.getCicloLimite() < enCpu.getCicloLimite();
    }

    @Override
    public void reordenar(Cola<Pcb> listos) {
        listos.ordenar(POR_LIMITE);
    }
}