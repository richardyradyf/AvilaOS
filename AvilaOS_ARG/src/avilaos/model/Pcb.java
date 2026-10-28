/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.model;

/**
 * 
 * Bloque de control de proceso (PCB). Como cada proceso de AvilaOS tiene un
 * unico hilo, el PCB y el TCB coinciden: esta clase es la ficha completa.
 *
 * Los cambios de estado se hacen solo con los metodos admitir, despachar,
 * expulsar, bloquear, desbloquear y terminar, que respetan el diagrama de estados.
 *
 * IMPORTANTE: no es segura entre hilos. El nucleo del computador la manipula
 * dentro de sus secciones criticas (semaforos de Java).
 */
public class Pcb {

    // ---- datos fijos desde la creacion ----
    private final int id;
    private final String nombre;
    private final int idComputador;
    private final TipoProceso tipo;
    /** Convencion del proyecto: menor valor = mayor prioridad (1 es la mas alta). */
    private final int prioridad;
    private final int memoria;
    private final int instruccionesTotales;
    private final long cicloCreacion;
    
    private final long cicloLimite;


    private EstadoProceso estado;
    private MotivoBloqueo motivoBloqueo;
    private int instruccionesRestantes;
    private int pc;
    private int mar;

    
    private long cicloPrimeraEjecucion;
    private long cicloFinalizacion;
    private boolean terminadoPorDeadline;

    /**
     * Crea un proceso en estado NUEVO. El ID se genera dentro, despues de validar,
     * para que un proceso invalido no consuma un ID.
     *
     * @param nombre               nombre del proceso (no vacio)
     * @param idComputador         computador donde se ejecuta (1, 2, 3...)
     * @param tipo                 tipo de proceso
     * @param prioridad            1 o mayor; menor valor = mayor prioridad
     * @param memoria              memoria que requiere (MB), mayor que 0
     * @param instruccionesTotales cantidad de instrucciones, mayor que 0
     * @param deadline             ciclos disponibles desde la creacion, mayor que 0
     * @param cicloCreacion        ciclo del reloj global en que se crea el proceso
     */
    public Pcb(String nombre, int idComputador, TipoProceso tipo, int prioridad,
               int memoria, int instruccionesTotales, int deadline, long cicloCreacion) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        }
        if (idComputador < 1) {
            throw new IllegalArgumentException("El computador debe ser 1 o mayor");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo no puede ser nulo");
        }
        if (prioridad < 1) {
            throw new IllegalArgumentException("La prioridad debe ser 1 o mayor");
        }
        if (memoria < 1) {
            throw new IllegalArgumentException("La memoria debe ser mayor que 0");
        }
        if (instruccionesTotales < 1) {
            throw new IllegalArgumentException("Las instrucciones deben ser mayor que 0");
        }
        if (deadline < 1) {
            throw new IllegalArgumentException("El deadline debe ser mayor que 0");
        }
        if (cicloCreacion < 0) {
            throw new IllegalArgumentException("El ciclo de creacion no puede ser negativo");
        }

        this.id = GeneradorId.siguienteId();
        this.nombre = nombre.trim();
        this.idComputador = idComputador;
        this.tipo = tipo;
        this.prioridad = prioridad;
        this.memoria = memoria;
        this.instruccionesTotales = instruccionesTotales;
        this.cicloCreacion = cicloCreacion;
        this.cicloLimite = cicloCreacion + deadline;

        this.estado = EstadoProceso.NUEVO;
        this.motivoBloqueo = MotivoBloqueo.NINGUNO;
        this.instruccionesRestantes = instruccionesTotales;
        this.pc = 0;
        this.mar = 0;
        this.cicloPrimeraEjecucion = -1;
        this.cicloFinalizacion = -1;
        this.terminadoPorDeadline = false;
    }

    // ---- transiciones de estado ----

    public void admitir() {
        exigirEstado(EstadoProceso.NUEVO, "admitir");
        estado = EstadoProceso.LISTO;
    }

    
    public void despachar(long cicloActual) {
        exigirEstado(EstadoProceso.LISTO, "despachar");
        estado = EstadoProceso.EJECUCION;
        if (cicloPrimeraEjecucion < 0) {
            cicloPrimeraEjecucion = cicloActual;
        }
    }


    public void expulsar() {
        exigirEstado(EstadoProceso.EJECUCION, "expulsar");
        estado = EstadoProceso.LISTO;
    }


    public void bloquear(MotivoBloqueo motivo) {
        if (motivo == null || motivo == MotivoBloqueo.NINGUNO) {
            throw new IllegalArgumentException("Un proceso bloqueado necesita un motivo");
        }
        exigirEstado(EstadoProceso.EJECUCION, "bloquear");
        estado = EstadoProceso.BLOQUEADO;
        motivoBloqueo = motivo;
    }


    public void desbloquear() {
        exigirEstado(EstadoProceso.BLOQUEADO, "desbloquear");
        estado = EstadoProceso.LISTO;
        motivoBloqueo = MotivoBloqueo.NINGUNO;
    }
    
    /**
     * BLOQUEADO -> BLOQUEADO con otro motivo. Se usa, por ejemplo, cuando un acceso
     * remoto a un buffer llega a destino y el proceso debe esperar en un semaforo.
     */
    public void cambiarMotivoBloqueo(MotivoBloqueo nuevoMotivo) {
        if (nuevoMotivo == null || nuevoMotivo == MotivoBloqueo.NINGUNO) {
            throw new IllegalArgumentException("Un proceso bloqueado necesita un motivo");
        }
        exigirEstado(EstadoProceso.BLOQUEADO, "cambiar el motivo de bloqueo");
        motivoBloqueo = nuevoMotivo;
    }

    public void terminar(long cicloActual, boolean porDeadline) {
        if (estado == EstadoProceso.TERMINADO) {
            throw new IllegalStateException(getEtiqueta() + ": ya esta terminado");
        }
        estado = EstadoProceso.TERMINADO;
        motivoBloqueo = MotivoBloqueo.NINGUNO;
        cicloFinalizacion = cicloActual;
        terminadoPorDeadline = porDeadline;
    }

    // ---- ejecucion ----

    /**
     * Ejecuta una instruccion: avanza el PC y el MAR una unidad y descuenta una
     * instruccion restante. Solo es valido en estado EJECUCION.
     */
    public void ejecutarInstruccion() {
        exigirEstado(EstadoProceso.EJECUCION, "ejecutar una instruccion");
        if (instruccionesRestantes <= 0) {
            throw new IllegalStateException(getEtiqueta() + ": ya ejecuto todas sus instrucciones");
        }
        instruccionesRestantes--;
        pc++;
        mar++;
    }

    public boolean completoSusInstrucciones() {
        return instruccionesRestantes == 0;
    }

    // ---- deadline ----

    public long deadlineRestante(long cicloActual) {
        return cicloLimite - cicloActual;
    }

    public boolean deadlineVencido(long cicloActual) {
        return deadlineRestante(cicloActual) <= 0;
    }

    // ---- consulta ----

    public int getId() {
        return id;
    }

    public String getEtiqueta() {
        return "P" + id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getIdComputador() {
        return idComputador;
    }

    public TipoProceso getTipo() {
        return tipo;
    }

    public int getPrioridad() {
        return prioridad;
    }

    public int getMemoria() {
        return memoria;
    }

    public int getInstruccionesTotales() {
        return instruccionesTotales;
    }

    /** Tiempo restante de ejecucion, medido en instrucciones (una por ciclo). */
    public int getInstruccionesRestantes() {
        return instruccionesRestantes;
    }

    public int getPc() {
        return pc;
    }

    public int getMar() {
        return mar;
    }

    public EstadoProceso getEstado() {
        return estado;
    }

    public MotivoBloqueo getMotivoBloqueo() {
        return motivoBloqueo;
    }

    public long getCicloCreacion() {
        return cicloCreacion;
    }

    public long getCicloLimite() {
        return cicloLimite;
    }

    public long getCicloPrimeraEjecucion() {
        return cicloPrimeraEjecucion;
    }

    public long getCicloFinalizacion() {
        return cicloFinalizacion;
    }

    public boolean isTerminadoPorDeadline() {
        return terminadoPorDeadline;
    }

    @Override
    public String toString() {
        return getEtiqueta() + " (" + nombre + ") " + estado
                + " pc=" + pc + " restantes=" + instruccionesRestantes;
    }

    // ---- privados ----

    private void exigirEstado(EstadoProceso esperado, String operacion) {
        if (estado != esperado) {
            throw new IllegalStateException(getEtiqueta() + ": no se puede " + operacion
                    + " desde el estado " + estado);
        }
    }
}
