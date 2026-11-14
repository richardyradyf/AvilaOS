/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.model;

/**
 *
 * @author richard
 * 
 * Politicas de planificacion de disco. Este enum es la lista que ve el usuario; el
 * comportamiento de cada una se define con la interfaz de planificacion de disco.
 */
public enum PoliticaDisco {

    FIFO("FIFO"),
    SSTF("SSTF"),
    SCAN("SCAN"),
    C_SCAN("C-SCAN");

    private final String nombreVisible;

    PoliticaDisco(String nombreVisible) {
        this.nombreVisible = nombreVisible;
    }

    public String getNombreVisible() {
        return nombreVisible;
    }

    /**
     * Convierte el texto del JSON ("FIFO", "SSTF", "SCAN", "C-SCAN") en la politica.
     * No distingue mayusculas y acepta tambien "C_SCAN" y "CSCAN".
     *
     */
    public static PoliticaDisco desdeJson(String texto) {
        if (texto != null) {
            String limpio = texto.trim().toUpperCase().replace("-", "").replace("_", "");
            for (PoliticaDisco p : values()) {
                if (p.name().replace("_", "").equals(limpio)) {
                    return p;
                }
            }
        }
        throw new IllegalArgumentException("Pol\u00edtica de disco no v\u00e1lida: '" + texto
                + "' (se esperaba FIFO, SSTF, SCAN o C-SCAN)");
    }

    @Override
    public String toString() {
        return nombreVisible;
    }
}
