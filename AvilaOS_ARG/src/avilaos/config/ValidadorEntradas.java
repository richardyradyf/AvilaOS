package avilaos.config;

import avilaos.model.SolicitudProceso;
import avilaos.model.TipoOperacionES;
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
    public static final int MAX_BLOQUES_ARCHIVO = 500;

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
            default:
                throw new EntradaInvalidaException("Tipo: valor no reconocido");
        }

        return new SolicitudProceso(nombreOk, tipo, instruccionesOk, memoriaOk, prioridadOk, deadlineOk,
                idComputador, ciclosEsOk, cadaOk, elementosOk, bufferOk);
    }
    
    /**
     * Valida un proceso de E/S sobre un archivo. Los bloques solo se piden al crear y
     * el nombre nuevo solo se usa al actualizar; sin usuario, el dueno es admin.
     */
    public static SolicitudProceso validarProcesoEs(String nombre, String instrucciones, String memoria,
                                                    String prioridad, String deadline,
                                                    TipoOperacionES operacion, String ruta, String bloques,
                                                    String nuevoNombre, String usuario) {
        String nombreOk = nombre(nombre);
        int instruccionesOk = entero(instrucciones, "Instrucciones", 1, MAX_INSTRUCCIONES);
        int memoriaOk = entero(memoria, "Memoria (MB)", 1, MAX_MEMORIA);
        int prioridadOk = entero(prioridad, "Prioridad", 1, MAX_PRIORIDAD);
        int deadlineOk = entero(deadline, "Deadline (ciclos)", 1, MAX_DEADLINE);
        if (operacion == null || !operacion.esDeArchivo()) {
            throw new EntradaInvalidaException("Operación: debe seleccionar Crear, Leer, Actualizar o Eliminar");
        }
        String rutaOk = ruta == null ? "" : ruta.trim();
        if (!rutaOk.startsWith("/") || rutaOk.length() == 1) {
            throw new EntradaInvalidaException("Ruta: debe comenzar con / e indicar un archivo o directorio");
        }
        int bloquesOk = 0;
        if (operacion == TipoOperacionES.CREAR) {
            bloquesOk = entero(bloques, "Bloques", 1, MAX_BLOQUES_ARCHIVO);
        }
        String nuevoOk = null;
        if (operacion == TipoOperacionES.ACTUALIZAR) {
            nuevoOk = textoOpcional(nuevoNombre, "Nombre nuevo");
        }
        String usuarioOk = textoOpcional(usuario, "Usuario");
        return SolicitudProceso.deEs(nombreOk, instruccionesOk, memoriaOk, prioridadOk, deadlineOk,
                operacion, rutaOk, bloquesOk, nuevoOk, usuarioOk);
    }

    /** Devuelve el texto limpio, o null si esta vacio. No admite / y tiene el largo de un nombre. */
    private static String textoOpcional(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.trim();
        if (limpio.contains("/")) {
            throw new EntradaInvalidaException(campo + ": no puede contener /");
        }
        if (limpio.length() > MAX_NOMBRE) {
            throw new EntradaInvalidaException(campo + ": máximo " + MAX_NOMBRE + " caracteres");
        }
        return limpio;
    }
}
