package avilaos.fs;

import java.util.concurrent.Semaphore;

/**
 * @author Gianm
 * Mapa de todos los bloques del disco, con asignacion encadenada.
 *
 * El disco se divide en dos areas:
 * - bloques 0 .. bloquesArchivos-1: area del sistema de archivos;
 * - bloques bloquesArchivos .. total-1: area de intercambio (swap), donde se guardan
 *   los procesos suspendidos. El swap se configura despues con configurarSwap(n)
 *   porque su tamano viene en otro JSON.
 *
 * Cada archivo ocupa una cadena de bloques: cada bloque apunta al siguiente y el
 * ultimo apunta a -1. Al asignar se toman los primeros bloques libres, aunque esten
 * separados, asi que no hay fragmentacion externa.
 *
 * La usan a la vez el hilo del reloj (el nucleo ejecuta operaciones) y el hilo de
 * Swing (los paneles leen), por eso todos los metodos publicos toman un mutex. Los
 * privados asumen que el mutex ya esta tomado (el semaforo no es reentrante).
 */
public class MapaBloques {

    /** Maximo de bloques del area de archivos y del area de swap. */
    public static final int MAX_BLOQUES = 500;

    private final int bloquesArchivos;
    private int bloquesSwap;
    private Bloque[] bloques;
    private final Semaphore mutex = new Semaphore(1);

    /** @param bloquesArchivos bloques del area de archivos, de 1 a 500 */
    public MapaBloques(int bloquesArchivos) {
        if (bloquesArchivos < 1 || bloquesArchivos > MAX_BLOQUES) {
            throw new IllegalArgumentException("El área de archivos debe tener entre 1 y " + MAX_BLOQUES
                    + " bloques (se pidieron " + bloquesArchivos + ")");
        }
        this.bloquesArchivos = bloquesArchivos;
        this.bloquesSwap = 0;
        this.bloques = new Bloque[bloquesArchivos];
        for (int i = 0; i < bloquesArchivos; i++) {
            bloques[i] = new Bloque(i);
        }
    }

    // ================= configuracion =================

    /**
     * Define el tamano del area de swap, que queda justo despues del area de archivos.
     * Los archivos no se mueven.
     *
     * @param cantidad bloques de swap, de 0 a 500
     * @throws IllegalStateException si el swap actual tiene algun bloque ocupado
     */
    public void configurarSwap(int cantidad) {
        if (cantidad < 0 || cantidad > MAX_BLOQUES) {
            throw new IllegalArgumentException("El área de swap debe tener entre 0 y " + MAX_BLOQUES
                    + " bloques (se pidieron " + cantidad + ")");
        }
        mutex.acquireUninterruptibly();
        try {
            if (contarLibres(bloquesArchivos, bloques.length) != bloquesSwap) {
                throw new IllegalStateException("No se puede cambiar el área de swap mientras tiene procesos suspendidos");
            }
            Bloque[] nuevos = new Bloque[bloquesArchivos + cantidad];
            for (int i = 0; i < bloquesArchivos; i++) {
                nuevos[i] = bloques[i];
            }
            for (int i = bloquesArchivos; i < nuevos.length; i++) {
                nuevos[i] = new Bloque(i);
            }
            bloques = nuevos;
            bloquesSwap = cantidad;
        } finally {
            mutex.release();
        }
    }

    // ================= consulta =================

    /** Bloques del area de archivos mas los del swap. */
    public int getTotalBloques() {
        mutex.acquireUninterruptibly();
        try {
            return bloques.length;
        } finally {
            mutex.release();
        }
    }

    public int getBloquesArchivos() {
        return bloquesArchivos;
    }

    public int getBloquesSwap() {
        mutex.acquireUninterruptibly();
        try {
            return bloquesSwap;
        } finally {
            mutex.release();
        }
    }

    /** Bloques libres del area de archivos (no cuenta el swap). */
    public int libres() {
        mutex.acquireUninterruptibly();
        try {
            return contarLibres(0, bloquesArchivos);
        } finally {
            mutex.release();
        }
    }

    /** Bloques libres del area de swap. */
    public int libresSwap() {
        mutex.acquireUninterruptibly();
        try {
            return contarLibres(bloquesArchivos, bloques.length);
        } finally {
            mutex.release();
        }
    }

    public boolean estaLibre(int bloque) {
        mutex.acquireUninterruptibly();
        try {
            validarBloque(bloque);
            return bloques[bloque].estaLibre();
        } finally {
            mutex.release();
        }
    }

    /** Indica si el bloque pertenece al area de swap. */
    public boolean esSwap(int bloque) {
        mutex.acquireUninterruptibly();
        try {
            validarBloque(bloque);
            return bloque >= bloquesArchivos;
        } finally {
            mutex.release();
        }
    }

    /** Ruta del archivo, "swap:P7" o null si esta libre. */
    public String getOcupante(int bloque) {
        mutex.acquireUninterruptibly();
        try {
            validarBloque(bloque);
            return bloques[bloque].getOcupante();
        } finally {
            mutex.release();
        }
    }

    /** Siguiente bloque del mismo archivo, o -1 si es el ultimo (o esta libre). */
    public int getSiguiente(int bloque) {
        mutex.acquireUninterruptibly();
        try {
            validarBloque(bloque);
            return bloques[bloque].getSiguiente();
        } finally {
            mutex.release();
        }
    }

    /** Primer bloque libre del area de archivos, o -1 si esta llena. */
    public int primerLibre() {
        mutex.acquireUninterruptibly();
        try {
            for (int i = 0; i < bloquesArchivos; i++) {
                if (bloques[i].estaLibre()) {
                    return i;
                }
            }
            return -1;
        } finally {
            mutex.release();
        }
    }

    /**
     * Los bloques de un archivo en orden, siguiendo la cadena desde su primer bloque.
     * Devuelve un arreglo vacio si el bloque no existe o esta libre.
     */
    public int[] cadena(int primerBloque) {
        mutex.acquireUninterruptibly();
        try {
            if (primerBloque < 0 || primerBloque >= bloques.length || bloques[primerBloque].estaLibre()) {
                return new int[0];
            }
            int cantidad = 0;
            int actual = primerBloque;
            while (actual != Bloque.SIN_SIGUIENTE && cantidad < bloques.length) {
                cantidad++;
                actual = bloques[actual].getSiguiente();
            }
            int[] resultado = new int[cantidad];
            actual = primerBloque;
            for (int i = 0; i < cantidad; i++) {
                resultado[i] = actual;
                actual = bloques[actual].getSiguiente();
            }
            return resultado;
        } finally {
            mutex.release();
        }
    }

    // ================= asignacion en el area de archivos =================

    /**
     * Asigna "cantidad" bloques a un archivo: toma los primeros libres desde el bloque
     * 0 (pueden quedar separados) y los encadena.
     *
     * @return el primer bloque del archivo, o -1 si no hay bloques libres suficientes
     *         (en ese caso no se asigna nada)
     */
    public int asignar(int cantidad, String ocupante) {
        validarPedido(cantidad, ocupante);
        mutex.acquireUninterruptibly();
        try {
            return asignarEnRango(0, bloquesArchivos, cantidad, ocupante);
        } finally {
            mutex.release();
        }
    }

    /**
     * Asigna bloques contiguos desde "inicio" (inicio, inicio+1, ...), encadenados en
     * orden. Se usa al cargar el JSON, que da el bloque inicial y la cantidad.
     *
     * @return inicio, o -1 si algun bloque esta ocupado o se sale del area de archivos
     *         (en ese caso no se asigna nada)
     */
    public int asignarEn(int inicio, int cantidad, String ocupante) {
        validarPedido(cantidad, ocupante);
        mutex.acquireUninterruptibly();
        try {
            if (inicio < 0 || inicio + cantidad > bloquesArchivos) {
                return -1;
            }
            for (int i = inicio; i < inicio + cantidad; i++) {
                if (!bloques[i].estaLibre()) {
                    return -1;
                }
            }
            for (int i = inicio; i < inicio + cantidad; i++) {
                bloques[i].ocupar(ocupante);
                if (i > inicio) {
                    bloques[i - 1].setSiguiente(i);
                }
            }
            return inicio;
        } finally {
            mutex.release();
        }
    }

    /**
     * Libera un archivo del area de archivos recorriendo su cadena.
     *
     * @param primerBloque el primer bloque del archivo
     * @return cuantos bloques libero (0 si el bloque no es el inicio de un archivo)
     */
    public int liberar(int primerBloque) {
        mutex.acquireUninterruptibly();
        try {
            if (!esInicioDeCadena(primerBloque, 0, bloquesArchivos)) {
                return 0;
            }
            return liberarCadena(primerBloque);
        } finally {
            mutex.release();
        }
    }

    /** Cambia el ocupante de todos los bloques de una cadena (al renombrar). */
    public void cambiarOcupante(int primerBloque, String nuevo) {
        if (nuevo == null || nuevo.isBlank()) {
            throw new IllegalArgumentException("El nuevo ocupante no puede estar vacio");
        }
        mutex.acquireUninterruptibly();
        try {
            if (primerBloque < 0 || primerBloque >= bloques.length || bloques[primerBloque].estaLibre()) {
                throw new IllegalArgumentException("El bloque " + primerBloque + " no pertenece a ningún archivo");
            }
            int actual = primerBloque;
            int recorridos = 0;
            while (actual != Bloque.SIN_SIGUIENTE && recorridos < bloques.length) {
                bloques[actual].cambiarOcupante(nuevo);
                actual = bloques[actual].getSiguiente();
                recorridos++;
            }
        } finally {
            mutex.release();
        }
    }

    // ================= area de swap =================

    /**
     * Asigna bloques del swap a un proceso suspendido (ocupante, por ejemplo "swap:P7").
     *
     * @return el primer bloque, o -1 si no hay swap libre suficiente
     */
    public int asignarSwap(int cantidad, String ocupante) {
        validarPedido(cantidad, ocupante);
        mutex.acquireUninterruptibly();
        try {
            return asignarEnRango(bloquesArchivos, bloques.length, cantidad, ocupante);
        } finally {
            mutex.release();
        }
    }

    /** Libera los bloques de swap de un proceso; devuelve cuantos libero. */
    public int liberarSwap(int primerBloque) {
        mutex.acquireUninterruptibly();
        try {
            if (!esInicioDeCadena(primerBloque, bloquesArchivos, bloques.length)) {
                return 0;
            }
            return liberarCadena(primerBloque);
        } finally {
            mutex.release();
        }
    }

    /** Vacia toda el area de swap (al reiniciar con procesos nuevos). */
    public void liberarTodoSwap() {
        mutex.acquireUninterruptibly();
        try {
            for (int i = bloquesArchivos; i < bloques.length; i++) {
                bloques[i].liberar();
            }
        } finally {
            mutex.release();
        }
    }

    @Override
    public String toString() {
        mutex.acquireUninterruptibly();
        try {
            return "archivos " + contarLibres(0, bloquesArchivos) + "/" + bloquesArchivos
                    + " libres, swap " + contarLibres(bloquesArchivos, bloques.length) + "/" + bloquesSwap + " libres";
        } finally {
            mutex.release();
        }
    }

    // ================= privados (el mutex ya esta tomado) =================

    /**
     * Toma los primeros "cantidad" bloques libres del rango [desde, hasta) y los
     * encadena. Si no alcanzan, no toca nada y devuelve -1.
     */
    private int asignarEnRango(int desde, int hasta, int cantidad, String ocupante) {
        if (contarLibres(desde, hasta) < cantidad) {
            return -1;
        }
        int primero = -1;
        int anterior = -1;
        int tomados = 0;
        for (int i = desde; i < hasta && tomados < cantidad; i++) {
            if (bloques[i].estaLibre()) {
                bloques[i].ocupar(ocupante);
                if (anterior == -1) {
                    primero = i;
                } else {
                    bloques[anterior].setSiguiente(i);
                }
                anterior = i;
                tomados++;
            }
        }
        return primero;
    }

    /** Recorre la cadena liberando cada bloque; devuelve cuantos libero. */
    private int liberarCadena(int primerBloque) {
        int liberados = 0;
        int actual = primerBloque;
        while (actual != Bloque.SIN_SIGUIENTE && liberados < bloques.length) {
            int siguiente = bloques[actual].getSiguiente();
            bloques[actual].liberar();
            liberados++;
            actual = siguiente;
        }
        return liberados;
    }

    /**
     * Indica si el bloque esta dentro del rango, ocupado y es el primero de su cadena
     * (ningun otro bloque apunta a el). Evita cortar un archivo por la mitad.
     */
    private boolean esInicioDeCadena(int bloque, int desde, int hasta) {
        if (bloque < desde || bloque >= hasta || bloques[bloque].estaLibre()) {
            return false;
        }
        for (Bloque b : bloques) {
            if (!b.estaLibre() && b.getSiguiente() == bloque) {
                return false;
            }
        }
        return true;
    }

    private int contarLibres(int desde, int hasta) {
        int libres = 0;
        for (int i = desde; i < hasta; i++) {
            if (bloques[i].estaLibre()) {
                libres++;
            }
        }
        return libres;
    }

    private void validarBloque(int bloque) {
        if (bloque < 0 || bloque >= bloques.length) {
            throw new IllegalArgumentException("El bloque " + bloque + " no existe (el disco tiene "
                    + bloques.length + " bloques)");
        }
    }

    private static void validarPedido(int cantidad, String ocupante) {
        if (cantidad < 1) {
            throw new IllegalArgumentException("Se debe pedir al menos 1 bloque");
        }
        if (ocupante == null || ocupante.isBlank()) {
            throw new IllegalArgumentException("El ocupante no puede estar vacio");
        }
    }
}
