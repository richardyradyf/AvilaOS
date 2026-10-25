/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.hardware;

import avilaos.model.EstadoProceso;
import avilaos.model.ModoEjecucion;
import avilaos.model.Pcb;

/**
 * @author richard
 * 
 * Procesador de un computador. No toma decisiones: ejecuta una instruccion por ciclo
 * del proceso que el nucleo le cargo, en el modo que el nucleo le indico.
 *
 * Solo el hilo del reloj la modifica; los demas hilos (la interfaz) solo leen, por
 * eso los campos son volatile y no hace falta un semaforo.
 */
public class Cpu implements Relojable {

    private final int idComputador;

    private volatile Pcb proceso;
    private volatile ModoEjecucion modo = ModoEjecucion.SISTEMA_OPERATIVO;

    private volatile long ciclosTotales;
    private volatile long ciclosOcupada;
    private volatile long ciclosUsuario;
    private volatile long ciclosSistema;

    public Cpu(int idComputador) {
        this.idComputador = idComputador;
    }

    /**
     * Carga un proceso en la CPU. El nucleo debe haberlo despachado antes (estado
     * EJECUCION). La CPU pasa a modo usuario.
     */
    public void cargar(Pcb nuevo) {
        if (nuevo == null) {
            throw new IllegalArgumentException("El proceso no puede ser nulo");
        }
        if (proceso != null) {
            throw new IllegalStateException("La CPU " + idComputador + " ya tiene cargado a "
                    + proceso.getEtiqueta());
        }
        if (nuevo.getEstado() != EstadoProceso.EJECUCION) {
            throw new IllegalStateException(nuevo.getEtiqueta()
                    + " debe estar en EJECUCION para cargarse en la CPU");
        }
        proceso = nuevo;
        modo = ModoEjecucion.USUARIO;
    }


    public Pcb descargar() {
        Pcb actual = proceso;
        proceso = null;
        modo = ModoEjecucion.SISTEMA_OPERATIVO;
        return actual;
    }

    public void setModo(ModoEjecucion nuevoModo) {
        if (nuevoModo == null) {
            throw new IllegalArgumentException("El modo no puede ser nulo");
        }
        modo = nuevoModo;
    }

    @Override
    public void alCiclo(long numeroCiclo) {
        ciclosTotales++;
        Pcb actual = proceso;
        if (actual == null || actual.completoSusInstrucciones()) {
            return;   // ciclo ocioso
        }
        actual.ejecutarInstruccion();
        ciclosOcupada++;
        if (modo == ModoEjecucion.USUARIO) {
            ciclosUsuario++;
        } else {
            ciclosSistema++;
        }
    }

    public Pcb getProceso() {
        return proceso;
    }

    public boolean estaOciosa() {
        return proceso == null;
    }

    public ModoEjecucion getModo() {
        return modo;
    }

    public int getIdComputador() {
        return idComputador;
    }

    public long getCiclosTotales() {
        return ciclosTotales;
    }

    public long getCiclosOcupada() {
        return ciclosOcupada;
    }

    public long getCiclosUsuario() {
        return ciclosUsuario;
    }

    public long getCiclosSistema() {
        return ciclosSistema;
    }

    /** Fraccion de ciclos en que la CPU ejecuto instrucciones (de 0.0 a 1.0). */
    public double getUtilizacion() {
        long total = ciclosTotales;
        return total == 0 ? 0.0 : (double) ciclosOcupada / total;
    }
}
