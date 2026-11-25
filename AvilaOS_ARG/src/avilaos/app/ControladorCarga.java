/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.app;

import avilaos.config.ConfiguracionProcesos;
import avilaos.fs.CargadorSistemaArchivos;
import avilaos.fs.EstadoDiscoInicial;
import avilaos.fs.SistemaArchivos;
import avilaos.gui.AccionesCarga;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 *
 * @author richard
 * 
 * Implementa la carga obligatoria de los dos JSON. Con los dos cargados arma el sistema
 * y arranca el reloj. Al reiniciar con procesos nuevos conserva los archivos y el
 * cabezal de la corrida anterior; el swap empieza vacio porque sus procesos ya no existen.
 */
public class ControladorCarga implements AccionesCarga {

    private final OyenteSistema oyente;
    private EstadoDiscoInicial estadoDisco;
    private ConfiguracionProcesos configuracion;
    private Sistema sistema;

    public ControladorCarga(OyenteSistema oyente) {
        this.oyente = oyente;
    }

    @Override
    public String cargarSistemaArchivos(String textoJson) {
        if (sistema != null) {
            return "La simulación ya está iniciada: el sistema de archivos no se puede cambiar";
        }
        try {
            estadoDisco = CargadorSistemaArchivos.desdeJson(textoJson);
            return null;
        } catch (RuntimeException e) {
            estadoDisco = null;
            return mensaje(e);
        }
    }

    @Override
    public String cargarProcesos(String textoJson) {
        if (sistema != null) {
            return "La simulación ya está iniciada: usa \"Reiniciar con nuevos procesos\"";
        }
        try {
            configuracion = ConfiguracionProcesos.desdeJson(textoJson);
            return null;
        } catch (RuntimeException e) {
            configuracion = null;
            return mensaje(e);
        }
    }

    @Override
    public String iniciar() {
        if (sistema != null) {
            return "La simulación ya está iniciada";
        }
        if (estadoDisco == null) {
            return "Falta cargar el JSON del sistema de archivos";
        }
        if (configuracion == null) {
            return "Falta cargar el JSON de procesos y configuración";
        }
        try {
            Sistema nuevo = Sistema.construirConDisco(configuracion, estadoDisco);
            arrancar(nuevo);
            return null;
        } catch (RuntimeException e) {
            return mensaje(e);
        }
    }

    @Override
    public String reiniciarConservandoDisco(String textoJson) {
        if (sistema == null) {
            return "Primero hay que iniciar la simulación";
        }
        try {
            ConfiguracionProcesos nuevaConfiguracion = ConfiguracionProcesos.desdeJson(textoJson);
            EstadoDiscoInicial discoActual = CargadorSistemaArchivos.desdeJson(jsonDiscoActual());
            Sistema nuevo = Sistema.construirConDisco(nuevaConfiguracion, discoActual);
            sistema.getCluster().detener();
            configuracion = nuevaConfiguracion;
            estadoDisco = discoActual;
            arrancar(nuevo);
            return null;
        } catch (RuntimeException e) {
            return mensaje(e);
        }
    }

    @Override
    public String guardarSistemaArchivos(File destino) {
        if (estadoDisco == null) {
            return "No hay un sistema de archivos cargado";
        }
        try {
            Files.writeString(destino.toPath(), jsonDiscoActual(), StandardCharsets.UTF_8);
            return null;
        } catch (IOException | RuntimeException e) {
            return "No se pudo guardar: " + mensaje(e);
        }
    }

    /** El sistema que esta corriendo, o null si todavia no se inicio. */
    public Sistema getSistema() {
        return sistema;
    }

    private void arrancar(Sistema nuevo) {
        sistema = nuevo;
        if (oyente != null) {
            oyente.sistemaNuevo(nuevo);
        }
        nuevo.getCluster().iniciar();
    }

    /**
     * El sistema de archivos actual en JSON. El formato solo admite el cabezal dentro del
     * area de archivos, asi que si esta en el swap se guarda en el ultimo bloque de archivos.
     */
    private String jsonDiscoActual() {
        SistemaArchivos fs = sistema != null ? sistema.getSistemaArchivos() : estadoDisco.getSistemaArchivos();
        int cabezal = sistema != null ? sistema.getDisco().getCabezal() : estadoDisco.getCabezalInicial();
        int ultimoBloqueArchivos = fs.getMapa().getBloquesArchivos() - 1;
        return CargadorSistemaArchivos.aJson(fs, Math.min(cabezal, ultimoBloqueArchivos));
    }

    private static String mensaje(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}
