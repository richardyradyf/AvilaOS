package avilaos.gui;

import avilaos.config.EntradaInvalidaException;
import avilaos.config.ValidadorEntradas;
import avilaos.model.SolicitudProceso;
import avilaos.model.TipoOperacionES;
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
    private final JComboBox<TipoProceso> comboTipo =
            new JComboBox<>(new TipoProceso[]{TipoProceso.CPU_BOUND, TipoProceso.IO_BOUND});
    private final JTextField campoInstrucciones = new JTextField(10);
    private final JTextField campoMemoria = new JTextField(10);
    private final JTextField campoPrioridad = new JTextField(10);
    private final JTextField campoDeadline = new JTextField(10);
    private final JTextField campoCiclosEs = new JTextField(10);
    private final JTextField campoCadaCiclos = new JTextField(10);
    private final JComboBox<Object> comboOperacion = new JComboBox<>(new Object[]{"Ninguna",
        TipoOperacionES.CREAR, TipoOperacionES.LEER, TipoOperacionES.ACTUALIZAR, TipoOperacionES.ELIMINAR});
    private final JTextField campoRuta = new JTextField(20);
    private final JTextField campoBloques = new JTextField(10);
    private final JTextField campoNuevoNombre = new JTextField(20);
    private final JTextField campoUsuario = new JTextField(20);
    private final JLabel etiquetaMensaje = new JLabel(" ");

    private int fila = 0;

    public PanelCrearProceso(AccionesUsuario acciones) {
        this.acciones = acciones;
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        agregarFila("Nombre", campoNombre);
        agregarFila("Tipo", comboTipo);
        agregarFila("Instrucciones", campoInstrucciones);
        agregarFila("Memoria (MB)", campoMemoria);
        agregarFila("Prioridad (1 = la más alta)", campoPrioridad);
        agregarFila("Deadline (ciclos desde la creación)", campoDeadline);
        agregarFila("Ciclos de cada E/S (I/O bound)", campoCiclosEs);
        agregarFila("Cada cuántas instrucciones pide E/S (I/O bound)", campoCadaCiclos);
        agregarFila("Operación sobre archivo", comboOperacion);
        agregarFila("Ruta (ej. /docs/a.txt)", campoRuta);
        agregarFila("Bloques (solo Crear)", campoBloques);
        agregarFila("Nombre nuevo (opcional, solo Actualizar)", campoNuevoNombre);
        agregarFila("Usuario (opcional, por defecto admin)", campoUsuario);

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
        comboOperacion.addActionListener(e -> actualizarCamposSegunTipo());
        actualizarCamposSegunTipo();
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

    /** La operacion elegida, o null si el proceso no trabaja con archivos. */
    private TipoOperacionES operacionElegida() {
        Object elegido = comboOperacion.getSelectedItem();
        return elegido instanceof TipoOperacionES ? (TipoOperacionES) elegido : null;
    }

    private void actualizarCamposSegunTipo() {
        TipoOperacionES operacion = operacionElegida();
        boolean esArchivo = operacion != null;
        if (esArchivo && comboTipo.getSelectedItem() != TipoProceso.IO_BOUND) {
            comboTipo.setSelectedItem(TipoProceso.IO_BOUND);
        }
        boolean esIo = comboTipo.getSelectedItem() == TipoProceso.IO_BOUND;
        comboTipo.setEnabled(!esArchivo);
        campoCiclosEs.setEnabled(esIo && !esArchivo);
        campoCadaCiclos.setEnabled(esIo && !esArchivo);
        campoRuta.setEnabled(esArchivo);
        campoBloques.setEnabled(operacion == TipoOperacionES.CREAR);
        campoNuevoNombre.setEnabled(operacion == TipoOperacionES.ACTUALIZAR);
        campoUsuario.setEnabled(esArchivo);
    }

    private SolicitudProceso leerSolicitud() {
        TipoOperacionES operacion = operacionElegida();
        if (operacion != null) {
            return ValidadorEntradas.validarProcesoEs(campoNombre.getText(), campoInstrucciones.getText(),
                    campoMemoria.getText(), campoPrioridad.getText(), campoDeadline.getText(), operacion,
                    campoRuta.getText(), campoBloques.getText(), campoNuevoNombre.getText(),
                    campoUsuario.getText());
        }
        int idComputador = 1;
        return ValidadorEntradas.validarProceso(
                campoNombre.getText(), (TipoProceso) comboTipo.getSelectedItem(),
                campoInstrucciones.getText(), campoMemoria.getText(), campoPrioridad.getText(),
                campoDeadline.getText(), idComputador, campoCiclosEs.getText(),
                campoCadaCiclos.getText(), "", 0);
    }

    private void crear() {
        try {
            SolicitudProceso solicitud = leerSolicitud();
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
        campoRuta.setText("");
        campoBloques.setText("");
        campoNuevoNombre.setText("");
        campoUsuario.setText("");
        comboOperacion.setSelectedIndex(0);
        comboTipo.setSelectedIndex(0);
    }

    private void mostrarMensaje(String texto, boolean error) {
        etiquetaMensaje.setForeground(error ? new Color(180, 0, 0) : new Color(0, 120, 0));
        etiquetaMensaje.setText(texto);
    }
}
