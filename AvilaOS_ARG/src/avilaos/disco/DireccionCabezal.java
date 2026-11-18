package avilaos.disco;

/**
 * @author andre
 *
 * Direccion en la que se mueve el cabezal del disco. SCAN y C-SCAN la usan para
 * saber hacia donde barren; el disco la actualiza cada vez que el cabezal se mueve.
 */
public enum DireccionCabezal {

    ASCENDENTE("↑ hacia bloques mayores"),
    DESCENDENTE("↓ hacia bloques menores");

    private final String descripcion;

    DireccionCabezal(String descripcion) {
        this.descripcion = descripcion;
    }

    /** Texto para la interfaz, por ejemplo "(flecha arriba) hacia bloques mayores". */
    public String getDescripcion() {
        return descripcion;
    }

    /** Solo la flecha (arriba o abajo), para mostrarla junto a la posicion del cabezal. */
    public String getSimbolo() {
        return descripcion.substring(0, 1);
    }

    /** La direccion contraria: lo que hace SCAN al llegar al extremo del disco. */
    public DireccionCabezal invertir() {
        return this == ASCENDENTE ? DESCENDENTE : ASCENDENTE;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
