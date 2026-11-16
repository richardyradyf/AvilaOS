package avilaos.fs;

/**
 * @author Gianm
 * Tipo de un nodo del arbol del sistema de archivos. Cada valor guarda el texto que
 * se usa en el JSON ("FILE" o "DIRECTORY").
 */
public enum TipoNodo {

    ARCHIVO("FILE"),
    DIRECTORIO("DIRECTORY");

    private final String textoJson;

    TipoNodo(String textoJson) {
        this.textoJson = textoJson;
    }

    /** Texto que se escribe en el JSON. */
    public String aJson() {
        return textoJson;
    }

    /**
     * Convierte el texto del JSON en un tipo de nodo. Acepta mayusculas o minusculas
     * y espacios alrededor.
     *
     * @throws IllegalArgumentException si el texto no es "FILE" ni "DIRECTORY"
     */
    public static TipoNodo desdeJson(String texto) {
        if (texto != null) {
            String limpio = texto.trim();
            for (TipoNodo t : values()) {
                if (t.textoJson.equalsIgnoreCase(limpio)) {
                    return t;
                }
            }
        }
        throw new IllegalArgumentException("Tipo de nodo desconocido: \"" + texto
                + "\" (se espera FILE o DIRECTORY)");
    }
}

