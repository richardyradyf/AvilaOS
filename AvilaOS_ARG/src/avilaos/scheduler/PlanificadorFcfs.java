/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.scheduler;

import avilaos.model.Pcb;
import avilaos.model.PoliticaPlanificacion;
import avilaos.util.Cola;

/**
 *
 * @author richard
 */


/** First Come First Served: el primero en llegar es el primero en ejecutarse. No apropiativa. */
public class PlanificadorFcfs implements Planificador {

    @Override
    public PoliticaPlanificacion getTipo() {
        return PoliticaPlanificacion.FCFS;
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
        return false;
    }

    @Override
    public void reordenar(Cola<Pcb> listos) {
    }
}