package avilaos.disco;

import avilaos.model.PoliticaDisco;
import avilaos.util.Iterador;
import avilaos.util.Lista;

/**
 * @author andre
 *
 * SCAN (algoritmo del ascensor): el cabezal avanza en una direccion atendiendo todas
 * las solicitudes que encuentra, llega hasta el extremo del disco y ahi invierte la
 * direccion. No causa inanicion: toda solicitud se atiende, a mas tardar, en la vuelta
 * siguiente.
 *
 * Empate (dos solicitudes al mismo bloque): gana la que llego primero.
 */
public class PoliticaScan implements PlanificacionDisco {

    @Override
    public PoliticaDisco getTipo() {
        return PoliticaDisco.SCAN;
    }

    @Override
    public SolicitudDisco elegir(Lista<SolicitudDisco> cola, int cabezal, DireccionCabezal direccion,
                                 int totalBloques) {
        if (cola == null || cola.estaVacia()) {
            return null;
        }
        // Primero la mas cercana en la direccion actual; si no hay ninguna, el cabezal
        // va al extremo, invierte y toma la mas cercana en la otra direccion.
        SolicitudDisco adelante = masCercanaEnDireccion(cola, cabezal, direccion);
        if (adelante != null) {
            return adelante;
        }
        return masCercanaEnDireccion(cola, cabezal, direccion.invertir());
    }

    /** Si la solicitud queda detras del cabezal, primero hay que llegar al extremo. */
    @Override
    public boolean vaAlExtremoAntes(int cabezal, int destino, DireccionCabezal direccion) {
        return direccion == DireccionCabezal.ASCENDENTE ? destino < cabezal : destino > cabezal;
    }

    /**
     * La solicitud mas cercana al cabezal entre las que estan en la direccion dada
     * (incluye la del mismo bloque del cabezal), o null si no hay ninguna.
     */
    private SolicitudDisco masCercanaEnDireccion(Lista<SolicitudDisco> cola, int cabezal,
                                                 DireccionCabezal direccion) {
        SolicitudDisco mejor = null;
        int menorDistancia = Integer.MAX_VALUE;
        Iterador<SolicitudDisco> it = cola.iterador();
        while (it.tieneSiguiente()) {
            SolicitudDisco s = it.siguiente();
            int bloque = s.getBloqueDestino();
            boolean enDireccion = direccion == DireccionCabezal.ASCENDENTE ? bloque >= cabezal : bloque <= cabezal;
            int distancia = Math.abs(bloque - cabezal);
            // Menor estricto: en un empate se queda la que llego antes.
            if (enDireccion && distancia < menorDistancia) {
                menorDistancia = distancia;
                mejor = s;
            }
        }
        return mejor;
    }
}
