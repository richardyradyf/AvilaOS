package avilaos.demo;

import avilaos.app.ControladorSimulador;
import avilaos.cluster.Cluster;
import avilaos.gui.VentanaPrincipal;
import avilaos.model.TipoProceso;
import avilaos.scheduler.PlanificadorFcfs;
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
        Cluster cluster = new Cluster(300, 500, 3);
        cluster.agregarComputador(256, new PlanificadorFcfs());
        cluster.agregarComputador(128, new PlanificadorRoundRobin(3));
        int buffer = cluster.crearBuffer(2, 3).getId();
        cluster.crearProceso(1, "Calculo", TipoProceso.CPU_BOUND, 2, 20, 40, 200);
        cluster.crearProcesoEs(1, "Lector", 3, 20, 20, 300, 4, 3);
        cluster.crearProcesoBuffer(1, "Productor", TipoProceso.PRODUCTOR, 1, 10, 500, buffer, 2, 10);
        cluster.crearProcesoBuffer(2, "Consumidor", TipoProceso.CONSUMIDOR, 1, 10, 500, buffer, 2, 10);

        ControladorSimulador controlador = new ControladorSimulador(cluster);
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal ventana = new VentanaPrincipal(controlador, controlador, cluster);
            ventana.setVisible(true);
        });
    }
}