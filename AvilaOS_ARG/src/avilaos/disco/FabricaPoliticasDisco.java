package avilaos.disco;

import avilaos.model.PoliticaDisco;

/**
 * @author andre
 *
 * Crea la politica de disco que corresponde a cada valor de PoliticaDisco. Es el unico
 * lugar que conoce las clases concretas: para agregar una politica nueva (por ejemplo
 * LOOK) se escribe su clase y se agrega un caso aqui, sin tocar el disco.
 */
public final class FabricaPoliticasDisco {

    private FabricaPoliticasDisco() {
    }

    /**
     * @param tipo politica pedida (por ejemplo, la que se leyo del JSON o del selector)
     * @return una instancia nueva de esa politica
     * @throws IllegalArgumentException si tipo es null
     */
    public static PlanificacionDisco crear(PoliticaDisco tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("La política de disco no puede ser nula");
        }
        switch (tipo) {
            case FIFO:
                return new PoliticaFifo();
            case SSTF:
                return new PoliticaSstf();
            case SCAN:
                return new PoliticaScan();
            case C_SCAN:
                return new PoliticaCScan();
            default:
                throw new IllegalArgumentException("Política de disco no soportada: " + tipo);
        }
    }
}
