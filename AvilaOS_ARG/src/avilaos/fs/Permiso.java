package avilaos.fs;

/**
 * @author Gianm
 * Visibilidad de un archivo o directorio. PUBLICO lo puede leer cualquier usuario;
 * PRIVADO solo su dueno (y el administrador). Cada valor guarda el texto que se usa
 * en el JSON del sistema de archivos ("public" o "private").
 */
public enum Permiso {

    PUBLICO("public"),
    PRIVADO("private");

    private final String textoJson;

    Permiso(String textoJson) {
        this.textoJson = textoJson;
    }

    /** Texto que se escribe en el JSON. */
    public String aJson() {
        return textoJson;
    }

    /**
     * Convierte el texto del JSON en un permiso. Acepta mayusculas o minusculas y
     * espacios alrededor.
     *
     * @throws IllegalArgumentException si el texto no es "public" ni "private"
     */
    public static Permiso desdeJson(String texto) {
        if (texto != null) {
            String limpio = texto.trim();
            for (Permiso p : values()) {
                if (p.textoJson.equalsIgnoreCase(limpio)) {
                    return p;
                }
            }
        }
        throw new IllegalArgumentException("Permiso desconocido: \"" + texto
                + "\" (se espera public o private)");
    }
}
