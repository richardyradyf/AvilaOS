package avilaos.gui;

import avilaos.model.PoliticaPlanificacion;

/**
 * Controles de ejecucion que la interfaz le pide al sistema (reloj, parametros en
 * caliente, buffers y politica). Todos devuelven null si la accion se realizo, o un
 * mensaje de error para mostrar en pantalla.
 * 
 * @author andre
 */
public interface ControlSimulacion {

    String iniciar();

    String pausar();

    String reanudar();

    String detener();

    /** Avanza un solo ciclo (modo paso a paso). */
    String avanzarCiclo();

    String setDuracionCicloMs(int ms);

    String setLatenciaRed(int ciclos);

    /** idAnfitrion 0 = automatico. */
    String crearBuffer(int idAnfitrion, int capacidad);

    /** Cambia la politica de un computador en caliente. */
    String cambiarPolitica(int idComputador, PoliticaPlanificacion politica, int quantum);
}