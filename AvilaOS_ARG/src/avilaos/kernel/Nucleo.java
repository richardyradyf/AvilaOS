package avilaos.kernel;

import avilaos.hardware.Cpu;
import avilaos.hardware.Ram;
import avilaos.hardware.Relojable;
import avilaos.model.EstadoProceso;
import avilaos.model.ModoEjecucion;
import avilaos.model.MotivoBloqueo;
import avilaos.model.Pcb;
import avilaos.scheduler.Planificador;
import avilaos.util.Bitacora;
import avilaos.util.Cola;
import avilaos.util.Iterador;
import avilaos.util.Lista;
import java.util.concurrent.Semaphore;
import avilaos.disco.OyenteDisco;
import avilaos.disco.PuertoEs;
import avilaos.disco.SolicitudDisco;

/**
 * @author richard
 * @author Gianm
 * Nucleo del sistema operativo de UN computador. Maneja las colas del modelo de
 * siete estados (nuevos, listos, bloqueados, listos/suspendidos,
 * bloqueados/suspendidos y terminados), la admision segun la memoria y el despacho
 * de procesos a la CPU con la politica de planificacion que tenga conectada.
 *
 * Debe registrarse en el reloj DESPUES de la CPU: en cada ciclo la CPU ejecuta una
 * instruccion y luego el nucleo decide (terminar, expulsar, admitir, despachar).
 *
 * Las colas las tocan varios hilos (reloj, interfaz, usuario creando procesos), asi
 * que todo se protege con un semaforo mutex de Java. Los metodos publicos toman el
 * mutex; los privados asumen que ya esta tomado (el semaforo no es reentrante).
 */
public class Nucleo implements Relojable, OyenteDisco {
 
    private final int idComputador;
    private final Ram ram;
    private final Cpu cpu;
    private final Bitacora bitacora;
    private Planificador planificador;
 
    private final Cola<Pcb> nuevos = new Cola<>();
    private final Cola<Pcb> listos = new Cola<>();
    private final Lista<Pcb> bloqueados = new Lista<>();
    private final Cola<Pcb> listosSuspendidos = new Cola<>();
    private final Lista<Pcb> bloqueadosSuspendidos = new Lista<>();
    private final Cola<Pcb> terminados = new Cola<>();
 
    private final Semaphore mutex = new Semaphore(1);
    private long cicloActual = 0;
    private int ciclosEnCpu = 0;
    /** PC del proceso en CPU la ultima vez que el nucleo lo reviso: detecta si ejecuto algo. */
    private int pcRevisado = 0;
    private ControladorInstrucciones controlador;
    private PuertoEs puertoEs;
 
    public Nucleo(int idComputador, Ram ram, Cpu cpu, Planificador planificador, Bitacora bitacora) {
        if (idComputador < 1) {
            throw new IllegalArgumentException("El computador debe ser 1 o mayor");
        }
        if (ram == null || cpu == null || planificador == null || bitacora == null) {
            throw new IllegalArgumentException("RAM, CPU, planificador y bitacora son obligatorios");
        }
        this.idComputador = idComputador;
        this.ram = ram;
        this.cpu = cpu;
        this.planificador = planificador;
        this.bitacora = bitacora;
    }
 
    // ================= operaciones publicas =================
 
    /** Un proceso nuevo llega al computador: entra a la cola de nuevos y se intenta admitir. */
    public void agregarProceso(Pcb proceso) {
        if (proceso == null) {
            throw new IllegalArgumentException("El proceso no puede ser nulo");
        }
        if (proceso.getIdComputador() != idComputador) {
            throw new IllegalArgumentException(proceso.getEtiqueta() + " pertenece al computador "
                    + proceso.getIdComputador() + ", no al " + idComputador);
        }
        if (proceso.getEstado() != EstadoProceso.NUEVO) {
            throw new IllegalArgumentException("Solo se agregan procesos en estado NUEVO");
        }
        if (proceso.getMemoria() > ram.getCapacidad()) {
            throw new IllegalArgumentException(proceso.getEtiqueta() + " requiere " + proceso.getMemoria()
                    + " MB y la RAM del computador " + idComputador + " es de " + ram.getCapacidad() + " MB");
        }
        mutex.acquireUninterruptibly();
        try {
            nuevos.encolar(proceso);
            registrar(proceso.getEtiqueta() + " (" + proceso.getNombre()
                    + ") llega a la cola de nuevos del computador " + idComputador);
            admitirPendientes();
        } finally {
            mutex.release();
        }
    }
 
    /**
     * Saca de la CPU al proceso en ejecucion y lo pasa a BLOQUEADO con el motivo
     * indicado (semWait sin permiso, espera de E/S, acceso remoto...). La CPU queda
     * ociosa y el nucleo despacha a otro proceso al final del ciclo.
     */
    public Pcb bloquearProcesoEnCpu(MotivoBloqueo motivo) {
        mutex.acquireUninterruptibly();
        try {
            Pcb proceso = cpu.getProceso();
            if (proceso == null) {
                throw new IllegalStateException("La CPU esta ociosa: no hay proceso que bloquear");
            }
            cpu.descargar();
            proceso.bloquear(motivo);
            bloqueados.agregarAlFinal(proceso);
            registrar(proceso.getEtiqueta() + " se bloquea: " + motivo);
            return proceso;
        } finally {
            mutex.release();
        }
    }
 
      /**
     * Ocurre el evento que el proceso esperaba. Si estaba bloqueado pasa a listos;
     * si estaba bloqueado/suspendido pasa a listos/suspendidos (sigue en el swap).
     */
    public void desbloquear(Pcb proceso) {
        mutex.acquireUninterruptibly();
        try {
            if (proceso != null && bloqueados.eliminar(proceso)) {
                pasarBloqueadoAListos(proceso);
            } else if (proceso != null && bloqueadosSuspendidos.eliminar(proceso)) {
                pasarAListosSuspendidos(proceso);
            } else {
                throw new IllegalArgumentException("El proceso no esta bloqueado en el computador " + idComputador);
            }
        } finally {
            mutex.release();
        }
    }

    /**
     * Planificador a mediano plazo: saca de la RAM a un proceso listo o bloqueado,
     * lo pasa a su cola de suspendidos y libera su memoria.
     */
    public void suspender(Pcb proceso) {
        mutex.acquireUninterruptibly();
        try {
            if (proceso != null && listos.contiene(proceso)) {
                listos.eliminar(proceso);
                proceso.suspender();
                listosSuspendidos.encolar(proceso);
            } else if (proceso != null && bloqueados.contiene(proceso)) {
                bloqueados.eliminar(proceso);
                proceso.suspender();
                bloqueadosSuspendidos.agregarAlFinal(proceso);
            } else {
                throw new IllegalArgumentException("El proceso no esta listo ni bloqueado en el computador "
                        + idComputador);
            }
            ram.liberar(proceso.getMemoria());
            registrar(proceso.getEtiqueta() + " se suspende y libera " + proceso.getMemoria()
                    + " MB (RAM " + ram + ")");
        } finally {
            mutex.release();
        }
    }

    /**
     * Vuelve a cargar en la RAM a un proceso suspendido: listo/suspendido pasa a listos
     * y bloqueado/suspendido a bloqueados. Devuelve false si no cabe en la RAM.
     */
    public boolean reactivar(Pcb proceso) {
        mutex.acquireUninterruptibly();
        try {
            boolean estabaListo = proceso != null && listosSuspendidos.contiene(proceso);
            boolean estabaBloqueado = proceso != null && bloqueadosSuspendidos.contiene(proceso);
            if (!estabaListo && !estabaBloqueado) {
                throw new IllegalArgumentException("El proceso no esta suspendido en el computador " + idComputador);
            }
            if (!ram.reservar(proceso.getMemoria())) {
                return false;
            }
            proceso.reactivar();
            if (estabaListo) {
                listosSuspendidos.eliminar(proceso);
                planificador.alEntrarAListos(listos, proceso);
            } else {
                bloqueadosSuspendidos.eliminar(proceso);
                bloqueados.agregarAlFinal(proceso);
            }
            registrar(proceso.getEtiqueta() + " se reactiva y reserva " + proceso.getMemoria()
                    + " MB (RAM " + ram + ")");
            return true;
        } finally {
            mutex.release();
        }
    }
     /**
     * Conecta el puerto por el que los procesos de E/S piden el disco. Sin puerto,
     * los procesos de E/S se ejecutan como procesos normales.
     */
    public void setPuertoEs(PuertoEs puertoEs) {
        mutex.acquireUninterruptibly();
        try {
            this.puertoEs = puertoEs;
        } finally {
            mutex.release();
        }
    }

    /**
     * Aviso del disco: termino la solicitud de un proceso de E/S. Si el proceso estaba
     * bloqueado vuelve a listos (o termina, si ya no le quedan instrucciones); si estaba
     * bloqueado/suspendido pasa a listos/suspendidos. Si ya no esta esperando (por
     * ejemplo, termino por deadline), el aviso se ignora.
     */
    
    @Override
    public void solicitudAtendida(SolicitudDisco solicitud, long ciclo) {
        Pcb proceso = solicitud.getProceso();
        if (proceso == null) {
            return;
        }
        mutex.acquireUninterruptibly();
        try {
            if (bloqueados.eliminar(proceso)) {
                completarOperacion(proceso, solicitud);
                proceso.desbloquear();
                if (proceso.completoSusInstrucciones()) {
                    proceso.terminar(ciclo, false);
                    ram.liberar(proceso.getMemoria());
                    terminados.encolar(proceso);
                    avisarTerminado(proceso);
                    registrar(proceso.getEtiqueta() + " termina y libera " + proceso.getMemoria() + " MB");
                } else {
                    planificador.alEntrarAListos(listos, proceso);
                    registrar(proceso.getEtiqueta() + " se desbloquea y pasa a listos");
                }
            } else if (bloqueadosSuspendidos.eliminar(proceso)) {
                completarOperacion(proceso, solicitud);
                pasarAListosSuspendidos(proceso);
            }
        } finally {
            mutex.release();
        }
    }
        
    /** Hace la operacion en el sistema de archivos y anota el resultado en la bitacora. */
    private void completarOperacion(Pcb proceso, SolicitudDisco solicitud) {
        String resultado = puertoEs != null ? puertoEs.completarOperacion(solicitud) : "sin sistema de archivos";
        registrar(proceso.getEtiqueta() + ": el disco atendio " + solicitud.getOperacion()
                + " " + solicitud.getRuta() + " -> " + resultado);
    }
    
    /**
     * Conecta un controlador que interpreta las instrucciones de los procesos
     * (por ejemplo, el programa del productor/consumidor). Es opcional: sin
     * controlador, todas las instrucciones son de usuario y no tienen efectos.
     */
    public void setControlador(ControladorInstrucciones controlador) {
        mutex.acquireUninterruptibly();
        try {
            this.controlador = controlador;
        } finally {
            mutex.release();
        }
    }
 
    /**
     * Cambia el motivo de bloqueo de un proceso que ya esta bloqueado en este
     * computador (sigue en la cola de bloqueados).
     */
    public void cambiarMotivoBloqueo(Pcb proceso, MotivoBloqueo motivo) {
        mutex.acquireUninterruptibly();
        try {
            if (proceso == null || !bloqueados.contiene(proceso)) {
                throw new IllegalArgumentException("El proceso no esta bloqueado en el computador " + idComputador);
            }
            proceso.cambiarMotivoBloqueo(motivo);
            registrar(proceso.getEtiqueta() + " sigue bloqueado: " + motivo);
        } finally {
            mutex.release();
        }
    }
 
    /** Cambia la politica en tiempo de ejecucion; la cola de listos se reordena. */
    public void cambiarPlanificador(Planificador nuevo) {
        if (nuevo == null) {
            throw new IllegalArgumentException("El planificador no puede ser nulo");
        }
        mutex.acquireUninterruptibly();
        try {
            planificador = nuevo;
            planificador.reordenar(listos);
            registrar("Computador " + idComputador + " cambia su politica de planificacion a "
                    + nuevo.getTipo());
        } finally {
            mutex.release();
        }
    }
 
    // ================= reloj =================
 
    /** Un ciclo, DESPUES de que la CPU ejecuto su instruccion. */
    @Override
    public void alCiclo(long numeroCiclo) {
        mutex.acquireUninterruptibly();
        try {
            cicloActual = numeroCiclo;
            revisarProcesoEnCpu();
            terminarProcesosConDeadlineVencido();
            completarEntradasSalidas();
            admitirPendientes();
            despacharSiOciosa();
        } finally {
            mutex.release();
        }
    }
 
    // ================= consulta (copias para la interfaz) =================
 
    public Pcb[] getNuevos() {
        mutex.acquireUninterruptibly();
        try {
            return aArreglo(nuevos);
        } finally {
            mutex.release();
        }
    }
 
    public Pcb[] getListos() {
        mutex.acquireUninterruptibly();
        try {
            return aArreglo(listos);
        } finally {
            mutex.release();
        }
    }
 
    public Pcb[] getBloqueados() {
        mutex.acquireUninterruptibly();
        try {
            return aArreglo(bloqueados);
        } finally {
            mutex.release();
        }
    }
    
        public Pcb[] getListosSuspendidos() {
        mutex.acquireUninterruptibly();
        try {
            return aArreglo(listosSuspendidos);
        } finally {
            mutex.release();
        }
    }

    public Pcb[] getBloqueadosSuspendidos() {
        mutex.acquireUninterruptibly();
        try {
            return aArreglo(bloqueadosSuspendidos);
        } finally {
            mutex.release();
        }
    }
 
    public Pcb[] getTerminados() {
        mutex.acquireUninterruptibly();
        try {
            return aArreglo(terminados);
        } finally {
            mutex.release();
        }
    }
 
    public Planificador getPlanificador() {
        mutex.acquireUninterruptibly();
        try {
            return planificador;
        } finally {
            mutex.release();
        }
    }
 
    public int getIdComputador() {
        return idComputador;
    }
 
    public Ram getRam() {
        return ram;
    }
 
    public Cpu getCpu() {
        return cpu;
    }
 
    // ================= privados (el mutex ya esta tomado) =================
 
    /** Termina o expulsa al proceso que acaba de ejecutar una instruccion. */
    private void revisarProcesoEnCpu() {
        Pcb proceso = cpu.getProceso();
        if (proceso == null) {
            return;
        }
        ciclosEnCpu++;
        boolean ejecutoInstruccion = proceso.getPc() > pcRevisado;
        pcRevisado = proceso.getPc();
        if (controlador != null && ejecutoInstruccion) {
            MotivoBloqueo motivo = controlador.despuesDeEjecutar(proceso, cicloActual);
            if (motivo != null) {
                cpu.descargar();
                proceso.bloquear(motivo);
                bloqueados.agregarAlFinal(proceso);
                registrar(proceso.getEtiqueta() + " se bloquea: " + motivo);
                return;
            }
        }
        if (ejecutoInstruccion && proceso.esDeArchivo() && !proceso.yaPidioDisco() && puertoEs != null) {
            cpu.descargar();
            proceso.marcarDiscoPedido();
            proceso.bloquear(MotivoBloqueo.ESPERA_DISCO);
            bloqueados.agregarAlFinal(proceso);
            SolicitudDisco s = puertoEs.pedirDisco(proceso, cicloActual);
            registrar(proceso.getEtiqueta() + " pide el disco (" + s.getOperacion() + " " + s.getRuta()
                    + ", bloque " + s.getBloqueDestino() + ") y se bloquea");
            return;
        }
        if (proceso.completoSusInstrucciones()) {
            cpu.descargar();
            proceso.terminar(cicloActual, false);
            ram.liberar(proceso.getMemoria());
            terminados.encolar(proceso);
            avisarTerminado(proceso);
            registrar(proceso.getEtiqueta() + " termina y libera " + proceso.getMemoria() + " MB");
        } else if (ejecutoInstruccion && proceso.debePedirEs()) {
            cpu.descargar();
            proceso.bloquear(MotivoBloqueo.ESPERA_ES);
            proceso.iniciarEs(cicloActual);
            bloqueados.agregarAlFinal(proceso);
            registrar(proceso.getEtiqueta() + " pide E/S (" + proceso.getCiclosEs() + " ciclos) y se bloquea");
        } else if (planificador.debeExpulsar(proceso, ciclosEnCpu, listos)) {
            cpu.descargar();
            proceso.expulsar();
            planificador.alEntrarAListos(listos, proceso);
            registrar(proceso.getEtiqueta() + " es expulsado de la CPU");
        } else {
            aplicarModoSiguiente(proceso);
        }
    }
 
    /** Un proceso bloqueado vuelve a listos (ya fue sacado de la cola de bloqueados). */
    private void pasarBloqueadoAListos(Pcb proceso) {
        proceso.desbloquear();
        planificador.alEntrarAListos(listos, proceso);
        registrar(proceso.getEtiqueta() + " se desbloquea y pasa a listos");
    }
    
        /** El evento llega estando suspendido: sigue en el swap, pero ya esta listo. */
    private void pasarAListosSuspendidos(Pcb proceso) {
        proceso.desbloquear();
        listosSuspendidos.encolar(proceso);
        registrar(proceso.getEtiqueta() + " recibe su evento estando suspendido y pasa a listos/suspendidos");
    }
 
    /** Los procesos cuya E/S ya termino dejan de estar bloqueados. */
    private void completarEntradasSalidas() {
        int i = 0;
        while (i < bloqueados.tamano()) {
            Pcb p = bloqueados.obtener(i);
            if (p.getMotivoBloqueo() == MotivoBloqueo.ESPERA_ES && p.esTerminada(cicloActual)) {
                bloqueados.eliminar(p);   // no se avanza i: el siguiente ocupo este lugar
                pasarBloqueadoAListos(p);
            } else {
                i++;
            }
        }
                i = 0;
        while (i < bloqueadosSuspendidos.tamano()) {
            Pcb p = bloqueadosSuspendidos.obtener(i);
            if (p.getMotivoBloqueo() == MotivoBloqueo.ESPERA_ES && p.esTerminada(cicloActual)) {
                bloqueadosSuspendidos.eliminar(p);
                pasarAListosSuspendidos(p);
            } else {
                i++;
            }
        }
    }
 
    /**
     * Termina a todo proceso cuyo deadline llego a 0 (en CPU, listos, bloqueados o
     * nuevos). Si un proceso termina normalmente en el mismo ciclo, ya fue tratado antes
     * y no se cuenta como fallo de deadline.
     */
    private void terminarProcesosConDeadlineVencido() {
        Pcb enCpu = cpu.getProceso();
        if (enCpu != null && enCpu.deadlineVencido(cicloActual)) {
            cpu.descargar();
            terminarPorDeadline(enCpu, true);
        }
        int i = 0;
        while (i < listos.tamano()) {
            Pcb p = listos.obtener(i);
            if (p.deadlineVencido(cicloActual)) {
                listos.eliminar(p);
                terminarPorDeadline(p, true);
            } else {
                i++;
            }
        }
        i = 0;
        while (i < bloqueados.tamano()) {
            Pcb p = bloqueados.obtener(i);
            if (p.deadlineVencido(cicloActual)) {
                bloqueados.eliminar(p);
                terminarPorDeadline(p, true);
            } else {
                i++;
            }
        }
        i = 0;
        while (i < nuevos.tamano()) {
            Pcb p = nuevos.obtener(i);
            if (p.deadlineVencido(cicloActual)) {
                nuevos.eliminar(p);
                terminarPorDeadline(p, false);   // nunca tuvo memoria
            } else {
                i++;
            }
        }
                i = 0;
        while (i < listosSuspendidos.tamano()) {
            Pcb p = listosSuspendidos.obtener(i);
            if (p.deadlineVencido(cicloActual)) {
                listosSuspendidos.eliminar(p);
                terminarPorDeadline(p, false);   // esta en el swap: no tiene RAM
            } else {
                i++;
            }
        }
        i = 0;
        while (i < bloqueadosSuspendidos.tamano()) {
            Pcb p = bloqueadosSuspendidos.obtener(i);
            if (p.deadlineVencido(cicloActual)) {
                bloqueadosSuspendidos.eliminar(p);
                terminarPorDeadline(p, false);
            } else {
                i++;
            }
        }
    }
 
    private void terminarPorDeadline(Pcb proceso, boolean teniaMemoria) {
        proceso.terminar(cicloActual, true);
        if (teniaMemoria) {
            ram.liberar(proceso.getMemoria());
        }
        terminados.encolar(proceso);
        avisarTerminado(proceso);
        registrar(proceso.getEtiqueta() + " excede su deadline y se termina"
                + (teniaMemoria ? ", libera " + proceso.getMemoria() + " MB" : ""));
    }
 
    private void avisarTerminado(Pcb proceso) {
        if (controlador != null) {
            controlador.alTerminar(proceso);
        }
    }
 
    /** Si hay controlador, la CPU ejecuta la proxima instruccion en el modo que corresponde. */
    private void aplicarModoSiguiente(Pcb proceso) {
        if (controlador == null) {
            return;
        }
        ModoEjecucion modo = controlador.modoSiguiente(proceso);
        if (modo != null) {
            cpu.setModo(modo);
        }
    }
 
    /** Planificacion a largo plazo: admite los procesos nuevos que caben en la RAM. */
    private void admitirPendientes() {
        int i = 0;
        while (i < nuevos.tamano()) {
            Pcb proceso = nuevos.obtener(i);
            if (ram.reservar(proceso.getMemoria())) {
                nuevos.eliminar(proceso);   // no se avanza i: el siguiente ocupo este lugar
                proceso.admitir();
                planificador.alEntrarAListos(listos, proceso);
                registrar(proceso.getEtiqueta() + " admitido: reserva " + proceso.getMemoria()
                        + " MB (RAM " + ram + ")");
            } else {
                i++;
            }
        }
    }
 
    /** Planificacion a corto plazo: si la CPU esta libre, carga el proximo proceso. */
    private void despacharSiOciosa() {
        if (!cpu.estaOciosa()) {
            return;
        }
        Pcb proceso = planificador.seleccionar(listos);
        if (proceso == null) {
            return;
        }
        proceso.despachar(cicloActual + 1);   // empieza a ejecutar en el ciclo siguiente
        cpu.cargar(proceso);
        ciclosEnCpu = 0;
        pcRevisado = proceso.getPc();
        aplicarModoSiguiente(proceso);
        registrar("Computador " + idComputador + " selecciona el proceso " + proceso.getEtiqueta()
                + " (" + planificador.getTipo() + ")");
    }
 
    private void registrar(String texto) {
        bitacora.registrar(cicloActual, texto);
    }
 
    private static Pcb[] aArreglo(Cola<Pcb> cola) {
        Pcb[] copia = new Pcb[cola.tamano()];
        Iterador<Pcb> it = cola.iterador();
        int i = 0;
        while (it.tieneSiguiente()) {
            copia[i++] = it.siguiente();
        }
        return copia;
    }
 
    private static Pcb[] aArreglo(Lista<Pcb> lista) {
        Pcb[] copia = new Pcb[lista.tamano()];
        Iterador<Pcb> it = lista.iterador();
        int i = 0;
        while (it.tieneSiguiente()) {
            copia[i++] = it.siguiente();
        }
        return copia;
    }
}