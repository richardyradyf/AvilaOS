/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package avilaos.app;

import avilaos.config.ConfiguracionSimulacion;
import avilaos.config.EntradaInvalidaException;
import avilaos.gui.VentanaPrincipal;
import avilaos.gui.VistaConfiguracion;
import avilaos.gui.VistaMetricas;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Punto de entrada de AvilaOS. Arma el sistema con la configuracion de "configuracion.json"
 * (si existe en la carpeta de trabajo) o con la de ejemplo, y abre la ventana. Desde la
 * pestaña "Configuración" se puede reiniciar con otra configuracion: se detiene el reloj
 * actual, se cierra la ventana y se arma un sistema nuevo.
 */
public class Main {

    private static final String ARCHIVO_CONFIGURACION = "configuracion.json";

    private static Sistema sistema;
    private static VentanaPrincipal ventana;
    private static VistaMetricas vistaMetricas;

    public static void main(String[] args) {
        ConfiguracionSimulacion config = cargarInicial();
        SwingUtilities.invokeLater(() -> {
            try {
                arrancar(config);
            } catch (EntradaInvalidaException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "No se pudo iniciar ÁvilaOS",
                        JOptionPane.ERROR_MESSAGE);
                arrancar(ConfiguracionSimulacion.porDefecto());
            }
        });
    }

    /** Lee configuracion.json si existe; si no existe o es invalido usa el ejemplo. */
    private static ConfiguracionSimulacion cargarInicial() {
        Path ruta = Paths.get(ARCHIVO_CONFIGURACION);
        if (Files.exists(ruta)) {
            try {
                return ConfiguracionSimulacion.desdeJson(new String(Files.readAllBytes(ruta), StandardCharsets.UTF_8));
            } catch (IOException | EntradaInvalidaException e) {
                System.out.println("No se pudo usar " + ARCHIVO_CONFIGURACION + " (" + e.getMessage()
                        + "); se usa la configuración de ejemplo.");
            }
        }
        return ConfiguracionSimulacion.porDefecto();
    }

    /** Debe llamarse en el hilo de eventos de Swing. */
    private static void arrancar(ConfiguracionSimulacion config) {
        Sistema nuevo = Sistema.construir(config);
        if (sistema != null) {
            sistema.getCluster().detener();
        }
        if (vistaMetricas != null) {
            vistaMetricas.detener();
        }
        if (ventana != null) {
            ventana.dispose();
        }
        sistema = nuevo;
        VentanaPrincipal nueva = new VentanaPrincipal(nuevo.getControlador(), nuevo.getControlador(), nuevo.getCluster());
        vistaMetricas = new VistaMetricas(nuevo.getRecolector());
        nueva.agregarPestana("Métricas", vistaMetricas);
        nueva.agregarPestana("Configuración", new VistaConfiguracion(Main::reiniciar, config));
        ventana = nueva;
        nueva.setVisible(true);
    }

    private static String reiniciar(ConfiguracionSimulacion config) {
        try {
            // se reinicia despues de este evento, para no cerrar la ventana desde su propio boton
            SwingUtilities.invokeLater(() -> {
                try {
                    arrancar(config);
                } catch (EntradaInvalidaException e) {
                    JOptionPane.showMessageDialog(ventana, e.getMessage(), "No se pudo aplicar",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
            return null;
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }
}
