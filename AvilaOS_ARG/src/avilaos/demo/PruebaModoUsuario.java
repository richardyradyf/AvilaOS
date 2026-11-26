/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.demo;

import avilaos.app.ControladorCarga;
import avilaos.app.ControladorSimulador;
import avilaos.app.Sistema;
import avilaos.fs.SesionUsuario;
import avilaos.gui.VistaDisco;
import avilaos.model.ModoUsuario;
import avilaos.model.Pcb;
import avilaos.model.PoliticaPlanificacion;
import avilaos.model.SolicitudProceso;
import avilaos.model.TipoOperacionES;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author richard
 * 
 * Pruebas del modo usuario en la aplicacion: el controlador rechaza lo que el usuario no
 * puede hacer, explica por que y la simulacion sigue corriendo.
 */
public class PruebaModoUsuario {

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
        carga.iniciar();
        Sistema s = carga.getSistema();
        s.getCluster().pausar();

        SesionUsuario sesion = new SesionUsuario();
        ControladorSimulador controlador = s.getControlador();
        controlador.setPermisos(sesion, s.getSistemaArchivos());

        probarAdministrador(controlador);
        probarUsuario(controlador, sesion, s);
        probarDisco(s, sesion);
        s.getCluster().detener();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static void probarAdministrador(ControladorSimulador controlador) {
        System.out.println("--- Administrador ---");
        verificar(controlador.crearProceso(es("Borra", TipoOperacionES.ELIMINAR, "/usuarios/data.csv")) == null,
                "puede borrar un archivo de otro usuario");
        verificar(controlador.cambiarPolitica(1, PoliticaPlanificacion.ROUND_ROBIN, 3) == null,
                "puede cambiar la politica de la CPU");
    }

    private static void probarUsuario(ControladorSimulador controlador, SesionUsuario sesion, Sistema s) {
        System.out.println("--- Usuario user1 ---");
        sesion.cambiar(ModoUsuario.USUARIO, "user1");
        verificar(controlador.crearProceso(es("Lee", TipoOperacionES.LEER, "/usuarios/notas.txt")) == null,
                "lee su propio archivo privado");
        Pcb creado = ultimoNuevo(s);
        verificar(creado != null && "user1".equals(creado.getUsuario()), "el proceso queda a nombre de user1");
        verificar(controlador.crearProceso(es("LeePublico", TipoOperacionES.LEER, "/sistema/boot.bin")) == null,
                "lee un archivo publico");
        verificar(controlador.crearProceso(es("Borra", TipoOperacionES.ELIMINAR, "/usuarios/data.csv")) != null,
                "no borra un archivo de user2");
        verificar(controlador.crearProceso(es("Renombra", TipoOperacionES.ACTUALIZAR, "/usuarios/video.mp4")) != null,
                "no renombra");
        verificar(controlador.crearProceso(es("Crea", TipoOperacionES.CREAR, "/usuarios/nuevo.txt")) != null,
                "no crea en un directorio del administrador");
        String rechazo = controlador.cambiarPolitica(1, PoliticaPlanificacion.FCFS, 1);
        verificar(rechazo != null && rechazo.contains("administrador"), "no cambia la politica y explica por que");

        System.out.println("--- Usuario user2 ---");
        sesion.cambiar(ModoUsuario.USUARIO, "user2");
        verificar(controlador.crearProceso(es("Lee", TipoOperacionES.LEER, "/usuarios/notas.txt")) != null,
                "no lee el archivo privado de user1");

        long ciclo = s.getCluster().getCicloActual();
        for (int i = 0; i < 50; i++) {
            s.getCluster().avanzarCiclo();
        }
        verificar(s.getCluster().getCicloActual() == ciclo + 50, "la simulacion sigue corriendo");
        sesion.cambiar(ModoUsuario.ADMINISTRADOR, null);
    }

    private static void probarDisco(Sistema s, SesionUsuario sesion) {
        System.out.println("--- Politica del disco ---");
        VistaDisco vista = new VistaDisco(s.getDisco(), s.getSistemaArchivos().getMapa(), s.getMetricasDisco());
        vista.setCambiosPermitidos(false);
        verificar(!vista.isCambiosPermitidos(), "en modo usuario se bloquea el selector del disco");
        vista.setCambiosPermitidos(true);
        verificar(vista.isCambiosPermitidos(), "en modo administrador se vuelve a habilitar");
        vista.detener();
    }

    private static Pcb ultimoNuevo(Sistema s) {
        Pcb[] nuevos = s.getCluster().getComputador(1).getNucleo().getNuevos();
        Pcb[] listos = s.getCluster().getComputador(1).getNucleo().getListos();
        if (nuevos.length > 0) {
            return nuevos[nuevos.length - 1];
        }
        return listos.length > 0 ? listos[listos.length - 1] : null;
    }

    private static SolicitudProceso es(String nombre, TipoOperacionES operacion, String ruta) {
        int bloques = operacion == TipoOperacionES.CREAR ? 2 : 0;
        return SolicitudProceso.deEs(nombre, 2, 16, 5, 500, operacion, ruta, bloques, null, "admin");
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
