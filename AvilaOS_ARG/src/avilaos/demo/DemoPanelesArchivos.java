package avilaos.demo;

import avilaos.fs.CargadorSistemaArchivos;
import avilaos.fs.ControlPermisos;
import avilaos.fs.Permiso;
import avilaos.fs.ResultadoFs;
import avilaos.fs.SesionUsuario;
import avilaos.fs.SistemaArchivos;
import avilaos.gui.PanelArbolArchivos;
import avilaos.gui.PanelModoUsuario;
import avilaos.gui.PanelTablaAsignacion;
import avilaos.model.TipoOperacionES;
import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * @author Gianm
 * Demo visual del sistema de archivos: carga el JSON del enunciado y muestra el
 * selector de modo, el arbol con su panel de informacion y la tabla de asignacion.
 * Al seleccionar un nodo imprime si la sesion actual puede leerlo. Cada 3 segundos
 * hace una operacion de ejemplo (como si la hiciera un proceso de E/S) para ver que
 * el arbol y la tabla se actualizan solos.
 */
public class DemoPanelesArchivos {

    /** JSON del sistema de archivos del enunciado. */
    private static final String ENUNCIADO = """
            {
              "diskConfig": { "totalBlocks": 200, "initialHead": 50 },
              "fileSystem": [
                { "path": "/mbr.bin", "type": "FILE", "startBlock": 0, "blocks": 11, "owner": "admin", "permissions": "public" },
                { "path": "/sistema", "type": "DIRECTORY", "owner": "admin", "permissions": "public" },
                { "path": "/usuarios", "type": "DIRECTORY", "owner": "admin", "permissions": "public" },
                { "path": "/sistema/boot.bin", "type": "FILE", "startBlock": 11, "blocks": 23, "owner": "admin", "permissions": "public" },
                { "path": "/usuarios/notas.txt", "type": "FILE", "startBlock": 34, "blocks": 28, "owner": "user1", "permissions": "private" },
                { "path": "/sistema/script.py", "type": "FILE", "startBlock": 62, "blocks": 2, "owner": "admin", "permissions": "public" },
                { "path": "/sistema/core.dll", "type": "FILE", "startBlock": 64, "blocks": 31, "owner": "admin", "permissions": "public" },
                { "path": "/sistema/config.sys", "type": "FILE", "startBlock": 95, "blocks": 24, "owner": "admin", "permissions": "public" },
                { "path": "/usuarios/img1.png", "type": "FILE", "startBlock": 119, "blocks": 4, "owner": "user1", "permissions": "private" },
                { "path": "/usuarios/data.csv", "type": "FILE", "startBlock": 123, "blocks": 57, "owner": "user2", "permissions": "public" },
                { "path": "/usuarios/video.mp4", "type": "FILE", "startBlock": 180, "blocks": 20, "owner": "user1", "permissions": "public" }
              ]
            }
            """;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DemoPanelesArchivos::mostrar);
    }

    private static void mostrar() {
        SistemaArchivos fs = CargadorSistemaArchivos.desdeJson(ENUNCIADO).getSistemaArchivos();
        SesionUsuario sesion = new SesionUsuario();

        PanelModoUsuario modo = new PanelModoUsuario(sesion);
        modo.agregarOyenteModo(s -> System.out.println("Sesión cambiada: " + s));

        PanelArbolArchivos arbol = new PanelArbolArchivos(fs);
        arbol.agregarOyenteSeleccion(ruta -> {
            String rechazo = ControlPermisos.verificar(sesion, TipoOperacionES.LEER, ruta, fs);
            System.out.println("Seleccionado: " + ruta + "  |  LEER como " + sesion.getUsuario() + ": "
                    + (rechazo == null ? "permitido" : rechazo));
        });

        PanelTablaAsignacion tabla = new PanelTablaAsignacion(fs);

        JFrame ventana = new JFrame("ÁvilaOS - Sistema de archivos (demo)");
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.add(modo, BorderLayout.NORTH);
        ventana.add(arbol, BorderLayout.CENTER);
        ventana.add(tabla, BorderLayout.SOUTH);
        ventana.setSize(860, 760);
        ventana.setLocationRelativeTo(null);
        ventana.setVisible(true);

        // Operaciones de ejemplo cada 3 segundos
        int[] paso = {0};
        Timer operaciones = new Timer(3000, null);
        operaciones.addActionListener(e -> {
            ResultadoFs r = siguienteOperacion(fs, paso[0]++);
            if (r == null) {
                operaciones.stop();
                System.out.println("Fin de las operaciones de ejemplo.");
            } else {
                System.out.println(r);
            }
        });
        operaciones.start();
    }

    private static ResultadoFs siguienteOperacion(SistemaArchivos fs, int paso) {
        switch (paso) {
            case 0:
                return fs.eliminar("/usuarios/img1.png");     // libera 119 a 122
            case 1:
                return fs.eliminar("/sistema/script.py");     // libera 62 y 63
            case 2:
                return fs.crearArchivo("/usuarios/nuevo.txt", 6, "user2", Permiso.PRIVADO, "P1");   // queda repartido
            case 3:
                return fs.crearDirectorio("/usuarios/user2", "user2", Permiso.PRIVADO);
            case 4:
                return fs.renombrar("/usuarios/notas.txt", "apuntes.txt");
            default:
                return null;
        }
    }
}