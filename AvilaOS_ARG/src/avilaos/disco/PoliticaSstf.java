package avilaos.disco;

import avilaos.model.PoliticaDisco;
import avilaos.util.Iterador;
import avilaos.util.Lista;

/**
 * @author andre
 *
 * SSTF (Shortest Seek Time First): atiende la solicitud mas cercana al cabezal.
 * Recorre poco, pero puede causar inanicion: una solicitud lejana puede esperar
 * para siempre si siguen llegando solicitudes cercanas.
 *
 * Empate: gana la que llego primero (la que aparece antes en la cola), para que el
 * resultado sea predecible.
 */
public class PoliticaSstf implements PlanificacionDisco {

    @Override
    public PoliticaDisco getTipo() {
        return PoliticaDisco.SSTF;
    }

    @Override
    public SolicitudDisco elegir(Lista<SolicitudDisco> cola, int cabezal, DireccionCabezal direccion,
                                 int totalBloques) {
        if (cola == null || cola.estaVacia()) {
            return null;
        }
        SolicitudDisco mejor = null;
        int menorDistancia = Integer.MAX_VALUE;
        Iterador<SolicitudDisco> it = cola.iterador();
        while (it.tieneSiguiente()) {
            SolicitudDisco s = it.siguiente();
            int distancia = Math.abs(s.getBloqueDestino() - cabezal);
            // Menor estricto: en un empate se queda la que ya estaba (la que llego antes).
            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                mejor = s;
            }
        }
        return mejor;
    }
}
