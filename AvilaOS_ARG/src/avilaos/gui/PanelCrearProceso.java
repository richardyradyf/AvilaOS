package avilaos.gui;

import avilaos.config.EntradaInvalidaException;
import avilaos.config.ValidadorEntradas;
import avilaos.model.SolicitudProceso;
import avilaos.model.TipoProceso;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Vista 1: formulario para crear procesos. Valida todos los campos (tipo de dato y
 * rango) y muestra los errores en pantalla sin interrumpir la simulacion.
 */
public class PanelCrearProceso extends JPanel {

    private final AccionesUsuario acciones;

    private final JTextField campoNombre = new JTextField(20);
    private final JComboBox<TipoProceso> comboTipo = new JComboBox<>(TipoProceso.values());
    private final JTextField campoInstrucciones = new JTextField(10);
    private final JTextField campoMemoria = new JTextField(10);
    private final JTextField campoPrioridad = new JTextField(10);
    private final JTextField campoDeadline = new JTextField(10);
    private final JComboBox<String> comboComputador = new JComboBox<>();
    private final JTextField campoCiclosEs = new JTextField(10);
    private final JTextField campoCadaCiclos = new JTextField(10);
    private final JTextField campoElementos = new JTextField(10);
    private final JComboBox<String> comboBuffer = new JComboBox<>();
    private final JLabel etiquetaMensaje = new JLabel(" ");

    private int[] idsBuffer = new int[0];
    private int fila = 0;

    public PanelCrearProceso(AccionesUsuario acciones, int numeroComputadores) {
        this.acciones = acciones;
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        comboComputador.addItem("Automático (ÁvilaOS elige)");
        for (int i = 1; i <= numeroComputadores; i++) {
            comboComputador.addItem("Computador " + i);
        }

        agregarFila("Nombre", campoNombre);
        agregarFila("Tipo", comboTipo);
        agregarFila("Instrucciones (CPU e I/O bound)", campoInstrucciones);
        agregarFila("Memoria (MB)", campoMemoria);
        agregarFila("Prioridad (1 = la más alta)", campoPrioridad);
        agregarFila("Deadline (ciclos desde la creación)", campoDeadline);
        agregarFila("Computador", comboComputador);
        agregarFila("Ciclos de cada E/S (I/O bound)", campoCiclosEs);
        agregarFila("Cada cuántas instrucciones pide E/S, o ciclos por elemento", campoCadaCiclos);
        agregarFila("Elementos a producir o consumir", campoElementos);
        agregarFila("Buffer (productor y consumidor)", comboBuffer);

        JButton botonCrear = new JButton("Crear proceso");
        botonCrear.addActionListener(e -> crear());
        JButton botonLimpiar = new JButton("Limpiar");
        botonLimpiar.addActionListener(e -> limpiar());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botones.add(botonCrear);
        botones.add(botonLimpiar);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = fila++;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(8, 4, 4, 4);
        add(botones, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = fila++;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.weighty = 1.0;
        c.insets = new Insets(4, 8, 4, 4);
        add(etiquetaMensaje, c);

        comboTipo.addActionListener(e -> actualizarCamposSegunTipo());
        actualizarCamposSegunTipo();
    }

    public void setBuffers(int[] ids) {
        idsBuffer = ids.clone();
        comboBuffer.removeAllItems();
        for (int id : ids) {
            comboBuffer.addItem("B" + id);
        }
    }

    private void agregarFila(String texto, JComponent campo) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = fila++;
        c.insets = new Insets(4, 8, 4, 8);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        add(new JLabel(texto), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        add(campo, c);
    }

    private void actualizarCamposSegunTipo() {
        TipoProceso tipo = (TipoProceso) comboTipo.getSelectedItem();
        boolean usaInstrucciones = tipo == TipoProceso.CPU_BOUND || tipo == TipoProceso.IO_BOUND;
        boolean esIo = tipo == TipoProceso.IO_BOUND;
        boolean usaBuffer = tipo == TipoProceso.PRODUCTOR || tipo == TipoProceso.CONSUMIDOR;
        campoInstrucciones.setEnabled(usaInstrucciones);
        campoCiclosEs.setEnabled(esIo);
        campoCadaCiclos.setEnabled(esIo || usaBuffer);
        campoElementos.setEnabled(usaBuffer);
        comboBuffer.setEnabled(usaBuffer);
    }

    private void crear() {
        try {
            int idComputador = comboComputador.getSelectedIndex();
            int posBuffer = comboBuffer.getSelectedIndex();
            int idBuffer = (posBuffer >= 0 && posBuffer < idsBuffer.length) ? idsBuffer[posBuffer] : 0;

            SolicitudProceso solicitud = ValidadorEntradas.validarProceso(
                    campoNombre.getText(), (TipoProceso) comboTipo.getSelectedItem(),
                    campoInstrucciones.getText(), campoMemoria.getText(), campoPrioridad.getText(),
                    campoDeadline.getText(), idComputador, campoCiclosEs.getText(),
                    campoCadaCiclos.getText(), campoElementos.getText(), idBuffer);

            String error = acciones.crearProceso(solicitud);
            if (error != null) {
                mostrarMensaje(error, true);
            } else {
                mostrarMensaje("Proceso \"" + solicitud.getNombre() + "\" creado", false);
                limpiar();
            }
        } catch (EntradaInvalidaException ex) {
            mostrarMensaje(ex.getMessage(), true);
        } catch (RuntimeException ex) {
            mostrarMensaje("Error inesperado: " + ex.getMessage(), true);
        }
    }

    private void limpiar() {
        campoNombre.setText("");
        campoInstrucciones.setText("");
        campoMemoria.setText("");
        campoPrioridad.setText("");
        campoDeadline.setText("");
        campoCiclosEs.setText("");
        campoCadaCiclos.setText("");
        campoElementos.setText("");
        comboTipo.setSelectedIndex(0);
        comboComputador.setSelectedIndex(0);
    }

    private void mostrarMensaje(String texto, boolean error) {
        etiquetaMensaje.setForeground(error ? new Color(180, 0, 0) : new Color(0, 120, 0));
        etiquetaMensaje.setText(texto);
    }
}
