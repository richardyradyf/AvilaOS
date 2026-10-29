package avilaos.gui;

import avilaos.cluster.Cluster;
import avilaos.metrics.MetricasBuffer;
import avilaos.metrics.MetricasComputador;
import avilaos.metrics.RecolectorMetricas;
import avilaos.metrics.ReporteMetricas;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

/**
 * @author Gianm
 * Pestaña de metricas: grafica de utilizacion de CPU y tablas con las metricas de cada
 * computador, del sistema completo y de los buffers. Se refresca sola cada 500 ms
 * mientras es visible; solo lee, nunca modifica la simulacion.
 */
public class VistaMetricas extends JPanel {

    private static final String[] COLUMNAS_PC = {
        "Computador", "Procesos", "Completados", "Vencidos", "Throughput (proc/ciclo)",
        "Utilización CPU", "T. respuesta prom. (ciclos)", "Cumplimiento deadlines", "Equidad (Jain)"
    };
    private static final String[] COLUMNAS_BUF = {
        "Buffer", "Anfitrión", "Capacidad", "Ocupados", "Producidos", "Consumidos"
    };

    private final RecolectorMetricas recolector;
    private final PanelGraficaUtilizacion grafica;
    private final DefaultTableModel modeloComputadores = modeloSoloLectura(COLUMNAS_PC);
    private final DefaultTableModel modeloBuffers = modeloSoloLectura(COLUMNAS_BUF);
    private final JLabel etiquetaBloqueo = new JLabel(" ");
    private final Timer temporizador;

    public VistaMetricas(RecolectorMetricas recolector) {
        super(new BorderLayout(6, 6));
        this.recolector = recolector;
        this.grafica = new PanelGraficaUtilizacion(recolector);
        grafica.setPreferredSize(new Dimension(600, 240));

        JTable tablaPc = new JTable(modeloComputadores);
        JTable tablaBuf = new JTable(modeloBuffers);
        JScrollPane scrollPc = new JScrollPane(tablaPc);
        scrollPc.setPreferredSize(new Dimension(600, 110));
        JScrollPane scrollBuf = new JScrollPane(tablaBuf);
        scrollBuf.setPreferredSize(new Dimension(600, 90));

        JPanel abajo = new JPanel(new BorderLayout(4, 4));
        abajo.add(scrollPc, BorderLayout.NORTH);
        JPanel buffers = new JPanel(new BorderLayout(4, 4));
        buffers.add(scrollBuf, BorderLayout.CENTER);
        buffers.add(etiquetaBloqueo, BorderLayout.SOUTH);
        abajo.add(buffers, BorderLayout.CENTER);

        add(grafica, BorderLayout.CENTER);
        add(abajo, BorderLayout.SOUTH);

        temporizador = new Timer(500, e -> refrescar());
        temporizador.start();
    }

    /** Vuelve a leer las metricas y redibuja. Se puede llamar a mano (por ejemplo, en pruebas). */
    public void refrescar() {
        ReporteMetricas r = recolector.generarReporte();

        modeloComputadores.setRowCount(0);
        MetricasComputador[] pcs = r.getPorComputador();
        for (MetricasComputador m : pcs) {
            modeloComputadores.addRow(fila(m, "Computador " + m.getIdComputador()));
        }
        modeloComputadores.addRow(fila(r.getSistema(), "SISTEMA"));

        modeloBuffers.setRowCount(0);
        for (MetricasBuffer b : r.getBuffers()) {
            modeloBuffers.addRow(new Object[]{b.getEtiqueta(), "Computador " + b.getIdAnfitrion(),
                b.getCapacidad(), b.getElementos(), b.getProducidos(), b.getConsumidos()});
        }
        etiquetaBloqueo.setText("Productores/consumidores: " + r.getProcesosConBuffer()
                + "  |  bloqueo promedio en semáforos: " + decimal(r.getBloqueoSemaforosPromedio())
                + " ciclos  |  espera promedio por acceso remoto: " + decimal(r.getBloqueoRemotoPromedio())
                + " ciclos");
        grafica.repaint();
    }

    public void detener() {
        temporizador.stop();
    }

    private static Object[] fila(MetricasComputador m, String nombre) {
        return new Object[]{nombre, m.getProcesosTotales(), m.getCompletados(), m.getVencidos(),
            decimal(m.getThroughput()), porcentaje(m.getUtilizacion()), decimal(m.getTiempoRespuestaPromedio()),
            porcentaje(m.getCumplimientoDeadlines()), decimal(m.getEquidad())};
    }

    private static String decimal(double valor) {
        return Double.isNaN(valor) ? "-" : String.format("%.2f", valor);
    }

    private static String porcentaje(double valor) {
        return Double.isNaN(valor) ? "-" : String.format("%.1f %%", valor * 100);
    }

    private static DefaultTableModel modeloSoloLectura(String[] columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }
}
