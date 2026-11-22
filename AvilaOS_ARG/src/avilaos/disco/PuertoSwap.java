/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package avilaos.disco;

import avilaos.model.Pcb;

/**
 *
 * @author richard
 * 
 * Lo que el planificador a mediano plazo necesita del area de intercambio: reservar
 * los bloques de un proceso, mandar su escritura a la cola del disco y liberarlos.
 */
public interface PuertoSwap {

    /**
     * Reserva en el swap un bloque por cada unidad de memoria del proceso y pone la
     * solicitud SUSPENDER en la cola del disco.
     *
     * @return la solicitud encolada, o null si no hay swap libre suficiente
     */
    SolicitudDisco escribirEnSwap(Pcb proceso, long ciclo);

    /** Pone en la cola del disco la solicitud REACTIVAR que lee al proceso desde el swap. */
    SolicitudDisco leerDeSwap(Pcb proceso, long ciclo);

    /** Libera los bloques de swap del proceso, si tiene. */
    void liberarSwap(Pcb proceso);
}
