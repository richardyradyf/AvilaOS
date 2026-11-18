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
 * Lo que el nucleo necesita para atender a un proceso de E/S: armar su solicitud al
 * disco (con el bloque destino y los bloques que calcula el sistema de archivos) y
 * ponerla en la cola del disco. En la integracion lo implementa una clase que une el
 * sistema de archivos con el disco; en las pruebas, una clase falsa.
 * 
 */

public interface PuertoEs {

    /**
     * Arma la solicitud del proceso y la manda a la cola del disco.
     *
     * @return la solicitud que se encolo
     */
    SolicitudDisco pedirDisco(Pcb proceso, long ciclo);
}