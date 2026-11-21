package avilaos.gui;

import java.awt.Color;

/**
 * @author andre
 *
 * Paleta de colores consistente para la interfaz grafica de AvilaOS (Proyecto 2):
 * - Representacion del disco, cabezal y bloques.
 * - Estados del disco y operaciones de E/S.
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
}
