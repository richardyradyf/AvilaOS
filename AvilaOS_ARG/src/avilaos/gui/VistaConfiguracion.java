/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.gui;

import avilaos.config.ConfiguracionSimulacion;
import avilaos.config.EntradaInvalidaException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * @author richard
 * 
 * Pestaña de configuracion: editor del JSON de la simulacion (duracion del ciclo, latencia,
 * computadores, buffers y procesos iniciales) con botones para validar, cargar y guardar
 * archivos y aplicar la configuracion (reinicia la simulacion con los nuevos datos).
 */
public class VistaConfiguracion extends JPanel {

    private final ReiniciadorSistema reiniciador;
    private final JTextArea editor = new JTextArea();
    private final JLabel mensaje = new JLabel(" ");
    private final JFileChooser selector = new JFileChooser();

    public VistaConfiguracion(ReiniciadorSistema reiniciador, ConfiguracionSimulacion inicial) {
        super(new BorderLayout(6, 6));
        this.reiniciador = reiniciador;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        editor.setText(inicial.aJson());
        editor.setCaretPosition(0);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        botones.add(boton("Validar", () -> validar()));
        botones.add(boton("Aplicar y reiniciar simulación", () -> aplicar()));
        botones.add(boton("Cargar archivo...", () -> cargar()));
        botones.add(boton("Guardar archivo...", () -> guardar()));
        botones.add(boton("Restaurar ejemplo", () -> restaurar()));

        JLabel ayuda = new JLabel("<html>Edite el JSON y pulse <b>Aplicar</b>. Políticas: FCFS, EDF, ROUND_ROBIN, PRIORIDADES. "
                + "Tipos de proceso: CPU_BOUND, IO_BOUND, PRODUCTOR, CONSUMIDOR. "
                + "\"computador\": 0 = automático. Los buffers se numeran B1, B2... en el orden de la lista.</html>");
        JPanel norte = new JPanel(new BorderLayout());
        norte.add(ayuda, BorderLayout.NORTH);
        norte.add(botones, BorderLayout.CENTER);

        mensaje.setForeground(new Color(0xB00020));
        add(norte, BorderLayout.NORTH);
        add(new JScrollPane(editor), BorderLayout.CENTER);
        add(mensaje, BorderLayout.SOUTH);
    }

    /** Texto actual del editor (para pruebas). */
    public String getTexto() {
        return editor.getText();
    }

    public void setTexto(String texto) {
        editor.setText(texto);
    }

    /** Valida el JSON del editor sin aplicarlo. Devuelve null si es correcto. */
    public String validar() {
        try {
            ConfiguracionSimulacion.desdeJson(editor.getText());
            mostrar("La configuración es válida.", false);
            return null;
        } catch (EntradaInvalidaException e) {
            mostrar(e.getMessage(), true);
            return e.getMessage();
        }
    }

    /** Valida y, si es correcta, reinicia la simulacion con ella. Devuelve null si se aplico. */
    public String aplicar() {
        try {
            ConfiguracionSimulacion config = ConfiguracionSimulacion.desdeJson(editor.getText());
            String error = reiniciador.reiniciar(config);
            if (error != null) {
                mostrar(error, true);
            }
            return error;
        } catch (EntradaInvalidaException e) {
            mostrar(e.getMessage(), true);
            return e.getMessage();
        }
    }

    private void restaurar() {
        editor.setText(ConfiguracionSimulacion.porDefecto().aJson());
        editor.setCaretPosition(0);
        mostrar("Se restauró la configuración de ejemplo (aún no aplicada).", false);
    }

    private void cargar() {
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Path ruta = selector.getSelectedFile().toPath();
            editor.setText(new String(Files.readAllBytes(ruta), StandardCharsets.UTF_8));
            editor.setCaretPosition(0);
            validar();
        } catch (IOException e) {
            mostrar("No se pudo leer el archivo: " + e.getMessage(), true);
        }
    }

    private void guardar() {
        if (validar() != null) {
            return;   // no se guarda una configuracion invalida
        }
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Files.write(selector.getSelectedFile().toPath(), editor.getText().getBytes(StandardCharsets.UTF_8));
            mostrar("Configuración guardada en " + selector.getSelectedFile().getName(), false);
        } catch (IOException e) {
            mostrar("No se pudo guardar el archivo: " + e.getMessage(), true);
        }
    }

    private void mostrar(String texto, boolean error) {
        mensaje.setForeground(error ? new Color(0xB00020) : new Color(0x1B5E20));
        mensaje.setText(texto);
    }

    private JButton boton(String texto, Runnable accion) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> accion.run());
        return b;
    }
}