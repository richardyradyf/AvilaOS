/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.demo;

import avilaos.app.ControladorCarga;
import avilaos.app.Sistema;
import avilaos.gui.VistaDisco;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author richard
 * 
 * Prueba de la pestaña del disco con el sistema real armado desde los JSON del enunciado:
 * se dibuja mientras el disco trabaja y deja de refrescarse al detenerla.
 */
public class PruebaVistaDisco {

    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) throws Exception {
        Path carpeta = Paths.get("json");
        if (!Files.exists(carpeta)) {
            carpeta = Paths.get("AvilaOS_ARG", "json");
        }
        ControladorCarga carga = new ControladorCarga(null);
        verificar(carga.cargarSistemaArchivos(leer(carpeta.resolve("sistema_archivos.json"))) == null,
                "carga sistema_archivos.json");
        verificar(carga.cargarProcesos(leer(carpeta.resolve("procesos.json"))) == null, "carga procesos.json");
        verificar(carga.iniciar() == null, "inicia la simulacion");
        Sistema s = carga.getSistema();
        s.getCluster().pausar();

        VistaDisco vista = new VistaDisco(s.getDisco(), s.getSistemaArchivos().getMapa(), s.getMetricasDisco());
        verificar(vista.estaRefrescando(), "la vista se refresca sola");
        boolean sinErrores = true;
        try {
            for (int i = 0; i < 300; i++) {
                s.getCluster().avanzarCiclo();
                if (i % 10 == 0) {
                    vista.actualizar();
                }
            }
            vista.actualizar();
        } catch (RuntimeException e) {
            sinErrores = false;
        }
        verificar(sinErrores, "se actualiza sin errores mientras el disco trabaja");
        verificar(s.getMetricasDisco().getTotalAtendidas() == 8, "el disco atendio las 8 solicitudes");
        vista.detener();
        verificar(!vista.estaRefrescando(), "deja de refrescarse al detenerla");
        s.getCluster().detener();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static String leer(Path ruta) throws Exception {
        return Files.readString(ruta, StandardCharsets.UTF_8);
    }

    private static void verificar(boolean condicion, String descripcion) {
        total++;
        if (!condicion) {
            fallos++;
        }
        System.out.println((condicion ? "OK    - " : "FALLO - ") + descripcion);
    }
}