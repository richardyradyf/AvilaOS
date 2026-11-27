package avilaos.app;

import avilaos.cluster.Cluster;
import avilaos.fs.ControlPermisos;
import avilaos.fs.SesionUsuario;
import avilaos.fs.SistemaArchivos;
import avilaos.gui.AccionesUsuario;
import avilaos.gui.ControlSimulacion;
import avilaos.model.PoliticaPlanificacion;
import avilaos.model.SolicitudProceso;
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
    private SesionUsuario sesion;
    private SistemaArchivos sistemaArchivos;

    public ControladorSimulador(Cluster cluster) {
        if (cluster == null) {
            throw new IllegalArgumentException("El cluster es obligatorio");
        }
        this.cluster = cluster;
    }

    /** Conecta el modo de usuario: desde aqui se rechazan las operaciones no permitidas. */
    public void setPermisos(SesionUsuario sesion, SistemaArchivos sistemaArchivos) {
        this.sesion = sesion;
        this.sistemaArchivos = sistemaArchivos;
    }

    @Override
    public String crearProceso(SolicitudProceso s) {
        try {
            if (enModoUsuario()) {
                if (!s.esDeEs()) {
                    return "Modo usuario: solo se pueden crear procesos de E/S sobre archivos";
                }
                String rechazo = ControlPermisos.verificar(sesion, s.getOperacion(), s.getRuta(), sistemaArchivos);
                if (rechazo != null) {
                    return rechazo;
                }
                s = SolicitudProceso.deEs(s.getNombre(), s.getInstrucciones(), s.getMemoria(), s.getPrioridad(),
                        s.getDeadline(), s.getOperacion(), s.getRuta(), s.getBloques(), s.getNuevoNombre(),
                        sesion.getUsuario());
            }
            if (s.esDeEs()) {
                cluster.crearProcesoArchivo(s);
                return null;
            }
            switch (s.getTipo()) {
                case CPU_BOUND:
                    cluster.crearProceso(s.getIdComputador(), s.getNombre(), s.getTipo(),
                            s.getPrioridad(), s.getMemoria(), s.getInstrucciones(), s.getDeadline());
                    break;
                case IO_BOUND:
                    cluster.crearProcesoEs(s.getIdComputador(), s.getNombre(), s.getPrioridad(),
                            s.getMemoria(), s.getInstrucciones(), s.getDeadline(),
                            s.getCadaCiclos(), s.getCiclosEs());
                    break;
                default:
                    return "Tipo de proceso no soportado: " + s.getTipo();
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
    public String cambiarPolitica(int idComputador, PoliticaPlanificacion politica, int quantum) {
        if (enModoUsuario()) {
            return "Modo usuario: solo el administrador puede cambiar la política de planificación";
        }
        return ejecutar(() -> cluster.cambiarPlanificador(idComputador,
                FabricaPlanificadores.crear(politica, quantum)));
    }

    private boolean enModoUsuario() {
        return sesion != null && sistemaArchivos != null && !sesion.esAdministrador();
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