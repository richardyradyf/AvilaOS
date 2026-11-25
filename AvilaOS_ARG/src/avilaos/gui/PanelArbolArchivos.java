package avilaos.gui;

import avilaos.fs.MapaBloques;
import avilaos.fs.NodoFs;
import avilaos.fs.Permiso;
import avilaos.fs.SistemaArchivos;
import avilaos.util.Iterador;
import avilaos.util.Lista;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.Timer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * @author Gianm
 * Vista del sistema de archivos: a la izquierda un JTree con el arbol de directorios
 * y archivos, y a la derecha los datos del nodo seleccionado, incluida su cadena de
 * bloques (lo que demuestra la asignacion encadenada).
 *
 * Solo observa: no tiene botones para crear ni borrar, porque cada operacion la hace
 * un proceso de E/S. Se refresca sola cada 500 ms, pero solo reconstruye el arbol si
 * la version del sistema de archivos cambio; al reconstruir conserva las carpetas
 * abiertas y la seleccion.
 */
public class PanelArbolArchivos extends JPanel {

    /** Recibe la ruta que el usuario selecciona en el arbol (por ejemplo, el formulario de E/S). */
    public interface OyenteSeleccion {
        void rutaSeleccionada(String ruta);
    }

    /** Lo que guarda cada nodo del JTree: el nodo del sistema de archivos y su ruta al dibujar. */
    private static final class Elemento {
        final NodoFs nodo;
        final String ruta;
        final String nombre;

        Elemento(NodoFs nodo) {
            this.nodo = nodo;
            this.ruta = nodo.getRuta();
            this.nombre = nodo.esRaiz() ? "/" : nodo.getNombre();
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    private static final String SIN_SELECCION = "Selecciona un archivo o directorio para ver sus datos.";

    private final SistemaArchivos fs;
    private final DefaultTreeModel modelo;
    private final JTree arbol;
    private final JTextArea info = new JTextArea();
    private final Lista<OyenteSeleccion> oyentes = new Lista<>();
    private final Timer temporizador;
    private int versionDibujada = -1;
    private boolean reconstruyendo = false;

    public PanelArbolArchivos(SistemaArchivos fs) {
        super(new BorderLayout());
        if (fs == null) {
            throw new IllegalArgumentException("El sistema de archivos es obligatorio");
        }
        this.fs = fs;

        modelo = new DefaultTreeModel(new DefaultMutableTreeNode("/"));
        modelo.setAsksAllowsChildren(true);   // un directorio vacio se ve como carpeta
        arbol = new JTree(modelo);
        arbol.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        arbol.addTreeSelectionListener(e -> alCambiarSeleccion());

        info.setEditable(false);
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        info.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        info.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        info.setText(SIN_SELECCION);

        JScrollPane izquierda = new JScrollPane(arbol);
        izquierda.setPreferredSize(new Dimension(260, 320));
        JScrollPane derecha = new JScrollPane(info);
        derecha.setPreferredSize(new Dimension(360, 320));
        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izquierda, derecha);
        division.setResizeWeight(0.4);
        add(division, BorderLayout.CENTER);

        refrescar();
        temporizador = new Timer(500, e -> refrescar());
        temporizador.start();
    }

    // ================= publico =================

    /**
     * Reconstruye el arbol solo si el sistema de archivos cambio desde la ultima vez.
     * Guarda antes las carpetas abiertas y la seleccion, y las restaura despues.
     */
    public void refrescar() {
        int version = fs.getVersion();
        if (version == versionDibujada) {
            return;
        }
        boolean primeraVez = versionDibujada < 0;
        Lista<String> abiertas = rutasExpandidas();
        String seleccionada = getRutaSeleccionada();

        reconstruyendo = true;
        try {
            DefaultMutableTreeNode raiz = construir();
            modelo.setRoot(raiz);
            arbol.expandPath(new TreePath(raiz.getPath()));
            if (primeraVez) {
                expandirPrimerNivel(raiz);
            } else {
                restaurarExpandidas(raiz, abiertas);
            }
            seleccionar(raiz, seleccionada);
        } finally {
            reconstruyendo = false;
        }
        versionDibujada = version;
        mostrarSeleccion();
    }

    /** Ruta del nodo seleccionado, o null si no hay seleccion. */
    public String getRutaSeleccionada() {
        Elemento el = elementoSeleccionado();
        return el == null ? null : el.ruta;
    }

    /** Avisa a "oyente" cada vez que el usuario selecciona un nodo distinto. */
    public void agregarOyenteSeleccion(OyenteSeleccion oyente) {
        if (oyente == null) {
            throw new IllegalArgumentException("El oyente no puede ser nulo");
        }
        oyentes.agregarAlFinal(oyente);
    }

    /** Detiene el refresco automatico (al cerrar la ventana o en pruebas). */
    public void detener() {
        temporizador.stop();
    }

    // ================= construccion del arbol =================

    /** Arma el JTree a partir de una copia de todos los nodos tomada con el semaforo del fs. */
    private DefaultMutableTreeNode construir() {
        NodoFs[] nodos = fs.todosLosNodos();   // cada directorio aparece antes que su contenido
        DefaultMutableTreeNode raiz = new DefaultMutableTreeNode(new Elemento(fs.getRaiz()), true);
        Lista<NodoFs> vistos = new Lista<>();
        Lista<DefaultMutableTreeNode> enArbol = new Lista<>();
        vistos.agregarAlFinal(fs.getRaiz());
        enArbol.agregarAlFinal(raiz);
        for (NodoFs n : nodos) {
            int i = vistos.indiceDe(n.getPadre());
            if (i < 0) {
                continue;   // su padre se borro mientras se leia: aparecera bien en el proximo refresco
            }
            DefaultMutableTreeNode nuevo = new DefaultMutableTreeNode(new Elemento(n), n.esDirectorio());
            enArbol.obtener(i).add(nuevo);
            vistos.agregarAlFinal(n);
            enArbol.agregarAlFinal(nuevo);
        }
        return raiz;
    }

    private void expandirPrimerNivel(DefaultMutableTreeNode raiz) {
        for (int i = 0; i < raiz.getChildCount(); i++) {
            DefaultMutableTreeNode hijo = (DefaultMutableTreeNode) raiz.getChildAt(i);
            if (hijo.getAllowsChildren()) {
                arbol.expandPath(new TreePath(hijo.getPath()));
            }
        }
    }

    /** Rutas de las carpetas que estan abiertas ahora mismo. */
    private Lista<String> rutasExpandidas() {
        Lista<String> rutas = new Lista<>();
        for (int fila = 0; fila < arbol.getRowCount(); fila++) {
            if (arbol.isExpanded(fila)) {
                String ruta = rutaDe(arbol.getPathForRow(fila));
                if (ruta != null) {
                    rutas.agregarAlFinal(ruta);
                }
            }
        }
        return rutas;
    }

    private void restaurarExpandidas(DefaultMutableTreeNode nodo, Lista<String> rutas) {
        Object dato = nodo.getUserObject();
        if (dato instanceof Elemento && rutas.contiene(((Elemento) dato).ruta)) {
            arbol.expandPath(new TreePath(nodo.getPath()));
        }
        for (int i = 0; i < nodo.getChildCount(); i++) {
            restaurarExpandidas((DefaultMutableTreeNode) nodo.getChildAt(i), rutas);
        }
    }

    /** Vuelve a seleccionar la ruta que estaba seleccionada (si todavia existe). */
    private void seleccionar(DefaultMutableTreeNode raiz, String ruta) {
        if (ruta == null) {
            return;
        }
        DefaultMutableTreeNode encontrado = buscar(raiz, ruta);
        if (encontrado == null) {
            arbol.clearSelection();
            return;
        }
        TreePath camino = new TreePath(encontrado.getPath());
        arbol.setSelectionPath(camino);
        arbol.scrollPathToVisible(camino);
    }

    private DefaultMutableTreeNode buscar(DefaultMutableTreeNode nodo, String ruta) {
        Object dato = nodo.getUserObject();
        if (dato instanceof Elemento && ((Elemento) dato).ruta.equals(ruta)) {
            return nodo;
        }
        for (int i = 0; i < nodo.getChildCount(); i++) {
            DefaultMutableTreeNode r = buscar((DefaultMutableTreeNode) nodo.getChildAt(i), ruta);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    // ================= seleccion y panel de informacion =================

    private void alCambiarSeleccion() {
        mostrarSeleccion();
        if (reconstruyendo) {
            return;   // la seleccion la restauro el refresco, no el usuario
        }
        String ruta = getRutaSeleccionada();
        if (ruta == null) {
            return;
        }
        Iterador<OyenteSeleccion> it = oyentes.iterador();
        while (it.tieneSiguiente()) {
            it.siguiente().rutaSeleccionada(ruta);
        }
    }

    private void mostrarSeleccion() {
        Elemento el = elementoSeleccionado();
        if (el == null) {
            info.setText(SIN_SELECCION);
            return;
        }
        NodoFs n = el.nodo;
        StringBuilder sb = new StringBuilder();
        sb.append("Nombre:         ").append(el.nombre).append('\n');
        sb.append("Ruta:           ").append(el.ruta).append('\n');
        sb.append("Tipo:           ").append(n.esArchivo() ? "Archivo" : "Directorio").append('\n');
        sb.append("Dueño:          ").append(n.getDueno()).append('\n');
        sb.append("Permiso:        ").append(n.getPermiso() == Permiso.PUBLICO ? "público" : "privado").append('\n');
        if (n.esArchivo()) {
            MapaBloques mapa = fs.getMapa();
            sb.append("Tamaño:         ").append(n.getCantidadBloques()).append(" bloques").append('\n');
            sb.append("Primer bloque:  ").append(n.getPrimerBloque()).append('\n');
            sb.append("Creado por:     ").append(n.getProcesoCreador()).append('\n');
            sb.append('\n').append("Cadena de bloques (asignación encadenada):").append('\n');
            sb.append(cadenaComoTexto(mapa.cadena(n.getPrimerBloque())));
        } else {
            sb.append("Contenido:      ").append(n.cantidadHijos()).append(" elementos").append('\n');
        }
        info.setText(sb.toString());
        info.setCaretPosition(0);
    }

    private static String cadenaComoTexto(int[] cadena) {
        if (cadena.length == 0) {
            return "(el archivo ya no está en el disco)";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cadena.length; i++) {
            sb.append(i > 0 ? " → " : "").append(cadena[i]);
        }
        return sb.append(" → fin").toString();
    }

    private Elemento elementoSeleccionado() {
        TreePath camino = arbol.getSelectionPath();
        if (camino == null) {
            return null;
        }
        Object dato = ((DefaultMutableTreeNode) camino.getLastPathComponent()).getUserObject();
        return dato instanceof Elemento ? (Elemento) dato : null;
    }

    private static String rutaDe(TreePath camino) {
        if (camino == null) {
            return null;
        }
        Object dato = ((DefaultMutableTreeNode) camino.getLastPathComponent()).getUserObject();
        return dato instanceof Elemento ? ((Elemento) dato).ruta : null;
    }
}
