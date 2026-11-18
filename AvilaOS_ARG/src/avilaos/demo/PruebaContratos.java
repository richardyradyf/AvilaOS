/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.demo;

import avilaos.disco.OyenteDisco;
import avilaos.disco.SolicitudDisco;
import avilaos.model.EstadoProceso;
import avilaos.model.ModoUsuario;
import avilaos.model.PoliticaDisco;
import avilaos.model.SolicitudProceso;
import avilaos.model.TipoOperacionES;
import avilaos.model.TipoProceso;


/**
 *
 * @author richard
 * 
 * Pruebas de los contratos compartidos del Proyecto 2.
 */
public class PruebaContratos {

    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        probarEstados();
        probarOperaciones();
        probarPoliticas();
        probarSolicitudProceso();
        probarSolicitudDisco();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static void probarEstados() {
        System.out.println("--- Siete estados ---");
        verificar(EstadoProceso.values().length == 7, "hay siete estados");
        verificar(EstadoProceso.LISTO_SUSPENDIDO.estaSuspendido()
                && EstadoProceso.BLOQUEADO_SUSPENDIDO.estaSuspendido(), "los dos suspendidos lo indican");
        verificar(!EstadoProceso.BLOQUEADO.estaSuspendido() && !EstadoProceso.LISTO.estaSuspendido(),
                "Bloqueado y Listo no estan suspendidos");
        verificar(ModoUsuario.values().length == 2, "hay dos modos de usuario");
    }

    private static void probarOperaciones() {
        System.out.println("--- Operaciones de E/S ---");
        verificar(TipoOperacionES.desdeJson("READ") == TipoOperacionES.LEER, "READ es LEER");
        verificar(TipoOperacionES.desdeJson(" update ") == TipoOperacionES.ACTUALIZAR,
                "acepta espacios y minusculas");
        verificar(TipoOperacionES.CREAR.esDeArchivo() && !TipoOperacionES.SUSPENDER.esDeArchivo(),
                "CREAR es de archivo y SUSPENDER no");
        verificar(lanza(() -> TipoOperacionES.desdeJson("MOVE")), "MOVE es rechazada");
        verificar(lanza(() -> TipoOperacionES.desdeJson("SUSPENDER")), "SUSPENDER no viene del JSON");
        verificar(lanza(() -> TipoOperacionES.desdeJson(null)), "null es rechazado");
    }

    private static void probarPoliticas() {
        System.out.println("--- Politicas de disco ---");
        verificar(PoliticaDisco.desdeJson("FIFO") == PoliticaDisco.FIFO, "FIFO");
        verificar(PoliticaDisco.desdeJson("C-SCAN") == PoliticaDisco.C_SCAN, "C-SCAN");
        verificar(PoliticaDisco.desdeJson("cscan") == PoliticaDisco.C_SCAN, "cscan en minusculas");
        verificar(PoliticaDisco.C_SCAN.toString().equals("C-SCAN"), "se muestra como C-SCAN");
        verificar(lanza(() -> PoliticaDisco.desdeJson("LIFO")), "LIFO es rechazada");
    }

    private static void probarSolicitudProceso() {
        System.out.println("--- SolicitudProceso ---");
        SolicitudProceso viejo = new SolicitudProceso("A", TipoProceso.CPU_BOUND, 5, 10, 1, 100,
                0, 0, 0, 0, 0);
        verificar(!viejo.esDeEs() && viejo.getOperacion() == null, "el constructor viejo no es de E/S");
        SolicitudProceso es = SolicitudProceso.deEs("Proc_34", 2, 16, 5, 500, TipoOperacionES.ACTUALIZAR,
                "/usuarios/notas.txt", 0, "apuntes.txt", null);
        verificar(es.esDeEs() && es.getTipo() == TipoProceso.IO_BOUND, "deEs crea un proceso I/O bound");
        verificar(es.getRuta().equals("/usuarios/notas.txt") && es.getNuevoNombre().equals("apuntes.txt"),
                "guarda la ruta y el nombre nuevo");
        verificar(es.getUsuario().equals("admin"), "sin usuario se usa admin");
        verificar(lanza(() -> SolicitudProceso.deEs("X", 1, 1, 1, 1, TipoOperacionES.SUSPENDER, "/a", 0, null, null)),
                "deEs rechaza SUSPENDER");
        verificar(lanza(() -> SolicitudProceso.deEs("X", 1, 1, 1, 1, TipoOperacionES.LEER, " ", 0, null, null)),
                "deEs rechaza una ruta vacia");
    }

    private static void probarSolicitudDisco() {
        System.out.println("--- SolicitudDisco y OyenteDisco ---");
        SolicitudDisco.reiniciarIds();
        SolicitudDisco a = new SolicitudDisco(null, TipoOperacionES.LEER, "/sistema/boot.bin", 11, 23, 5);
        SolicitudDisco b = new SolicitudDisco(null, TipoOperacionES.ELIMINAR, "/x", 40, 1, 6);
        verificar(a.getId().equals("S1") && b.getId().equals("S2"), "los ids son S1 y S2");
        verificar(a.getEspera() == -1, "sin atender la espera es -1");
        a.setCicloInicio(12);
        verificar(a.getEspera() == 7, "llego en 5, se atiende en 12: espero 7");
        verificar(a.getNombreProceso().equals("sistema"), "sin proceso se llama sistema");
        verificar(lanza(() -> new SolicitudDisco(null, TipoOperacionES.LEER, "/a", 5, 0, 0)),
                "cero bloques es rechazado");
        verificar(lanza(() -> new SolicitudDisco(null, null, "/a", 5, 1, 0)), "sin operacion es rechazada");

        long[] avisado = {-1};
        OyenteDisco oyente = (s, ciclo) -> avisado[0] = ciclo;
        oyente.solicitudAtendida(a, 40);
        verificar(avisado[0] == 40, "el oyente recibe el aviso con el ciclo");
    }

    private static boolean lanza(Runnable accion) {
        try {
            accion.run();
        } catch (IllegalArgumentException e) {
            return true;
        }
        return false;
    }

    private static void verificar(boolean condicion, String descripcion) {
        total++;
        if (condicion) {
            System.out.println("OK    - " + descripcion);
        } else {
            fallos++;
            System.out.println("FALLO - " + descripcion);
        }
    }
}
