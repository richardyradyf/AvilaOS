package avilaos.demo;

import avilaos.app.ControladorSimulador;
import avilaos.cluster.Cluster;
import avilaos.gui.VentanaPrincipal;
import avilaos.model.TipoProceso;
import avilaos.scheduler.PlanificadorRoundRobin;
import javax.swing.SwingUtilities;

/**
 * Prueba visual: abre la ventana con un cluster de 2 computadores y algunos procesos y
 * buffers de ejemplo para revisar las vistas. Ejecutar y probar los botones a mano.
 * 
 * @author andre
 */
public class DemoInterfaz {

    public static void main(String[] args) {
        Cluster cluster = new Cluster(300, 500);
        cluster.agregarComputador(256, new PlanificadorRoundRobin(3));
        cluster.crearProceso(1, "Calculo", TipoProceso.CPU_BOUND, 2, 20, 40, 200);
        cluster.crearProcesoEs(1, "Lector", 3, 20, 20, 300, 4, 3);
        cluster.crearProceso(1, "Calculo B", TipoProceso.CPU_BOUND, 1, 10, 30, 500);

        ControladorSimulador controlador = new ControladorSimulador(cluster);
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal ventana = new VentanaPrincipal(controlador, controlador, cluster);
            ventana.setVisible(true);
        });
    }
}