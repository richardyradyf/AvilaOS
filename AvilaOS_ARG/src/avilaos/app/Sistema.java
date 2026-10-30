/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.app;

import avilaos.cluster.Cluster;
import avilaos.config.ConfiguracionSimulacion;
import avilaos.config.EntradaInvalidaException;
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

    private Sistema(Cluster cluster, ControladorSimulador controlador, RecolectorMetricas recolector,
                    ConfiguracionSimulacion configuracion) {
        this.cluster = cluster;
        this.controlador = controlador;
        this.recolector = recolector;
        this.configuracion = configuracion;
    }

    /**
     * Arma el sistema. Reinicia los identificadores (los procesos vuelven a empezar en P1).
     * Si algo no se puede crear (por ejemplo, un proceso que no cabe en la RAM) lanza
     * EntradaInvalidaException con un mensaje claro y no deja nada a medias.
     */
    public static Sistema construir(ConfiguracionSimulacion config) {
        GeneradorId.reiniciar();
        Cluster cluster = new Cluster(config.getDuracionCicloMs(), MAX_MENSAJES_BITACORA, config.getLatenciaRed());
        for (ConfiguracionSimulacion.Computador c : config.getComputadores()) {
            cluster.agregarComputador(c.memoriaMb, FabricaPlanificadores.crear(c.politica, c.quantum));
        }
        int numero = 0;
        try {
            for (ConfiguracionSimulacion.Buffer b : config.getBuffers()) {
                numero++;
                cluster.crearBuffer(b.anfitrion, b.capacidad);
            }
        } catch (RuntimeException e) {
            throw new EntradaInvalidaException("No se pudo crear el buffer " + numero + ": " + e.getMessage());
        }
        RecolectorMetricas recolector = new RecolectorMetricas(cluster, INTERVALO_MUESTREO, MAX_PUNTOS_GRAFICA);
        cluster.getReloj().registrar(recolector);
        ControladorSimulador controlador = new ControladorSimulador(cluster);
        numero = 0;
        for (SolicitudProceso s : config.getProcesos()) {
            numero++;
            String error = controlador.crearProceso(s);
            if (error != null) {
                throw new EntradaInvalidaException("No se pudo crear el proceso " + numero + " ("
                        + s.getNombre() + "): " + error);
            }
        }
        return new Sistema(cluster, controlador, recolector, config);
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