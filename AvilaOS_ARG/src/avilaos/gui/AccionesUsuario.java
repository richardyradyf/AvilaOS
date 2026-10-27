package avilaos.gui;

import avilaos.model.SolicitudProceso;

/**
 * Lo que la interfaz le puede pedir al sistema. La ventana solo conoce esta interfaz:
 * observa al sistema y le hace pedidos, pero no contiene su logica. Mas adelante se
 * le conecta el cluster real.
 */
public interface AccionesUsuario {

    /**
     * Crea el proceso pedido por el usuario.
     *
     * @return null si se acepto, o un mensaje de error para mostrar en pantalla
     */
    String crearProceso(SolicitudProceso solicitud);
}
