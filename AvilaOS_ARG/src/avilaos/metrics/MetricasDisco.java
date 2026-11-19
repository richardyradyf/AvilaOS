package avilaos.metrics;

import avilaos.disco.Disco;
import avilaos.disco.OyenteDisco;
import avilaos.disco.SolicitudDisco;
import avilaos.hardware.Relojable;
import avilaos.model.PoliticaDisco;
import java.util.concurrent.Semaphore;

/**
 * @author andre
 *
 * Recolecta y expone las metricas de rendimiento del disco simulado:
 * - Desplazamiento total del cabezal (en bloques, sin contar el salto de C-SCAN).
 * - Tiempo promedio de espera en la cola de E/S.
 * - Cantidad de solicitudes atendidas por cada politica (FIFO, SSTF, SCAN, C-SCAN).
 * - Serie temporal (ciclo, desplazamientoAcumulado) para la grafica del cabezal vs. tiempo.
 * - Sincronizacion: Semaphore(1) protege la lectura y actualizacion concurrente.
 */
public class MetricasDisco implements Relojable, OyenteDisco {

    public static final int PUNTOS_SERIE_DEFAULT = 500;
    public static final int INTERVALO_MUESTREO_DEFAULT = 1;

    private final Disco disco;
    private final int intervaloMuestreo;
    private final SerieTemporal serieDesplazamiento;
    private final Semaphore mutex;

    private final int[] atendidasPorPolitica;
    private long sumaTiempoEspera;
    private int totalAtendidas;

    public MetricasDisco(Disco disco) {
        this(disco, INTERVALO_MUESTREO_DEFAULT, PUNTOS_SERIE_DEFAULT);
    }

    public MetricasDisco(Disco disco, int intervaloMuestreo, int maxPuntosSerie) {
        if (disco == null) {
            throw new IllegalArgumentException("El disco no puede ser nulo");
        }
        if (intervaloMuestreo < 1 || maxPuntosSerie < 1) {
            throw new IllegalArgumentException("El intervalo y el máximo de puntos deben ser mayores a 0");
        }
        this.disco = disco;
        this.intervaloMuestreo = intervaloMuestreo;
        this.serieDesplazamiento = new SerieTemporal(maxPuntosSerie);
        this.mutex = new Semaphore(1);
        this.atendidasPorPolitica = new int[PoliticaDisco.values().length];
        this.sumaTiempoEspera = 0;
        this.totalAtendidas = 0;

        // Registrarse como oyente del disco para registrar cada solicitud atendida
        disco.agregarOyente(this);
    }

    @Override
    public void alCiclo(long numeroCiclo) {
        if (numeroCiclo % intervaloMuestreo == 0) {
            int desp = disco.getTotalDesplazamiento();
            serieDesplazamiento.agregar(numeroCiclo, desp);
        }
    }

    @Override
    public void solicitudAtendida(SolicitudDisco solicitud, long ciclo) {
        if (solicitud == null) {
            return;
        }
        mutex.acquireUninterruptibly();
        try {
            long espera = solicitud.getEspera();
            if (espera >= 0) {
                sumaTiempoEspera += espera;
            }
            totalAtendidas++;

            PoliticaDisco pol = disco.getPoliticaUltimaAtendida();
            if (pol == null) {
                pol = disco.getTipoPolitica();
            }
            if (pol != null) {
                atendidasPorPolitica[pol.ordinal()]++;
            }
        } finally {
            mutex.release();
        }
    }

    /**
     * Desplazamiento acumulado total del cabezal (en bloques).
     */
    public int getDesplazamientoTotal() {
        return disco.getTotalDesplazamiento();
    }

    /**
     * Cantidad total de saltos al inicio (C-SCAN).
     */
    public int getTotalSaltos() {
        return disco.getTotalSaltos();
    }

    /**
     * Tiempo promedio de espera en ciclos de las solicitudes finalizadas.
     */
    public double getTiempoEsperaPromedio() {
        mutex.acquireUninterruptibly();
        try {
            if (totalAtendidas == 0) {
                return 0.0;
            }
            return (double) sumaTiempoEspera / totalAtendidas;
        } finally {
            mutex.release();
        }
    }

    /**
     * Cantidad de solicitudes atendidas por una politica especifica.
     */
    public int getAtendidasPorPolitica(PoliticaDisco politica) {
        if (politica == null) {
            return 0;
        }
        mutex.acquireUninterruptibly();
        try {
            return atendidasPorPolitica[politica.ordinal()];
        } finally {
            mutex.release();
        }
    }

    public int getTotalAtendidas() {
        mutex.acquireUninterruptibly();
        try {
            return totalAtendidas;
        } finally {
            mutex.release();
        }
    }

    /**
     * Serie temporal con los puntos (ciclo, desplazamientoAcumulado) para la grafica.
     */
    public SerieTemporal getSerieDesplazamiento() {
        return serieDesplazamiento;
    }

    /**
     * Reinicia los acumuladores de metricas (para pruebas o reinicio de simulacion).
     */
    public void reiniciar() {
        mutex.acquireUninterruptibly();
        try {
            for (int i = 0; i < atendidasPorPolitica.length; i++) {
                atendidasPorPolitica[i] = 0;
            }
            sumaTiempoEspera = 0;
            totalAtendidas = 0;
        } finally {
            mutex.release();
        }
    }
}
