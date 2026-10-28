package avilaos.config;

import avilaos.model.SolicitudProceso;
import avilaos.model.TipoProceso;

/**
 * Validacion de tipo de dato y rango de todo lo que ingresa el usuario. No usa Swing,
 * asi que tambien sirve para validar los datos que se carguen desde JSON.
 */
public final class ValidadorEntradas {

    public static final int MAX_NOMBRE = 30;
    public static final int MAX_INSTRUCCIONES = 100000;
    public static final int MAX_MEMORIA = 65536;     // MB
    public static final int MAX_PRIORIDAD = 10;
    public static final int MAX_DEADLINE = 1000000;  // ciclos
    public static final int MAX_CICLOS = 1000;
    public static final int MAX_ELEMENTOS = 100000;

    private ValidadorEntradas() {
    }

    /** Convierte el texto a entero y comprueba que este entre min y max (inclusive). */
    public static int entero(String texto, String campo, int min, int max) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new EntradaInvalidaException(campo + ": no puede estar vacío");
        }
        long valor;
        try {
            valor = Long.parseLong(texto.trim());
        } catch (NumberFormatException e) {
            throw new EntradaInvalidaException(campo + ": debe ser un número entero entre " + min + " y " + max);
        }
        if (valor < min || valor > max) {
            throw new EntradaInvalidaException(campo + ": debe estar entre " + min + " y " + max);
        }
        return (int) valor;
    }

    /** Nombre: no vacio, hasta 30 caracteres; letras, numeros, espacio, guion, guion bajo y punto. */
    public static String nombre(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new EntradaInvalidaException("Nombre: no puede estar vacío");
        }
        String limpio = texto.trim();
        if (limpio.length() > MAX_NOMBRE) {
            throw new EntradaInvalidaException("Nombre: máximo " + MAX_NOMBRE + " caracteres");
        }
        for (int i = 0; i < limpio.length(); i++) {
            char c = limpio.charAt(i);
            boolean permitido = Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-' || c == '.';
            if (!permitido) {
                throw new EntradaInvalidaException("Nombre: el carácter '" + c + "' no está permitido");
            }
        }
        return limpio;
    }

    /**
     * Valida los datos de un proceso. Solo se revisan los campos que corresponden al
     * tipo elegido; los demas se ignoran.
     *
     * @param idComputador 0 = automatico
     * @param idBuffer     0 = ninguno seleccionado
     */
    public static SolicitudProceso validarProceso(String nombre, TipoProceso tipo, String instrucciones,
                                                  String memoria, String prioridad, String deadline,
                                                  int idComputador, String ciclosEs, String cadaCiclos,
                                                  String elementos, int idBuffer) {
        String nombreOk = nombre(nombre);
        if (tipo == null) {
            throw new EntradaInvalidaException("Tipo: debe seleccionar un tipo de proceso");
        }
        int memoriaOk = entero(memoria, "Memoria (MB)", 1, MAX_MEMORIA);
        int prioridadOk = entero(prioridad, "Prioridad", 1, MAX_PRIORIDAD);
        int deadlineOk = entero(deadline, "Deadline (ciclos)", 1, MAX_DEADLINE);
        if (idComputador < 0) {
            throw new EntradaInvalidaException("Computador: selección inválida");
        }

        int instruccionesOk = 0;
        int ciclosEsOk = 0;
        int cadaOk = 0;
        int elementosOk = 0;
        int bufferOk = 0;

        switch (tipo) {
            case CPU_BOUND:
                instruccionesOk = entero(instrucciones, "Instrucciones", 1, MAX_INSTRUCCIONES);
                break;
            case IO_BOUND:
                instruccionesOk = entero(instrucciones, "Instrucciones", 1, MAX_INSTRUCCIONES);
                ciclosEsOk = entero(ciclosEs, "Ciclos de E/S", 1, MAX_CICLOS);
                cadaOk = entero(cadaCiclos, "Instrucciones entre cada E/S", 1, MAX_CICLOS);
                break;
            case PRODUCTOR:
            case CONSUMIDOR:
                cadaOk = entero(cadaCiclos, "Ciclos por elemento", 1, MAX_CICLOS);
                elementosOk = entero(elementos, "Elementos", 1, MAX_ELEMENTOS);
                if (idBuffer < 1) {
                    throw new EntradaInvalidaException("Buffer: debe existir un buffer y seleccionarlo");
                }
                bufferOk = idBuffer;
                break;
            default:
                throw new EntradaInvalidaException("Tipo: valor no reconocido");
        }

        return new SolicitudProceso(nombreOk, tipo, instruccionesOk, memoriaOk, prioridadOk, deadlineOk,
                idComputador, ciclosEsOk, cadaOk, elementosOk, bufferOk);
    }
}
