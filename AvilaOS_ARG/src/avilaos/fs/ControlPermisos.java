package avilaos.fs;

import avilaos.model.ModoUsuario;
import avilaos.model.TipoOperacionES;

/**
 * @author Gianm
 * Decide si la sesion activa puede pedir una operacion sobre una ruta. Se llama
 * ANTES de crear el proceso de E/S: si devuelve un mensaje, la interfaz lo muestra y
 * no crea nada, y la simulacion sigue.
 *
 * Reglas del equipo:
 * - Administrador: puede hacer todo.
 * - Usuario:
 *   - LEER: si el archivo es suyo o es publico.
 *   - CREAR: solo si el directorio padre es suyo.
 *   - ACTUALIZAR (renombrar): nunca.
 *   - ELIMINAR: solo archivos propios, nunca directorios.
 * - SUSPENDER y REACTIVAR son operaciones del sistema: no se verifican.
 *
 * Si la ruta (o su padre) no existe, no es un problema de permisos: se permite y la
 * operacion del sistema de archivos fallara sola con su mensaje.
 */
public final class ControlPermisos {

    private ControlPermisos() {
    }

    /**
     * @return null si la operacion se permite, o un mensaje que explica por que no
     */
    public static String verificar(SesionUsuario sesion, TipoOperacionES op, String ruta, SistemaArchivos fs) {
        if (sesion == null || fs == null) {
            throw new IllegalArgumentException("La sesión y el sistema de archivos son obligatorios");
        }
        if (op == null) {
            return "Falta la operación";
        }
        if (op == TipoOperacionES.SUSPENDER || op == TipoOperacionES.REACTIVAR) {
            return null;
        }
        ModoUsuario modo = sesion.getModo();
        String usuario = sesion.getUsuario();
        if (modo == ModoUsuario.ADMINISTRADOR) {
            return null;
        }
        String limpia = ruta == null ? "" : ruta.trim();
        switch (op) {
            case LEER:
                return verificarLectura(usuario, limpia, fs);
            case CREAR:
                return verificarCreacion(usuario, limpia, fs);
            case ACTUALIZAR:
                return "Permiso denegado: solo el administrador puede renombrar";
            case ELIMINAR:
                return verificarEliminacion(usuario, limpia, fs);
            default:
                return null;
        }
    }

    // ================= reglas del modo usuario =================

    private static String verificarLectura(String usuario, String ruta, SistemaArchivos fs) {
        NodoFs nodo = fs.buscar(ruta);
        if (nodo == null) {
            return null;
        }
        if (nodo.getPermiso() == Permiso.PUBLICO || nodo.getDueno().equals(usuario)) {
            return null;
        }
        return "Permiso denegado: " + nodo.getRuta() + " es privado y pertenece a " + nodo.getDueno();
    }

    private static String verificarCreacion(String usuario, String ruta, SistemaArchivos fs) {
        int corte = ruta.lastIndexOf('/');
        if (corte < 0) {
            return null;
        }
        String rutaPadre = corte == 0 ? "/" : ruta.substring(0, corte);
        NodoFs padre = fs.buscar(rutaPadre);
        if (padre == null || padre.esArchivo()) {
            return null;
        }
        if (padre.getDueno().equals(usuario)) {
            return null;
        }
        return "Permiso denegado: solo puedes crear archivos en tus propios directorios ("
                + padre.getRuta() + " pertenece a " + padre.getDueno() + ")";
    }

    private static String verificarEliminacion(String usuario, String ruta, SistemaArchivos fs) {
        NodoFs nodo = fs.buscar(ruta);
        if (nodo == null) {
            return null;
        }
        if (nodo.esDirectorio()) {
            return "Permiso denegado: solo el administrador puede eliminar directorios";
        }
        if (!nodo.getDueno().equals(usuario)) {
            return "Permiso denegado: " + nodo.getRuta() + " pertenece a " + nodo.getDueno()
                    + "; solo puedes eliminar tus propios archivos";
        }
        return null;
    }
}
