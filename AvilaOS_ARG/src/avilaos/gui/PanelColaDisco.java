package avilaos.gui;

import avilaos.disco.Disco;
import avilaos.disco.FabricaPoliticasDisco;
import avilaos.disco.SolicitudDisco;
import avilaos.metrics.MetricasDisco;
import avilaos.model.PoliticaDisco;
import avilaos.util.Lista;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * @author andre
 *
 * Panel de gestion de la cola de E/S del disco (Proyecto 2 - Dia 7):
 * - Visualizacion de solicitudes en espera (JTable).
 * - Indicador detallado de la solicitud actualmente en atencion.
 * - Selector de politica de planificacion en caliente (FIFO, SSTF, SCAN, C-SCAN).
 * - Selector de velocidad del cabezal.
 * - Resumen numerico en tiempo real de metricas de desplazamiento y esperas.
 */
public class PanelColaDisco extends JPanel {

    private final Disco disco;
    private final MetricasDisco metricas;

    // Controles interactivos
    private final JComboBox<PoliticaDisco> comboPoliticas;
    private final JSpinner spinnerVelocidad;
    private boolean actualizandoControles = false;

    // Componentes de visualizacion
    private final JLabel lblSolicitudActual;
    private final JLabel lblEstadoActual;
    private final JLabel lblMetricaDesplazamiento;
    private final JLabel lblMetricaEspera;
    private final JLabel lblMetricaPoliticas;

    // Tabla de cola de solicitudes
    private final DefaultTableModel modeloTabla;
    private final JTable tablaCola;

    public PanelColaDisco(Disco disco, MetricasDisco metricas) {
        this.disco = disco;
        this.metricas = metricas;

        setLayout(new BorderLayout(10, 10));
        setBackground(PaletaColores.FONDO_PANEL);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(PaletaColores.BORDE_SUAVE),
                        "Cola de E/S y Control de Planificación",
                        0, 0,
                        new Font("SansSerif", Font.BOLD, 13),
                        new Color(0x333333)
                ),
                BorderFactory.createEmptyBorder(6, 8, 8, 8)
        ));

        // 1. Barra superior: Controles de politica y velocidad + Solicitud en curso
        JPanel panelNorte = new JPanel();
        panelNorte.setLayout(new BoxLayout(panelNorte, BoxLayout.Y_AXIS));
        panelNorte.setBackground(PaletaColores.FONDO_PANEL);

        JPanel panelControles = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        panelControles.setBackground(PaletaColores.FONDO_PANEL);

        panelControles.add(new JLabel("Política:"));
        comboPoliticas = new JComboBox<>(PoliticaDisco.values());
        comboPoliticas.setSelectedItem(disco.getTipoPolitica());
        comboPoliticas.addActionListener(e -> {
            if (!actualizandoControles) {
                PoliticaDisco seleccionada = (PoliticaDisco) comboPoliticas.getSelectedItem();
                if (seleccionada != null) {
                    disco.setPolitica(FabricaPoliticasDisco.crear(seleccionada));
                }
            }
        });
        panelControles.add(comboPoliticas);

        panelControles.add(Box.createHorizontalStrut(10));
        panelControles.add(new JLabel("Velocidad:"));
        spinnerVelocidad = new JSpinner(new SpinnerNumberModel(disco.getVelocidad(), 1, 10, 1));
        spinnerVelocidad.setPreferredSize(new Dimension(55, 24));
        spinnerVelocidad.addChangeListener(e -> {
            if (!actualizandoControles) {
                int vel = (Integer) spinnerVelocidad.getValue();
                disco.setVelocidad(vel);
            }
        });
        panelControles.add(spinnerVelocidad);
        panelControles.add(new JLabel("bloques/ciclo"));

        // Subpanel con la solicitud activa en su propia fila para evitar truncamiento
        JPanel panelActiva = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        panelActiva.setBackground(PaletaColores.FONDO_PANEL);
        lblEstadoActual = new JLabel("Estado: " + disco.getEstado());
        lblEstadoActual.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblSolicitudActual = new JLabel("En reposo");
        lblSolicitudActual.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSolicitudActual.setForeground(new Color(0x1565C0));

        panelActiva.add(lblEstadoActual);
        panelActiva.add(new JLabel("|"));
        panelActiva.add(lblSolicitudActual);

        panelNorte.add(panelControles);
        panelNorte.add(panelActiva);

        add(panelNorte, BorderLayout.NORTH);

        // 2. Centro: Tabla de la cola de E/S
        String[] columnas = {"#", "ID", "Proceso", "Operación", "Bloque Objetivo", "Llegada"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaCola = new JTable(modeloTabla);
        tablaCola.setRowHeight(20);
        tablaCola.getTableHeader().setReorderingAllowed(false);
        tablaCola.setFont(new Font("SansSerif", Font.PLAIN, 11));

        DefaultTableCellRenderer centro = new DefaultTableCellRenderer();
        centro.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < columnas.length; i++) {
            tablaCola.getColumnModel().getColumn(i).setCellRenderer(centro);
        }
        tablaCola.getColumnModel().getColumn(0).setPreferredWidth(30);
        tablaCola.getColumnModel().getColumn(1).setPreferredWidth(70);
        tablaCola.getColumnModel().getColumn(2).setPreferredWidth(100);
        tablaCola.getColumnModel().getColumn(3).setPreferredWidth(90);
        tablaCola.getColumnModel().getColumn(4).setPreferredWidth(110);
        tablaCola.getColumnModel().getColumn(5).setPreferredWidth(70);

        JScrollPane scrollTabla = new JScrollPane(tablaCola);
        scrollTabla.setPreferredSize(new Dimension(500, 120));
        add(scrollTabla, BorderLayout.CENTER);

        // 3. Sur: Métricas numéricas acumuladas
        JPanel panelMetricas = new JPanel(new GridLayout(1, 3, 10, 4));
        panelMetricas.setBackground(new Color(0xF5F5F5));
        panelMetricas.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PaletaColores.BORDE_SUAVE),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        lblMetricaDesplazamiento = new JLabel("Desplazamiento total: 0");
        lblMetricaDesplazamiento.setFont(new Font("SansSerif", Font.PLAIN, 11));

        lblMetricaEspera = new JLabel("Espera promedio: 0.00 ciclos");
        lblMetricaEspera.setFont(new Font("SansSerif", Font.PLAIN, 11));

        lblMetricaPoliticas = new JLabel("Atendidas: 0");
        lblMetricaPoliticas.setFont(new Font("SansSerif", Font.PLAIN, 11));

        panelMetricas.add(lblMetricaDesplazamiento);
        panelMetricas.add(lblMetricaEspera);
        panelMetricas.add(lblMetricaPoliticas);

        add(panelMetricas, BorderLayout.SOUTH);

        actualizar();
    }
    
        /**
     * En modo usuario la politica y la velocidad quedan bloqueadas; el texto de ayuda
     * explica por que.
     */
    public void setCambiosPermitidos(boolean permitidos) {
        comboPoliticas.setEnabled(permitidos);
        spinnerVelocidad.setEnabled(permitidos);
        String ayuda = permitidos ? null : "Modo usuario: solo el administrador puede cambiar la política del disco";
        comboPoliticas.setToolTipText(ayuda);
        spinnerVelocidad.setToolTipText(ayuda);
    }

    public boolean isCambiosPermitidos() {
        return comboPoliticas.isEnabled();
    }

    /**
     * Sincroniza la tabla, controles y etiquetas con el estado actual del disco y metricas.
     */
    public void actualizar() {
        actualizandoControles = true;
        try {
            // Sincronizar politica y velocidad
            if (disco.getTipoPolitica() != comboPoliticas.getSelectedItem()) {
                comboPoliticas.setSelectedItem(disco.getTipoPolitica());
            }
            if ((Integer) spinnerVelocidad.getValue() != disco.getVelocidad()) {
                spinnerVelocidad.setValue(disco.getVelocidad());
            }
        } finally {
            actualizandoControles = false;
        }

        // Estado y solicitud activa
        lblEstadoActual.setText("Estado: " + disco.getEstado());
        switch (disco.getEstado()) {
            case OCIOSO -> lblEstadoActual.setForeground(PaletaColores.ESTADO_OCIOSO);
            case MOVIENDO -> lblEstadoActual.setForeground(PaletaColores.ESTADO_MOVIENDO);
            case TRANSFIRIENDO -> lblEstadoActual.setForeground(PaletaColores.ESTADO_TRANSFIRIENDO);
        }

        SolicitudDisco activa = disco.getSolicitudActual();
        if (activa != null) {
            lblSolicitudActual.setText(String.format("Proceso: %s | Bloque: %d | Op: %s",
                    activa.getNombreProceso(), activa.getBloqueDestino(), activa.getOperacion()));
        } else {
            lblSolicitudActual.setText("Sin solicitud activa (Cola vacía)");
        }

        // Repoblar tabla de solicitudes pendientes
        modeloTabla.setRowCount(0);
        Lista<SolicitudDisco> cola = disco.clonarCola();
        int orden = 1;
        for (int i = 0; i < cola.tamano(); i++) {
            SolicitudDisco s = cola.obtener(i);
            modeloTabla.addRow(new Object[]{
                orden++,
                s.getId(),
                s.getNombreProceso(),
                s.getOperacion(),
                s.getBloqueDestino(),
                "ciclo " + s.getCicloLlegada()
            });
        }

        // Métricas
        if (metricas != null) {
            lblMetricaDesplazamiento.setText(String.format("Desplazamiento: %d bloques (saltos: %d)",
                    metricas.getDesplazamientoTotal(), metricas.getTotalSaltos()));
            lblMetricaEspera.setText(String.format(Locale.US, "Espera prom: %.2f ciclos",
                    metricas.getTiempoEsperaPromedio()));
            lblMetricaPoliticas.setText(String.format("Atendidas: %d [F:%d S:%d SC:%d CS:%d]",
                    metricas.getTotalAtendidas(),
                    metricas.getAtendidasPorPolitica(PoliticaDisco.FIFO),
                    metricas.getAtendidasPorPolitica(PoliticaDisco.SSTF),
                    metricas.getAtendidasPorPolitica(PoliticaDisco.SCAN),
                    metricas.getAtendidasPorPolitica(PoliticaDisco.C_SCAN)));
        } else {
            lblMetricaDesplazamiento.setText("Desplazamiento: " + disco.getTotalDesplazamiento());
            lblMetricaEspera.setText("Espera: N/D");
            lblMetricaPoliticas.setText("Atendidas: " + disco.getTotalSolicitudesAtendidas());
        }
    }
}
