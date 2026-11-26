package avilaos.config;

import avilaos.model.PoliticaDisco;
import avilaos.model.PoliticaPlanificacion;
import avilaos.model.SolicitudProceso;
import avilaos.model.TipoOperacionES;
import avilaos.util.Lista;

/**
 * @author andre
 *
 * Configuracion del segundo JSON ("Archivo de Procesos y Configuracion"):
 * - globalConfig: duracion del ciclo, tamano del swap, politica inicial de disco y velocidad del cabezal.
 * - computer: capacidad de RAM, politica de planificacion de CPU y quantum (si aplica).
 * - processes: procesos de E/S iniciales sobre archivos/directorios.
 *
 * Toda la validacion ocurre aqui: si desdeJson devuelve un objeto, todos sus datos son validos.
 */
public final class ConfiguracionProcesos {

    public static final int MIN_DURACION_MS = 50;
    public static final int MAX_DURACION_MS = 5000;
    public static final int MIN_SWAP_BLOQUES = 0;
    public static final int MAX_SWAP_BLOQUES = 500;
    public static final int MIN_VELOCIDAD_CABEZAL = 1;
    public static final int MAX_VELOCIDAD_CABEZAL = 50;
    public static final int VELOCIDAD_CABEZAL_POR_DEFECTO = 5;

    public static final int MIN_BLOQUES_ARCHIVO = 1;
    public static final int MAX_BLOQUES_ARCHIVO = 500;
    public static final int PRIORIDAD_POR_DEFECTO = 5;
    public static final String DUENO_POR_DEFECTO = "admin";

    private final int cycleDurationMs;
    private final int swapSizeBlocks;
    private final PoliticaDisco initialDiskPolicy;
    private final int headSpeed;

    private final int ramCapacity;
    private final PoliticaPlanificacion computerPolicy;
    private final int quantum;

    private final SolicitudProceso[] procesos;

    public ConfiguracionProcesos(int cycleDurationMs, int swapSizeBlocks, PoliticaDisco initialDiskPolicy,
                                 int headSpeed, int ramCapacity, PoliticaPlanificacion computerPolicy,
                                 int quantum, SolicitudProceso[] procesos) {
        this.cycleDurationMs = cycleDurationMs;
        this.swapSizeBlocks = swapSizeBlocks;
        this.initialDiskPolicy = initialDiskPolicy;
        this.headSpeed = headSpeed;
        this.ramCapacity = ramCapacity;
        this.computerPolicy = computerPolicy;
        this.quantum = quantum;
        this.procesos = procesos != null ? procesos.clone() : new SolicitudProceso[0];
    }

    public int getCycleDurationMs() {
        return cycleDurationMs;
    }

    public int getSwapSizeBlocks() {
        return swapSizeBlocks;
    }

    public PoliticaDisco getInitialDiskPolicy() {
        return initialDiskPolicy;
    }

    public int getHeadSpeed() {
        return headSpeed;
    }

    public int getRamCapacity() {
        return ramCapacity;
    }

    public PoliticaPlanificacion getComputerPolicy() {
        return computerPolicy;
    }

    public int getQuantum() {
        return quantum;
    }

    public SolicitudProceso[] getProcesos() {
        return procesos.clone();
    }

    // ================= lectura =================

    /**
     * Lee y valida el JSON de procesos y configuracion.
     * Lanza EntradaInvalidaException indicando el campo y/o proceso exacto si algo esta mal.
     */
    public static ConfiguracionProcesos desdeJson(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new EntradaInvalidaException("JSON: el texto está vacío");
        }
        Object raiz = LectorJson.leer(texto);
        if (!(raiz instanceof JsonObjeto)) {
            throw new EntradaInvalidaException("La configuración debe ser un objeto JSON { ... }");
        }
        JsonObjeto o = (JsonObjeto) raiz;

        // --- globalConfig ---
        if (!o.tiene("globalConfig")) {
            throw new EntradaInvalidaException("globalConfig: falta este objeto de configuración");
        }
        JsonObjeto gc = objeto(o.obtener("globalConfig"), "globalConfig");
        int cicloMs = entero(gc, "cycleDurationMs", "globalConfig.cycleDurationMs", MIN_DURACION_MS, MAX_DURACION_MS);
        int swapBloques = entero(gc, "swapSizeBlocks", "globalConfig.swapSizeBlocks", MIN_SWAP_BLOQUES, MAX_SWAP_BLOQUES);
        PoliticaDisco politicaDisco = politicaDisco(gc, "globalConfig.initialDiskPolicy");
        int velocidadCabezal = gc.tiene("headSpeed")
                ? entero(gc, "headSpeed", "globalConfig.headSpeed", MIN_VELOCIDAD_CABEZAL, MAX_VELOCIDAD_CABEZAL)
                : VELOCIDAD_CABEZAL_POR_DEFECTO;

        // --- computer ---
        if (!o.tiene("computer")) {
            throw new EntradaInvalidaException("computer: falta este objeto de configuración");
        }
        JsonObjeto comp = objeto(o.obtener("computer"), "computer");
        int ram = entero(comp, "ramCapacity", "computer.ramCapacity", 1, ValidadorEntradas.MAX_MEMORIA);
        PoliticaPlanificacion politicaCpu = politicaCpu(comp, "computer.policy");
        int quantum = 0;
        if (politicaCpu == PoliticaPlanificacion.ROUND_ROBIN) {
            quantum = comp.tiene("quantum")
                    ? entero(comp, "quantum", "computer.quantum", 1, 1000)
                    : 1;
        } else if (comp.tiene("quantum")) {
            // Si viene en otra politica, se valida que sea un numero positivo o cero
            quantum = entero(comp, "quantum", "computer.quantum", 0, 1000);
        }

        // --- processes ---
        if (!o.tiene("processes")) {
            throw new EntradaInvalidaException("processes: falta la lista de procesos");
        }
        Lista<Object> listaProcs = lista(o, "processes");
        SolicitudProceso[] procs = new SolicitudProceso[listaProcs.tamano()];
        for (int i = 0; i < procs.length; i++) {
            String etiqueta = "Proceso " + (i + 1);
            JsonObjeto p = objeto(listaProcs.obtener(i), "processes[" + i + "]");
            String idNombre = p.tiene("name") && p.obtener("name") != null ? p.obtener("name").toString() : "";
            if (!idNombre.isEmpty()) {
                etiqueta += " (" + idNombre + ")";
            }
            procs[i] = parsearProceso(p, etiqueta);
        }

        return new ConfiguracionProcesos(cicloMs, swapBloques, politicaDisco, velocidadCabezal,
                ram, politicaCpu, quantum, procs);
    }

    private static SolicitudProceso parsearProceso(JsonObjeto p, String prefijoError) {
        String nombreRaw = texto(p, "name", prefijoError + ": name");
        String nombre;
        try {
            nombre = ValidadorEntradas.nombre(nombreRaw);
        } catch (EntradaInvalidaException e) {
            throw new EntradaInvalidaException(prefijoError + ": " + e.getMessage());
        }

        int memoria = entero(p, "memory", prefijoError + ": memory", 1, ValidadorEntradas.MAX_MEMORIA);
        int deadline = entero(p, "deadline", prefijoError + ": deadline", 1, ValidadorEntradas.MAX_DEADLINE);
        int instrucciones = entero(p, "instructions", prefijoError + ": instructions", 1, ValidadorEntradas.MAX_INSTRUCCIONES);

        String ruta = texto(p, "targetPath", prefijoError + ": targetPath");
        if (ruta == null || !ruta.startsWith("/")) {
            throw new EntradaInvalidaException(prefijoError + ": targetPath debe comenzar con '/'");
        }

        TipoOperacionES operacion;
        String opStr = texto(p, "operation", prefijoError + ": operation");
        try {
            operacion = TipoOperacionES.desdeJson(opStr);
            if (!operacion.esDeArchivo()) {
                throw new EntradaInvalidaException(prefijoError + ": operation '" + opStr + "' no es una operación de archivo");
            }
        } catch (IllegalArgumentException e) {
            throw new EntradaInvalidaException(prefijoError + ": operation '" + opStr + "' no es válida (esperaba CREATE, READ, UPDATE o DELETE)");
        }

        int bloques = 0;
        if (operacion == TipoOperacionES.CREAR) {
            if (!p.tiene("blocks")) {
                throw new EntradaInvalidaException(prefijoError + ": blocks es obligatorio para la operación CREATE");
            }
            bloques = entero(p, "blocks", prefijoError + ": blocks", MIN_BLOQUES_ARCHIVO, MAX_BLOQUES_ARCHIVO);
        } else if (p.tiene("blocks")) {
            // En READ, UPDATE y DELETE se descarta o valida si viene
            bloques = 0;
        }

        String nuevoNombre = null;
        if (operacion == TipoOperacionES.ACTUALIZAR && p.tiene("newName")) {
            Object nn = p.obtener("newName");
            if (nn != null && !nn.toString().trim().isEmpty()) {
                nuevoNombre = nn.toString().trim();
            }
        }

        int prioridad = p.tiene("priority")
                ? entero(p, "priority", prefijoError + ": priority", 1, ValidadorEntradas.MAX_PRIORIDAD)
                : PRIORIDAD_POR_DEFECTO;

        String owner = p.tiene("owner")
                ? textoOpcional(p, "owner", DUENO_POR_DEFECTO)
                : DUENO_POR_DEFECTO;

        return SolicitudProceso.deEs(nombre, instrucciones, memoria, prioridad, deadline,
                operacion, ruta, bloques, nuevoNombre, owner);
    }

    // ================= escritura =================

    /**
     * Escribe la configuracion en formato JSON legible.
     * aJson() seguido de desdeJson() produce una configuracion equivalente.
     */
    public String aJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"globalConfig\": {\n");
        sb.append("    \"cycleDurationMs\": ").append(cycleDurationMs).append(",\n");
        sb.append("    \"swapSizeBlocks\": ").append(swapSizeBlocks).append(",\n");
        sb.append("    \"initialDiskPolicy\": \"").append(initialDiskPolicy.name().replace('_', '-')).append("\",\n");
        sb.append("    \"headSpeed\": ").append(headSpeed).append("\n");
        sb.append("  },\n");
        sb.append("  \"computer\": {\n");
        sb.append("    \"ramCapacity\": ").append(ramCapacity).append(",\n");
        sb.append("    \"policy\": \"").append(computerPolicy.name()).append("\",\n");
        sb.append("    \"quantum\": ").append(quantum).append("\n");
        sb.append("  },\n");
        sb.append("  \"processes\": [\n");
        for (int i = 0; i < procesos.length; i++) {
            SolicitudProceso p = procesos[i];
            sb.append("    {\n");
            sb.append("      \"name\": \"").append(escapar(p.getNombre())).append("\",\n");
            sb.append("      \"memory\": ").append(p.getMemoria()).append(",\n");
            sb.append("      \"deadline\": ").append(p.getDeadline()).append(",\n");
            sb.append("      \"instructions\": ").append(p.getInstrucciones()).append(",\n");
            sb.append("      \"targetPath\": \"").append(escapar(p.getRuta())).append("\",\n");
            sb.append("      \"operation\": \"").append(p.getOperacion().getNombreJson()).append("\"");
            if (p.getOperacion() == TipoOperacionES.CREAR) {
                sb.append(",\n      \"blocks\": ").append(p.getBloques());
            }
            if (p.getNuevoNombre() != null) {
                sb.append(",\n      \"newName\": \"").append(escapar(p.getNuevoNombre())).append("\"");
            }
            if (p.getPrioridad() != PRIORIDAD_POR_DEFECTO) {
                sb.append(",\n      \"priority\": ").append(p.getPrioridad());
            }
            if (p.getUsuario() != null && !p.getUsuario().equals(DUENO_POR_DEFECTO)) {
                sb.append(",\n      \"owner\": \"").append(escapar(p.getUsuario())).append("\"");
            }
            sb.append("\n    }").append(i < procesos.length - 1 ? "," : "").append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String escapar(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ================= metodos auxiliares de lectura =================

    private static Object requerido(JsonObjeto o, String clave, String campo) {
        if (!o.tiene(clave) || o.obtener(clave) == null) {
            throw new EntradaInvalidaException(campo + ": falta este campo obligatorio");
        }
        return o.obtener(clave);
    }

    private static int entero(JsonObjeto o, String clave, String campo, int min, int max) {
        Object v = requerido(o, clave, campo);
        if (!(v instanceof Long)) {
            throw new EntradaInvalidaException(campo + ": debe ser un número entero entre " + min + " y " + max);
        }
        long l = (Long) v;
        if (l < min || l > max) {
            throw new EntradaInvalidaException(campo + ": debe estar entre " + min + " y " + max + " (se recibió " + l + ")");
        }
        return (int) l;
    }

    private static String texto(JsonObjeto o, String clave, String campo) {
        Object v = requerido(o, clave, campo);
        if (!(v instanceof String)) {
            throw new EntradaInvalidaException(campo + ": debe ser un texto");
        }
        String s = ((String) v).trim();
        if (s.isEmpty()) {
            throw new EntradaInvalidaException(campo + ": no puede estar vacío");
        }
        return s;
    }

    private static String textoOpcional(JsonObjeto o, String clave, String porDefecto) {
        if (!o.tiene(clave) || o.obtener(clave) == null) {
            return porDefecto;
        }
        Object v = o.obtener(clave);
        if (!(v instanceof String)) {
            return porDefecto;
        }
        String s = ((String) v).trim();
        return s.isEmpty() ? porDefecto : s;
    }

    private static JsonObjeto objeto(Object valor, String campo) {
        if (!(valor instanceof JsonObjeto)) {
            throw new EntradaInvalidaException(campo + ": se esperaba un objeto JSON { ... }");
        }
        return (JsonObjeto) valor;
    }

    @SuppressWarnings("unchecked")
    private static Lista<Object> lista(JsonObjeto o, String clave) {
        Object v = requerido(o, clave, clave);
        if (!(v instanceof Lista)) {
            throw new EntradaInvalidaException(clave + ": se esperaba una lista [ ... ]");
        }
        return (Lista<Object>) v;
    }

    private static PoliticaDisco politicaDisco(JsonObjeto o, String campo) {
        String t = texto(o, "initialDiskPolicy", campo);
        try {
            return PoliticaDisco.desdeJson(t);
        } catch (IllegalArgumentException e) {
            throw new EntradaInvalidaException(campo + ": política no válida: '" + t + "' (esperaba FIFO, SSTF, SCAN o C-SCAN)");
        }
    }

    private static PoliticaPlanificacion politicaCpu(JsonObjeto o, String campo) {
        String t = texto(o, "policy", campo).trim().toUpperCase();
        if (t.equals("FCFS")) {
            return PoliticaPlanificacion.FCFS;
        }
        if (t.equals("RR") || t.equals("ROUND_ROBIN") || t.equals("ROUND ROBIN")) {
            return PoliticaPlanificacion.ROUND_ROBIN;
        }
        if (t.equals("PRIORITY") || t.equals("PRIORIDADES")) {
            return PoliticaPlanificacion.PRIORIDADES;
        }
        if (t.equals("EDF")) {
            return PoliticaPlanificacion.EDF;
        }
        throw new EntradaInvalidaException(campo + ": política de CPU no válida: '" + t + "' (esperaba FCFS, RR/ROUND_ROBIN, PRIORITY/PRIORIDADES o EDF)");
    }
}
