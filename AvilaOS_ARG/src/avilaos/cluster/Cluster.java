/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.cluster;

import avilaos.hardware.RelojGlobal;
import avilaos.model.Pcb;
import avilaos.model.TipoProceso;
import avilaos.scheduler.Planificador;
import avilaos.util.Bitacora;
import avilaos.util.Iterador;
import avilaos.util.Lista;
import java.util.concurrent.Semaphore;

/**
 * @author richard
 * 
 * El sistema distribuido completo: un reloj global, una bitacora compartida y
 * dos o mas computadores. Es la unica puerta de entrada para la interfaz grafica
 * y para el cargador de configuracion.
 *
 * Los computadores se agregan antes de iniciar el reloj (el minimo es 2).
 */
public class Cluster {

    public static final int MINIMO_COMPUTADORES = 2;

    private final RelojGlobal reloj;
    private final Bitacora bitacora;
    private final Lista<Computador> computadores = new Lista<>();
    private final Semaphore mutex = new Semaphore(1);

    public Cluster(int duracionCicloMs, int maximoMensajesBitacora) {
        this.reloj = new RelojGlobal(duracionCicloMs);
        this.bitacora = new Bitacora(maximoMensajesBitacora);
    }

    public Computador agregarComputador(int memoriaMb, Planificador planificador) {
        mutex.acquireUninterruptibly();
        try {
            if (reloj.estaActivo()) {
                throw new IllegalStateException("No se pueden agregar computadores con el reloj en marcha");
            }
            Computador nuevo = new Computador(computadores.tamano() + 1, memoriaMb, planificador, bitacora);
            nuevo.registrarEn(reloj);
            computadores.agregarAlFinal(nuevo);
            bitacora.registrar(reloj.getCicloActual(), "Computador " + nuevo.getId() + " agregado al sistema: " + nuevo);
            return nuevo;
        } finally {
            mutex.release();
        }
    }

    public Pcb crearProceso(int idComputador, String nombre, TipoProceso tipo, int prioridad,
                            int memoria, int instrucciones, int deadline) {
        return getComputador(idComputador).crearProceso(nombre, tipo, prioridad, memoria,
                instrucciones, deadline, reloj.getCicloActual());
    }

    public void cambiarPlanificador(int idComputador, Planificador nuevo) {
        getComputador(idComputador).getNucleo().cambiarPlanificador(nuevo);
    }

    // ---- control del reloj ----

    public void iniciar() {
        if (cantidadComputadores() < MINIMO_COMPUTADORES) {
            throw new IllegalStateException("El sistema necesita al menos " + MINIMO_COMPUTADORES
                    + " computadores para iniciar");
        }
        reloj.iniciar();
    }

    public void pausar() {
        reloj.pausar();
    }

    public void reanudar() {
        reloj.reanudar();
    }

    public void detener() {
        reloj.detener();
    }

    public void avanzarCiclo() {
        reloj.avanzarCiclo();
    }

    public void setDuracionCicloMs(int ms) {
        reloj.setDuracionCicloMs(ms);
    }

    // ---- consulta ----

    public Computador getComputador(int id) {
        mutex.acquireUninterruptibly();
        try {
            if (id < 1 || id > computadores.tamano()) {
                throw new IllegalArgumentException("No existe el computador " + id);
            }
            return computadores.obtener(id - 1);
        } finally {
            mutex.release();
        }
    }

    public Computador[] getComputadores() {
        mutex.acquireUninterruptibly();
        try {
            Computador[] copia = new Computador[computadores.tamano()];
            Iterador<Computador> it = computadores.iterador();
            int i = 0;
            while (it.tieneSiguiente()) {
                copia[i++] = it.siguiente();
            }
            return copia;
        } finally {
            mutex.release();
        }
    }

    public int cantidadComputadores() {
        mutex.acquireUninterruptibly();
        try {
            return computadores.tamano();
        } finally {
            mutex.release();
        }
    }

    public long getCicloActual() {
        return reloj.getCicloActual();
    }

    public RelojGlobal getReloj() {
        return reloj;
    }

    public Bitacora getBitacora() {
        return bitacora;
    }

    public int contarTerminados() {
        int total = 0;
        Computador[] todos = getComputadores();
        for (Computador c : todos) {
            total += c.getNucleo().getTerminados().length;
        }
        return total;
    }
}