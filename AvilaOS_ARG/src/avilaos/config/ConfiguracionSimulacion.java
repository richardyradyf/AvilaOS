/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.config;

import avilaos.model.PoliticaPlanificacion;
import avilaos.model.SolicitudProceso;
import avilaos.model.TipoProceso;
import avilaos.scheduler.FabricaPlanificadores;
import avilaos.util.Lista;

/**
 * @author richard
 * 
 * Configuracion completa de una simulacion: duracion del ciclo, latencia de red,
 * computadores (RAM y politica), buffers y procesos iniciales. Se lee y se escribe como
 * JSON (desdeJson / aJson). Toda la validacion ocurre aqui: si desdeJson devuelve un
 * objeto, sus datos son validos.
 */
public final class ConfiguracionSimulacion {

    public static final int MIN_COMPUTADORES = 2;
    public static final int MAX_COMPUTADORES = 6;
    public static final int MAX_PROCESOS_INICIALES = 200;
    public static final int MAX_BUFFERS = 20;

    /** Un computador de la configuracion. */
    public static final class Computador {
        public final int memoriaMb;
        public final PoliticaPlanificacion politica;
        public final int quantum;

        public Computador(int memoriaMb, PoliticaPlanificacion politica, int quantum) {
            this.memoriaMb = memoriaMb;
            this.politica = politica;
            this.quantum = quantum;
        }
    }

    /** Un buffer de la configuracion. anfitrion 0 = automatico. */
    public static final class Buffer {
        public final int anfitrion;
        public final int capacidad;

        public Buffer(int anfitrion, int capacidad) {
            this.anfitrion = anfitrion;
            this.capacidad = capacidad;
        }
    }

    private final int duracionCicloMs;
    private final int latenciaRed;
    private final Computador[] computadores;
    private final Buffer[] buffers;
    private final SolicitudProceso[] procesos;

    public ConfiguracionSimulacion(int duracionCicloMs, int latenciaRed, Computador[] computadores,
                                   Buffer[] buffers, SolicitudProceso[] procesos) {
        this.duracionCicloMs = duracionCicloMs;
        this.latenciaRed = latenciaRed;
        this.computadores = computadores;
        this.buffers = buffers;
        this.procesos = procesos;
    }

    public int getDuracionCicloMs() {
        return duracionCicloMs;
    }

    public int getLatenciaRed() {
        return latenciaRed;
    }

    public Computador[] getComputadores() {
        return computadores.clone();
    }

    public Buffer[] getBuffers() {
        return buffers.clone();
    }

    public SolicitudProceso[] getProcesos() {
        return procesos.clone();
    }

    // ================= configuracion por defecto =================

    /** Escenario de ejemplo: 2 computadores, 1 buffer remoto, un productor y un consumidor y 3 procesos mas. */
    public static ConfiguracionSimulacion porDefecto() {
        Computador[] pcs = {
            new Computador(512, PoliticaPlanificacion.ROUND_ROBIN, 4),
            new Computador(256, PoliticaPlanificacion.EDF, FabricaPlanificadores.QUANTUM_POR_DEFECTO)
        };
        Buffer[] bufs = {new Buffer(2, 5)};
        SolicitudProceso[] procs = {
            new SolicitudProceso("Calculo A", TipoProceso.CPU_BOUND, 30, 20, 2, 300, 1, 0, 0, 0, 0),
            new SolicitudProceso("Lector", TipoProceso.IO_BOUND, 20, 20, 3, 400, 1, 3, 4, 0, 0),
            new SolicitudProceso("Calculo B", TipoProceso.CPU_BOUND, 25, 30, 1, 250, 2, 0, 0, 0, 0),
            new SolicitudProceso("Productor", TipoProceso.PRODUCTOR, 0, 10, 1, 600, 1, 0, 2, 12, 1),
            new SolicitudProceso("Consumidor", TipoProceso.CONSUMIDOR, 0, 10, 1, 600, 2, 0, 2, 12, 1)
        };
        return new ConfiguracionSimulacion(300, 3, pcs, bufs, procs);
    }

    // ================= lectura =================

    /** Lee y valida el JSON. Lanza EntradaInvalidaException con un mensaje claro si algo esta mal. */
    public static ConfiguracionSimulacion desdeJson(String texto) {
        Object raiz = LectorJson.leer(texto);
        if (!(raiz instanceof JsonObjeto)) {
            throw new EntradaInvalidaException("La configuración debe ser un objeto JSON { ... }");
        }
        JsonObjeto o = (JsonObjeto) raiz;
        int duracion = entero(o, "duracionCicloMs", "Duración del ciclo (ms)", 10, 10000);
        int latencia = entero(o, "latenciaRed", "Latencia de red (ciclos)", 0, 1000);

        Lista<Object> listaPcs = lista(o, "computadores");
        if (listaPcs.tamano() < MIN_COMPUTADORES || listaPcs.tamano() > MAX_COMPUTADORES) {
            throw new EntradaInvalidaException("computadores: debe haber entre " + MIN_COMPUTADORES
                    + " y " + MAX_COMPUTADORES + " (hay " + listaPcs.tamano() + ")");
        }
        Computador[] pcs = new Computador[listaPcs.tamano()];
        for (int i = 0; i < pcs.length; i++) {
            String nombre = "computadores[" + (i + 1) + "]";
            JsonObjeto c = objeto(listaPcs.obtener(i), nombre);
            int ram = entero(c, "memoriaMb", nombre + ".memoriaMb", 1, 65536);
            PoliticaPlanificacion pol = politica(c, nombre);
            int quantum = c.tiene("quantum") ? entero(c, "quantum", nombre + ".quantum", 1, 1000)
                    : FabricaPlanificadores.QUANTUM_POR_DEFECTO;
            pcs[i] = new Computador(ram, pol, quantum);
        }

        Lista<Object> listaBufs = o.tiene("buffers") ? lista(o, "buffers") : new Lista<Object>();
        if (listaBufs.tamano() > MAX_BUFFERS) {
            throw new EntradaInvalidaException("buffers: máximo " + MAX_BUFFERS);
        }
        Buffer[] bufs = new Buffer[listaBufs.tamano()];
        for (int i = 0; i < bufs.length; i++) {
            String nombre = "buffers[" + (i + 1) + "]";
            JsonObjeto b = objeto(listaBufs.obtener(i), nombre);
            int anfitrion = b.tiene("anfitrion") ? entero(b, "anfitrion", nombre + ".anfitrion", 0, pcs.length) : 0;
            int capacidad = entero(b, "capacidad", nombre + ".capacidad", 1, 1000);
            bufs[i] = new Buffer(anfitrion, capacidad);
        }

        Lista<Object> listaProcs = o.tiene("procesos") ? lista(o, "procesos") : new Lista<Object>();
        if (listaProcs.tamano() > MAX_PROCESOS_INICIALES) {
            throw new EntradaInvalidaException("procesos: máximo " + MAX_PROCESOS_INICIALES);
        }
        SolicitudProceso[] procs = new SolicitudProceso[listaProcs.tamano()];
        for (int i = 0; i < procs.length; i++) {
            procs[i] = proceso(objeto(listaProcs.obtener(i), "procesos[" + (i + 1) + "]"),
                    "procesos[" + (i + 1) + "]", pcs.length, bufs.length);
        }
        return new ConfiguracionSimulacion(duracion, latencia, pcs, bufs, procs);
    }

    private static SolicitudProceso proceso(JsonObjeto p, String nombre, int numPcs, int numBufs) {
        TipoProceso tipo;
        String textoTipo = texto(p, "tipo", nombre + ".tipo");
        try {
            tipo = TipoProceso.valueOf(textoTipo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new EntradaInvalidaException(nombre + ".tipo: debe ser CPU_BOUND, IO_BOUND, PRODUCTOR o CONSUMIDOR");
        }
        int computador = p.tiene("computador") ? entero(p, "computador", nombre + ".computador", 0, numPcs) : 0;
        int buffer = p.tiene("buffer") ? entero(p, "buffer", nombre + ".buffer", 0, numBufs) : 0;
        try {
            return ValidadorEntradas.validarProceso(
                    texto(p, "nombre", nombre + ".nombre"), tipo,
                    opcional(p, "instrucciones"), opcional(p, "memoria"), opcional(p, "prioridad"),
                    opcional(p, "deadline"), computador, opcional(p, "ciclosEs"), opcional(p, "cadaCiclos"),
                    opcional(p, "elementos"), buffer);
        } catch (EntradaInvalidaException e) {
            throw new EntradaInvalidaException(nombre + " (" + textoOpcional(p, "nombre") + "): " + e.getMessage());
        }
    }

    // ================= escritura =================

    /** Escribe la configuracion como JSON con sangria. aJson -> desdeJson devuelve lo mismo. */
    public String aJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"duracionCicloMs\": ").append(duracionCicloMs).append(",\n");
        sb.append("  \"latenciaRed\": ").append(latenciaRed).append(",\n");
        sb.append("  \"computadores\": [\n");
        for (int i = 0; i < computadores.length; i++) {
            Computador c = computadores[i];
            sb.append("    {\"memoriaMb\": ").append(c.memoriaMb)
              .append(", \"politica\": \"").append(c.politica.name()).append("\"")
              .append(", \"quantum\": ").append(c.quantum).append("}")
              .append(i < computadores.length - 1 ? "," : "").append("\n");
        }
        sb.append("  ],\n  \"buffers\": [\n");
        for (int i = 0; i < buffers.length; i++) {
            sb.append("    {\"anfitrion\": ").append(buffers[i].anfitrion)
              .append(", \"capacidad\": ").append(buffers[i].capacidad).append("}")
              .append(i < buffers.length - 1 ? "," : "").append("\n");
        }
        sb.append("  ],\n  \"procesos\": [\n");
        for (int i = 0; i < procesos.length; i++) {
            SolicitudProceso p = procesos[i];
            sb.append("    {\"nombre\": \"").append(escapar(p.getNombre())).append("\"")
              .append(", \"tipo\": \"").append(p.getTipo().name()).append("\"");
            if (p.getTipo() == TipoProceso.CPU_BOUND || p.getTipo() == TipoProceso.IO_BOUND) {
                sb.append(", \"instrucciones\": ").append(p.getInstrucciones());
            }
            sb.append(", \"memoria\": ").append(p.getMemoria())
              .append(", \"prioridad\": ").append(p.getPrioridad())
              .append(", \"deadline\": ").append(p.getDeadline())
              .append(", \"computador\": ").append(p.getIdComputador());
            if (p.getTipo() == TipoProceso.IO_BOUND) {
                sb.append(", \"ciclosEs\": ").append(p.getCiclosEs())
                  .append(", \"cadaCiclos\": ").append(p.getCadaCiclos());
            }
            if (p.getTipo() == TipoProceso.PRODUCTOR || p.getTipo() == TipoProceso.CONSUMIDOR) {
                sb.append(", \"cadaCiclos\": ").append(p.getCadaCiclos())
                  .append(", \"elementos\": ").append(p.getElementos())
                  .append(", \"buffer\": ").append(p.getIdBuffer());
            }
            sb.append("}").append(i < procesos.length - 1 ? "," : "").append("\n");
        }
        sb.append("  ]\n}\n");
        return sb.toString();
    }

    private static String escapar(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ================= ayudas de lectura =================

    private static Object requerido(JsonObjeto o, String clave, String campo) {
        if (!o.tiene(clave)) {
            throw new EntradaInvalidaException(campo + ": falta este campo");
        }
        return o.obtener(clave);
    }

    private static int entero(JsonObjeto o, String clave, String campo, int min, int max) {
        Object v = requerido(o, clave, campo);
        if (!(v instanceof Long)) {
            throw new EntradaInvalidaException(campo + ": debe ser un número entero entre " + min + " y " + max);
        }
        return ValidadorEntradas.entero(String.valueOf(v), campo, min, max);
    }

    private static String texto(JsonObjeto o, String clave, String campo) {
        Object v = requerido(o, clave, campo);
        if (!(v instanceof String)) {
            throw new EntradaInvalidaException(campo + ": debe ser un texto entre comillas");
        }
        return (String) v;
    }

    private static String opcional(JsonObjeto o, String clave) {
        Object v = o.obtener(clave);
        return v == null ? "" : String.valueOf(v);
    }

    private static String textoOpcional(JsonObjeto o, String clave) {
        Object v = o.obtener(clave);
        return v instanceof String ? (String) v : "?";
    }

    @SuppressWarnings("unchecked")
    private static Lista<Object> lista(JsonObjeto o, String clave) {
        Object v = requerido(o, clave, clave);
        if (!(v instanceof Lista)) {
            throw new EntradaInvalidaException(clave + ": debe ser una lista [ ... ]");
        }
        return (Lista<Object>) v;
    }

    private static JsonObjeto objeto(Object v, String campo) {
        if (!(v instanceof JsonObjeto)) {
            throw new EntradaInvalidaException(campo + ": debe ser un objeto { ... }");
        }
        return (JsonObjeto) v;
    }

    private static PoliticaPlanificacion politica(JsonObjeto c, String nombre) {
        String t = texto(c, "politica", nombre + ".politica");
        try {
            return PoliticaPlanificacion.valueOf(t.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new EntradaInvalidaException(nombre + ".politica: debe ser FCFS, EDF, ROUND_ROBIN o PRIORIDADES");
        }
    }
}
