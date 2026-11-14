/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.disco;

/**
 *
 * @author richard
 * 
 * Aviso del disco al sistema operativo: cuando el disco termina de atender una
 * solicitud, llama a este metodo. El nucleo lo implementa para ejecutar la
 * operacion y despertar al proceso (Bloqueado -> Listo, o
 * Bloqueado/Suspendido -> Listo/Suspendido).
 */
public interface OyenteDisco {

    /**
     * @param solicitud la solicitud que se acaba de atender
     * @param ciclo     ciclo del reloj en que termino
     */
    void solicitudAtendida(SolicitudDisco solicitud, long ciclo);
}
