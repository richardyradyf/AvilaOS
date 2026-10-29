package avilaos.gui;

import avilaos.metrics.RecolectorMetricas;
import avilaos.metrics.SerieTemporal;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * @author Gianm
 * Grafica de lineas de la utilizacion de CPU de cada computador a lo largo del tiempo
 * (eje X: ciclo de reloj, eje Y: 0 % a 100 %). Se dibuja a mano con Graphics2D.
 */
public class PanelGraficaUtilizacion extends JPanel {

    private static final Color[] COLORES = {
        new Color(0x1F77B4), new Color(0xD62728), new Color(0x2CA02C),
        new Color(0xFF7F0E), new Color(0x9467BD), new Color(0x8C564B)
    };

    private final RecolectorMetricas recolector;

    public PanelGraficaUtilizacion(RecolectorMetricas recolector) {
        this.recolector = recolector;
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int izq = 48, der = 16, arriba = 30, abajo = 34;
        int ancho = getWidth() - izq - der;
        int alto = getHeight() - arriba - abajo;
        if (ancho < 50 || alto < 50) {
            g2.dispose();
            return;
        }
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Utilización de CPU por computador", izq, 18);

        // rejilla y eje Y
        for (int i = 0; i <= 4; i++) {
            int y = arriba + alto - alto * i / 4;
            g2.setColor(new Color(0xDDDDDD));
            g2.drawLine(izq, y, izq + ancho, y);
            g2.setColor(Color.DARK_GRAY);
            String texto = (i * 25) + " %";
            g2.drawString(texto, izq - fm.stringWidth(texto) - 6, y + fm.getAscent() / 2 - 1);
        }
        g2.setColor(Color.DARK_GRAY);
        g2.drawLine(izq, arriba, izq, arriba + alto);
        g2.drawLine(izq, arriba + alto, izq + ancho, arriba + alto);

        // rango de ciclos de todas las series
        long minCiclo = Long.MAX_VALUE;
        long maxCiclo = Long.MIN_VALUE;
        int cantidad = recolector.cantidadSeries();
        for (int s = 1; s <= cantidad; s++) {
            long[] ciclos = recolector.getSerie(s).getCiclos();
            if (ciclos.length > 0) {
                minCiclo = Math.min(minCiclo, ciclos[0]);
                maxCiclo = Math.max(maxCiclo, ciclos[ciclos.length - 1]);
            }
        }
        if (minCiclo == Long.MAX_VALUE) {
            g2.drawString("Sin datos todavía: inicie la simulación", izq + 20, arriba + alto / 2);
            g2.dispose();
            return;
        }
        if (maxCiclo == minCiclo) {
            maxCiclo = minCiclo + 1;
        }
        g2.drawString("ciclo " + minCiclo, izq, arriba + alto + 16);
        String fin = "ciclo " + maxCiclo;
        g2.drawString(fin, izq + ancho - fm.stringWidth(fin), arriba + alto + 16);

        // lineas y leyenda
        g2.setStroke(new BasicStroke(2f));
        int xLeyenda = izq + 280;
        for (int s = 1; s <= cantidad; s++) {
            Color color = COLORES[(s - 1) % COLORES.length];
            SerieTemporal serie = recolector.getSerie(s);
            long[] ciclos = serie.getCiclos();
            double[] valores = serie.getValores();
            g2.setColor(color);
            int xAnt = 0, yAnt = 0;
            for (int i = 0; i < ciclos.length && i < valores.length; i++) {
                int x = izq + (int) ((ciclos[i] - minCiclo) * ancho / (maxCiclo - minCiclo));
                int y = arriba + alto - (int) Math.round(valores[i] * alto);
                if (i > 0) {
                    g2.drawLine(xAnt, yAnt, x, y);
                }
                xAnt = x;
                yAnt = y;
            }
            g2.fillRect(xLeyenda, 8, 14, 4);
            g2.setColor(Color.DARK_GRAY);
            g2.drawString("Computador " + s, xLeyenda + 20, 14);
            xLeyenda += 110;
        }
        g2.dispose();
    }
}