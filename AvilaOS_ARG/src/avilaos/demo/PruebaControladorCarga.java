/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.demo;

import avilaos.app.ControladorCarga;
import avilaos.app.Sistema;
import avilaos.config.ConfiguracionProcesos;
import avilaos.fs.CargadorSistemaArchivos;
import avilaos.fs.EstadoDiscoInicial;
import avilaos.model.PoliticaDisco;
import avilaos.model.PoliticaPlanificacion;
import avilaos.model.SolicitudProceso;
import avilaos.model.TipoOperacionES;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 *
 * @author richard
 * 
 * Pruebas de la carga de los dos JSON: no se inicia sin ellos, se reinicia conservando
 * el disco y se guarda el sistema de archivos.
 */
public class PruebaControladorCarga {

    private static final String JSON_DISCO = "{\"diskConfig\":{\"totalBlocks\":200,\"initialHead\":50},"
            + "\"fileSystem\":["
            + "{\"path\":\"/usuarios\",\"type\":\"DIRECTORY\",\"owner\":\"admin\",\"permissions\":\"public\"},"
            + archivo("/usuarios/img1.png", 119, 4) + ","
            + archivo("/usuarios/video.mp4", 180, 20)
            + "]}";

    private static int total = 0;
    private static int fallos = 0;
    private static int avisos = 0;

    public static void main(String[] args) throws Exception {
        ControladorCarga carga = new ControladorCarga(s -> avisos++);
        try {
            probarSinJson(carga);
            probarInicio(carga);
            probarReinicio(carga);
            probarGuardar(carga);
        } finally {
            if (carga.getSistema() != null) {
                carga.getSistema().getCluster().detener();
            }
        }

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static void probarSinJson(ControladorCarga carga) {
        System.out.println("--- Sin los JSON ---");
        verificar(carga.iniciar() != null, "no inicia sin los dos JSON");
        verificar(carga.cargarSistemaArchivos("{ roto") != null, "rechaza un JSON de disco invalido");
        verificar(carga.cargarSistemaArchivos(JSON_DISCO) == null, "carga el JSON del disco");
        verificar(carga.iniciar() != null, "no inicia sin el JSON de procesos");
        verificar(carga.cargarProcesos("{}") != null, "rechaza un JSON de procesos incompleto");
        verificar(carga.getSistema() == null && avisos == 0, "todavia no hay sistema");
    }

    private static void probarInicio(ControladorCarga carga) {
        System.out.println("--- Inicio ---");
        String procesos = procesos(proceso("Borra", "/usuarios/img1.png", TipoOperacionES.ELIMINAR, 0));
        verificar(carga.cargarProcesos(procesos) == null, "carga el JSON de procesos");
        verificar(carga.iniciar() == null, "inicia con los dos JSON");
        Sistema s = carga.getSistema();
        verificar(s != null && avisos == 1, "avisa el sistema nuevo");
        verificar(s.getCluster().getReloj().estaActivo(), "el reloj arranca");
        s.getCluster().pausar();
        verificar(carga.iniciar() != null, "no inicia dos veces");
        verificar(carga.cargarProcesos(procesos) != null, "no cambia los procesos con la simulacion en marcha");
        verificar(carga.cargarSistemaArchivos(JSON_DISCO) != null, "no cambia el disco con la simulacion en marcha");
        avanzar(s);
        verificar(!s.getSistemaArchivos().leer("/usuarios/img1.png").isExito(), "el proceso borra img1.png");
    }

    private static void probarReinicio(ControladorCarga carga) {
        System.out.println("--- Reinicio conservando el disco ---");
        Sistema anterior = carga.getSistema();
        verificar(carga.reiniciarConservandoDisco("{ roto") != null && carga.getSistema() == anterior,
                "un JSON invalido no toca la simulacion");
        String procesos = procesos(proceso("Crea", "/usuarios/nuevo.txt", TipoOperacionES.CREAR, 3));
        verificar(carga.reiniciarConservandoDisco(procesos) == null, "reinicia con procesos nuevos");
        Sistema nuevo = carga.getSistema();
        verificar(nuevo != anterior && avisos == 2, "avisa el sistema reiniciado");
        verificar(!anterior.getCluster().getReloj().estaActivo(), "detiene el reloj anterior");
        nuevo.getCluster().pausar();
        verificar(!nuevo.getSistemaArchivos().leer("/usuarios/img1.png").isExito(), "img1.png sigue borrado");
        verificar(nuevo.getSistemaArchivos().leer("/usuarios/video.mp4").isExito(), "video.mp4 se conserva");
        verificar(nuevo.getDisco().getCabezal() == anterior.getDisco().getCabezal(), "conserva el cabezal");
        avanzar(nuevo);
        verificar(nuevo.getSistemaArchivos().leer("/usuarios/nuevo.txt").isExito(), "el proceso nuevo crea su archivo");
    }

    private static void probarGuardar(ControladorCarga carga) throws Exception {
        System.out.println("--- Guardar ---");
        File destino = File.createTempFile("avilaos", ".json");
        try {
            verificar(carga.guardarSistemaArchivos(destino) == null, "guarda el sistema de archivos");
            EstadoDiscoInicial leido = CargadorSistemaArchivos.desdeJson(
                    Files.readString(destino.toPath(), StandardCharsets.UTF_8));
            verificar(leido.getSistemaArchivos().leer("/usuarios/nuevo.txt").isExito()
                    && !leido.getSistemaArchivos().leer("/usuarios/img1.png").isExito(),
                    "el JSON guardado tiene el estado actual");
        } finally {
            destino.delete();
        }
    }

    private static void avanzar(Sistema s) {
        for (int i = 0; i < 300; i++) {
            s.getCluster().avanzarCiclo();
        }
    }

    private static String procesos(SolicitudProceso p) {
        return new ConfiguracionProcesos(5000, 100, PoliticaDisco.FIFO, 5, 512,
                PoliticaPlanificacion.FCFS, 0, new SolicitudProceso[]{p}).aJson();
    }

    private static SolicitudProceso proceso(String nombre, String ruta, TipoOperacionES operacion, int bloques) {
        return SolicitudProceso.deEs(nombre, 2, 16, 5, 2000, operacion, ruta, bloques, null, "admin");
    }

    private static String archivo(String ruta, int inicio, int bloques) {
        return "{\"path\":\"" + ruta + "\",\"type\":\"FILE\",\"startBlock\":" + inicio + ",\"blocks\":" + bloques
                + ",\"owner\":\"admin\",\"permissions\":\"public\"}";
    }

    private static void verificar(boolean condicion, String descripcion) {
        total++;
        if (!condicion) {
            fallos++;
        }
        System.out.println((condicion ? "OK    - " : "FALLO - ") + descripcion);
    }
}
