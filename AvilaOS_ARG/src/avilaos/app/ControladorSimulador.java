package avilaos.app;

import avilaos.cluster.Cluster;
import avilaos.config.ValidadorEntradas;
import avilaos.gui.AccionesUsuario;
import avilaos.gui.ControlSimulacion;
import avilaos.model.PoliticaPlanificacion;
import avilaos.model.SolicitudProceso;
import avilaos.model.TipoProceso;
import avilaos.scheduler.FabricaPlanificadores;

/**
 * Conecta la interfaz con el cluster. Es el unico lugar donde las acciones del usuario
 * se convierten en llamadas al sistema; cualquier error se devuelve como mensaje en vez
 * de dejar que la ventana se caiga.
 * 
 * @author andres
 */
public class ControladorSimulador implements AccionesUsuario, ControlSimulacion {

    private final Cluster cluster;

    public ControladorSimulador(Cluster cluster) {
        if (cluster == null) {
            throw new IllegalArgumentException("El cluster es obligatorio");
        }
        this.cluster = cluster;
    }

    @Override
    public String crearProceso(SolicitudProceso s) {
        try {
            switch (s.getTipo()) {
                case IO_BOUND:
                    cluster.crearProcesoEs(s.getIdComputador(), s.getNombre(), s.getPrioridad(),
                            s.getMemoria(), s.getInstrucciones(), s.getDeadline(),
                            s.getCadaCiclos(), s.getCiclosEs());
                    break;
                case PRODUCTOR:
                case CONSUMIDOR:
                    cluster.crearProcesoBuffer(s.getIdComputador(), s.getNombre(), s.getTipo(),
                            s.getPrioridad(), s.getMemoria(), s.getDeadline(), s.getIdBuffer(),
                            s.getCadaCiclos(), s.getElementos());
                    break;
                default:
                    cluster.crearProceso(s.getIdComputador(), s.getNombre(), s.getTipo(),
                            s.getPrioridad(), s.getMemoria(), s.getInstrucciones(), s.getDeadline());
                    break;
            }
            return null;
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    @Override
    public String iniciar() {
        return ejecutar(() -> cluster.iniciar());
    }

    @Override
    public String pausar() {
        return ejecutar(() -> cluster.pausar());
    }

    @Override
    public String reanudar() {
        return ejecutar(() -> cluster.reanudar());
    }

    @Override
    public String detener() {
        return ejecutar(() -> cluster.detener());
    }

    @Override
    public String avanzarCiclo() {
        return ejecutar(() -> cluster.avanzarCiclo());
    }

    @Override
    public String setDuracionCicloMs(int ms) {
        return ejecutar(() -> cluster.setDuracionCicloMs(ms));
    }

    @Override
    public String setLatenciaRed(int ciclos) {
        return ejecutar(() -> cluster.setLatenciaRed(ciclos));
    }

    @Override
    public String crearBuffer(int idAnfitrion, int capacidad) {
        return ejecutar(() -> cluster.crearBuffer(idAnfitrion, capacidad));
    }

    @Override
    public String cambiarPolitica(int idComputador, PoliticaPlanificacion politica, int quantum) {
        return ejecutar(() -> cluster.cambiarPlanificador(idComputador,
                FabricaPlanificadores.crear(politica, quantum)));
    }

    private static String ejecutar(Runnable accion) {
        try {
            accion.run();
            return null;
        } catch (RuntimeException e) {
            return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        }
    }
}