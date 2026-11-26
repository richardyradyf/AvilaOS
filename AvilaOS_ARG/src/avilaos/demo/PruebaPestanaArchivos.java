/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.demo;

import avilaos.app.ControladorCarga;
import avilaos.app.Sistema;
import avilaos.gui.PanelArbolArchivos;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author richard
 * 
 *  Prueba de la pestaña de archivos con el sistema real armado desde los JSON del
 * enunciado: el arbol se actualiza cuando un proceso borra un archivo y deja de
 * refrescarse al detenerlo.
 */
public class PruebaPestanaArchivos {

    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) throws Exception {
        Path carpeta = Paths.get("json");
        if (!Files.exists(carpeta)) {
            carpeta = Paths.get("AvilaOS_ARG", "json");
        }
        ControladorCarga carga = new ControladorCarga(null);
        carga.cargarSistemaArchivos(leer(carpeta.resolve("sistema_archivos.json")));
        carga.cargarProcesos(leer(carpeta.resolve("procesos.json")));
        verificar(carga.iniciar() == null, "inicia con los JSON del enunciado");
        Sistema s = carga.getSistema();
        s.getCluster().pausar();

        PanelArbolArchivos arbol = new PanelArbolArchivos(s.getSistemaArchivos());
        int versionInicial = s.getSistemaArchivos().getVersion();
        boolean sinErrores = true;
        try {
            for (int i = 0; i < 300; i++) {
                s.getCluster().avanzarCiclo();
                if (i % 10 == 0) {
                    arbol.refrescar();
                }
            }
            arbol.refrescar();
        } catch (RuntimeException e) {
            sinErrores = false;
        }
        verificar(sinErrores, "el arbol se refresca sin errores mientras corren los procesos");
        verificar(s.getSistemaArchivos().getVersion() != versionInicial, "los procesos cambiaron el sistema de archivos");
        verificar(!s.getSistemaArchivos().leer("/usuarios/img1.png").isExito(), "img1.png ya no esta");
        arbol.detener();
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