package avilaos.metrics;

import avilaos.cluster.Cluster;
import avilaos.cluster.Computador;
import avilaos.hardware.Relojable;
import avilaos.model.EstadoProceso;
import avilaos.model.Pcb;
import avilaos.model.TipoProceso;
import avilaos.sync.BufferAcotado;
import avilaos.util.Iterador;
import avilaos.util.Lista;
import java.util.concurrent.Semaphore;

/**
 * @author Gianm
 * Recolecta las metricas de rendimiento del sistema. Hace dos cosas:
 *
 * 1) Como Relojable, en cada ciclo cuenta cuantos ciclos pasa cada proceso bloqueado
 *    (en semaforos o esperando un acceso remoto) y, cada "intervaloMuestreo" ciclos,
 *    guarda la utilizacion reciente de cada CPU para la grafica.
 * 2) generarReporte calcula, a partir del estado actual, throughput, utilizacion,
 *    tiempo de respuesta, cumplimiento de deadlines, equidad y datos de los buffers.
 *
 * Debe crearse cuando ya estan agregados todos los computadores del cluster y
 * registrarse en el reloj (cluster.getReloj().registrar(recolector)).
 */
public class RecolectorMetricas implements Relojable {

    private static class RegistroBloqueo {
        final Pcb proceso;
        long ciclosSemaforo;
        long ciclosRemoto;

        RegistroBloqueo(Pcb proceso) {
            this.proceso = proceso;
        }
    }

    private final Cluster cluster;
    private final int intervaloMuestreo;
    private final int maxPuntos;
    private final Semaphore mutex = new Semaphore(1);

    private SerieTemporal[] series = new SerieTemporal[0];
    private long[] ultimaOcupada = new long[0];
    private long[] ultimaTotal = new long[0];
    private final Lista<RegistroBloqueo> bloqueos = new Lista<>();

    /**
     * @param intervaloMuestreo cada cuantos ciclos se guarda un punto de la grafica
     * @param maxPuntos         puntos que se conservan por computador
     */
    public RecolectorMetricas(Cluster cluster, int intervaloMuestreo, int maxPuntos) {
        if (cluster == null) {
            throw new IllegalArgumentException("El cluster es obligatorio");
        }
        if (intervaloMuestreo < 1 || maxPuntos < 1) {
            throw new IllegalArgumentException("El intervalo y el maximo de puntos deben ser 1 o mas");
        }
        this.cluster = cluster;
        this.intervaloMuestreo = intervaloMuestreo;
        this.maxPuntos = maxPuntos;
        asegurarSeries(cluster.cantidadComputadores());
    }

    // ================= reloj =================

    @Override
    public void alCiclo(long numeroCiclo) {
        Computador[] computadores = cluster.getComputadores();
        mutex.acquireUninterruptibly();
        try {
            asegurarSeries(computadores.length);
            for (Computador c : computadores) {
                Pcb[] bloqueados = c.getNucleo().getBloqueados();
                for (Pcb p : bloqueados) {
                    contarBloqueo(p);
                }
            }
            if (numeroCiclo % intervaloMuestreo == 0) {
                for (int i = 0; i < computadores.length; i++) {
                    muestrear(i, computadores[i], numeroCiclo);
                }
            }
        } finally {
            mutex.release();
        }
    }

    // ================= consulta =================

    /** Serie de utilizacion (0.0 a 1.0) del computador con ese identificador (1, 2, 3...). */
    public SerieTemporal getSerie(int idComputador) {
        mutex.acquireUninterruptibly();
        try {
            if (idComputador < 1 || idComputador > series.length) {
                throw new IllegalArgumentException("No hay serie para el computador " + idComputador);
            }
            return series[idComputador - 1];
        } finally {
            mutex.release();
        }
    }

    public int cantidadSeries() {
        mutex.acquireUninterruptibly();
        try {
            return series.length;
        } finally {
            mutex.release();
        }
    }

    /** Calcula todas las metricas con el estado actual del sistema. */
    public ReporteMetricas generarReporte() {
        Computador[] computadores = cluster.getComputadores();
        long ciclo = cluster.getCicloActual();

        MetricasComputador[] porComputador = new MetricasComputador[computadores.length];
        Pcb[] todos = new Pcb[0];
        long ocupadaTotal = 0;
        long ciclosTotal = 0;
        for (int i = 0; i < computadores.length; i++) {
            Computador c = computadores[i];
            Pcb[] procesos = procesosDe(c);
            porComputador[i] = calcular(c.getId(), procesos, c.getCpu().getCiclosOcupada(),
                    c.getCpu().getCiclosTotales(), ciclo);
            ocupadaTotal += c.getCpu().getCiclosOcupada();
            ciclosTotal += c.getCpu().getCiclosTotales();
            todos = concatenar(todos, procesos);
        }
        MetricasComputador sistema = calcular(0, todos, ocupadaTotal, ciclosTotal, ciclo);

        BufferAcotado[] bufs = cluster.getBuffers();
        MetricasBuffer[] mb = new MetricasBuffer[bufs.length];
        for (int i = 0; i < bufs.length; i++) {
            BufferAcotado b = bufs[i];
            mb[i] = new MetricasBuffer(b.getId(), b.getIdComputadorAnfitrion(), b.getCapacidad(),
                    b.getElementos(), b.getProducidos(), b.getConsumidos());
        }

        int procesosConBuffer = 0;
        long ciclosSemaforo = 0;
        long ciclosRemoto = 0;
        mutex.acquireUninterruptibly();
        try {
            for (Pcb p : todos) {
                if (usaBuffer(p)) {
                    procesosConBuffer++;
                    RegistroBloqueo r = buscarRegistro(p);
                    if (r != null) {
                        ciclosSemaforo += r.ciclosSemaforo;
                        ciclosRemoto += r.ciclosRemoto;
                    }
                }
            }
        } finally {
            mutex.release();
        }
        double bloqueoSem = procesosConBuffer > 0 ? (double) ciclosSemaforo / procesosConBuffer : Double.NaN;
        double bloqueoRem = procesosConBuffer > 0 ? (double) ciclosRemoto / procesosConBuffer : Double.NaN;
        return new ReporteMetricas(ciclo, porComputador, sistema, mb, procesosConBuffer, bloqueoSem, bloqueoRem);
    }

    // ================= calculo =================

    /** Calcula las metricas de un conjunto de procesos y los ciclos de CPU asociados. */
    private static MetricasComputador calcular(int id, Pcb[] procesos, long ocupada, long ciclosCpu, long ciclo) {
        int completados = 0;
        int vencidos = 0;
        double sumaRespuesta = 0;
        int conRespuesta = 0;
        double sumaX = 0;
        double sumaX2 = 0;
        int n = 0;

        for (Pcb p : procesos) {
            if (p.getEstado() == EstadoProceso.TERMINADO) {
                if (p.isTerminadoPorDeadline()) {
                    vencidos++;
                } else {
                    completados++;
                }
            }
            if (p.getCicloPrimeraEjecucion() >= 0) {
                sumaRespuesta += p.getCicloPrimeraEjecucion() - p.getCicloCreacion();
                conRespuesta++;
            }
            long fin = p.getCicloFinalizacion() >= 0 ? p.getCicloFinalizacion() : ciclo;
            long edad = fin - p.getCicloCreacion();
            if (edad >= 1) {
                double x = (double) p.getPc() / edad;   // fraccion de su vida que uso la CPU
                sumaX += x;
                sumaX2 += x * x;
                n++;
            }
        }
        double throughput = ciclo > 0 ? (double) completados / ciclo : 0.0;
        double utilizacion = ciclosCpu > 0 ? (double) ocupada / ciclosCpu : 0.0;
        double respuesta = conRespuesta > 0 ? sumaRespuesta / conRespuesta : Double.NaN;
        double cumplimiento = (completados + vencidos) > 0
                ? (double) completados / (completados + vencidos) : Double.NaN;
        double equidad = (n > 0 && sumaX2 > 0) ? (sumaX * sumaX) / (n * sumaX2) : Double.NaN;
        return new MetricasComputador(id, ciclo, procesos.length, completados, vencidos, throughput,
                utilizacion, respuesta, cumplimiento, equidad);
    }

    /** Todos los procesos de un computador: colas mas el que esta en la CPU, sin repetidos. */
    private static Pcb[] procesosDe(Computador c) {
        Pcb[] resultado = new Pcb[0];
        Pcb enCpu = c.getCpu().getProceso();
        if (enCpu != null) {
            resultado = new Pcb[]{enCpu};
        }
        resultado = concatenar(resultado, c.getNucleo().getNuevos());
        resultado = concatenar(resultado, c.getNucleo().getListos());
        resultado = concatenar(resultado, c.getNucleo().getBloqueados());
        resultado = concatenar(resultado, c.getNucleo().getTerminados());
        return resultado;
    }

    /** Une dos arreglos sin repetir procesos (un proceso puede cambiar de cola mientras se lee). */
    private static Pcb[] concatenar(Pcb[] a, Pcb[] b) {
        Pcb[] mezcla = new Pcb[a.length + b.length];
        int n = 0;
        for (Pcb p : a) {
            mezcla[n++] = p;
        }
        for (Pcb p : b) {
            boolean repetido = false;
            for (int i = 0; i < n && !repetido; i++) {
                repetido = mezcla[i] == p;
            }
            if (!repetido) {
                mezcla[n++] = p;
            }
        }
        Pcb[] exacto = new Pcb[n];
        for (int i = 0; i < n; i++) {
            exacto[i] = mezcla[i];
        }
        return exacto;
    }

    private static boolean usaBuffer(Pcb p) {
        return p.getTipo() == TipoProceso.PRODUCTOR || p.getTipo() == TipoProceso.CONSUMIDOR;
    }

    // ================= privados (el mutex ya esta tomado) =================

    private void asegurarSeries(int cantidad) {
        if (series.length == cantidad) {
            return;
        }
        SerieTemporal[] nuevas = new SerieTemporal[cantidad];
        long[] ocupada = new long[cantidad];
        long[] total = new long[cantidad];
        for (int i = 0; i < cantidad; i++) {
            if (i < series.length) {
                nuevas[i] = series[i];
                ocupada[i] = ultimaOcupada[i];
                total[i] = ultimaTotal[i];
            } else {
                nuevas[i] = new SerieTemporal(maxPuntos);
            }
        }
        series = nuevas;
        ultimaOcupada = ocupada;
        ultimaTotal = total;
    }

    private void muestrear(int indice, Computador c, long ciclo) {
        long ocupada = c.getCpu().getCiclosOcupada();
        long total = c.getCpu().getCiclosTotales();
        long deltaTotal = total - ultimaTotal[indice];
        if (deltaTotal > 0) {
            double utilizacion = (double) (ocupada - ultimaOcupada[indice]) / deltaTotal;
            series[indice].agregar(ciclo, utilizacion);
        }
        ultimaOcupada[indice] = ocupada;
        ultimaTotal[indice] = total;
    }

    private void contarBloqueo(Pcb p) {
        if (!usaBuffer(p)) {
            return;
        }
        RegistroBloqueo r = buscarRegistro(p);
        if (r == null) {
            r = new RegistroBloqueo(p);
            bloqueos.agregarAlFinal(r);
        }
        switch (p.getMotivoBloqueo()) {
            case ACCESO_REMOTO:
                r.ciclosRemoto++;
                break;
            case BUFFER_LLENO:
            case BUFFER_VACIO:
            case REGION_CRITICA_OCUPADA:
                r.ciclosSemaforo++;
                break;
            default:
                break;   // la E/S no cuenta como bloqueo de semaforo
        }
    }

    private RegistroBloqueo buscarRegistro(Pcb p) {
        Iterador<RegistroBloqueo> it = bloqueos.iterador();
        while (it.tieneSiguiente()) {
            RegistroBloqueo r = it.siguiente();
            if (r.proceso == p) {
                return r;
            }
        }
        return null;
    }
}