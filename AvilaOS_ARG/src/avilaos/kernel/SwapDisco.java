/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.kernel;

import avilaos.disco.Disco;
import avilaos.disco.PuertoSwap;
import avilaos.disco.SolicitudDisco;
import avilaos.fs.MapaBloques;
import avilaos.model.Pcb;
import avilaos.model.TipoOperacionES;

/**
 *
 * @author richard
 * 
 * Area de intercambio real: reserva los bloques en el MapaBloques y manda la
 * escritura a la cola del disco. Un proceso ocupa tantos bloques como unidades de
 * memoria tiene, y en el mapa aparece como "swap:P7".
 */
public class SwapDisco implements PuertoSwap {

    private final MapaBloques mapa;
    private final Disco disco;

    public SwapDisco(MapaBloques mapa, Disco disco) {
        if (mapa == null || disco == null) {
            throw new IllegalArgumentException("El swap necesita el mapa de bloques y el disco");
        }
        this.mapa = mapa;
        this.disco = disco;
    }

    @Override
    public SolicitudDisco escribirEnSwap(Pcb proceso, long ciclo) {
        String ocupante = "swap:" + proceso.getEtiqueta();
        int primero = mapa.asignarSwap(proceso.getMemoria(), ocupante);
        if (primero < 0) {
            return null;
        }
        proceso.setBloqueSwap(primero);
        SolicitudDisco solicitud = new SolicitudDisco(proceso, TipoOperacionES.SUSPENDER, ocupante,
                primero, proceso.getMemoria(), ciclo);
        disco.encolar(solicitud);
        return solicitud;
    }
    @Override
    public SolicitudDisco leerDeSwap(Pcb proceso, long ciclo) {
        SolicitudDisco solicitud = new SolicitudDisco(proceso, TipoOperacionES.REACTIVAR,
                "swap:" + proceso.getEtiqueta(), proceso.getBloqueSwap(), proceso.getMemoria(), ciclo);
        disco.encolar(solicitud);
        return solicitud;
    }

    @Override
    public void liberarSwap(Pcb proceso) {
        if (proceso.getBloqueSwap() >= 0) {
            mapa.liberarSwap(proceso.getBloqueSwap());
            proceso.setBloqueSwap(-1);
        }
    }
}
