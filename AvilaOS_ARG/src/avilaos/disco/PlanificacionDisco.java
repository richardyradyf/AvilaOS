package avilaos.disco;

import avilaos.model.PoliticaDisco;
import avilaos.util.Lista;

/**
 * @author andre
 *
 * Politica de planificacion del disco (patron estrategia, igual que Planificador en
 * el Proyecto 1). El disco no sabe que politica usa: solo le pregunta cual es la
 * proxima solicitud y si debe ir al extremo antes de llegar a ella. Por eso agregar
 * una politica nueva (por ejemplo LOOK) es escribir otra clase que implemente esta
 * interfaz y agregarla a la fabrica, sin tocar el disco.
 */
public interface PlanificacionDisco {

    PoliticaDisco getTipo();

    /**
     * Elige, sin sacarla de la cola, la proxima solicitud a atender.
     *
     * @param cola         solicitudes pendientes, en orden de llegada
     * @param cabezal      bloque donde esta el cabezal
     * @param direccion    direccion en la que se mueve el cabezal
     * @param totalBloques cantidad de bloques del disco (el ultimo es totalBloques - 1)
     * @return la solicitud elegida, o null si la cola esta vacia
     */
    SolicitudDisco elegir(Lista<SolicitudDisco> cola, int cabezal, DireccionCabezal direccion,
                          int totalBloques);

    /**
     * true si, para llegar a destino, el cabezal debe ir primero hasta el extremo del
     * disco (SCAN y C-SCAN).
     */
    default boolean vaAlExtremoAntes(int cabezal, int destino, DireccionCabezal direccion) {
        return false;
    }

    /**
     * true si al llegar al extremo el cabezal salta al bloque 0 en vez de invertir la
     * direccion (solo C-SCAN).
     */
    default boolean saltaAlInicio() {
        return false;
    }
}
