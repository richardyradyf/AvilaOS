/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.scheduler;

import avilaos.model.Pcb;
import avilaos.model.PoliticaPlanificacion;
import avilaos.util.Cola;

/**
 * @author richard
 * 
 * Politica de planificacion (patron estrategia). Agregar una politica nueva no
 * requiere tocar el nucleo: basta con escribir otra clase que implemente esta
 * interfaz.
 *
 * La cola de listos se mantiene ordenada al insertar (alEntrarAListos), asi que
 * seleccionar solo saca el primero. Si la politica cambia en tiempo de ejecucion,
 * el nucleo llama a reordenar.
 */
public interface Planificador {

    PoliticaPlanificacion getTipo();

    void alEntrarAListos(Cola<Pcb> listos, Pcb proceso);

    Pcb seleccionar(Cola<Pcb> listos);

    /**
     * Indica si hay que sacar de la CPU al proceso actual (fin de quantum o llegada
     * de un proceso con mas prioridad). Se consulta al final de cada ciclo.
     *
     * @param ciclosEnCpu ciclos consecutivos que el proceso lleva en la CPU
     */
    boolean debeExpulsar(Pcb enCpu, int ciclosEnCpu, Cola<Pcb> listos);

    void reordenar(Cola<Pcb> listos);
}