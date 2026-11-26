/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.demo;

import avilaos.app.Sistema;
import avilaos.config.ConfiguracionProcesos;
import avilaos.fs.CargadorSistemaArchivos;
import avilaos.model.PoliticaDisco;
import avilaos.model.PoliticaPlanificacion;
import avilaos.model.SolicitudProceso;
import avilaos.model.TipoOperacionES;

/**
 *
 * @author richard
 * 
 * Casos P1 a P4 del enunciado con el sistema armado de punta a punta: procesos, nucleo,
 * disco, swap y sistema de archivos.
 */
public class PruebaCasosDisco {

    private static final String JSON_DISCO = "{\"diskConfig\":{\"totalBlocks\":200,\"initialHead\":50},"
            + "\"fileSystem\":["
            + archivo("/mbr.bin", 0, 11) + ","
            + "{\"path\":\"/sistema\",\"type\":\"DIRECTORY\",\"owner\":\"admin\",\"permissions\":\"public\"},"
            + "{\"path\":\"/usuarios\",\"type\":\"DIRECTORY\",\"owner\":\"admin\",\"permissions\":\"public\"},"
            + archivo("/sistema/boot.bin", 11, 23) + ","
            + archivo("/usuarios/notas.txt", 34, 28) + ","
            + archivo("/sistema/script.py", 62, 2) + ","
            + archivo("/sistema/core.dll", 64, 31) + ","
            + archivo("/sistema/config.sys", 95, 24) + ","
            + archivo("/usuarios/img1.png", 119, 4) + ","
            + archivo("/usuarios/data.csv", 123, 57) + ","
            + archivo("/usuarios/video.mp4", 180, 20)
            + "]}";

    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        probarCaso("P1 FIFO", PoliticaDisco.FIFO, "95 180 34 119 11 123 62 64 ", 644);
        probarCaso("P2 SSTF", PoliticaDisco.SSTF, "62 64 34 11 95 119 123 180 ", 236);
        probarCaso("P3 SCAN", PoliticaDisco.SCAN, "62 64 95 119 123 180 34 11 ", 337);
        probarCaso("P4 C-SCAN", PoliticaDisco.C_SCAN, "62 64 95 119 123 180 11 34 ", 183);
        probarPocaRam();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static void probarCaso(String nombre, PoliticaDisco politica, String ordenEsperado, int desplazamiento) {
        System.out.println("--- " + nombre + " ---");
        Sistema s = armar(politica, 1024);
        StringBuilder orden = new StringBuilder();
        s.getDisco().agregarOyente((solicitud, ciclo) -> orden.append(solicitud.getBloqueDestino()).append(' '));
        avanzar(s);
        verificar(s.getCluster().contarTerminados() == 8, "terminan los 8 procesos");
        verificar(orden.toString().equals(ordenEsperado), "orden: " + orden);
        verificar(s.getDisco().getTotalDesplazamiento() == desplazamiento,
                "desplazamiento: " + s.getDisco().getTotalDesplazamiento());
        verificar(!s.getSistemaArchivos().leer("/usuarios/img1.png").isExito(), "el DELETE borra img1.png");
    }

    /** Con poca RAM, el disco no puede quedarse retenido esperando a procesos que no caben. */
    private static void probarPocaRam() {
        System.out.println("--- Poca RAM ---");
        Sistema s = armar(PoliticaDisco.SSTF, 32);
        avanzar(s);
        verificar(!s.getDisco().isRetenido(), "el disco se suelta");
        verificar(s.getCluster().contarTerminados() == 8, "terminan los 8 procesos usando el swap");
        verificar(s.getCluster().getComputador(1).getRam().getUsada() == 0, "la RAM queda vacia");
    }

    private static Sistema armar(PoliticaDisco politica, int ram) {
        SolicitudProceso[] procesos = {
            proceso("Proc_95", "/sistema/config.sys", TipoOperacionES.LEER),
            proceso("Proc_180", "/usuarios/video.mp4", TipoOperacionES.LEER),
            proceso("Proc_34", "/usuarios/notas.txt", TipoOperacionES.ACTUALIZAR),
            proceso("Proc_119", "/usuarios/img1.png", TipoOperacionES.ELIMINAR),
            proceso("Proc_11", "/sistema/boot.bin", TipoOperacionES.LEER),
            proceso("Proc_123", "/usuarios/data.csv", TipoOperacionES.ACTUALIZAR),
            proceso("Proc_62", "/sistema/script.py", TipoOperacionES.LEER),
            proceso("Proc_64", "/sistema/core.dll", TipoOperacionES.LEER)
        };
        ConfiguracionProcesos config = new ConfiguracionProcesos(300, 100, politica, 5, ram,
                PoliticaPlanificacion.FCFS, 0, procesos);
        return Sistema.construirConDisco(config, CargadorSistemaArchivos.desdeJson(JSON_DISCO));
    }

    private static SolicitudProceso proceso(String nombre, String ruta, TipoOperacionES operacion) {
        return SolicitudProceso.deEs(nombre, 2, 16, 5, 2000, operacion, ruta, 0, null, "admin");
    }

    private static String archivo(String ruta, int inicio, int bloques) {
        return "{\"path\":\"" + ruta + "\",\"type\":\"FILE\",\"startBlock\":" + inicio + ",\"blocks\":" + bloques
                + ",\"owner\":\"admin\",\"permissions\":\"public\"}";
    }

    private static void avanzar(Sistema s) {
        for (int i = 0; i < 1800 && s.getCluster().contarTerminados() < 8; i++) {
            s.getCluster().avanzarCiclo();
        }
        for (int i = 0; i < 200; i++) {
            s.getCluster().avanzarCiclo();
        }
    }

    private static void verificar(boolean condicion, String descripcion) {
        total++;
        if (!condicion) {
            fallos++;
        }
        System.out.println((condicion ? "OK    - " : "FALLO - ") + descripcion);
    }
}
