package avilaos.sync;

import avilaos.model.MotivoBloqueo;
import avilaos.model.Pcb;
import java.util.concurrent.Semaphore;

/**
 * Buffer de capacidad limitada con el algoritmo productor-consumidor de Stallings:
 * tres semaforos (s = mutex, n = elementos, e = espacios libres). Reside en la RAM
 * de su computador anfitrion; cada espacio ocupa 1 MB (decision del equipo).
 *
 * El buffer no guarda datos reales, solo cuenta elementos. Cada operacion se ejecuta
 * de a una (una instruccion por ciclo) con ejecutar(operacion, proceso).
 */
public class BufferAcotado {

    private final int id;
    private final int capacidad;
    private final int idComputadorAnfitrion;

    private final SemaforoSO mutex;          // s: exclusion mutua (semaforo binario)
    private final SemaforoSO hayElementos;   // n: elementos disponibles (de conteo)
    private final SemaforoSO hayEspacio;     // e: espacios libres (de conteo)

    /** Capa 1: protege los contadores internos de accesos simultaneos de hilos de Java. */
    private final Semaphore mutexJava = new Semaphore(1);
    private int elementos = 0;
    private long producidos = 0;
    private long consumidos = 0;

    public BufferAcotado(int id, int capacidad, int idComputadorAnfitrion) {
        if (id < 1) {
            throw new IllegalArgumentException("El id del buffer debe ser 1 o mayor");
        }
        if (capacidad < 1) {
            throw new IllegalArgumentException("La capacidad debe ser 1 o mayor");
        }
        if (idComputadorAnfitrion < 1) {
            throw new IllegalArgumentException("El computador anfitrion debe ser 1 o mayor");
        }
        this.id = id;
        this.capacidad = capacidad;
        this.idComputadorAnfitrion = idComputadorAnfitrion;
        this.mutex = new SemaforoSO("s (mutex)", 1);
        this.hayElementos = new SemaforoSO("n (elementos)", 0);
        this.hayEspacio = new SemaforoSO("e (espacios)", capacidad);
    }

    /**
     * Ejecuta UNA operacion del programa de un productor o consumidor.
     * PRODUCIR y CONSUMIR no tocan el buffer: son trabajo propio del proceso.
     */
    public ResultadoOperacion ejecutar(OperacionBuffer operacion, Pcb proceso) {
        switch (operacion) {
            case PRODUCIR:
            case CONSUMIR:
                return ResultadoOperacion.continuar();
            case SEM_WAIT_ESPACIOS:
                return esperar(hayEspacio, proceso, MotivoBloqueo.BUFFER_LLENO);
            case SEM_WAIT_MUTEX:
                return esperar(mutex, proceso, MotivoBloqueo.REGION_CRITICA_OCUPADA);
            case SEM_WAIT_ELEMENTOS:
                return esperar(hayElementos, proceso, MotivoBloqueo.BUFFER_VACIO);
            case ANADIR:
                anadir();
                return ResultadoOperacion.continuar();
            case EXTRAER:
                extraer();
                return ResultadoOperacion.continuar();
            case SEM_SIGNAL_MUTEX:
                return ResultadoOperacion.continuarDespertando(mutex.senalar());
            case SEM_SIGNAL_ELEMENTOS:
                return ResultadoOperacion.continuarDespertando(hayElementos.senalar());
            case SEM_SIGNAL_ESPACIOS:
                return ResultadoOperacion.continuarDespertando(hayEspacio.senalar());
            default:
                throw new IllegalArgumentException("Operacion desconocida: " + operacion);
        }
    }

    /**
     * Saca al proceso de la cola de cualquiera de los tres semaforos (por ejemplo, si
     * termina por deadline mientras esta bloqueado).
     *
     * @return true si estaba esperando en alguno
     */
    public boolean retirarProceso(Pcb proceso) {
        boolean estaba = mutex.retirar(proceso);
        estaba |= hayElementos.retirar(proceso);
        estaba |= hayEspacio.retirar(proceso);
        return estaba;
    }

    // ---- consulta ----

    public int getId() {
        return id;
    }

    /** Etiqueta para el log y la interfaz, por ejemplo "B1". */
    public String getEtiqueta() {
        return "B" + id;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public int getIdComputadorAnfitrion() {
        return idComputadorAnfitrion;
    }

    /** Memoria que ocupa en la RAM del anfitrion, en MB (1 MB por espacio). */
    public int getMemoriaRequerida() {
        return capacidad;
    }

    public int getElementos() {
        mutexJava.acquireUninterruptibly();
        try {
            return elementos;
        } finally {
            mutexJava.release();
        }
    }

    public long getProducidos() {
        mutexJava.acquireUninterruptibly();
        try {
            return producidos;
        } finally {
            mutexJava.release();
        }
    }

    public long getConsumidos() {
        mutexJava.acquireUninterruptibly();
        try {
            return consumidos;
        } finally {
            mutexJava.release();
        }
    }

    public SemaforoSO getMutex() {
        return mutex;
    }

    public SemaforoSO getHayElementos() {
        return hayElementos;
    }

    public SemaforoSO getHayEspacio() {
        return hayEspacio;
    }

    // ---- privados ----

    private ResultadoOperacion esperar(SemaforoSO semaforo, Pcb proceso, MotivoBloqueo motivo) {
        return semaforo.esperar(proceso)
                ? ResultadoOperacion.continuar()
                : ResultadoOperacion.bloquear(motivo);
    }

    private void anadir() {
        mutexJava.acquireUninterruptibly();
        try {
            if (elementos >= capacidad) {
                throw new IllegalStateException(getEtiqueta()
                        + ": desbordamiento, el protocolo de semaforos fallo");
            }
            elementos++;
            producidos++;
        } finally {
            mutexJava.release();
        }
    }

    private void extraer() {
        mutexJava.acquireUninterruptibly();
        try {
            if (elementos <= 0) {
                throw new IllegalStateException(getEtiqueta()
                        + ": se extrae de un buffer vacio, el protocolo de semaforos fallo");
            }
            elementos--;
            consumidos++;
        } finally {
            mutexJava.release();
        }
    }
}

