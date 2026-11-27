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
    private final TipoOperacionES operacion;
    private final String ruta;
    private final int bloques;
    private final String nuevoNombre;
    private final String usuario;

    /** Constructor del Proyecto 1 (procesos CPU bound e I/O bound que no tocan archivos). */
    public SolicitudProceso(String nombre, TipoProceso tipo, int instrucciones, int memoria,
                            int prioridad, int deadline, int idComputador, int ciclosEs,
                            int cadaCiclos, int elementos, int idBuffer) {
        this(nombre, tipo, instrucciones, memoria, prioridad, deadline, idComputador, ciclosEs,
                cadaCiclos, elementos, idBuffer, null, null, 0, null, "admin");
    }

    private SolicitudProceso(String nombre, TipoProceso tipo, int instrucciones, int memoria,
                             int prioridad, int deadline, int idComputador, int ciclosEs,
                             int cadaCiclos, int elementos, int idBuffer,
                             TipoOperacionES operacion, String ruta, int bloques,
                             String nuevoNombre, String usuario) {
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
        this.operacion = operacion;
        this.ruta = ruta;
        this.bloques = bloques;
        this.nuevoNombre = nuevoNombre;
        this.usuario = usuario;
    }

    /**
     * Crea la solicitud de un proceso de E/S sobre un archivo o directorio (CRUD).
     * El proceso es I/O bound y el computador se asigna automaticamente.
     *
     * @param bloques     bloques que ocupa el archivo nuevo (solo CREAR; en el resto, 0)
     * @param nuevoNombre nombre nuevo (solo ACTUALIZAR); puede ser null
     * @param usuario     dueno de la solicitud; si es null o vacio se usa "admin"
     * - operacion, ruta, bloques, nuevoNombre, usuario: solo procesos de E/S sobre archivos (deEs).
     */
    public static SolicitudProceso deEs(String nombre, int instrucciones, int memoria, int prioridad,
                                        int deadline, TipoOperacionES operacion, String ruta,
                                        int bloques, String nuevoNombre, String usuario) {
        if (operacion == null || !operacion.esDeArchivo()) {
            throw new IllegalArgumentException("Un proceso de E/S necesita una operaci\u00f3n CRUD");
        }
        if (ruta == null || ruta.isBlank()) {
            throw new IllegalArgumentException("Un proceso de E/S necesita la ruta del archivo");
        }
        String dueno = (usuario == null || usuario.isBlank()) ? "admin" : usuario.trim();
        return new SolicitudProceso(nombre, TipoProceso.IO_BOUND, instrucciones, memoria, prioridad,
                deadline, 0, 0, 0, 0, 0, operacion, ruta.trim(), bloques, nuevoNombre, dueno);
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
    
    
    /** true si es un proceso de E/S sobre un archivo (creado con deEs). */
    public boolean esDeEs() {
        return operacion != null;
    }

    public TipoOperacionES getOperacion() {
        return operacion;
    }

    public String getRuta() {
        return ruta;
    }

    public int getBloques() {
        return bloques;
    }

    public String getNuevoNombre() {
        return nuevoNombre;
    }

    public String getUsuario() {
        return usuario;
    }

    @Override
    public String toString() {
        if (esDeEs()) {
            return nombre + " [E/S " + operacion + " " + ruta + ", memoria=" + memoria
                    + " MB, prioridad=" + prioridad + ", deadline=" + deadline + ", usuario=" + usuario + "]";
        }
        return nombre + " [" + tipo + ", memoria=" + memoria + " MB, prioridad=" + prioridad
                + ", deadline=" + deadline + ", computador=" + (esAutomatico() ? "automático" : idComputador) + "]";
    }
}
