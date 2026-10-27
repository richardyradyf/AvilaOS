package avilaos.sync;

import avilaos.model.TipoProceso;

/**
 * Programa lineal de un productor o consumidor: dado el contador de programa (PC),
 * dice que operacion le toca. Cada elemento ocupa (cadaCiclos + 5) instrucciones:
 * cadaCiclos de producir/consumir mas 5 operaciones del sistema operativo.
 *
 *   Productor:  producir x cadaCiclos, semWait(e), semWait(s), añadir, semSignal(s), semSignal(n)
 *   Consumidor: semWait(n), semWait(s), extraer, semSignal(s), semSignal(e), consumir x cadaCiclos
 */
public final class ProgramaBuffer {

    private static final int OPERACIONES_DE_BUFFER = 5;

    private ProgramaBuffer() {
    }

    /** Instrucciones que ocupa un elemento. */
    public static int periodo(int cadaCiclos) {
        if (cadaCiclos < 1) {
            throw new IllegalArgumentException("cadaCiclos debe ser 1 o mayor");
        }
        return cadaCiclos + OPERACIONES_DE_BUFFER;
    }

    /** Instrucciones totales de un proceso que debe producir o consumir N elementos. */
    public static int instruccionesTotales(TipoProceso tipo, int elementos, int cadaCiclos) {
        exigirProductorOConsumidor(tipo);
        if (elementos < 1) {
            throw new IllegalArgumentException("Los elementos deben ser 1 o mas");
        }
        return elementos * periodo(cadaCiclos);
    }

    /** Operacion que corresponde ejecutar cuando el contador de programa vale pc. */
    public static OperacionBuffer operacionEn(TipoProceso tipo, int pc, int cadaCiclos) {
        exigirProductorOConsumidor(tipo);
        if (pc < 0) {
            throw new IllegalArgumentException("El PC no puede ser negativo");
        }
        int posicion = pc % periodo(cadaCiclos);

        if (tipo == TipoProceso.PRODUCTOR) {
            if (posicion < cadaCiclos) {
                return OperacionBuffer.PRODUCIR;
            }
            switch (posicion - cadaCiclos) {
                case 0:
                    return OperacionBuffer.SEM_WAIT_ESPACIOS;
                case 1:
                    return OperacionBuffer.SEM_WAIT_MUTEX;
                case 2:
                    return OperacionBuffer.ANADIR;
                case 3:
                    return OperacionBuffer.SEM_SIGNAL_MUTEX;
                default:
                    return OperacionBuffer.SEM_SIGNAL_ELEMENTOS;
            }
        }

        switch (posicion) {
            case 0:
                return OperacionBuffer.SEM_WAIT_ELEMENTOS;
            case 1:
                return OperacionBuffer.SEM_WAIT_MUTEX;
            case 2:
                return OperacionBuffer.EXTRAER;
            case 3:
                return OperacionBuffer.SEM_SIGNAL_MUTEX;
            case 4:
                return OperacionBuffer.SEM_SIGNAL_ESPACIOS;
            default:
                return OperacionBuffer.CONSUMIR;
        }
    }

    private static void exigirProductorOConsumidor(TipoProceso tipo) {
        if (tipo != TipoProceso.PRODUCTOR && tipo != TipoProceso.CONSUMIDOR) {
            throw new IllegalArgumentException("Solo los productores y consumidores usan un buffer");
        }
    }
}
