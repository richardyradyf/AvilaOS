package avilaos.gui;

import java.io.File;

/**
 * @author andre
 *
 * Contrato de acciones para la carga de los dos JSON y el control inicial de la simulacion.
 * Richard la implementa durante la integracion.
 *
 * Cada metodo devuelve null si la operacion fue exitosa, o un mensaje de error legible
 * si fallo para ser mostrado en la interfaz con JOptionPane.
 */
public interface AccionesCarga {

    /**
     * Carga y valida el JSON del sistema de archivos e inicializa el disco y los bloques.
     *
     * @param textoJson contenido en texto del archivo JSON
     * @return null si salio bien, o el mensaje de error si fallo
     */
    String cargarSistemaArchivos(String textoJson);

    /**
     * Carga y valida el JSON de procesos y configuracion.
     *
     * @param textoJson contenido en texto del archivo JSON
     * @return null si salio bien, o el mensaje de error si fallo
     */
    String cargarProcesos(String textoJson);

    /**
     * Inicia la ejecucion de la simulacion una vez que ambos JSON fueron cargados con exito.
     *
     * @return null si salio bien, o el mensaje de error si fallo
     */
    String iniciar();

    /**
     * Reinicia la simulacion cargando nuevos procesos pero preservando el estado modificado
     * del disco y del sistema de archivos de la ejecucion previa.
     *
     * @param textoJson nuevo JSON de procesos
     * @return null si salio bien, o el mensaje de error si fallo
     */
    String reiniciarConservandoDisco(String textoJson);

    /**
     * Exporta el estado actual del sistema de archivos al archivo destino en formato JSON.
     *
     * @param destino archivo en disco donde se guardara el JSON
     * @return null si salio bien, o el mensaje de error si fallo
     */
    String guardarSistemaArchivos(File destino);
}
