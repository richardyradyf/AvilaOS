package avilaos.disco;

/**
 * @author andre
 *
 * Estados posibles del disco simulado durante la ejecucion:
 * - OCIOSO: no hay solicitudes en cola o se acaba de encender el sistema.
 * - MOVIENDO: el cabezal se desplaza hacia el bloque destino de la solicitud.
 * - TRANSFIRIENDO: el cabezal llego al bloque y lee/escribe los bloques pedidos (1 ciclo por bloque).
 */
public enum EstadoDisco {

    OCIOSO("Ocioso"),
    MOVIENDO("Moviendo cabezal"),
    TRANSFIRIENDO("Transfiriendo datos");

    private final String descripcion;

    EstadoDisco(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
