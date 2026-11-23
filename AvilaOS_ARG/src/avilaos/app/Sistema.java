/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.app;

import avilaos.cluster.Cluster;
import avilaos.config.ConfiguracionProcesos;
import avilaos.config.ConfiguracionSimulacion;
import avilaos.config.EntradaInvalidaException;
import avilaos.disco.Disco;
import avilaos.disco.FabricaPoliticasDisco;
import avilaos.disco.SolicitudDisco;
import avilaos.fs.EstadoDiscoInicial;
import avilaos.fs.MapaBloques;
import avilaos.fs.SistemaArchivos;
import avilaos.kernel.Nucleo;
import avilaos.kernel.PuertoEsArchivos;
import avilaos.kernel.SwapDisco;
import avilaos.metrics.MetricasDisco;
import avilaos.metrics.RecolectorMetricas;
import avilaos.model.GeneradorId;
import avilaos.model.SolicitudProceso;
import avilaos.scheduler.FabricaPlanificadores;

/**
 * @author richard
 * 
 * Un sistema ya armado a partir de una configuracion: el cluster con sus computadores,
 * buffers y procesos iniciales, el controlador que usa la interfaz y el recolector de
 * metricas (ya registrado en el reloj).
 */
public final class Sistema {

    private static final int MAX_MENSAJES_BITACORA = 500;
    private static final int INTERVALO_MUESTREO = 2;
    private static final int MAX_PUNTOS_GRAFICA = 300;

    private final Cluster cluster;
    private final ControladorSimulador controlador;
    private final RecolectorMetricas recolector;
    private final ConfiguracionSimulacion configuracion;
    private final Disco disco;
    private final SistemaArchivos sistemaArchivos;
    private final MetricasDisco metricasDisco;

    private Sistema(Cluster cluster, ControladorSimulador controlador, RecolectorMetricas recolector,
                    ConfiguracionSimulacion configuracion, Disco disco, SistemaArchivos sistemaArchivos,
                    MetricasDisco metricasDisco) {
        this.cluster = cluster;
        this.controlador = controlador;
        this.recolector = recolector;
        this.configuracion = configuracion;
        this.disco = disco;
        this.sistemaArchivos = sistemaArchivos;
        this.metricasDisco = metricasDisco;
    }

    /**
     * Arma el sistema. Reinicia los identificadores (los procesos vuelven a empezar en P1).
     * Si algo no se puede crear (por ejemplo, un proceso que no cabe en la RAM) lanza
     * EntradaInvalidaException con un mensaje claro y no deja nada a medias.
     */
    public static Sistema construir(ConfiguracionSimulacion config) {
        GeneradorId.reiniciar();
        Cluster cluster = new Cluster(config.getDuracionCicloMs(), MAX_MENSAJES_BITACORA);
        for (ConfiguracionSimulacion.Computador c : config.getComputadores()) {
            cluster.agregarComputador(c.memoriaMb, FabricaPlanificadores.crear(c.politica, c.quantum));
        }
        RecolectorMetricas recolector = new RecolectorMetricas(cluster, INTERVALO_MUESTREO, MAX_PUNTOS_GRAFICA);
        cluster.getReloj().registrar(recolector);
        ControladorSimulador controlador = new ControladorSimulador(cluster);
        int numero = 0;
        for (SolicitudProceso s : config.getProcesos()) {
            numero++;
            String error = controlador.crearProceso(s);
            if (error != null) {
                throw new EntradaInvalidaException("No se pudo crear el proceso " + numero + " ("
                        + s.getNombre() + "): " + error);
            }
        }
        return new Sistema(cluster, controlador, recolector, config, null, null, null);
    }

    /** Arma el sistema real a partir de los dos JSON: conecta el nucleo, el disco y el swap. */
    public static Sistema construirConDisco(ConfiguracionProcesos config, EstadoDiscoInicial estado) {
        GeneradorId.reiniciar();
        SolicitudDisco.reiniciarIds();
        Cluster cluster = new Cluster(config.getCycleDurationMs(), MAX_MENSAJES_BITACORA);
        Nucleo nucleo = cluster.agregarComputador(config.getRamCapacity(),
                FabricaPlanificadores.crear(config.getComputerPolicy(), config.getQuantum())).getNucleo();
        MapaBloques mapa = estado.getMapa();
        mapa.configurarSwap(config.getSwapSizeBlocks());
        Disco disco = new Disco(mapa.getTotalBloques(), FabricaPoliticasDisco.crear(config.getInitialDiskPolicy()),
                estado.getCabezalInicial(), config.getHeadSpeed());
        nucleo.setPuertoEs(new PuertoEsArchivos(estado.getSistemaArchivos(), disco));
        nucleo.setPuertoSwap(new SwapDisco(mapa, disco));
        disco.agregarOyente(nucleo);
        MetricasDisco metricasDisco = new MetricasDisco(disco);
        cluster.conectarAlReloj();
        cluster.getReloj().registrar(disco);
        cluster.getReloj().registrar(metricasDisco);
        RecolectorMetricas recolector = new RecolectorMetricas(cluster, INTERVALO_MUESTREO, MAX_PUNTOS_GRAFICA);
        cluster.getReloj().registrar(recolector);
        ControladorSimulador controlador = new ControladorSimulador(cluster);
        int numero = 0;
        for (SolicitudProceso s : config.getProcesos()) {
            numero++;
            String error = controlador.crearProceso(s);
            if (error != null) {
                throw new EntradaInvalidaException("No se pudo crear el proceso " + numero + " ("
                        + s.getNombre() + "): " + error);
            }
        }
        return new Sistema(cluster, controlador, recolector, null, disco, estado.getSistemaArchivos(),
                metricasDisco);
    }

    public Disco getDisco() {
        return disco;
    }

    public SistemaArchivos getSistemaArchivos() {
        return sistemaArchivos;
    }

    public MetricasDisco getMetricasDisco() {
        return metricasDisco;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public ControladorSimulador getControlador() {
        return controlador;
    }

    public RecolectorMetricas getRecolector() {
        return recolector;
    }

    public ConfiguracionSimulacion getConfiguracion() {
        return configuracion;
    }
}