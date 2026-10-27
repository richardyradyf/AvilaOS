package avilaos.sync;

import avilaos.model.Pcb;
import avilaos.util.Cola;
import avilaos.util.Iterador;
import java.util.concurrent.Semaphore;

/**
 * Semaforo de ÁvilaOS (capa 2): bloquea PROCESOS SIMULADOS, no hilos de Java.
 * Sigue a Stallings: el valor puede ser negativo, y su valor absoluto es la
 * cantidad de procesos esperando en la cola.
 *
 * semWait:   valor--; si valor < 0, el proceso se bloquea en la cola.
 * semSignal: valor++; si valor <= 0, se despierta al primero de la cola.
 *
 * Esta clase NO cambia el estado del proceso: solo dice si puede continuar y a quien
 * despertar. El nucleo es quien pasa el PCB a BLOQUEADO o a LISTO. Internamente usa
 * un Semaphore de Java (capa 1) para que dos hilos no corrompan el valor ni la cola.
 */
public class SemaforoSO {

    private final String nombre;
    private int valor;
    private final Cola<Pcb> bloqueados = new Cola<>();
    private final Semaphore mutex = new Semaphore(1);

    public SemaforoSO(String nombre, int valorInicial) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El semaforo necesita un nombre");
        }
        if (valorInicial < 0) {
            throw new IllegalArgumentException("El valor inicial no puede ser negativo");
        }
        this.nombre = nombre;
        this.valor = valorInicial;
    }

    /**
     * semWait.
     *
     * @return true si el proceso puede continuar; false si debe bloquearse (ya quedo
     *         en la cola de este semaforo)
     */
    public boolean esperar(Pcb proceso) {
        if (proceso == null) {
            throw new IllegalArgumentException("El proceso no puede ser nulo");
        }
        mutex.acquireUninterruptibly();
        try {
            valor--;
            if (valor < 0) {
                bloqueados.encolar(proceso);
                return false;
            }
            return true;
        } finally {
            mutex.release();
        }
    }

    /**
     * semSignal.
     *
     * @return el proceso que hay que despertar, o null si no habia nadie esperando
     */
    public Pcb senalar() {
        mutex.acquireUninterruptibly();
        try {
            valor++;
            if (valor <= 0) {
                return bloqueados.desencolar();
            }
            return null;
        } finally {
            mutex.release();
        }
    }

    /**
     * Saca de la cola a un proceso que dejo de esperar (por ejemplo, terminado por
     * deadline). Se devuelve su "voto" al valor para mantener la cuenta correcta.
     *
     * @return true si estaba en la cola
     */
    public boolean retirar(Pcb proceso) {
        mutex.acquireUninterruptibly();
        try {
            if (bloqueados.eliminar(proceso)) {
                valor++;
                return true;
            }
            return false;
        } finally {
            mutex.release();
        }
    }

    public String getNombre() {
        return nombre;
    }

    public int getValor() {
        mutex.acquireUninterruptibly();
        try {
            return valor;
        } finally {
            mutex.release();
        }
    }

    public int cantidadBloqueados() {
        mutex.acquireUninterruptibly();
        try {
            return bloqueados.tamano();
        } finally {
            mutex.release();
        }
    }

    /** Copia de la cola de espera, para mostrarla en la interfaz. */
    public Pcb[] getBloqueados() {
        mutex.acquireUninterruptibly();
        try {
            Pcb[] copia = new Pcb[bloqueados.tamano()];
            Iterador<Pcb> it = bloqueados.iterador();
            int i = 0;
            while (it.tieneSiguiente()) {
                copia[i++] = it.siguiente();
            }
            return copia;
        } finally {
            mutex.release();
        }
    }

    @Override
    public String toString() {
        return nombre + " = " + getValor();
    }
}
