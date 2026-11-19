package avilaos.gui;

import avilaos.metrics.MetricasDisco;
import avilaos.metrics.SerieTemporal;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * @author andre
 *
 * Grafica de lineas del desplazamiento acumulado del cabezal a lo largo del tiempo
 * (Proyecto 2 - Dia 7):
 * - Eje X: ciclo de reloj del sistema.
 * - Eje Y: cilindros/bloques de desplazamiento acumulados.
 * - Dibujada a mano con Graphics2D y antialiasing, siguiendo el estilo de AvilaOS.
 */
public class PanelGraficaDisco extends JPanel {

    private final MetricasDisco metricas;

    // Colores visuales
    private static final Color COLOR_LINEA = new Color(0xE65100);       // Naranja oscuro
    private static final Color COLOR_AREA = new Color(0xFF, 0x98, 0x00, 35); // Naranja suave transparente
    private static final Color COLOR_REJILLA = new Color(0xEEEEEE);
    private static final Color COLOR_EJES = new Color(0x9E9E9E);
    private static final Color COLOR_TEXTO = new Color(0x616161);

    public PanelGraficaDisco(MetricasDisco metricas) {
        this.metricas = metricas;
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(PaletaColores.BORDE_SUAVE),
                        "Evolución Temporal del Desplazamiento del Cabezal",
                        0, 0,
                        new Font("SansSerif", Font.BOLD, 13),
                        new Color(0x333333)
                ),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));
    }

    public void actualizar() {
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int izq = 55;
        int der = 20;
        int arriba = 25;
        int abajo = 35;
        int ancho = getWidth() - izq - der;
        int alto = getHeight() - arriba - abajo;

        if (ancho < 60 || alto < 50) {
            g2.dispose();
            return;
        }

        FontMetrics fm = g2.getFontMetrics();

        if (metricas == null) {
            dibujarMensajeVacio(g2, "Sin módulo de métricas configurado", izq, arriba, ancho, alto);
            g2.dispose();
            return;
        }

        SerieTemporal serie = metricas.getSerieDesplazamiento();
        long[] ciclos = serie != null ? serie.getCiclos() : new long[0];
        double[] valores = serie != null ? serie.getValores() : new double[0];

        if (ciclos.length == 0 || valores.length == 0) {
            dibujarMensajeVacio(g2, "Sin datos de desplazamiento aún: encole solicitudes y avance ciclos",
                    izq, arriba, ancho, alto);
            g2.dispose();
            return;
        }

        // Rango de ciclos (X) y desplazamientos (Y)
        long minCiclo = ciclos[0];
        long maxCiclo = ciclos[ciclos.length - 1];
        if (maxCiclo == minCiclo) {
            maxCiclo = minCiclo + 1;
        }

        double maxValor = 10.0;
        for (double v : valores) {
            if (v > maxValor) {
                maxValor = v;
            }
        }
        // Redondear maxValor hacia arriba para rejilla limpia
        double topeY = Math.ceil(maxValor / 10.0) * 10.0;
        if (topeY <= 0) {
            topeY = 10.0;
        }

        // 1. Rejilla y etiquetas del eje Y
        int divisionesY = 4;
        for (int i = 0; i <= divisionesY; i++) {
            double val = (topeY * i) / divisionesY;
            int y = arriba + alto - (int) Math.round((val / topeY) * alto);

            g2.setColor(COLOR_REJILLA);
            g2.drawLine(izq, y, izq + ancho, y);

            g2.setColor(COLOR_TEXTO);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            String etiqueta = String.valueOf((int) Math.round(val));
            g2.drawString(etiqueta, izq - fm.stringWidth(etiqueta) - 6, y + fm.getAscent() / 2 - 1);
        }

        // Ejes X e Y
        g2.setColor(COLOR_EJES);
        g2.drawLine(izq, arriba, izq, arriba + alto);
        g2.drawLine(izq, arriba + alto, izq + ancho, arriba + alto);

        // Etiquetas del eje X (ciclos)
        g2.setColor(COLOR_TEXTO);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        String txtInicio = "ciclo " + minCiclo;
        g2.drawString(txtInicio, izq, arriba + alto + 16);

        String txtFin = "ciclo " + maxCiclo;
        g2.drawString(txtFin, izq + ancho - fm.stringWidth(txtFin), arriba + alto + 16);

        // Subtítulo con valor actual
        int despActual = metricas.getDesplazamientoTotal();
        int saltos = metricas.getTotalSaltos();
        String resumen = String.format("Total: %d bloques recorridos | Saltos C-SCAN: %d", despActual, saltos);
        g2.setColor(COLOR_LINEA);
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.drawString(resumen, izq + ancho - fm.stringWidth(resumen), arriba - 6);

        // 2. Trazar curva y relleno
        Polygon poligono = new Polygon();
        poligono.addPoint(izq, arriba + alto);

        int[] xPuntos = new int[ciclos.length];
        int[] yPuntos = new int[ciclos.length];

        for (int i = 0; i < ciclos.length; i++) {
            int x = izq + (int) ((ciclos[i] - minCiclo) * ancho / (maxCiclo - minCiclo));
            int y = arriba + alto - (int) Math.round((valores[i] / topeY) * alto);
            xPuntos[i] = x;
            yPuntos[i] = y;
            poligono.addPoint(x, y);
        }

        poligono.addPoint(xPuntos[ciclos.length - 1], arriba + alto);

        // Area bajo la curva
        g2.setColor(COLOR_AREA);
        g2.fillPolygon(poligono);

        // Linea principal
        g2.setColor(COLOR_LINEA);
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < ciclos.length - 1; i++) {
            g2.drawLine(xPuntos[i], yPuntos[i], xPuntos[i + 1], yPuntos[i + 1]);
        }

        // Punto final destacado
        int ultX = xPuntos[ciclos.length - 1];
        int ultY = yPuntos[ciclos.length - 1];
        g2.setColor(COLOR_LINEA);
        g2.fillOval(ultX - 4, ultY - 4, 8, 8);
        g2.setColor(Color.WHITE);
        g2.fillOval(ultX - 2, ultY - 2, 4, 4);

        g2.dispose();
    }

    private void dibujarMensajeVacio(Graphics2D g2, String msg, int izq, int arriba, int ancho, int alto) {
        g2.setColor(COLOR_EJES);
        g2.drawRect(izq, arriba, ancho, alto);
        g2.setColor(PaletaColores.TEXTO_MUTED);
        g2.setFont(new Font("SansSerif", Font.ITALIC, 12));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, izq + (ancho - fm.stringWidth(msg)) / 2, arriba + alto / 2);
    }
}
