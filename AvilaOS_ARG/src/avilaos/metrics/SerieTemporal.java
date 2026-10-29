package avilaos.metrics;

import avilaos.util.Iterador;
import avilaos.util.Lista;
import java.util.concurrent.Semaphore;

/**
 * @author Gianm
 * Serie de puntos (ciclo, valor) con un maximo de puntos: al llegar al limite, el mas
 * antiguo se descarta. Es seguro entre hilos: el reloj agrega puntos y la interfaz
 * los lee para dibujar la grafica.
 */
public class SerieTemporal {

    private final int maxPuntos;
    private final Lista<Long> ciclos = new Lista<>();
    private final Lista<Double> valores = new Lista<>();
    private final Semaphore mutex = new Semaphore(1);

    public SerieTemporal(int maxPuntos) {
        if (maxPuntos < 1) {
            throw new IllegalArgumentException("La serie debe guardar al menos 1 punto");
        }
        this.maxPuntos = maxPuntos;
    }

    public void agregar(long ciclo, double valor) {
        mutex.acquireUninterruptibly();
        try {
            ciclos.agregarAlFinal(ciclo);
            valores.agregarAlFinal(valor);
            if (ciclos.tamano() > maxPuntos) {
                ciclos.eliminarPrimero();
                valores.eliminarPrimero();
            }
        } finally {
            mutex.release();
        }
    }

    public int tamano() {
        mutex.acquireUninterruptibly();
        try {
            return ciclos.tamano();
        } finally {
            mutex.release();
        }
    }

    /** Copia de los ciclos, del mas antiguo al mas reciente. */
    public long[] getCiclos() {
        mutex.acquireUninterruptibly();
        try {
            long[] copia = new long[ciclos.tamano()];
            Iterador<Long> it = ciclos.iterador();
            int i = 0;
            while (it.tieneSiguiente()) {
                copia[i++] = it.siguiente();
            }
            return copia;
        } finally {
            mutex.release();
        }
    }

    /** Copia de los valores, en el mismo orden que getCiclos. */
    public double[] getValores() {
        mutex.acquireUninterruptibly();
        try {
            double[] copia = new double[valores.tamano()];
            Iterador<Double> it = valores.iterador();
            int i = 0;
            while (it.tieneSiguiente()) {
                copia[i++] = it.siguiente();
            }
            return copia;
        } finally {
            mutex.release();
        }
    }
}
