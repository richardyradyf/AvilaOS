package avilaos.fs;

/**
 * @author Gianm
 * Resultado de una operacion del sistema de archivos: si salio bien y un mensaje
 * para la bitacora. Las operaciones nunca lanzan excepciones hacia el reloj: si algo
 * falla, devuelven un error con su explicacion y la simulacion sigue.
 */
public final class ResultadoFs {

    private final boolean exito;
    private final String mensaje;

    private ResultadoFs(boolean exito, String mensaje) {
        this.exito = exito;
        this.mensaje = mensaje;
    }

    /** La operacion se hizo. */
    public static ResultadoFs ok(String mensaje) {
        return new ResultadoFs(true, mensaje);
    }

    /** La operacion no se hizo; el mensaje explica por que. */
    public static ResultadoFs error(String mensaje) {
        return new ResultadoFs(false, mensaje);
    }

    public boolean isExito() {
        return exito;
    }

    public String getMensaje() {
        return mensaje;
    }

    @Override
    public String toString() {
        return (exito ? "OK: " : "ERROR: ") + mensaje;
    }
}
