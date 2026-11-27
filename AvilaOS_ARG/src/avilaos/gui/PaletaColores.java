package avilaos.gui;

import java.awt.Color;

/**
 * @author andre
 *
 * Paleta de colores consistente para la interfaz grafica de AvilaOS (Proyecto 2):
 * - Representacion del disco, cabezal y bloques.
 * - Estados del disco y operaciones de E/S.
 * - Mapeo determinista de colores por archivo para sincronizar el mapa de bloques
 *   con la tabla de asignacion de archivos.
 */
public final class PaletaColores {

    private PaletaColores() {
    }

    // Cabezal y cursor del disco
    public static final Color CABEZAL = new Color(0xFF9800);            // Naranja brillante
    public static final Color CABEZAL_BORDE = new Color(0xE65100);      // Naranja oscuro
    public static final Color CABEZAL_FONDO = new Color(0xFFF3E0);      // Naranja muy claro

    // Bloques del mapa
    public static final Color BLOQUE_LIBRE = new Color(0xEEEEEE);       // Gris claro
    public static final Color BLOQUE_ARCHIVO = new Color(0x2196F3);     // Azul archivo
    public static final Color BLOQUE_SWAP = new Color(0x9C27B0);        // Morado swap
    public static final Color BLOQUE_BORDE = new Color(0xBDBDBD);       // Gris borde

    // Estados del disco
    public static final Color ESTADO_OCIOSO = new Color(0x4CAF50);       // Verde
    public static final Color ESTADO_MOVIENDO = new Color(0xFF9800);     // Naranja
    public static final Color ESTADO_TRANSFIRIENDO = new Color(0x2196F3);// Azul

    // Fondos y bordes generales
    public static final Color FONDO_PANEL = new Color(0xFAFAFA);
    public static final Color BORDE_SUAVE = new Color(0xE0E0E0);
    public static final Color TEXTO_MUTED = new Color(0x757575);

    // Paleta de colores distintivos y legibles para archivos individuales
    private static final Color[] COLORES_ARCHIVOS = {
        new Color(0x1976D2), // Azul
        new Color(0x388E3C), // Verde
        new Color(0xD32F2F), // Rojo oscuro
        new Color(0x7B1FA2), // Purpura
        new Color(0x0097A7), // Cian
        new Color(0xC2185B), // Rosa
        new Color(0x0288D1), // Azul claro
        new Color(0x5D4037), // Marron
        new Color(0x455A64), // Azul grisaceo
        new Color(0x689F38), // Verde lima
        new Color(0x512DA8), // Indigo
        new Color(0x00796B)  // Verde azulado
    };

    /**
     * Devuelve el color asociado a un bloque segun su ocupante (ruta de archivo, swap o libre).
     * Garantiza consistencia visual entre el mapa visual de bloques del disco y la tabla de
     * asignacion de archivos del sistema de archivos.
     *
     * @param ruta ruta del archivo ("/..."), ocupante de swap ("swap:...") o null si esta libre
     * @return Color correspondiente para pintar el bloque o la fila en la tabla
     */
    public static Color colorDe(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return BLOQUE_LIBRE;
        }
        String limpia = ruta.trim();
        if (limpia.toLowerCase().startsWith("swap")) {
            return BLOQUE_SWAP;
        }
        int hash = limpia.hashCode() & 0x7FFFFFFF;
        return COLORES_ARCHIVOS[hash % COLORES_ARCHIVOS.length];
    }
}
