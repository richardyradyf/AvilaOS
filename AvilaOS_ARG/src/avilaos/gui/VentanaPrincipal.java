package avilaos.gui;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Ventana principal de ÁvilaOS. Por ahora tiene la vista de creación de procesos; las
 * vistas de los computadores, buffers y log se agregan en las siguientes fases.
 */
public class VentanaPrincipal extends JFrame {

    private final PanelCrearProceso panelCrear;

    public VentanaPrincipal(AccionesUsuario acciones, int numeroComputadores) {
        super("ÁvilaOS - Simulador de sistema operativo distribuido");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panelCrear = new PanelCrearProceso(acciones, numeroComputadores);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Crear proceso", panelCrear);
        pestanas.addTab("Computadores", marcador("Vista de los computadores (pendiente)"));
        pestanas.addTab("Buffers y log", marcador("Buffers y registro de eventos (pendiente)"));
        add(pestanas, BorderLayout.CENTER);

        setSize(900, 620);
        setLocationRelativeTo(null);
    }

    public PanelCrearProceso getPanelCrear() {
        return panelCrear;
    }

    private static JPanel marcador(String texto) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(texto, SwingConstants.CENTER), BorderLayout.CENTER);
        return panel;
    }

    /** Prueba visual: abre la ventana con un sistema de mentira que solo imprime lo que recibe. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AccionesUsuario ficticias = solicitud -> {
                if ("rechazar".equalsIgnoreCase(solicitud.getNombre())) {
                    return "El sistema rechazó el proceso (prueba del mensaje de error)";
                }
                System.out.println("Solicitud recibida: " + solicitud);
                return null;
            };
            VentanaPrincipal ventana = new VentanaPrincipal(ficticias, 2);
            ventana.getPanelCrear().setBuffers(new int[]{1, 2});
            ventana.setVisible(true);
        });
    }
}
