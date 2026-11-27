package avilaos.disco;

import avilaos.model.PoliticaDisco;
import avilaos.util.Iterador;
import avilaos.util.Lista;

/**
 * @author andre
 *
 * C-SCAN (SCAN circular): el cabezal solo atiende subiendo. Al llegar al ultimo bloque
 * salta al bloque 0 sin atender nada y vuelve a subir. Asi las esperas son mas parejas
 * que en SCAN, porque los extremos del disco no quedan en desventaja.
 *
 * El salto del ultimo bloque al 0 no se suma al desplazamiento; el disco lo cuenta
 * aparte.
 *
 * Empate (dos solicitudes al mismo bloque): gana la que llego primero.
 */
public class PoliticaCScan implements PlanificacionDisco {

    @Override
    public PoliticaDisco getTipo() {
        return PoliticaDisco.C_SCAN;
    }

    @Override
    public SolicitudDisco elegir(Lista<SolicitudDisco> cola, int cabezal, DireccionCabezal direccion,
                                 int totalBloques) {
        if (cola == null || cola.estaVacia()) {
            return null;
        }
        // La mas cercana por encima del cabezal (incluido su bloque). Si no hay, despues
        // del salto al 0 toca la de bloque mas bajo.
        SolicitudDisco siguienteArriba = null;
        SolicitudDisco masBaja = null;
        Iterador<SolicitudDisco> it = cola.iterador();
        while (it.tieneSiguiente()) {
            SolicitudDisco s = it.siguiente();
            int bloque = s.getBloqueDestino();
            // Menor estricto: en un empate se queda la que llego antes.
            if (bloque >= cabezal
                    && (siguienteArriba == null || bloque < siguienteArriba.getBloqueDestino())) {
                siguienteArriba = s;
            }
            if (masBaja == null || bloque < masBaja.getBloqueDestino()) {
                masBaja = s;
            }
        }
        return siguienteArriba != null ? siguienteArriba : masBaja;
    }

    /** Las solicitudes por debajo del cabezal se atienden despues de llegar al extremo y saltar. */
    @Override
    public boolean vaAlExtremoAntes(int cabezal, int destino, DireccionCabezal direccion) {
        return destino < cabezal;
    }

    @Override
    public boolean saltaAlInicio() {
        return true;
    }
}
