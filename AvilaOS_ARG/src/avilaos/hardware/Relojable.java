package avilaos.hardware;

/**
 * Interfaz comun de los componentes que reaccionan al reloj global (CPU, DMA, etc.).
 * El reloj llama a alCiclo una vez por cada ciclo; cada componente avanza
 * exactamente un ciclo de ejecucion.
 */
public interface Relojable {

    /**
     * Se invoca una vez por cada ciclo del reloj global.
     *
     * @param numeroCiclo numero del ciclo actual, contado desde el inicio de la simulacion
     */
    void alCiclo(long numeroCiclo);
}