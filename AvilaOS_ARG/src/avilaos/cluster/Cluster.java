/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.cluster;

import avilaos.hardware.RelojGlobal;
import avilaos.model.Pcb;
import avilaos.model.TipoProceso;
import avilaos.scheduler.Planificador;
import avilaos.sync.BufferAcotado;
import avilaos.sync.GestorBuffers;
import avilaos.sync.ProgramaBuffer;
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
    private final GestorBuffers gestor;
    private final Lista<Computador> computadores = new Lista<>();
    private final Semaphore mutex = new Semaphore(1);
    private boolean conectadoAlReloj = false;
 
    public Cluster(int duracionCicloMs, int maximoMensajesBitacora) {
        this(duracionCicloMs, maximoMensajesBitacora, 0);
    }
 
    /** @param latenciaRed ciclos que tarda cada acceso a un buffer de otro computador */
    public Cluster(int duracionCicloMs, int maximoMensajesBitacora, int latenciaRed) {
        this.reloj = new RelojGlobal(duracionCicloMs);
        this.bitacora = new Bitacora(maximoMensajesBitacora);
        this.gestor = new GestorBuffers(latenciaRed, bitacora);
    }
 
    /** Agrega un computador; su identificador es el siguiente (1, 2, 3...). */
    public Computador agregarComputador(int memoriaMb, Planificador planificador) {
        mutex.acquireUninterruptibly();
        try {
            if (conectadoAlReloj || reloj.estaActivo()) {
                throw new IllegalStateException("No se pueden agregar computadores con el reloj en marcha");
            }
            Computador nuevo = new Computador(computadores.tamano() + 1, memoriaMb, planificador, bitacora);
            gestor.registrarNucleo(nuevo.getNucleo());
            computadores.agregarAlFinal(nuevo);
            bitacora.registrar(reloj.getCicloActual(), "Computador " + nuevo.getId() + " agregado al sistema: " + nuevo);
            return nuevo;
        } finally {
            mutex.release();
        }
    }
 
    // ---- creacion de procesos y buffers ----
 
    /** Crea un proceso generico (por ejemplo CPU bound) con el ciclo actual como ciclo de creacion. */
    public Pcb crearProceso(int idComputador, String nombre, TipoProceso tipo, int prioridad,
                            int memoria, int instrucciones, int deadline) {
        return resolverComputador(idComputador, memoria).crearProceso(nombre, tipo, prioridad, memoria,
                instrucciones, deadline, reloj.getCicloActual());
    }
 
    /** Crea un proceso I/O bound: cada "cadaInstrucciones" instrucciones pide una E/S de "ciclosEs" ciclos. */
    public Pcb crearProcesoEs(int idComputador, String nombre, int prioridad, int memoria,
                              int instrucciones, int deadline, int cadaInstrucciones, int ciclosEs) {
        Computador c = resolverComputador(idComputador, memoria);
        Pcb p = new Pcb(nombre, c.getId(), TipoProceso.IO_BOUND, prioridad, memoria, instrucciones,
                deadline, reloj.getCicloActual());
        p.configurarEs(cadaInstrucciones, ciclosEs);
        c.getNucleo().agregarProceso(p);
        return p;
    }
 
    /**
     * Crea un productor o consumidor que trabaja sobre un buffer. Sus instrucciones se
     * calculan: elementos x (cadaCiclos + 5).
     */
    public Pcb crearProcesoBuffer(int idComputador, String nombre, TipoProceso tipo, int prioridad,
                                  int memoria, int deadline, int idBuffer, int cadaCiclos, int elementos) {
        int instrucciones = ProgramaBuffer.instruccionesTotales(tipo, elementos, cadaCiclos);
        gestor.getBuffer(idBuffer);   // falla pronto si el buffer no existe
        Computador c = resolverComputador(idComputador, memoria);
        Pcb p = new Pcb(nombre, c.getId(), tipo, prioridad, memoria, instrucciones, deadline,
                reloj.getCicloActual());
        gestor.asignar(p, idBuffer, cadaCiclos);
        try {
            c.getNucleo().agregarProceso(p);
        } catch (RuntimeException e) {
            gestor.alTerminar(p);   // deshace la asignacion si el nucleo lo rechazo
            throw e;
        }
        return p;
    }
 
    /** Crea un buffer en la RAM de un computador (0 = automatico); 1 MB por espacio. */
    public BufferAcotado crearBuffer(int idAnfitrion, int capacidad) {
        return gestor.crearBuffer(resolverComputador(idAnfitrion, capacidad).getId(), capacidad);
    }
 
    /** Cambia en caliente la politica de un computador. */
    public void cambiarPlanificador(int idComputador, Planificador nuevo) {
        getComputador(idComputador).getNucleo().cambiarPlanificador(nuevo);
    }
 
    // ---- control del reloj ----
 
    public void iniciar() {
        if (cantidadComputadores() < MINIMO_COMPUTADORES) {
            throw new IllegalStateException("El sistema necesita al menos " + MINIMO_COMPUTADORES
                    + " computadores para iniciar");
        }
        conectarAlReloj();
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
 
    /** Avanza un ciclo a mano (pruebas y modo paso a paso). */
    public void avanzarCiclo() {
        conectarAlReloj();
        reloj.avanzarCiclo();
    }
 
    public void setDuracionCicloMs(int ms) {
        reloj.setDuracionCicloMs(ms);
    }
 
    public void setLatenciaRed(int ciclos) {
        gestor.setLatenciaRed(ciclos);
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
 
    public GestorBuffers getGestorBuffers() {
        return gestor;
    }
 
    public BufferAcotado[] getBuffers() {
        return gestor.getBuffers();
    }
 
    /** Cantidad de procesos que ya terminaron, sumando todos los computadores. */
    public int contarTerminados() {
        int total = 0;
        Computador[] todos = getComputadores();
        for (Computador c : todos) {
            total += c.getNucleo().getTerminados().length;
        }
        return total;
    }
 
    // ---- privados ----
 
    /** Conecta los computadores y el gestor al reloj, una sola vez y en el orden correcto. */
    private void conectarAlReloj() {
        mutex.acquireUninterruptibly();
        try {
            if (conectadoAlReloj) {
                return;
            }
            Iterador<Computador> it = computadores.iterador();
            while (it.tieneSiguiente()) {
                it.siguiente().registrarEn(reloj);
            }
            reloj.registrar(gestor);   // siempre despues de todos los nucleos
            conectadoAlReloj = true;
        } finally {
            mutex.release();
        }
    }
 
    /** id > 0: ese computador. id == 0: el de mas memoria libre en el que quepa "memoria" MB. */
    private Computador resolverComputador(int id, int memoria) {
        if (id > 0) {
            return getComputador(id);
        }
        if (id < 0) {
            throw new IllegalArgumentException("Computador invalido: " + id);
        }
        Computador mejor = null;
        Computador[] todos = getComputadores();
        for (Computador c : todos) {
            if (c.getRam().getCapacidad() >= memoria
                    && (mejor == null || c.getRam().getLibre() > mejor.getRam().getLibre())) {
                mejor = c;
            }
        }
        if (mejor == null) {
            throw new IllegalArgumentException("Ningun computador tiene RAM para " + memoria + " MB");
        }
        return mejor;
    }
}