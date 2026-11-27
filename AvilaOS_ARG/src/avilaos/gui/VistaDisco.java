/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.gui;

import avilaos.disco.Disco;
import avilaos.fs.MapaBloques;
import avilaos.metrics.MetricasDisco;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.Timer;

/**
 *
 * @author richard
 * 
 * Pestaña del disco: arriba el mapa de bloques con el cabezal; abajo la cola de E/S
 * (con la politica y la velocidad modificables en caliente) y la grafica del
 * desplazamiento acumulado. Se refresca sola cada 300 ms.
 */
public class VistaDisco extends JPanel {

    private final PanelDisco panelDisco;
    private final PanelColaDisco panelCola;
    private final PanelGraficaDisco panelGrafica;
    private final Timer temporizador;

    public VistaDisco(Disco disco, MapaBloques mapa, MetricasDisco metricas) {
        super(new BorderLayout());
        panelDisco = new PanelDisco(disco, mapa);
        panelCola = new PanelColaDisco(disco, metricas);
        panelGrafica = new PanelGraficaDisco(metricas);

        JPanel abajo = new JPanel(new GridLayout(1, 2, 6, 6));
        abajo.add(panelCola);
        abajo.add(panelGrafica);
        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(panelDisco), abajo);
        division.setResizeWeight(0.5);
        add(division, BorderLayout.CENTER);

        actualizar();
        temporizador = new Timer(300, e -> actualizar());
        temporizador.start();
    }
    
        /** En modo usuario bloquea el cambio de politica y de velocidad del disco. */
    public void setCambiosPermitidos(boolean permitidos) {
        panelCola.setCambiosPermitidos(permitidos);
    }

    public boolean isCambiosPermitidos() {
        return panelCola.isCambiosPermitidos();
    }

    /** Vuelve a leer el disco y las metricas y los muestra. */
    public void actualizar() {
        panelDisco.actualizar();
        panelCola.actualizar();
        panelGrafica.actualizar();
    }

    /** Detiene el refresco; se llama al cerrar la ventana o al reiniciar. */
    public void detener() {
        temporizador.stop();
    }

    public boolean estaRefrescando() {
        return temporizador.isRunning();
    }
}