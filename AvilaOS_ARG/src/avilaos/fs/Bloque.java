package avilaos.fs;

/**
 * @author Gianm
 * Un bloque del disco. Guarda quien lo ocupa (la ruta de un archivo, "swap:P7" o
 * null si esta libre) y el numero del siguiente bloque del mismo archivo (-1 si es
 * el ultimo). Asi los bloques de un archivo forman una lista enlazada dentro del
 * disco: eso es la asignacion encadenada.
 *
 * No es segura entre hilos: solo la modifica MapaBloques dentro de su mutex. Por eso
 * los metodos que cambian el bloque no son publicos.
 */
public class Bloque {

    /** Valor de "siguiente" en el ultimo bloque de una cadena. */
    public static final int SIN_SIGUIENTE = -1;

    private final int numero;
    private String ocupante;
    private int siguiente;

    public Bloque(int numero) {
        if (numero < 0) {
            throw new IllegalArgumentException("El numero de bloque no puede ser negativo");
        }
        this.numero = numero;
        this.ocupante = null;
        this.siguiente = SIN_SIGUIENTE;
    }

    public int getNumero() {
        return numero;
    }

    /** Ruta del archivo, "swap:P7" o null si el bloque esta libre. */
    public String getOcupante() {
        return ocupante;
    }

    /** Numero del siguiente bloque del mismo archivo, o -1 si es el ultimo. */
    public int getSiguiente() {
        return siguiente;
    }

    public boolean estaLibre() {
        return ocupante == null;
    }

    // ---- cambios (solo desde MapaBloques) ----

    /** Marca el bloque como ocupado; queda como ultimo de su cadena hasta que se enlace. */
    void ocupar(String nuevoOcupante) {
        if (nuevoOcupante == null || nuevoOcupante.isBlank()) {
            throw new IllegalArgumentException("El ocupante no puede estar vacio");
        }
        ocupante = nuevoOcupante;
        siguiente = SIN_SIGUIENTE;
    }

    void setSiguiente(int nuevoSiguiente) {
        if (nuevoSiguiente < SIN_SIGUIENTE) {
            throw new IllegalArgumentException("El siguiente bloque no es valido: " + nuevoSiguiente);
        }
        siguiente = nuevoSiguiente;
    }

    void cambiarOcupante(String nuevoOcupante) {
        if (nuevoOcupante == null || nuevoOcupante.isBlank()) {
            throw new IllegalArgumentException("El ocupante no puede estar vacio");
        }
        ocupante = nuevoOcupante;
    }

    void liberar() {
        ocupante = null;
        siguiente = SIN_SIGUIENTE;
    }

    @Override
    public String toString() {
        return "Bloque " + numero + (estaLibre() ? " (libre)" : " (" + ocupante + ", siguiente " + siguiente + ")");
    }
}

