package avilaos.gui;

import avilaos.metrics.MetricasComputador;
import avilaos.metrics.RecolectorMetricas;
import avilaos.metrics.ReporteMetricas;
import java.awt.BorderLayout;
import java.awt.Dimension;
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

    private final RecolectorMetricas recolector;
    private final PanelGraficaUtilizacion grafica;
    private final DefaultTableModel modeloComputadores = modeloSoloLectura(COLUMNAS_PC);
    private final Timer temporizador;

    public VistaMetricas(RecolectorMetricas recolector) {
        super(new BorderLayout(6, 6));
        this.recolector = recolector;
        this.grafica = new PanelGraficaUtilizacion(recolector);
        grafica.setPreferredSize(new Dimension(600, 240));

        JTable tablaPc = new JTable(modeloComputadores);
        JScrollPane scrollPc = new JScrollPane(tablaPc);
        scrollPc.setPreferredSize(new Dimension(600, 130));

        add(grafica, BorderLayout.CENTER);
        add(scrollPc, BorderLayout.SOUTH);

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