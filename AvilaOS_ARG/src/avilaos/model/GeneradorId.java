package avilaos.model;

import java.util.concurrent.Semaphore;

/**
 * Genera IDs de proceso unicos en todo el sistema distribuido.
 * Todos los computadores comparten la misma JVM, asi que un unico contador
 * protegido con un semaforo mutex garantiza que no se repitan, aunque varios
 * hilos pidan un ID a la vez.
 */
public final class GeneradorId {

    private static final Semaphore mutex = new Semaphore(1);
    private static int siguiente = 1;

    private GeneradorId() {
        // clase de utilidad: no se instancia
    }

    /** Devuelve el siguiente ID disponible, empezando en 1. */
    public static int siguienteId() {
        mutex.acquireUninterruptibly();   // semWait
        try {
            return siguiente++;           // seccion critica
        } finally {
            mutex.release();              // semSignal
        }
    }

    /** Reinicia el contador (para una nueva simulacion o para pruebas). */
    public static void reiniciar() {
        mutex.acquireUninterruptibly();
        try {
            siguiente = 1;
        } finally {
            mutex.release();
        }
    }
}