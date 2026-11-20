/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.demo;

import avilaos.disco.Disco;
import avilaos.disco.PoliticaFifo;
import avilaos.disco.SolicitudDisco;
import avilaos.fs.MapaBloques;
import avilaos.fs.NodoFs;
import avilaos.fs.Permiso;
import avilaos.fs.SistemaArchivos;
import avilaos.hardware.Cpu;
import avilaos.hardware.Ram;
import avilaos.hardware.RelojGlobal;
import avilaos.kernel.Nucleo;
import avilaos.kernel.PuertoEsArchivos;
import avilaos.model.EstadoProceso;
import avilaos.model.GeneradorId;
import avilaos.model.Pcb;
import avilaos.model.TipoOperacionES;
import avilaos.model.TipoProceso;
import avilaos.scheduler.PlanificadorFcfs;
import avilaos.util.Bitacora;

/**
 *
 * @author richard
 * 
 * Pruebas del puerto de E/S real: el nucleo, el disco y el sistema de archivos
 * conectados. Un proceso de E/S pide el disco, el disco mueve el cabezal y
 * transfiere, y al terminar se hace la operacion en el arbol de archivos.
 */
public class PruebaPuertoEsArchivos {

    private static int total = 0;
    private static int fallos = 0;

    private static class Sistema {
        final Bitacora bitacora = new Bitacora(500);
        final Ram ram = new Ram(100);
        final Cpu cpu = new Cpu(1);
        final Nucleo nucleo;
        final RelojGlobal reloj = new RelojGlobal(1000);
        final MapaBloques mapa = new MapaBloques(200);
        final SistemaArchivos fs = new SistemaArchivos(mapa);
        final Disco disco = new Disco(200, new PoliticaFifo(), 50, 5);
        final PuertoEsArchivos puerto = new PuertoEsArchivos(fs, disco);

        Sistema() {
            GeneradorId.reiniciar();
            SolicitudDisco.reiniciarIds();
            fs.crearDirectorio("/docs", "admin", Permiso.PUBLICO);
            fs.crearArchivo("/docs/a.txt", 5, "admin", Permiso.PUBLICO, "sistema");
            nucleo = new Nucleo(1, ram, cpu, new PlanificadorFcfs(), bitacora);
            nucleo.setPuertoEs(puerto);
            disco.agregarOyente(nucleo);
            reloj.registrar(cpu);
            reloj.registrar(nucleo);
            reloj.registrar(disco);
        }

        boolean avanzarHasta(Pcb p, EstadoProceso estado, int limite) {
            for (int i = 0; i < limite && p.getEstado() != estado; i++) {
                reloj.avanzarCiclo();
            }
            return p.getEstado() == estado;
        }

        /** Crea el proceso de E/S, lo agrega al nucleo y corre hasta que termine. */
        Pcb correr(String nombre, TipoOperacionES op, String ruta, int bloques, String nuevoNombre, String usuario) {
            Pcb p = new Pcb(nombre, 1, TipoProceso.IO_BOUND, 1, 20, 2, 1000, 0);
            p.configurarArchivo(op, ruta, bloques, nuevoNombre, usuario);
            nucleo.agregarProceso(p);
            avanzarHasta(p, EstadoProceso.TERMINADO, 300);
            return p;
        }
    }

    public static void main(String[] args) {
        probarSolicitudCalculada();
        probarCrear();
        probarLeer();
        probarRenombrar();
        probarEliminar();
        probarErrores();
        probarSuspendido();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static void probarSolicitudCalculada() {
        System.out.println("--- La solicitud la calcula el sistema de archivos ---");
        Sistema s = new Sistema();
        Pcb p = new Pcb("Leer", 1, TipoProceso.IO_BOUND, 1, 20, 2, 1000, 0);
        p.configurarArchivo(TipoOperacionES.LEER, "/docs/a.txt", 0, null, null);
        SolicitudDisco sol = s.puerto.pedirDisco(p, 7);
        NodoFs a = s.fs.buscar("/docs/a.txt");
        verificar(sol.getBloqueDestino() == a.getPrimerBloque(), "el destino es el primer bloque del archivo");
        verificar(sol.getBloquesATransferir() == 5, "leer transfiere los 5 bloques del archivo");
        verificar(sol.getProceso() == p && sol.getCicloLlegada() == 7, "lleva el proceso y el ciclo de llegada");
        verificar(s.disco.getCantidadEnCola() == 1, "la solicitud queda en la cola del disco");
        verificar(lanza(() -> new PuertoEsArchivos(null, s.disco)), "el puerto necesita el sistema de archivos");
    }

    private static void probarCrear() {
        System.out.println("--- Crear ---");
        Sistema s = new Sistema();
        int libresAntes = s.mapa.libres();
        Pcb p = s.correr("Crear", TipoOperacionES.CREAR, "/docs/notas.txt", 3, null, "maria");
        NodoFs nuevo = s.fs.buscar("/docs/notas.txt");
        verificar(p.getEstado() == EstadoProceso.TERMINADO, "el proceso termina");
        verificar(nuevo != null && nuevo.getCantidadBloques() == 3, "el archivo existe con 3 bloques");
        verificar(nuevo != null && nuevo.getDueno().equals("maria") && nuevo.getPermiso() == Permiso.PRIVADO,
                "su dueno es el usuario del proceso y es privado");
        verificar(nuevo != null && nuevo.getProcesoCreador().equals(p.getEtiqueta()), "guarda el proceso que lo creo");
        verificar(s.mapa.libres() == libresAntes - 3, "ocupa 3 bloques del mapa");
        verificar(s.bitacora.contiene("OK - Archivo /docs/notas.txt creado"), "la bitacora registra el resultado");
    }

    private static void probarLeer() {
        System.out.println("--- Leer ---");
        Sistema s = new Sistema();
        Pcb p = s.correr("Leer", TipoOperacionES.LEER, "/docs/a.txt", 0, null, null);
        verificar(p.getEstado() == EstadoProceso.TERMINADO, "el proceso termina");
        verificar(s.disco.getTotalDesplazamiento() == 50, "el cabezal va del bloque 50 al bloque 0");
        verificar(s.disco.getTotalSolicitudesAtendidas() == 1, "el disco atiende 1 solicitud");
        verificar(s.bitacora.contiene("OK - Archivo /docs/a.txt le"), "la bitacora registra la lectura");
    }

    private static void probarRenombrar() {
        System.out.println("--- Renombrar ---");
        Sistema s = new Sistema();
        s.correr("Renombrar", TipoOperacionES.ACTUALIZAR, "/docs/a.txt", 0, "b.txt", null);
        verificar(s.fs.buscar("/docs/a.txt") == null && s.fs.buscar("/docs/b.txt") != null,
                "a.txt pasa a llamarse b.txt");
        verificar(s.bitacora.contiene("renombrado a /docs/b.txt"), "la bitacora registra el cambio");
    }

    private static void probarEliminar() {
        System.out.println("--- Eliminar ---");
        Sistema s = new Sistema();
        int libresAntes = s.mapa.libres();
        s.correr("Borrar", TipoOperacionES.ELIMINAR, "/docs/a.txt", 0, null, null);
        verificar(s.fs.buscar("/docs/a.txt") == null, "el archivo ya no existe");
        verificar(s.mapa.libres() == libresAntes + 5, "libera sus 5 bloques");
    }

    private static void probarErrores() {
        System.out.println("--- Errores del sistema de archivos ---");
        Sistema s = new Sistema();
        Pcb p = s.correr("Fantasma", TipoOperacionES.LEER, "/no/existe.txt", 0, null, null);
        verificar(p.getEstado() == EstadoProceso.TERMINADO, "si la ruta no existe, el proceso termina igual");
        verificar(s.bitacora.contiene("ERROR - No existe"), "la bitacora registra el error");
        Pcb q = s.correr("Repetido", TipoOperacionES.CREAR, "/docs/a.txt", 2, null, null);
        verificar(q.getEstado() == EstadoProceso.TERMINADO && s.fs.buscar("/docs/a.txt").getCantidadBloques() == 5,
                "crear un archivo que ya existe falla y no cambia el original");
        Pcb r = s.correr("SinNombre", TipoOperacionES.ACTUALIZAR, "/docs/a.txt", 0, null, null);
        verificar(r.getEstado() == EstadoProceso.TERMINADO && s.fs.buscar("/docs/a.txt") != null,
                "renombrar sin nombre nuevo falla sin romper nada");
    }

    private static void probarSuspendido() {
        System.out.println("--- Proceso suspendido mientras espera el disco ---");
        Sistema s = new Sistema();
        Pcb p = new Pcb("Crear", 1, TipoProceso.IO_BOUND, 1, 20, 2, 1000, 0);
        p.configurarArchivo(TipoOperacionES.CREAR, "/docs/tarde.txt", 2, null, null);
        s.nucleo.agregarProceso(p);
        s.avanzarHasta(p, EstadoProceso.BLOQUEADO, 10);
        s.nucleo.suspender(p);
        verificar(s.avanzarHasta(p, EstadoProceso.LISTO_SUSPENDIDO, 100),
                "el disco termina y el proceso pasa a listos/suspendidos");
        verificar(s.fs.buscar("/docs/tarde.txt") != null, "la operacion se hace aunque este suspendido");
    }

    private static boolean lanza(Runnable accion) {
        try {
            accion.run();
            return false;
        } catch (IllegalArgumentException | IllegalStateException e) {
            return true;
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
