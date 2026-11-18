package avilaos.demo;

import avilaos.fs.ControlPermisos;
import avilaos.fs.MapaBloques;
import avilaos.fs.Permiso;
import avilaos.fs.ResultadoFs;
import avilaos.fs.SesionUsuario;
import avilaos.fs.SistemaArchivos;
import avilaos.model.ModoUsuario;
import avilaos.model.TipoOperacionES;

/**
 * @author Gianm
 * Pruebas de la sesion y del control de permisos con un arbol parecido al del JSON
 * de ejemplo, recorriendo la tabla de reglas con admin, user1 y user2.
 */
public class PruebaPermisos {

    private static final TipoOperacionES LEER = TipoOperacionES.LEER;
    private static final TipoOperacionES CREAR = TipoOperacionES.CREAR;
    private static final TipoOperacionES ACTUALIZAR = TipoOperacionES.ACTUALIZAR;
    private static final TipoOperacionES ELIMINAR = TipoOperacionES.ELIMINAR;

    private static SistemaArchivos fs;
    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        fs = arbolDeEjemplo();
        probarSesion();
        probarAdministrador();
        probarUser1();
        probarUser2();
        probarOperacionesDelSistema();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    /** Arbol parecido al del JSON del enunciado, mas un directorio propio de user1. */
    private static SistemaArchivos arbolDeEjemplo() {
        SistemaArchivos s = new SistemaArchivos(new MapaBloques(200));
        exigir(s.crearDirectorio("/sistema", "admin", Permiso.PUBLICO));
        exigir(s.crearArchivo("/sistema/boot.bin", 10, "admin", Permiso.PRIVADO, "sistema"));
        exigir(s.crearDirectorio("/usuarios", "admin", Permiso.PUBLICO));
        exigir(s.crearArchivo("/usuarios/notas.txt", 28, "user1", Permiso.PRIVADO, "sistema"));
        exigir(s.crearArchivo("/usuarios/video.mp4", 40, "user1", Permiso.PUBLICO, "sistema"));
        exigir(s.crearArchivo("/usuarios/img1.png", 4, "user2", Permiso.PRIVADO, "sistema"));
        exigir(s.crearDirectorio("/usuarios/user1", "user1", Permiso.PRIVADO));
        return s;
    }

    private static void probarSesion() {
        System.out.println("--- SesionUsuario ---");
        SesionUsuario s = new SesionUsuario();
        verificar(s.getModo() == ModoUsuario.ADMINISTRADOR && "admin".equals(s.getUsuario()),
                "una sesion nueva empieza en modo administrador (admin)");
        s.cambiar(ModoUsuario.USUARIO, "user1");
        verificar(s.getModo() == ModoUsuario.USUARIO && "user1".equals(s.getUsuario()),
                "cambia a modo usuario como user1");
        verificar(lanza(() -> s.cambiar(ModoUsuario.USUARIO, "")) && "user1".equals(s.getUsuario()),
                "un nombre vacio se rechaza y la sesion no cambia");
        verificar(lanza(() -> s.cambiar(ModoUsuario.USUARIO, "user 1")), "un nombre con espacios se rechaza");
        verificar(lanza(() -> s.cambiar(ModoUsuario.USUARIO, "u".repeat(21))),
                "un nombre de mas de 20 caracteres se rechaza");
        verificar(lanza(() -> s.cambiar(ModoUsuario.USUARIO, "admin")),
                "en modo usuario no se puede usar el nombre admin");
        verificar(lanza(() -> s.cambiar(null, "user1")), "el modo no puede ser nulo");
        s.cambiar(ModoUsuario.ADMINISTRADOR, "otro");
        verificar(s.getModo() == ModoUsuario.ADMINISTRADOR && "admin".equals(s.getUsuario()),
                "en modo administrador el usuario siempre es admin");
    }

    private static void probarAdministrador() {
        System.out.println("--- Administrador ---");
        SesionUsuario admin = new SesionUsuario();
        verificar(permite(admin, LEER, "/usuarios/notas.txt"), "el administrador lee un archivo privado de user1");
        verificar(permite(admin, CREAR, "/sistema/nuevo.cfg"), "el administrador crea en /sistema");
        verificar(permite(admin, ACTUALIZAR, "/usuarios/notas.txt"), "el administrador puede renombrar");
        verificar(permite(admin, ELIMINAR, "/sistema/boot.bin"), "el administrador elimina archivos del sistema");
        verificar(permite(admin, ELIMINAR, "/usuarios"), "el administrador elimina directorios");
    }

    private static void probarUser1() {
        System.out.println("--- Usuario user1 ---");
        SesionUsuario u1 = como("user1");
        verificar(permite(u1, LEER, "/usuarios/notas.txt"), "user1 lee su archivo privado notas.txt");
        verificar(niega(u1, LEER, "/usuarios/img1.png", "user2"), "user1 no lee img1.png (privado de user2)");
        verificar(permite(u1, CREAR, "/usuarios/user1/tarea.txt"), "user1 crea dentro de su propio directorio");
        verificar(niega(u1, CREAR, "/usuarios/otro.txt", "tus propios directorios"),
                "user1 no crea en /usuarios (pertenece a admin)");
        verificar(niega(u1, CREAR, "/sistema/x.cfg", "tus propios directorios"), "user1 no crea en /sistema");
        verificar(niega(u1, ACTUALIZAR, "/usuarios/notas.txt", "solo el administrador puede renombrar"),
                "user1 no puede renombrar, ni siquiera su archivo");
        verificar(permite(u1, ELIMINAR, "/usuarios/notas.txt"), "user1 elimina su propio archivo");
        verificar(niega(u1, ELIMINAR, "/sistema/boot.bin", "admin"), "user1 no puede borrar /sistema/boot.bin");
        verificar(niega(u1, ELIMINAR, "/usuarios/user1", "directorios"),
                "user1 no elimina directorios, ni siquiera el suyo");
        verificar(permite(u1, LEER, "/usuarios/nada.txt"),
                "una ruta inexistente no es un problema de permisos (la operacion fallara sola)");
    }

    private static void probarUser2() {
        System.out.println("--- Usuario user2 ---");
        SesionUsuario u2 = como("user2");
        String mensaje = ControlPermisos.verificar(u2, LEER, "/usuarios/notas.txt", fs);
        verificar("Permiso denegado: /usuarios/notas.txt es privado y pertenece a user1".equals(mensaje),
                "user2 no lee notas.txt y el mensaje explica por que");
        verificar(permite(u2, LEER, "/usuarios/video.mp4"), "user2 lee video.mp4 (publico de user1)");
        verificar(permite(u2, LEER, "/usuarios/img1.png"), "user2 lee su propio img1.png");
        verificar(niega(u2, LEER, "/sistema/boot.bin", "privado"), "user2 no lee boot.bin (privado de admin)");
        verificar(niega(u2, ELIMINAR, "/usuarios/video.mp4", "user1"),
                "user2 no borra un archivo publico de otro usuario");
    }

    private static void probarOperacionesDelSistema() {
        System.out.println("--- Operaciones del sistema y estructura ---");
        SesionUsuario u2 = como("user2");
        verificar(permite(u2, TipoOperacionES.SUSPENDER, "/") && permite(u2, TipoOperacionES.REACTIVAR, "/"),
                "SUSPENDER y REACTIVAR no se verifican");
        verificar(permite(u2, LEER, "/sistema"), "cualquier usuario puede ver un directorio publico");
    }

    // ---- auxiliares ----

    private static SesionUsuario como(String usuario) {
        SesionUsuario s = new SesionUsuario();
        s.cambiar(ModoUsuario.USUARIO, usuario);
        return s;
    }

    private static boolean permite(SesionUsuario s, TipoOperacionES op, String ruta) {
        return ControlPermisos.verificar(s, op, ruta, fs) == null;
    }

    private static boolean niega(SesionUsuario s, TipoOperacionES op, String ruta, String debeDecir) {
        String mensaje = ControlPermisos.verificar(s, op, ruta, fs);
        return mensaje != null && mensaje.contains(debeDecir);
    }

    private static void exigir(ResultadoFs r) {
        if (!r.isExito()) {
            throw new IllegalStateException("No se pudo preparar el árbol de prueba: " + r.getMensaje());
        }
    }

    private static boolean lanza(Runnable accion) {
        try {
            accion.run();
        } catch (IllegalArgumentException | IllegalStateException e) {
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
