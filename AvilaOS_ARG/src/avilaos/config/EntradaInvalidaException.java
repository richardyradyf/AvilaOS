package avilaos.config;

/**
 * Se lanza cuando un dato ingresado por el usuario (o leido de un archivo) no es
 * valido. El mensaje esta pensado para mostrarse tal cual en la interfaz.
 */
public class EntradaInvalidaException extends RuntimeException {

    public EntradaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
