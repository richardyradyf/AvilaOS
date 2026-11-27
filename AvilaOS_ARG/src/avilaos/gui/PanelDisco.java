package avilaos.gui;

import avilaos.disco.DireccionCabezal;
import avilaos.disco.Disco;
import avilaos.disco.EstadoDisco;
import avilaos.fs.MapaBloques;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * @author andre
 *
 * Panel visual del disco simulado (SD):
 * - Muestra la posicion actual del cabezal, su direccion (↑/↓) y el estado (OCIOSO, MOVIENDO, TRANSFIRIENDO).
 * - Grilla visual con todos los bloques numerados del disco (archivos y swap).
 * - Distingue bloques libres, ocupados por archivos y ocupados por swap.
 * - Resalta con borde e indicador naranja el bloque donde esta posicionado el cabezal.
 */
public class PanelDisco extends JPanel {

    private final Disco disco;
    private final MapaBloques mapaBloques;

    private final JLabel etiquetaCabezal = new JLabel("Cabezal: 0");
    private final JLabel etiquetaDireccion = new JLabel("Dirección: ↑");
    private final JLabel etiquetaEstado = new JLabel("Estado: Ocioso");
    private final JLabel etiquetaSolicitud = new JLabel("Solicitud: ninguna");
    private final LienzoMapaBloques lienzo;

    public PanelDisco(Disco disco, MapaBloques mapaBloques) {
        super(new BorderLayout(6, 6));
        if (disco == null || mapaBloques == null) {
            throw new IllegalArgumentException("Disco y MapaBloques no pueden ser nulos");
        }
        this.disco = disco;
        this.mapaBloques = mapaBloques;
        this.lienzo = new LienzoMapaBloques();

        setBorder(BorderFactory.createTitledBorder("Disco simulado y mapa de bloques"));

        // Barra superior con informacion del cabezal y estado
        JPanel barraInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        barraInfo.setBackground(PaletaColores.FONDO_PANEL);

        Font negrita = new Font("SansSerif", Font.BOLD, 12);
        etiquetaCabezal.setFont(negrita);
        etiquetaDireccion.setFont(negrita);
        etiquetaEstado.setFont(negrita);

        barraInfo.add(etiquetaCabezal);
        barraInfo.add(etiquetaDireccion);
        barraInfo.add(etiquetaEstado);
        barraInfo.add(etiquetaSolicitud);

        add(barraInfo, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(lienzo);
        scroll.setBorder(BorderFactory.createLineBorder(PaletaColores.BORDE_SUAVE));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // Leyenda inferior
        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 2));
        leyenda.add(crearMuestra(PaletaColores.BLOQUE_LIBRE, "Libre"));
        leyenda.add(crearMuestra(PaletaColores.BLOQUE_ARCHIVO, "Archivo"));
        leyenda.add(crearMuestra(PaletaColores.BLOQUE_SWAP, "Swap"));
        leyenda.add(crearMuestra(PaletaColores.CABEZAL, "Cabezal actual"));
        add(leyenda, BorderLayout.SOUTH);

        actualizar();
    }

    private JPanel crearMuestra(Color color, String texto) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JPanel cuadro = new JPanel();
        cuadro.setPreferredSize(new Dimension(14, 14));
        cuadro.setBackground(color);
        cuadro.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        p.add(cuadro);
        p.add(new JLabel(texto));
        return p;
    }

    /**
     * Refresca las etiquetas y repinta el mapa de bloques. Seguro para invocar desde Swing.
     */
    public void actualizar() {
        int posCabezal = disco.getCabezal();
        DireccionCabezal dir = disco.getDireccion();
        EstadoDisco est = disco.getEstado();
        var sol = disco.getSolicitudActual();

        etiquetaCabezal.setText("Cabezal: bloque " + posCabezal);
        etiquetaDireccion.setText("Dirección: " + (dir != null ? dir.getSimbolo() : "-"));
        etiquetaEstado.setText("Estado: " + (est != null ? est.getDescripcion() : "-"));
        etiquetaSolicitud.setText("Solicitud: " + (sol != null ? sol.getId() + " (" + sol.getOperacion() + ")" : "ninguna"));

        lienzo.repaint();
    }

    private class LienzoMapaBloques extends JPanel {

        private static final int TAMANO_CELDA = 22;
        private static final int MARGEN = 4;
        private static final int COLUMNAS = 20;

        LienzoMapaBloques() {
            setBackground(Color.WHITE);
        }

        @Override
        public Dimension getPreferredSize() {
            int total = mapaBloques.getTotalBloques();
            int filas = (int) Math.ceil((double) total / COLUMNAS);
            int ancho = COLUMNAS * (TAMANO_CELDA + MARGEN) + MARGEN * 2;
            int alto = filas * (TAMANO_CELDA + MARGEN) + MARGEN * 2;
            return new Dimension(Math.max(ancho, 400), Math.max(alto, 200));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int total = mapaBloques.getTotalBloques();
            int cabezalActual = disco.getCabezal();

            for (int i = 0; i < total; i++) {
                int fila = i / COLUMNAS;
                int col = i % COLUMNAS;
                int x = MARGEN + col * (TAMANO_CELDA + MARGEN);
                int y = MARGEN + fila * (TAMANO_CELDA + MARGEN);

                String ocupante = mapaBloques.getOcupante(i);
                boolean esLibre = (ocupante == null || ocupante.isBlank());
                Color fondo = PaletaColores.colorDe(ocupante);

                g2.setColor(fondo);
                g2.fillRect(x, y, TAMANO_CELDA, TAMANO_CELDA);

                // Borde regular
                g2.setColor(PaletaColores.BLOQUE_BORDE);
                g2.drawRect(x, y, TAMANO_CELDA, TAMANO_CELDA);

                // Si el cabezal esta en este bloque, se resalta
                if (i == cabezalActual) {
                    g2.setColor(PaletaColores.CABEZAL_BORDE);
                    g2.drawRect(x - 1, y - 1, TAMANO_CELDA + 2, TAMANO_CELDA + 2);
                    g2.setColor(PaletaColores.CABEZAL);
                    g2.fillRect(x + 2, y + 2, TAMANO_CELDA - 4, TAMANO_CELDA - 4);
                }

                // Numero de bloque
                g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
                g2.setColor(i == cabezalActual || !esLibre ? Color.WHITE : Color.DARK_GRAY);
                String numStr = String.valueOf(i);
                int strAncho = g2.getFontMetrics().stringWidth(numStr);
                g2.drawString(numStr, x + (TAMANO_CELDA - strAncho) / 2, y + 15);
            }

            g2.dispose();
        }
    }
}
