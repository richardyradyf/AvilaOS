package avilaos.gui;

import avilaos.cluster.Cluster;
import java.awt.BorderLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Ventana principal de AvilaOS. Arriba los controles de ejecucion; debajo las pestañas:
 * crear proceso, computadores y log. Otras pestañas (configuracion, metricas) se agregan
 * con agregarPestana. La ventana solo observa al sistema y le hace pedidos a traves de
 * AccionesUsuario y ControlSimulacion.
 */
public class VentanaPrincipal extends JFrame {

    private final PanelControles controles;
    private final PanelCrearProceso panelCrear;
    private final PanelComputador panelComputador;
    private final PanelLog panelLog;
    private final JTabbedPane pestanas = new JTabbedPane();
    private final Timer temporizador;

    public VentanaPrincipal(AccionesUsuario acciones, ControlSimulacion control, Cluster cluster) {
        super("ÁvilaOS - Simulador de sistema operativo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        controles = new PanelControles(control, cluster);
        panelCrear = new PanelCrearProceso(acciones);
        panelComputador = new PanelComputador(cluster, cluster.getComputador(1), control);

        panelLog = new PanelLog(cluster.getBitacora());

        pestanas.addTab("Crear proceso", panelCrear);
        pestanas.addTab("Computador", new JScrollPane(panelComputador));
        pestanas.addTab("Log", panelLog);

        add(controles, BorderLayout.NORTH);
        add(pestanas, BorderLayout.CENTER);
        setSize(1100, 720);
        setLocationRelativeTo(null);

        refrescar();
        temporizador = new Timer(300, e -> refrescar());
        temporizador.start();
    }

    @Override
    public void dispose() {
        temporizador.stop();
        super.dispose();
    }

    /** Permite agregar pestañas desde fuera (configuracion, metricas). */
    public void agregarPestana(String titulo, JComponent contenido) {
        pestanas.addTab(titulo, contenido);
    }

    public PanelCrearProceso getPanelCrear() {
        return panelCrear;
    }

    /** Vuelve a leer el estado del sistema y lo muestra. Se llama sola cada 300 ms. */
    public void refrescar() {
        controles.refrescar();
        panelComputador.refrescar();
        
    }
    /** Abre la ventana en el hilo de eventos de Swing. */
    public void mostrar() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }
}