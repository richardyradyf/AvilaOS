package avilaos.model;

/**
 * Datos que el usuario pide para crear un proceso (Vista 1). Es solo un contenedor
 * inmutable; las reglas de validacion estan en ValidadorEntradas.
 *
 * - idComputador: 0 significa "que ÁvilaOS lo asigne automaticamente".
 * - instrucciones: solo CPU bound e I/O bound. En productores y consumidores vale 0:
 *   el sistema la calcula a partir de los elementos y los ciclos por elemento.
 * - ciclosEs: duracion de cada espera de E/S (solo I/O bound).
 * - cadaCiclos: en I/O bound, cada cuantas instrucciones pide E/S; en productor y
 *   consumidor, cada cuantos ciclos produce o consume un elemento.
 * - elementos: cuantos elementos produce o consume antes de terminar.
 * - idBuffer: buffer con el que trabaja (solo productor y consumidor); 0 si no aplica.
 */
public final class SolicitudProceso {

    private final String nombre;
    private final TipoProceso tipo;
    private final int instrucciones;
    private final int memoria;
    private final int prioridad;
    private final int deadline;
    private final int idComputador;
    private final int ciclosEs;
    private final int cadaCiclos;
    private final int elementos;
    private final int idBuffer;

    public SolicitudProceso(String nombre, TipoProceso tipo, int instrucciones, int memoria,
                            int prioridad, int deadline, int idComputador, int ciclosEs,
                            int cadaCiclos, int elementos, int idBuffer) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.instrucciones = instrucciones;
        this.memoria = memoria;
        this.prioridad = prioridad;
        this.deadline = deadline;
        this.idComputador = idComputador;
        this.ciclosEs = ciclosEs;
        this.cadaCiclos = cadaCiclos;
        this.elementos = elementos;
        this.idBuffer = idBuffer;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoProceso getTipo() {
        return tipo;
    }

    public int getInstrucciones() {
        return instrucciones;
    }

    public int getMemoria() {
        return memoria;
    }

    public int getPrioridad() {
        return prioridad;
    }

    public int getDeadline() {
        return deadline;
    }

    public int getIdComputador() {
        return idComputador;
    }

    public boolean esAutomatico() {
        return idComputador == 0;
    }

    public int getCiclosEs() {
        return ciclosEs;
    }

    public int getCadaCiclos() {
        return cadaCiclos;
    }

    public int getElementos() {
        return elementos;
    }

    public int getIdBuffer() {
        return idBuffer;
    }

    @Override
    public String toString() {
        return nombre + " [" + tipo + ", memoria=" + memoria + " MB, prioridad=" + prioridad
                + ", deadline=" + deadline + ", computador=" + (esAutomatico() ? "automático" : idComputador) + "]";
    }
}
