/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.demo;

import avilaos.config.EntradaInvalidaException;
import avilaos.config.ValidadorEntradas;
import avilaos.model.SolicitudProceso;
import avilaos.model.TipoOperacionES;
import avilaos.model.TipoProceso;

/**
 *
 * @author richard
 * 
 * Pruebas de la validacion del formulario de procesos de E/S sobre archivos.
 */
public class PruebaFormularioEs {

    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        probarValidos();
        probarInvalidos();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static SolicitudProceso crear(TipoOperacionES op, String ruta, String bloques,
                                          String nuevo, String usuario) {
        return ValidadorEntradas.validarProcesoEs("Proceso", "10", "32", "2", "500", op, ruta, bloques,
                nuevo, usuario);
    }

    private static void probarValidos() {
        System.out.println("--- Datos validos ---");
        SolicitudProceso c = crear(TipoOperacionES.CREAR, " /docs/a.txt ", "8", "ignorado", "maria");
        verificar(c.esDeEs() && c.getTipo() == TipoProceso.IO_BOUND, "es un proceso de E/S");
        verificar(c.getOperacion() == TipoOperacionES.CREAR, "conserva la operacion");
        verificar("/docs/a.txt".equals(c.getRuta()), "limpia los espacios de la ruta");
        verificar(c.getBloques() == 8, "crear guarda los bloques");
        verificar(c.getNuevoNombre() == null, "crear ignora el nombre nuevo");
        verificar("maria".equals(c.getUsuario()), "guarda el usuario");

        SolicitudProceso l = crear(TipoOperacionES.LEER, "/docs/a.txt", "abc", "", "");
        verificar(l.getBloques() == 0, "leer ignora el campo de bloques");
        verificar("admin".equals(l.getUsuario()), "sin usuario, el dueno es admin");

        SolicitudProceso r = crear(TipoOperacionES.ACTUALIZAR, "/docs/a.txt", "", " b.txt ", "");
        verificar("b.txt".equals(r.getNuevoNombre()), "actualizar guarda el nombre nuevo");
        SolicitudProceso m = crear(TipoOperacionES.ACTUALIZAR, "/docs/a.txt", "", "  ", "");
        verificar(m.getNuevoNombre() == null, "actualizar sin nombre nuevo escribe metadatos");

        SolicitudProceso e = crear(TipoOperacionES.ELIMINAR, "/docs", "", "", "");
        verificar(e.getOperacion() == TipoOperacionES.ELIMINAR, "eliminar acepta un directorio");
    }

    private static void probarInvalidos() {
        System.out.println("--- Datos invalidos ---");
        verificar(rechaza(() -> crear(null, "/a", "1", "", "")), "exige una operacion");
        verificar(rechaza(() -> crear(TipoOperacionES.SUSPENDER, "/a", "1", "", "")),
                "no acepta operaciones del sistema");
        verificar(rechaza(() -> crear(TipoOperacionES.LEER, "", "", "", "")), "la ruta no puede estar vacia");
        verificar(rechaza(() -> crear(TipoOperacionES.LEER, "docs/a.txt", "", "", "")), "la ruta empieza con /");
        verificar(rechaza(() -> crear(TipoOperacionES.ELIMINAR, "/", "", "", "")), "no se puede usar la raiz");
        verificar(rechaza(() -> crear(TipoOperacionES.CREAR, "/a", "", "", "")), "crear exige los bloques");
        verificar(rechaza(() -> crear(TipoOperacionES.CREAR, "/a", "0", "", "")), "los bloques empiezan en 1");
        verificar(rechaza(() -> crear(TipoOperacionES.CREAR, "/a", "501", "", "")), "maximo 500 bloques");
        verificar(rechaza(() -> crear(TipoOperacionES.CREAR, "/a", "dos", "", "")), "los bloques son un numero");
        verificar(rechaza(() -> crear(TipoOperacionES.ACTUALIZAR, "/a", "", "x/y", "")),
                "el nombre nuevo no lleva /");
        verificar(rechaza(() -> crear(TipoOperacionES.LEER, "/a", "", "", "1234567890123456789012345678901")),
                "el usuario tiene maximo 30 caracteres");
        verificar(rechaza(() -> ValidadorEntradas.validarProcesoEs("", "10", "32", "2", "500",
                TipoOperacionES.LEER, "/a", "", "", "")), "el nombre del proceso es obligatorio");
        verificar(rechaza(() -> ValidadorEntradas.validarProcesoEs("P", "x", "32", "2", "500",
                TipoOperacionES.LEER, "/a", "", "", "")), "valida las instrucciones");
    }

    private static boolean rechaza(Runnable accion) {
        try {
            accion.run();
            return false;
        } catch (EntradaInvalidaException e) {
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
