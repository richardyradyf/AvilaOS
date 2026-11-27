package avilaos.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * @author andre
 *
 * Panel de carga obligatoria de los dos archivos JSON antes de iniciar el reloj (Dia 9):
 * 1. JSON del Sistema de Archivos (mapa de bloques, tamano y archivos iniciales).
 * 2. JSON de Procesos y Configuracion (politicas, RAM y solicitudes de E/S).
 *
 * El boton "Iniciar simulacion" permanece deshabilitado hasta que ambos JSON esten
 * cargados y validados correctamente. Ademas, incluye los controles de "Reiniciar con
 * nuevos procesos (conservar disco)" y "Guardar sistema de archivos...".
 */
public class PanelCarga extends JPanel {

    private static final Color COLOR_OK = new Color(0x2E7D32);     // Verde oscuro legible
    private static final Color COLOR_ERROR = new Color(0xC62828);  // Rojo oscuro legible
    private static final Color COLOR_ESPERA = new Color(0x616161); // Gris neutro

    private final AccionesCarga acciones;

    private boolean sistemaArchivosCargado = false;
    private boolean procesosCargados = false;
    private boolean simulacionIniciada = false;

    private final JButton btnCargarFs;
    private final JLabel lblEstadoFs;

    private final JButton btnCargarProcesos;
    private final JLabel lblEstadoProcesos;

    private final JButton btnIniciar;
    private final JButton btnReiniciar;
    private final JButton btnGuardarFs;

    private final JFileChooser fileChooser;

    public PanelCarga(AccionesCarga acciones) {
        if (acciones == null) {
            throw new IllegalArgumentException("acciones no puede ser null");
        }
        this.acciones = acciones;

        setLayout(new BorderLayout(10, 10));
        setBackground(PaletaColores.FONDO_PANEL);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(PaletaColores.BORDE_SUAVE, 1),
                        "Carga de Configuración y Control de Simulación (Día 9)",
                        0, 0, new Font("SansSerif", Font.BOLD, 12), Color.DARK_GRAY),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        // Configurar selector de archivos
        fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos JSON (*.json)", "json"));
        fileChooser.setMultiSelectionEnabled(false);

        // --- Panel Central con los selectores y estados ---
        JPanel panelArchivos = new JPanel(new GridBagLayout());
        panelArchivos.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        // Fila 1: Sistema de Archivos
        gbc.gridx = 0;
        gbc.gridy = 0;
        btnCargarFs = new JButton("1. Cargar sistema de archivos…");
        btnCargarFs.setFocusPainted(false);
        btnCargarFs.addActionListener(e -> seleccionarYCargarFs());
        panelArchivos.add(btnCargarFs, gbc);

        gbc.gridx = 1;
        lblEstadoFs = new JLabel("✖ Sin cargar (obligatorio)");
        lblEstadoFs.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblEstadoFs.setForeground(COLOR_ESPERA);
        panelArchivos.add(lblEstadoFs, gbc);

        // Fila 2: Procesos y Configuracion
        gbc.gridx = 0;
        gbc.gridy = 1;
        btnCargarProcesos = new JButton("2. Cargar procesos y config…");
        btnCargarProcesos.setFocusPainted(false);
        btnCargarProcesos.addActionListener(e -> seleccionarYCargarProcesos());
        panelArchivos.add(btnCargarProcesos, gbc);

        gbc.gridx = 1;
        lblEstadoProcesos = new JLabel("✖ Sin cargar (obligatorio)");
        lblEstadoProcesos.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblEstadoProcesos.setForeground(COLOR_ESPERA);
        panelArchivos.add(lblEstadoProcesos, gbc);

        add(panelArchivos, BorderLayout.CENTER);

        // --- Panel Inferior con los botones de accion ---
        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        panelAcciones.setOpaque(false);

        btnIniciar = new JButton("▶ Iniciar simulación");
        btnIniciar.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnIniciar.setEnabled(false);
        btnIniciar.addActionListener(e -> iniciarSimulacion());
        panelAcciones.add(btnIniciar);

        btnReiniciar = new JButton("Reiniciar con nuevos procesos");
        btnReiniciar.setToolTipText("Reinicia la simulación cargando nuevos procesos pero conservando el disco");
        btnReiniciar.setEnabled(false);
        btnReiniciar.addActionListener(e -> reiniciarConNuevosProcesos());
        panelAcciones.add(btnReiniciar);

        btnGuardarFs = new JButton("Guardar sistema de archivos…");
        btnGuardarFs.setToolTipText("Exporta el estado actual del disco y archivos a un JSON");
        btnGuardarFs.addActionListener(e -> guardarSistemaArchivos());
        panelAcciones.add(btnGuardarFs);

        add(panelAcciones, BorderLayout.SOUTH);
    }

    // ================= Logica de seleccion y carga =================

    private void seleccionarYCargarFs() {
        int resp = fileChooser.showOpenDialog(this);
        if (resp != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File archivo = fileChooser.getSelectedFile();
        try {
            String contenido = Files.readString(archivo.toPath(), StandardCharsets.UTF_8);
            String error = acciones.cargarSistemaArchivos(contenido);
            if (error == null) {
                sistemaArchivosCargado = true;
                lblEstadoFs.setText("✔ " + archivo.getName());
                lblEstadoFs.setForeground(COLOR_OK);
            } else {
                sistemaArchivosCargado = false;
                lblEstadoFs.setText("✖ " + archivo.getName() + " (error)");
                lblEstadoFs.setForeground(COLOR_ERROR);
                JOptionPane.showMessageDialog(this, error, "Error al cargar Sistema de Archivos", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            sistemaArchivosCargado = false;
            lblEstadoFs.setText("✖ " + archivo.getName() + " (error de lectura)");
            lblEstadoFs.setForeground(COLOR_ERROR);
            JOptionPane.showMessageDialog(this, "No se pudo leer el archivo: " + ex.getMessage(),
                    "Error de lectura", JOptionPane.ERROR_MESSAGE);
        }
        actualizarEstadoBotonIniciar();
    }

    private void seleccionarYCargarProcesos() {
        int resp = fileChooser.showOpenDialog(this);
        if (resp != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File archivo = fileChooser.getSelectedFile();
        try {
            String contenido = Files.readString(archivo.toPath(), StandardCharsets.UTF_8);
            String error = acciones.cargarProcesos(contenido);
            if (error == null) {
                procesosCargados = true;
                lblEstadoProcesos.setText("✔ " + archivo.getName());
                lblEstadoProcesos.setForeground(COLOR_OK);
            } else {
                procesosCargados = false;
                lblEstadoProcesos.setText("✖ " + archivo.getName() + " (error)");
                lblEstadoProcesos.setForeground(COLOR_ERROR);
                JOptionPane.showMessageDialog(this, error, "Error al cargar Procesos", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            procesosCargados = false;
            lblEstadoProcesos.setText("✖ " + archivo.getName() + " (error de lectura)");
            lblEstadoProcesos.setForeground(COLOR_ERROR);
            JOptionPane.showMessageDialog(this, "No se pudo leer el archivo: " + ex.getMessage(),
                    "Error de lectura", JOptionPane.ERROR_MESSAGE);
        }
        actualizarEstadoBotonIniciar();
    }

    private void actualizarEstadoBotonIniciar() {
        boolean ambosCargados = sistemaArchivosCargado && procesosCargados;
        btnIniciar.setEnabled(ambosCargados && !simulacionIniciada);
    }

    private void iniciarSimulacion() {
        String error = acciones.iniciar();
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Error al iniciar", JOptionPane.ERROR_MESSAGE);
            return;
        }
        simulacionIniciada = true;
        btnIniciar.setEnabled(false);
        btnCargarFs.setEnabled(false);
        btnCargarProcesos.setEnabled(false);
        btnReiniciar.setEnabled(true);
        JOptionPane.showMessageDialog(this, "Simulación iniciada correctamente con ambos JSON cargados.",
                "Simulación Iniciada", JOptionPane.INFORMATION_MESSAGE);
    }

    private void reiniciarConNuevosProcesos() {
        int resp = fileChooser.showOpenDialog(this);
        if (resp != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File archivo = fileChooser.getSelectedFile();
        try {
            String contenido = Files.readString(archivo.toPath(), StandardCharsets.UTF_8);
            String error = acciones.reiniciarConservandoDisco(contenido);
            if (error == null) {
                lblEstadoProcesos.setText("✔ " + archivo.getName() + " (reinicio)");
                lblEstadoProcesos.setForeground(COLOR_OK);
                JOptionPane.showMessageDialog(this, "Procesos cargados exitosamente preservando el estado actual del disco.",
                        "Reinicio Exitoso", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, error, "Error al reiniciar procesos", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo leer el archivo: " + ex.getMessage(),
                    "Error de lectura", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardarSistemaArchivos() {
        int resp = fileChooser.showSaveDialog(this);
        if (resp != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File archivo = fileChooser.getSelectedFile();
        if (!archivo.getName().toLowerCase().endsWith(".json")) {
            archivo = new File(archivo.getParentFile(), archivo.getName() + ".json");
        }
        String error = acciones.guardarSistemaArchivos(archivo);
        if (error == null) {
            JOptionPane.showMessageDialog(this, "Sistema de archivos guardado correctamente en:\n" + archivo.getAbsolutePath(),
                    "Guardado Exitoso", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, error, "Error al guardar", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================= Getters para verificacion y pruebas =================

    public boolean isSistemaArchivosCargado() {
        return sistemaArchivosCargado;
    }

    public boolean isProcesosCargados() {
        return procesosCargados;
    }

    public boolean isSimulacionIniciada() {
        return simulacionIniciada;
    }

    public JButton getBtnIniciar() {
        return btnIniciar;
    }

    public JButton getBtnCargarFs() {
        return btnCargarFs;
    }

    public JButton getBtnCargarProcesos() {
        return btnCargarProcesos;
    }

    public JButton getBtnReiniciar() {
        return btnReiniciar;
    }

    public JButton getBtnGuardarFs() {
        return btnGuardarFs;
    }
}
