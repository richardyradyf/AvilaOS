package avilaos.gui;

import avilaos.cluster.Cluster;
import avilaos.config.EntradaInvalidaException;
import avilaos.config.ValidadorEntradas;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Barra superior con los controles de ejecucion: iniciar, pausar, reanudar, detener,
 * paso a paso, duracion del ciclo y latencia de red (ambos modificables en caliente).
 * 
 * @author andre
 */
public class PanelControles extends JPanel {

    private final ControlSimulacion control;
    private final Cluster cluster;
    private final JLabel etiquetaCiclo = new JLabel("Ciclo: 0");
    private final JLabel etiquetaMensaje = new JLabel(" ");
    private final JTextField campoDuracion;
    private final JTextField campoLatencia;

    public PanelControles(ControlSimulacion control, Cluster cluster) {
        super(new BorderLayout());
        this.control = control;
        this.cluster = cluster;
        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        campoDuracion = new JTextField(String.valueOf(cluster.getReloj().getDuracionCicloMs()), 5);
        campoLatencia = new JTextField(String.valueOf(cluster.getGestorBuffers().getLatenciaRed()), 4);

        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        fila.add(boton("Iniciar", () -> control.iniciar()));
        fila.add(boton("Pausar", () -> control.pausar()));
        fila.add(boton("Reanudar", () -> control.reanudar()));
        fila.add(boton("Detener", () -> control.detener()));
        fila.add(boton("Paso (1 ciclo)", () -> control.avanzarCiclo()));
        fila.add(new JLabel("  Duración del ciclo (ms):"));
        fila.add(campoDuracion);
        fila.add(boton("Aplicar", () -> aplicarDuracion()));
        fila.add(new JLabel("  Latencia de red (ciclos):"));
        fila.add(campoLatencia);
        fila.add(boton("Aplicar", () -> aplicarLatencia()));
        etiquetaCiclo.setFont(etiquetaCiclo.getFont().deriveFont(java.awt.Font.BOLD));
        fila.add(etiquetaCiclo);

        etiquetaMensaje.setForeground(new Color(0xB00020));
        add(fila, BorderLayout.CENTER);
        add(etiquetaMensaje, BorderLayout.SOUTH);
    }

    /** Actualiza el contador de ciclos. */
    public void refrescar() {
        String estado = cluster.getReloj().estaActivo()
                ? (cluster.getReloj().estaPausado() ? "pausado" : "en ejecución") : "detenido";
        etiquetaCiclo.setText("Ciclo: " + cluster.getCicloActual() + " (" + estado + ")");
    }

    private JButton boton(String texto, Accion accion) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> mostrar(accion.ejecutar()));
        return b;
    }

    private String aplicarDuracion() {
        try {
            int ms = ValidadorEntradas.entero(campoDuracion.getText(), "Duración del ciclo (ms)", 10, 10000);
            return control.setDuracionCicloMs(ms);
        } catch (EntradaInvalidaException e) {
            return e.getMessage();
        }
    }

    private String aplicarLatencia() {
        try {
            int ciclos = ValidadorEntradas.entero(campoLatencia.getText(), "Latencia de red (ciclos)", 0, 1000);
            return control.setLatenciaRed(ciclos);
        } catch (EntradaInvalidaException e) {
            return e.getMessage();
        }
    }

    private void mostrar(String error) {
        etiquetaMensaje.setText(error == null ? " " : error);
        refrescar();
    }

    private interface Accion {
        String ejecutar();
    }
}