/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package avilaos.app;

import avilaos.fs.SesionUsuario;
import avilaos.gui.PanelArbolArchivos;
import avilaos.gui.PanelCarga;
import avilaos.gui.PanelModoUsuario;
import avilaos.gui.PanelTablaAsignacion;
import avilaos.gui.VentanaPrincipal;
import avilaos.gui.VistaDisco;
import avilaos.gui.VistaMetricas;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

/**
 * Punto de entrada de AvilaOS. Primero se abre el panel de carga, que exige los dos JSON
 * (sistema de archivos y procesos) antes de iniciar el reloj. Cada vez que se inicia o se
 * reinicia con procesos nuevos, se cierra la ventana anterior y se abre una con el
 * sistema que esta corriendo; el panel de carga pasa a su pestaña "Carga". El modo de
 * usuario se conserva entre reinicios.
 */
public class Main {

    private static final SesionUsuario sesion = new SesionUsuario();

    private static PanelCarga panelCarga;
    private static JFrame ventanaInicio;
    private static VentanaPrincipal ventana;
    private static VistaMetricas vistaMetricas;
    private static VistaDisco vistaDisco;
    private static PanelArbolArchivos panelArchivos;
    private static PanelTablaAsignacion panelTabla;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::mostrarCarga);
    }

    /** Ventana inicial: solo el panel de carga. */
    private static void mostrarCarga() {
        panelCarga = new PanelCarga(new ControladorCarga(Main::mostrarSistema));
        ventanaInicio = new JFrame("ÁvilaOS - Carga inicial");
        ventanaInicio.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventanaInicio.add(panelCarga);
        ventanaInicio.pack();
        ventanaInicio.setLocationRelativeTo(null);
        ventanaInicio.setVisible(true);
    }

    /** Se llama en el hilo de Swing, desde los botones del panel de carga. */
    private static void mostrarSistema(Sistema sistema) {
        detenerVistas();
        if (ventana != null) {
            ventana.dispose();
        }
        VentanaPrincipal nueva = new VentanaPrincipal(sistema.getControlador(), sistema.getControlador(),
                sistema.getCluster());
        sistema.getControlador().setPermisos(sesion, sistema.getSistemaArchivos());
        PanelModoUsuario panelModo = new PanelModoUsuario(sesion);
        nueva.agregarArriba(panelModo);

        panelArchivos = new PanelArbolArchivos(sistema.getSistemaArchivos());
        panelTabla = new PanelTablaAsignacion(sistema.getSistemaArchivos());
        JSplitPane archivos = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelArchivos, panelTabla);
        archivos.setResizeWeight(0.6);
        vistaDisco = new VistaDisco(sistema.getDisco(), sistema.getSistemaArchivos().getMapa(),
                sistema.getMetricasDisco());
        vistaDisco.setCambiosPermitidos(sesion.esAdministrador());
        panelModo.agregarOyenteModo(s -> vistaDisco.setCambiosPermitidos(s.esAdministrador()));
        vistaMetricas = new VistaMetricas(sistema.getRecolector());

        nueva.agregarPestana("Archivos", archivos);
        nueva.agregarPestana("Disco", vistaDisco);
        nueva.agregarPestana("Métricas", vistaMetricas);
        nueva.agregarPestana("Carga", panelCarga);
        ventana = nueva;
        nueva.setVisible(true);
        if (ventanaInicio != null) {
            ventanaInicio.dispose();
            ventanaInicio = null;
        }
    }

    /** Detiene el refresco de las vistas de la ventana anterior. */
    private static void detenerVistas() {
        if (vistaMetricas != null) {
            vistaMetricas.detener();
        }
        if (vistaDisco != null) {
            vistaDisco.detener();
        }
        if (panelArchivos != null) {
            panelArchivos.detener();
        }
        if (panelTabla != null) {
            panelTabla.detener();
        }
    }
}