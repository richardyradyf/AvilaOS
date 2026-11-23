package avilaos.gui;

import avilaos.fs.SesionUsuario;
import avilaos.model.ModoUsuario;
import avilaos.util.Iterador;
import avilaos.util.Lista;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * @author Gianm
 * Selector del modo de la simulacion: administrador o usuario.
 *
 * - Un JComboBox con el modo y un campo con el nombre de usuario, que solo se habilita
 *   en modo usuario.
 * - El boton "Cambiar" (o Enter en el campo) aplica el cambio en la SesionUsuario.
 * - Una etiqueta grande muestra siempre el modo activo, por ejemplo "Modo: USUARIO (user1)".
 * - Si el nombre no es valido, muestra el mensaje en rojo y deja el modo como estaba;
 *   nunca detiene la simulacion.
 *
 * La interfaz solo pide el cambio: las reglas del nombre viven en SesionUsuario.
 */
public class PanelModoUsuario extends JPanel {

    /** Recibe la sesion cada vez que el usuario cambia el modo desde este panel. */
    public interface OyenteModo {
        void modoCambiado(SesionUsuario sesion);
    }

    private final SesionUsuario sesion;
    private final JComboBox<ModoUsuario> selectorModo = new JComboBox<>(ModoUsuario.values());
    private final JTextField campoUsuario = new JTextField(12);
    private final JButton botonCambiar = new JButton("Cambiar");
    private final JLabel etiquetaModo = new JLabel();
    private final JLabel etiquetaError = new JLabel(" ");
    private final Lista<OyenteModo> oyentes = new Lista<>();

    public PanelModoUsuario(SesionUsuario sesion) {
        super(new BorderLayout(8, 4));
        if (sesion == null) {
            throw new IllegalArgumentException("La sesión es obligatoria");
        }
        this.sesion = sesion;

        etiquetaModo.setFont(etiquetaModo.getFont().deriveFont(Font.BOLD, 18f));
        etiquetaError.setForeground(new Color(0xC62828));
        campoUsuario.setToolTipText("De 1 a 20 caracteres: letras, números y _");

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        controles.add(new JLabel("Modo:"));
        controles.add(selectorModo);
        controles.add(new JLabel("Usuario:"));
        controles.add(campoUsuario);
        controles.add(botonCambiar);

        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        add(etiquetaModo, BorderLayout.NORTH);
        add(controles, BorderLayout.CENTER);
        add(etiquetaError, BorderLayout.SOUTH);

        selectorModo.addActionListener(e -> actualizarCampoUsuario());
        botonCambiar.addActionListener(e -> cambiar());
        campoUsuario.addActionListener(e -> cambiar());   // Enter en el campo

        mostrarSesion();
    }

    // ================= publico =================

    /** Avisa a "oyente" cada vez que el modo cambia desde este panel. */
    public void agregarOyenteModo(OyenteModo oyente) {
        if (oyente == null) {
            throw new IllegalArgumentException("El oyente no puede ser nulo");
        }
        oyentes.agregarAlFinal(oyente);
    }

    /** Vuelve a mostrar la sesion actual (por si otra parte del programa la cambio). */
    public void refrescar() {
        mostrarSesion();
    }

    // ================= privados =================

    /** Valida el nombre y aplica el cambio; si algo falla, muestra el mensaje y no cambia nada. */
    private void cambiar() {
        ModoUsuario modo = (ModoUsuario) selectorModo.getSelectedItem();
        String nombre = campoUsuario.getText();
        if (modo == ModoUsuario.USUARIO) {
            String error = SesionUsuario.validarNombreUsuario(nombre);
            if (error != null) {
                etiquetaError.setText(error);
                campoUsuario.requestFocusInWindow();
                return;
            }
        }
        try {
            sesion.cambiar(modo, nombre);
        } catch (IllegalArgumentException ex) {
            etiquetaError.setText(ex.getMessage());
            return;
        }
        etiquetaError.setText(" ");
        mostrarSesion();
        Iterador<OyenteModo> it = oyentes.iterador();
        while (it.tieneSiguiente()) {
            it.siguiente().modoCambiado(sesion);
        }
    }

    /** Pone la etiqueta grande, el selector y el campo segun la sesion real. */
    private void mostrarSesion() {
        etiquetaModo.setText(sesion.toString());   // "Modo: ADMINISTRADOR" o "Modo: USUARIO (user1)"
        selectorModo.setSelectedItem(sesion.getModo());
        campoUsuario.setText(sesion.esAdministrador() ? "" : sesion.getUsuario());
        actualizarCampoUsuario();
    }

    /** El nombre de usuario solo se puede escribir en modo usuario. */
    private void actualizarCampoUsuario() {
        campoUsuario.setEnabled(selectorModo.getSelectedItem() == ModoUsuario.USUARIO);
    }
}
