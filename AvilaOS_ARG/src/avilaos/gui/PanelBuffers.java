package avilaos.gui;

import avilaos.cluster.Cluster;
import avilaos.config.EntradaInvalidaException;
import avilaos.config.ValidadorEntradas;
import avilaos.sync.BufferAcotado;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Vista de los buffers acotados: estado de cada uno (ocupacion, semaforos s, n y e,
 * procesos bloqueados) y formulario para crear un buffer nuevo en la RAM de un computador.
 * 
 * @author andre
 */
public class PanelBuffers extends JPanel {

    private static final String[] COLUMNAS = {
        "Buffer", "Anfitrión", "Capacidad", "Ocupados", "Producidos", "Consumidos",
        "s (mutex)", "n (elementos)", "e (espacios)", "Bloqueados"
    };

    private final Cluster cluster;
    private final ControlSimulacion control;
    private final JComboBox<String> comboAnfitrion = new JComboBox<>();
    private final JTextField campoCapacidad = new JTextField("5", 5);
    private final JLabel etiquetaMensaje = new JLabel(" ");
    private final DefaultTableModel modelo = new DefaultTableModel(COLUMNAS, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    public PanelBuffers(Cluster cluster, ControlSimulacion control) {
        super(new BorderLayout(6, 6));
        this.cluster = cluster;
        this.control = control;
        setBorder(BorderFactory.createTitledBorder("Buffers"));

        comboAnfitrion.addItem("Automático");
        for (int i = 1; i <= cluster.cantidadComputadores(); i++) {
            comboAnfitrion.addItem("Computador " + i);
        }
        JPanel formulario = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        formulario.add(new JLabel("Nuevo buffer en:"));
        formulario.add(comboAnfitrion);
        formulario.add(new JLabel("Capacidad (espacios, 1 MB c/u):"));
        formulario.add(campoCapacidad);
        JButton crear = new JButton("Crear buffer");
        crear.addActionListener(e -> crear());
        formulario.add(crear);

        etiquetaMensaje.setForeground(new Color(0xB00020));
        JPanel norte = new JPanel(new BorderLayout());
        norte.add(formulario, BorderLayout.CENTER);
        norte.add(etiquetaMensaje, BorderLayout.SOUTH);

        add(norte, BorderLayout.NORTH);
        add(new JScrollPane(new JTable(modelo)), BorderLayout.CENTER);
    }

    public void refrescar() {
        modelo.setRowCount(0);
        for (BufferAcotado b : cluster.getBuffers()) {
            modelo.addRow(new Object[]{b.getEtiqueta(), "Computador " + b.getIdComputadorAnfitrion(),
                b.getCapacidad(), b.getElementos(), b.getProducidos(), b.getConsumidos(),
                b.getMutex().getValor(), b.getHayElementos().getValor(), b.getHayEspacio().getValor(),
                b.getMutex().cantidadBloqueados() + b.getHayElementos().cantidadBloqueados()
                + b.getHayEspacio().cantidadBloqueados()});
        }
    }

    private void crear() {
        try {
            int capacidad = ValidadorEntradas.entero(campoCapacidad.getText(), "Capacidad", 1, 1000);
            String error = control.crearBuffer(comboAnfitrion.getSelectedIndex(), capacidad);
            etiquetaMensaje.setText(error == null ? " " : error);
            refrescar();
        } catch (EntradaInvalidaException e) {
            etiquetaMensaje.setText(e.getMessage());
        }
    }
}