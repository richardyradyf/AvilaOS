package avilaos.fs;

import avilaos.model.ModoUsuario;

/**
 * @author Gianm
 * Sesion activa de la simulacion: el modo (administrador o usuario) y el nombre del
 * usuario. En modo administrador el usuario siempre es "admin".
 *
 * La leen varios hilos (el formulario, los paneles, el control de permisos), por eso
 * los campos son volatile: cuando la interfaz cambia el modo, los demas hilos ven el
 * valor nuevo enseguida. Al cambiar se escribe primero el usuario y despues el modo.
 */
public class SesionUsuario {

    /** Largo maximo del nombre de usuario. */
    public static final int MAX_USUARIO = 20;

    private volatile ModoUsuario modo;
    private volatile String usuario;

    /** Una sesion nueva empieza en modo administrador. */
    public SesionUsuario() {
        this.usuario = SistemaArchivos.ADMIN;
        this.modo = ModoUsuario.ADMINISTRADOR;
    }

    public ModoUsuario getModo() {
        return modo;
    }

    /** Nombre del usuario activo ("admin" en modo administrador). */
    public String getUsuario() {
        return usuario;
    }

    public boolean esAdministrador() {
        return modo == ModoUsuario.ADMINISTRADOR;
    }

    /**
     * Cambia el modo. En modo administrador el nombre se ignora y queda "admin".
     * En modo usuario el nombre se valida con validarNombreUsuario.
     *
     * @throws IllegalArgumentException si el modo es nulo o el nombre no es valido;
     *         en ese caso la sesion queda como estaba
     */
    public void cambiar(ModoUsuario nuevoModo, String nuevoUsuario) {
        if (nuevoModo == null) {
            throw new IllegalArgumentException("Elige un modo: administrador o usuario");
        }
        if (nuevoModo == ModoUsuario.ADMINISTRADOR) {
            usuario = SistemaArchivos.ADMIN;
            modo = ModoUsuario.ADMINISTRADOR;
            return;
        }
        String error = validarNombreUsuario(nuevoUsuario);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        usuario = nuevoUsuario.trim();
        modo = ModoUsuario.USUARIO;
    }

    /**
     * Devuelve null si el nombre sirve para el modo usuario, o el mensaje que explica
     * por que no. Reglas: de 1 a 20 caracteres, solo letras, numeros y _, y distinto
     * de "admin" (si no, un usuario tendria los permisos de dueno de los archivos del
     * administrador).
     */
    public static String validarNombreUsuario(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "Escribe un nombre de usuario";
        }
        String limpio = nombre.trim();
        if (limpio.length() > MAX_USUARIO) {
            return "El nombre de usuario tiene " + limpio.length() + " caracteres (máximo " + MAX_USUARIO + ")";
        }
        if (!limpio.matches("[A-Za-z0-9_]+")) {
            return "El nombre de usuario solo puede tener letras, números y _";
        }
        if (limpio.equalsIgnoreCase(SistemaArchivos.ADMIN)) {
            return "El nombre admin está reservado para el modo administrador";
        }
        return null;
    }

    @Override
    public String toString() {
        return esAdministrador() ? "Modo: ADMINISTRADOR" : "Modo: USUARIO (" + usuario + ")";
    }
}
