package avilaos.disco;

import avilaos.model.PoliticaDisco;
import avilaos.util.Lista;

/**
 * @author andre
 *
 * FIFO: atiende las solicitudes en el orden en que llegaron. Es justa (nadie se
 * cuela), pero el cabezal va y viene por todo el disco.
 */
public class PoliticaFifo implements PlanificacionDisco {

    @Override
    public PoliticaDisco getTipo() {
        return PoliticaDisco.FIFO;
    }

    @Override
    public SolicitudDisco elegir(Lista<SolicitudDisco> cola, int cabezal, DireccionCabezal direccion,
                                 int totalBloques) {
        if (cola == null || cola.estaVacia()) {
            return null;
        }
        return cola.verPrimero();
    }
}
