/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.kernel;

import avilaos.hardware.Cpu;
import avilaos.hardware.Ram;
import avilaos.hardware.Relojable;
import avilaos.model.EstadoProceso;
import avilaos.model.MotivoBloqueo;
import avilaos.model.Pcb;
import avilaos.scheduler.Planificador;
import avilaos.util.Bitacora;
import avilaos.util.Cola;
import avilaos.util.Iterador;
import avilaos.util.Lista;
import java.util.concurrent.Semaphore;

/**
 * @author richard
 * 
 * Nucleo del sistema operativo de UN computador. Maneja las colas (nuevos, listos,
 * bloqueados, terminados), la admision segun la memoria y el despacho de procesos
 * a la CPU con la politica de planificacion que tenga conectada.
 *
 * Debe registrarse en el reloj DESPUES de la CPU: en cada ciclo la CPU ejecuta una
 * instruccion y luego el nucleo decide (terminar, expulsar, admitir, despachar).
 *
 * Las colas las tocan varios hilos (reloj, interfaz, usuario creando procesos), asi
 * que todo se protege con un semaforo mutex de Java. Los metodos publicos toman el
 * mutex; los privados asumen que ya esta tomado (el semaforo no es reentrante).
 */
public class Nucleo implements Relojable {

    private final int idComputador;
    private final Ram ram;
    private final Cpu cpu;
    private final Bitacora bitacora;
    private Planificador planificador;

    private final Cola<Pcb> nuevos = new Cola<>();
    private final Cola<Pcb> listos = new Cola<>();
    private final Lista<Pcb> bloqueados = new Lista<>();
    private final Cola<Pcb> terminados = new Cola<>();

    private final Semaphore mutex = new Semaphore(1);
    private long cicloActual = 0;
    private int ciclosEnCpu = 0;

    public Nucleo(int idComputador, Ram ram, Cpu cpu, Planificador planificador, Bitacora bitacora) {
        if (idComputador < 1) {
            throw new IllegalArgumentException("El computador debe ser 1 o mayor");
        }
        if (ram == null || cpu == null || planificador == null || bitacora == null) {
            throw new IllegalArgumentException("RAM, CPU, planificador y bitacora son obligatorios");
        }
        this.idComputador = idComputador;
        this.ram = ram;
        this.cpu = cpu;
        this.planificador = planificador;
        this.bitacora = bitacora;
    }

    // ================= operaciones publicas =================

    public void agregarProceso(Pcb proceso) {
        if (proceso == null) {
            throw new IllegalArgumentException("El proceso no puede ser nulo");
        }
        if (proceso.getIdComputador() != idComputador) {
            throw new IllegalArgumentException(proceso.getEtiqueta() + " pertenece al computador "
                    + proceso.getIdComputador() + ", no al " + idComputador);
        }
        if (proceso.getEstado() != EstadoProceso.NUEVO) {
            throw new IllegalArgumentException("Solo se agregan procesos en estado NUEVO");
        }
        if (proceso.getMemoria() > ram.getCapacidad()) {
            throw new IllegalArgumentException(proceso.getEtiqueta() + " requiere " + proceso.getMemoria()
                    + " MB y la RAM del computador " + idComputador + " es de " + ram.getCapacidad() + " MB");
        }
        mutex.acquireUninterruptibly();
        try {
            nuevos.encolar(proceso);
            registrar(proceso.getEtiqueta() + " (" + proceso.getNombre()
                    + ") llega a la cola de nuevos del computador " + idComputador);
            admitirPendientes();
        } finally {
            mutex.release();
        }
    }

    /**
     * Saca de la CPU al proceso en ejecucion y lo pasa a BLOQUEADO con el motivo
     * indicado (semWait sin permiso, espera de E/S, acceso remoto...). La CPU queda
     * ociosa y el nucleo despacha a otro proceso al final del ciclo.
     */
    public Pcb bloquearProcesoEnCpu(MotivoBloqueo motivo) {
        mutex.acquireUninterruptibly();
        try {
            Pcb proceso = cpu.getProceso();
            if (proceso == null) {
                throw new IllegalStateException("La CPU esta ociosa: no hay proceso que bloquear");
            }
            cpu.descargar();
            proceso.bloquear(motivo);
            bloqueados.agregarAlFinal(proceso);
            registrar(proceso.getEtiqueta() + " se bloquea: " + motivo);
            return proceso;
        } finally {
            mutex.release();
        }
    }

    public void desbloquear(Pcb proceso) {
        mutex.acquireUninterruptibly();
        try {
            if (proceso == null || !bloqueados.eliminar(proceso)) {
                throw new IllegalArgumentException("El proceso no esta bloqueado en el computador " + idComputador);
            }
            proceso.desbloquear();
            planificador.alEntrarAListos(listos, proceso);
            registrar(proceso.getEtiqueta() + " se desbloquea y pasa a listos");
        } finally {
            mutex.release();
        }
    }

    public void cambiarPlanificador(Planificador nuevo) {
        if (nuevo == null) {
            throw new IllegalArgumentException("El planificador no puede ser nulo");
        }
        mutex.acquireUninterruptibly();
        try {
            planificador = nuevo;
            planificador.reordenar(listos);
            registrar("Computador " + idComputador + " cambia su politica de planificacion a "
                    + nuevo.getTipo());
        } finally {
            mutex.release();
        }
    }

    // ================= reloj =================

    @Override
    public void alCiclo(long numeroCiclo) {
        mutex.acquireUninterruptibly();
        try {
            cicloActual = numeroCiclo;
            revisarProcesoEnCpu();
            admitirPendientes();
            despacharSiOciosa();
        } finally {
            mutex.release();
        }
    }

    // ================= consulta (copias para la interfaz) =================

    public Pcb[] getNuevos() {
        mutex.acquireUninterruptibly();
        try {
            return aArreglo(nuevos);
        } finally {
            mutex.release();
        }
    }

    public Pcb[] getListos() {
        mutex.acquireUninterruptibly();
        try {
            return aArreglo(listos);
        } finally {
            mutex.release();
        }
    }

    public Pcb[] getBloqueados() {
        mutex.acquireUninterruptibly();
        try {
            return aArreglo(bloqueados);
        } finally {
            mutex.release();
        }
    }

    public Pcb[] getTerminados() {
        mutex.acquireUninterruptibly();
        try {
            return aArreglo(terminados);
        } finally {
            mutex.release();
        }
    }

    public Planificador getPlanificador() {
        mutex.acquireUninterruptibly();
        try {
            return planificador;
        } finally {
            mutex.release();
        }
    }

    public int getIdComputador() {
        return idComputador;
    }

    public Ram getRam() {
        return ram;
    }

    public Cpu getCpu() {
        return cpu;
    }

    // ================= privados (el mutex ya esta tomado) =================

    private void revisarProcesoEnCpu() {
        Pcb proceso = cpu.getProceso();
        if (proceso == null) {
            return;
        }
        ciclosEnCpu++;
        if (proceso.completoSusInstrucciones()) {
            cpu.descargar();
            proceso.terminar(cicloActual, false);
            ram.liberar(proceso.getMemoria());
            terminados.encolar(proceso);
            registrar(proceso.getEtiqueta() + " termina y libera " + proceso.getMemoria() + " MB");
        } else if (planificador.debeExpulsar(proceso, ciclosEnCpu, listos)) {
            cpu.descargar();
            proceso.expulsar();
            planificador.alEntrarAListos(listos, proceso);
            registrar(proceso.getEtiqueta() + " es expulsado de la CPU");
        }
    }

    private void admitirPendientes() {
        int i = 0;
        while (i < nuevos.tamano()) {
            Pcb proceso = nuevos.obtener(i);
            if (ram.reservar(proceso.getMemoria())) {
                nuevos.eliminar(proceso);
                proceso.admitir();
                planificador.alEntrarAListos(listos, proceso);
                registrar(proceso.getEtiqueta() + " admitido: reserva " + proceso.getMemoria()
                        + " MB (RAM " + ram + ")");
            } else {
                i++;
            }
        }
    }

    private void despacharSiOciosa() {
        if (!cpu.estaOciosa()) {
            return;
        }
        Pcb proceso = planificador.seleccionar(listos);
        if (proceso == null) {
            return;
        }
        proceso.despachar(cicloActual + 1);
        cpu.cargar(proceso);
        ciclosEnCpu = 0;
        registrar("Computador " + idComputador + " selecciona el proceso " + proceso.getEtiqueta()
                + " (" + planificador.getTipo() + ")");
    }

    private void registrar(String texto) {
        bitacora.registrar(cicloActual, texto);
    }

    private static Pcb[] aArreglo(Cola<Pcb> cola) {
        Pcb[] copia = new Pcb[cola.tamano()];
        Iterador<Pcb> it = cola.iterador();
        int i = 0;
        while (it.tieneSiguiente()) {
            copia[i++] = it.siguiente();
        }
        return copia;
    }

    private static Pcb[] aArreglo(Lista<Pcb> lista) {
        Pcb[] copia = new Pcb[lista.tamano()];
        Iterador<Pcb> it = lista.iterador();
        int i = 0;
        while (it.tieneSiguiente()) {
            copia[i++] = it.siguiente();
        }
        return copia;
    }
}