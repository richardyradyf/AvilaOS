package avilaos.fs;

import avilaos.util.Iterador;
import avilaos.util.Lista;

/**
 * @author Gianm
 * Un nodo del arbol del sistema de archivos: un directorio o un archivo.
 *
 * - Los directorios tienen hijos (en una Lista propia) y no ocupan bloques.
 * - Los archivos son hojas: no tienen hijos, pero guardan su primer bloque y su
 *   cantidad de bloques en el MapaBloques (asignacion encadenada).
 *
 * No es seguro entre hilos: SistemaArchivos lo crea y lo modifica dentro de su
 * mutex. Por eso los metodos que cambian el nodo no son publicos.
 */
public class NodoFs {

    /** Proceso creador de lo que viene del JSON inicial (o de la raiz). */
    public static final String CREADOR_SISTEMA = "sistema";

    private String nombre;
    private final TipoNodo tipo;
    private final NodoFs padre;
    private final Lista<NodoFs> hijos;
    private final String dueno;
    private final Permiso permiso;
    private final int primerBloque;
    private final int cantidadBloques;
    private final String procesoCreador;

    private NodoFs(String nombre, TipoNodo tipo, NodoFs padre, String dueno, Permiso permiso,
                   int primerBloque, int cantidadBloques, String procesoCreador) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.padre = padre;
        this.hijos = tipo == TipoNodo.DIRECTORIO ? new Lista<>() : null;
        this.dueno = dueno;
        this.permiso = permiso;
        this.primerBloque = primerBloque;
        this.cantidadBloques = cantidadBloques;
        this.procesoCreador = procesoCreador;
    }

    /** Crea un directorio vacio (sin bloques: primerBloque -1 y 0 bloques). */
    static NodoFs nuevoDirectorio(String nombre, NodoFs padre, String dueno, Permiso permiso,
                                  String procesoCreador) {
        return new NodoFs(nombre, TipoNodo.DIRECTORIO, padre, dueno, permiso, -1, 0, procesoCreador);
    }

    /** Crea un archivo que ya tiene sus bloques asignados en el mapa. */
    static NodoFs nuevoArchivo(String nombre, NodoFs padre, String dueno, Permiso permiso,
                               int primerBloque, int cantidadBloques, String procesoCreador) {
        return new NodoFs(nombre, TipoNodo.ARCHIVO, padre, dueno, permiso, primerBloque,
                cantidadBloques, procesoCreador);
    }

    // ================= consulta =================

    /**
     * Ruta completa del nodo, armada subiendo por los padres. Por ejemplo
     * "/sistema/boot.bin". La raiz devuelve "/".
     */
    public String getRuta() {
        if (padre == null) {
            return "/";
        }
        String rutaPadre = padre.getRuta();
        return rutaPadre.equals("/") ? "/" + nombre : rutaPadre + "/" + nombre;
    }

    /** Hijo con ese nombre, o null si no existe (o si este nodo es un archivo). */
    public NodoFs buscarHijo(String nombreHijo) {
        if (hijos == null || nombreHijo == null) {
            return null;
        }
        Iterador<NodoFs> it = hijos.iterador();
        while (it.tieneSiguiente()) {
            NodoFs hijo = it.siguiente();
            if (hijo.nombre.equals(nombreHijo)) {
                return hijo;
            }
        }
        return null;
    }

    /** Copia de los hijos, en el orden en que se agregaron (vacia si es un archivo). */
    public NodoFs[] getHijos() {
        if (hijos == null) {
            return new NodoFs[0];
        }
        NodoFs[] copia = new NodoFs[hijos.tamano()];
        Iterador<NodoFs> it = hijos.iterador();
        int i = 0;
        while (it.tieneSiguiente()) {
            copia[i++] = it.siguiente();
        }
        return copia;
    }

    public int cantidadHijos() {
        return hijos == null ? 0 : hijos.tamano();
    }

    public boolean esArchivo() {
        return tipo == TipoNodo.ARCHIVO;
    }

    public boolean esDirectorio() {
        return tipo == TipoNodo.DIRECTORIO;
    }

    public boolean esRaiz() {
        return padre == null;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoNodo getTipo() {
        return tipo;
    }

    /** Directorio que contiene a este nodo, o null si es la raiz. */
    public NodoFs getPadre() {
        return padre;
    }

    public String getDueno() {
        return dueno;
    }

    public Permiso getPermiso() {
        return permiso;
    }

    /** Primer bloque del archivo en el mapa, o -1 si es un directorio. */
    public int getPrimerBloque() {
        return primerBloque;
    }

    /** Bloques que ocupa el archivo (0 en un directorio). */
    public int getCantidadBloques() {
        return cantidadBloques;
    }

    /** Proceso que creo el archivo, o "sistema" si vino del JSON. */
    public String getProcesoCreador() {
        return procesoCreador;
    }

    @Override
    public String toString() {
        return getRuta();
    }

    // ================= cambios (solo desde SistemaArchivos) =================

    void agregarHijo(NodoFs hijo) {
        if (hijos == null) {
            throw new IllegalStateException(getRuta() + " es un archivo: no puede tener hijos");
        }
        hijos.agregarAlFinal(hijo);
    }

    boolean quitarHijo(NodoFs hijo) {
        return hijos != null && hijos.eliminar(hijo);
    }

    void cambiarNombre(String nuevoNombre) {
        nombre = nuevoNombre;
    }
}
