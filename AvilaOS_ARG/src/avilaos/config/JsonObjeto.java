/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.config;

import avilaos.util.Lista;

/** 
 * @author richard
 * 
 * Objeto JSON ya leido: pares clave/valor en orden. Los valores son String, Long, Double, Boolean, null, JsonObjeto o Lista.
 */
public final class JsonObjeto {

    private final Lista<String> claves = new Lista<>();
    private final Lista<Object> valores = new Lista<>();

    void poner(String clave, Object valor) {
        if (claves.contiene(clave)) {
            throw new EntradaInvalidaException("JSON: la clave \"" + clave + "\" está repetida");
        }
        claves.agregarAlFinal(clave);
        valores.agregarAlFinal(valor);
    }

    public boolean tiene(String clave) {
        return claves.contiene(clave);
    }

    /** Valor de la clave, o null si no existe. */
    public Object obtener(String clave) {
        int i = claves.indiceDe(clave);
        return i < 0 ? null : valores.obtener(i);
    }

    public int tamano() {
        return claves.tamano();
    }

    public String claveEn(int indice) {
        return claves.obtener(indice);
    }
}