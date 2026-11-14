/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.disco;

import avilaos.model.Pcb;
import avilaos.model.TipoOperacionES;

/**
 *
 * @author richard
 * 
 * Una solicitud que espera en la cola de E/S del disco. La crea el nucleo cuando un
 * proceso de E/S obtiene la CPU (o el planificador a mediano plazo al suspender o
 * reactivar), y la atiende el disco segun la politica activa.
 *
 * - bloqueDestino: bloque al que debe ir el cabezal; -1 si no hay que moverlo.
 * - bloquesATransferir: bloques que se leen o escriben al llegar (cada uno cuesta un ciclo).
 * - Los ciclos de inicio y fin los marca el disco; valen -1 mientras no ocurren.
 */
public class SolicitudDisco {

    private static int contador = 0;

    private final String id;
    private final Pcb proceso;
    private final TipoOperacionES operacion;
    private final String ruta;
    private final int bloqueDestino;
    private final int bloquesATransferir;
    private final long cicloLlegada;
    private long cicloInicio = -1;
    private long cicloFin = -1;

    /**
     * @param proceso puede ser null en las pruebas del disco
     * @throws IllegalArgumentException si los datos no tienen sentido
     */
    public SolicitudDisco(Pcb proceso, TipoOperacionES operacion, String ruta, int bloqueDestino,
                          int bloquesATransferir, long cicloLlegada) {
        if (operacion == null) {
            throw new IllegalArgumentException("La solicitud necesita una operaci\u00f3n");
        }
        if (bloqueDestino < -1) {
            throw new IllegalArgumentException("Bloque destino inv\u00e1lido: " + bloqueDestino);
        }
        if (bloquesATransferir < 1) {
            throw new IllegalArgumentException("Debe transferirse al menos un bloque");
        }
        if (cicloLlegada < 0) {
            throw new IllegalArgumentException("El ciclo de llegada no puede ser negativo");
        }
        this.id = "S" + siguienteNumero();
        this.proceso = proceso;
        this.operacion = operacion;
        this.ruta = ruta;
        this.bloqueDestino = bloqueDestino;
        this.bloquesATransferir = bloquesATransferir;
        this.cicloLlegada = cicloLlegada;
    }

    private static synchronized int siguienteNumero() {
        contador++;
        return contador;
    }

    public static synchronized void reiniciarIds() {
        contador = 0;
    }

    public String getId() {
        return id;
    }

    public Pcb getProceso() {
        return proceso;
    }

    public TipoOperacionES getOperacion() {
        return operacion;
    }

    public String getRuta() {
        return ruta;
    }

    public int getBloqueDestino() {
        return bloqueDestino;
    }

    public int getBloquesATransferir() {
        return bloquesATransferir;
    }

    public long getCicloLlegada() {
        return cicloLlegada;
    }

    public long getCicloInicio() {
        return cicloInicio;
    }

    public void setCicloInicio(long cicloInicio) {
        this.cicloInicio = cicloInicio;
    }

    public long getCicloFin() {
        return cicloFin;
    }

    public void setCicloFin(long cicloFin) {
        this.cicloFin = cicloFin;
    }

    public long getEspera() {
        return cicloInicio < 0 ? -1 : cicloInicio - cicloLlegada;
    }

    public String getNombreProceso() {
        return proceso == null ? "sistema" : proceso.getNombre();
    }

    @Override
    public String toString() {
        return id + " " + getNombreProceso() + " " + operacion + " "
                + (ruta == null ? "" : ruta + " ") + "-> bloque " + bloqueDestino;
    }
}
