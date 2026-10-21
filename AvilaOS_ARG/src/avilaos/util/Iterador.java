/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.util;


public interface Iterador<T> {

    /** Indica si quedan elementos por recorrer. */
    boolean tieneSiguiente();

    /** Devuelve el elemento actual y avanza al siguiente. */
    T siguiente();
}
