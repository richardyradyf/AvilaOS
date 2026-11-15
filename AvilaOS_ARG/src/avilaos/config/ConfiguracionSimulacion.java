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

    private final int duracionCicloMs;
    private final Computador[] computadores;
    private final SolicitudProceso[] procesos;

    public ConfiguracionSimulacion(int duracionCicloMs, Computador[] computadores, SolicitudProceso[] procesos) {
        this.duracionCicloMs = duracionCicloMs;
        this.computadores = computadores;
        this.procesos = procesos;
    }

    public int getDuracionCicloMs() {
        return duracionCicloMs;
    }

    public Computador[] getComputadores() {
        return computadores.clone();
    }

    public SolicitudProceso[] getProcesos() {
        return procesos.clone();
    }

    // ================= configuracion por defecto =================

    /** Escenario de ejemplo: 2 computadores, 3 procesos CPU bound y uno I/O bound. */
    public static ConfiguracionSimulacion porDefecto() {
        Computador[] pcs = {
            new Computador(512, PoliticaPlanificacion.ROUND_ROBIN, 4),
            new Computador(256, PoliticaPlanificacion.EDF, FabricaPlanificadores.QUANTUM_POR_DEFECTO)
        };
        SolicitudProceso[] procs = {
            new SolicitudProceso("Calculo A", TipoProceso.CPU_BOUND, 30, 20, 2, 300, 1, 0, 0, 0, 0),
            new SolicitudProceso("Lector", TipoProceso.IO_BOUND, 20, 20, 3, 400, 1, 3, 4, 0, 0),
            new SolicitudProceso("Calculo B", TipoProceso.CPU_BOUND, 25, 30, 1, 250, 2, 0, 0, 0, 0),
            new SolicitudProceso("Calculo C", TipoProceso.CPU_BOUND, 15, 20, 2, 300, 2, 0, 0, 0, 0)
        };
        return new ConfiguracionSimulacion(300, pcs, procs);
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

        Lista<Object> listaProcs = o.tiene("procesos") ? lista(o, "procesos") : new Lista<Object>();
        if (listaProcs.tamano() > MAX_PROCESOS_INICIALES) {
            throw new EntradaInvalidaException("procesos: máximo " + MAX_PROCESOS_INICIALES);
        }
        SolicitudProceso[] procs = new SolicitudProceso[listaProcs.tamano()];
        for (int i = 0; i < procs.length; i++) {
            procs[i] = proceso(objeto(listaProcs.obtener(i), "procesos[" + (i + 1) + "]"),
                    "procesos[" + (i + 1) + "]", pcs.length);
        }
        return new ConfiguracionSimulacion(duracion, pcs, procs);
    }

    private static SolicitudProceso proceso(JsonObjeto p, String nombre, int numPcs) {
        TipoProceso tipo = tipo(p, nombre);
        int computador = p.tiene("computador") ? entero(p, "computador", nombre + ".computador", 0, numPcs) : 0;
        try {
            return ValidadorEntradas.validarProceso(
                    texto(p, "nombre", nombre + ".nombre"), tipo,
                    opcional(p, "instrucciones"), opcional(p, "memoria"), opcional(p, "prioridad"),
                    opcional(p, "deadline"), computador, opcional(p, "ciclosEs"), opcional(p, "cadaCiclos"),
                    "", 0);
        } catch (EntradaInvalidaException e) {
            throw new EntradaInvalidaException(nombre + " (" + textoOpcional(p, "nombre") + "): " + e.getMessage());
        }
    }

    /** Solo se aceptan procesos CPU bound e I/O bound. */
    private static TipoProceso tipo(JsonObjeto p, String nombre) {
        String textoTipo = texto(p, "tipo", nombre + ".tipo").trim().toUpperCase();
        if (textoTipo.equals("CPU_BOUND")) {
            return TipoProceso.CPU_BOUND;
        }
        if (textoTipo.equals("IO_BOUND")) {
            return TipoProceso.IO_BOUND;
        }
        throw new EntradaInvalidaException(nombre + ".tipo: debe ser CPU_BOUND o IO_BOUND");
    }

    // ================= escritura =================

    /** Escribe la configuracion como JSON con sangria. aJson -> desdeJson devuelve lo mismo. */
    public String aJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"duracionCicloMs\": ").append(duracionCicloMs).append(",\n");
        sb.append("  \"computadores\": [\n");
        for (int i = 0; i < computadores.length; i++) {
            Computador c = computadores[i];
            sb.append("    {\"memoriaMb\": ").append(c.memoriaMb)
              .append(", \"politica\": \"").append(c.politica.name()).append("\"")
              .append(", \"quantum\": ").append(c.quantum).append("}")
              .append(i < computadores.length - 1 ? "," : "").append("\n");
        }
        sb.append("  ],\n  \"procesos\": [\n");
        for (int i = 0; i < procesos.length; i++) {
            SolicitudProceso p = procesos[i];
            sb.append("    {\"nombre\": \"").append(escapar(p.getNombre())).append("\"")
              .append(", \"tipo\": \"").append(p.getTipo().name()).append("\"")
              .append(", \"instrucciones\": ").append(p.getInstrucciones())
              .append(", \"memoria\": ").append(p.getMemoria())
              .append(", \"prioridad\": ").append(p.getPrioridad())
              .append(", \"deadline\": ").append(p.getDeadline())
              .append(", \"computador\": ").append(p.getIdComputador());
            if (p.getTipo() == TipoProceso.IO_BOUND) {
                sb.append(", \"ciclosEs\": ").append(p.getCiclosEs())
                  .append(", \"cadaCiclos\": ").append(p.getCadaCiclos());
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
