package avilaos.sync;

import avilaos.hardware.Relojable;
import avilaos.kernel.ControladorInstrucciones;
import avilaos.kernel.Nucleo;
import avilaos.model.EstadoProceso;
import avilaos.model.ModoEjecucion;
import avilaos.model.MotivoBloqueo;
import avilaos.model.Pcb;
import avilaos.model.TipoProceso;
import avilaos.util.Bitacora;
import avilaos.util.Iterador;
import avilaos.util.Lista;
import java.util.concurrent.Semaphore;

/**
 * @author Gianm
 * Une los procesos productores y consumidores con sus buffers. Hace dos papeles:
 *
 * 1) ControladorInstrucciones: despues de cada instruccion que ejecuta un productor
 *    o consumidor, decide que operacion del programa le tocaba (semWait, semSignal,
 *    añadir...) y la ejecuta sobre su buffer. Si el semaforo no deja pasar, le dice
 *    al nucleo que bloquee al proceso.
 * 2) Relojable: se registra en el reloj DESPUES de todos los nucleos. En cada ciclo
 *    completa los accesos remotos cuya latencia ya paso y despierta a los procesos
 *    que otro proceso libero con un semSignal.
 *
 * Acceso remoto: si el buffer esta en otro computador, cada operacion del sistema
 * operativo sobre el buffer viaja por la red: el proceso queda BLOQUEADO con motivo
 * ACCESO_REMOTO durante latenciaRed ciclos y la operacion se aplica al llegar.
 *
 * Los despertares no se hacen dentro del nucleo (tiene su mutex tomado): se anotan
 * y se ejecutan al final del ciclo, cuando ningun nucleo esta trabajando.
 */
public class GestorBuffers implements ControladorInstrucciones, Relojable {
 
    private static class Asignacion {
        final Pcb proceso;
        final BufferAcotado buffer;
        final int cadaCiclos;
 
        Asignacion(Pcb proceso, BufferAcotado buffer, int cadaCiclos) {
            this.proceso = proceso;
            this.buffer = buffer;
            this.cadaCiclos = cadaCiclos;
        }
    }
 
    private static class AccesoRemoto {
        final Pcb proceso;
        final BufferAcotado buffer;
        final OperacionBuffer operacion;
        final long cicloLlegada;
 
        AccesoRemoto(Pcb proceso, BufferAcotado buffer, OperacionBuffer operacion, long cicloLlegada) {
            this.proceso = proceso;
            this.buffer = buffer;
            this.operacion = operacion;
            this.cicloLlegada = cicloLlegada;
        }
    }
 
    private final Bitacora bitacora;
    private volatile int latenciaRed;
 
    private final Lista<Nucleo> nucleos = new Lista<>();
    private final Lista<BufferAcotado> buffers = new Lista<>();
    private final Lista<Asignacion> asignaciones = new Lista<>();
    private final Lista<AccesoRemoto> enViaje = new Lista<>();
    private final Lista<Pcb> porDespertar = new Lista<>();
    private final Semaphore mutex = new Semaphore(1);
    private int siguienteIdBuffer = 1;
 
    /** @param latenciaRed ciclos que tarda cada acceso remoto (0 = sin latencia) */
    public GestorBuffers(int latenciaRed, Bitacora bitacora) {
        validarLatencia(latenciaRed);
        if (bitacora == null) {
            throw new IllegalArgumentException("La bitacora es obligatoria");
        }
        this.latenciaRed = latenciaRed;
        this.bitacora = bitacora;
    }
 
    // ================= configuracion =================
 
    /** Conecta el nucleo de un computador: este gestor pasa a ser su controlador. */
    public void registrarNucleo(Nucleo nucleo) {
        if (nucleo == null) {
            throw new IllegalArgumentException("El nucleo no puede ser nulo");
        }
        mutex.acquireUninterruptibly();
        try {
            nucleos.agregarAlFinal(nucleo);
        } finally {
            mutex.release();
        }
        nucleo.setControlador(this);
    }
 
    /**
     * Crea un buffer en la RAM del computador anfitrion (1 MB por espacio).
     *
     * @throws IllegalStateException si la RAM del anfitrion no alcanza
     */
    public BufferAcotado crearBuffer(int idComputadorAnfitrion, int capacidad) {
        Nucleo anfitrion = nucleoDe(idComputadorAnfitrion);
        if (!anfitrion.getRam().reservar(capacidad)) {
            throw new IllegalStateException("El computador " + idComputadorAnfitrion
                    + " no tiene " + capacidad + " MB libres para el buffer");
        }
        BufferAcotado buffer;
        mutex.acquireUninterruptibly();
        try {
            buffer = new BufferAcotado(siguienteIdBuffer++, capacidad, idComputadorAnfitrion);
            buffers.agregarAlFinal(buffer);
        } finally {
            mutex.release();
        }
        bitacora.registrar(0, "Buffer " + buffer.getEtiqueta() + " creado en el computador "
                + idComputadorAnfitrion + " (" + capacidad + " espacios, " + capacidad + " MB)");
        return buffer;
    }
 
    /**
     * Indica que el proceso (productor o consumidor) trabaja sobre ese buffer.
     * El proceso debe tener (elementos x (cadaCiclos + 5)) instrucciones; usar
     * ProgramaBuffer.instruccionesTotales para calcularlas.
     */
    public void asignar(Pcb proceso, int idBuffer, int cadaCiclos) {
        if (proceso == null) {
            throw new IllegalArgumentException("El proceso no puede ser nulo");
        }
        if (proceso.getTipo() != TipoProceso.PRODUCTOR && proceso.getTipo() != TipoProceso.CONSUMIDOR) {
            throw new IllegalArgumentException(proceso.getEtiqueta() + " no es productor ni consumidor");
        }
        ProgramaBuffer.periodo(cadaCiclos);   // valida cadaCiclos >= 1
        BufferAcotado buffer = getBuffer(idBuffer);
        mutex.acquireUninterruptibly();
        try {
            if (buscar(proceso) != null) {
                throw new IllegalArgumentException(proceso.getEtiqueta() + " ya tiene un buffer asignado");
            }
            asignaciones.agregarAlFinal(new Asignacion(proceso, buffer, cadaCiclos));
        } finally {
            mutex.release();
        }
    }
 
    public void setLatenciaRed(int ciclos) {
        validarLatencia(ciclos);
        this.latenciaRed = ciclos;
    }
 
    public int getLatenciaRed() {
        return latenciaRed;
    }
 
    // ================= ControladorInstrucciones =================
 
    @Override
    public ModoEjecucion modoSiguiente(Pcb proceso) {
        mutex.acquireUninterruptibly();
        try {
            Asignacion a = buscar(proceso);
            if (a == null || proceso.completoSusInstrucciones()) {
                return null;
            }
            return ProgramaBuffer.operacionEn(proceso.getTipo(), proceso.getPc(), a.cadaCiclos).getModo();
        } finally {
            mutex.release();
        }
    }
 
    @Override
    public MotivoBloqueo despuesDeEjecutar(Pcb proceso, long ciclo) {
        mutex.acquireUninterruptibly();
        try {
            Asignacion a = buscar(proceso);
            if (a == null) {
                return null;
            }
            OperacionBuffer operacion =
                    ProgramaBuffer.operacionEn(proceso.getTipo(), proceso.getPc() - 1, a.cadaCiclos);
            if (!operacion.esModoSistemaOperativo()) {
                return null;   // producir y consumir son trabajo de usuario
            }
            boolean remoto = a.buffer.getIdComputadorAnfitrion() != proceso.getIdComputador()
                    && latenciaRed > 0;
            if (remoto) {
                enViaje.agregarAlFinal(new AccesoRemoto(proceso, a.buffer, operacion, ciclo + latenciaRed));
                bitacora.registrar(ciclo, proceso.getEtiqueta() + " envia " + operacion + " a "
                        + a.buffer.getEtiqueta() + " (computador " + a.buffer.getIdComputadorAnfitrion()
                        + "), llegara en el ciclo " + (ciclo + latenciaRed));
                return MotivoBloqueo.ACCESO_REMOTO;
            }
            ResultadoOperacion r = a.buffer.ejecutar(operacion, proceso);
            registrarOperacion(ciclo, proceso, a.buffer, operacion, r);
            if (r.getDespertado() != null) {
                porDespertar.agregarAlFinal(r.getDespertado());
            }
            return r.bloqueado() ? r.getMotivo() : null;
        } finally {
            mutex.release();
        }
    }
 
    /**
     * Un proceso termino (normal o por deadline): se le saca de las colas de los
     * semaforos, se descartan sus accesos en viaje y, si moria con la region critica
     * tomada, se libera el mutex para que no quede cerrado para siempre.
     *
     * Limitacion: si moria a mitad del protocolo (por ejemplo despues de semWait(e)
     * y antes de semSignal(n)), el buffer puede perder un espacio. Se anota en el informe.
     */
    @Override
    public void alTerminar(Pcb proceso) {
        mutex.acquireUninterruptibly();
        try {
            Asignacion a = buscar(proceso);
            if (a == null) {
                return;
            }
            OperacionBuffer pendiente = null;
            int i = 0;
            while (i < enViaje.tamano()) {
                AccesoRemoto viaje = enViaje.obtener(i);
                if (viaje.proceso == proceso) {
                    pendiente = viaje.operacion;
                    enViaje.eliminar(viaje);
                } else {
                    i++;
                }
            }
            porDespertar.eliminar(proceso);
            boolean esperabaMutex = a.buffer.getMutex().retirar(proceso);
            a.buffer.getHayElementos().retirar(proceso);
            a.buffer.getHayEspacio().retirar(proceso);
 
            int ultimaAplicada = proceso.getPc() - 1 - (pendiente != null ? 1 : 0);
            if (!esperabaMutex && ultimaAplicada >= 0) {
                OperacionBuffer op = ProgramaBuffer.operacionEn(proceso.getTipo(), ultimaAplicada, a.cadaCiclos);
                boolean teniaMutex = op == OperacionBuffer.SEM_WAIT_MUTEX
                        || op == OperacionBuffer.ANADIR || op == OperacionBuffer.EXTRAER;
                if (teniaMutex) {
                    Pcb siguiente = a.buffer.getMutex().senalar();
                    if (siguiente != null) {
                        porDespertar.agregarAlFinal(siguiente);
                    }
                    bitacora.registrar(0, proceso.getEtiqueta() + " termino con la region critica de "
                            + a.buffer.getEtiqueta() + " tomada: se libera el mutex");
                }
            }
            asignaciones.eliminar(a);
        } finally {
            mutex.release();
        }
    }
 
    // ================= reloj =================
 
    /** Va DESPUES de todos los nucleos: completa accesos remotos y despierta procesos. */
    @Override
    public void alCiclo(long numeroCiclo) {
        AccesoRemoto[] llegados = extraerLlegados(numeroCiclo);
        for (AccesoRemoto acceso : llegados) {
            ResultadoOperacion r = acceso.buffer.ejecutar(acceso.operacion, acceso.proceso);
            registrarOperacion(numeroCiclo, acceso.proceso, acceso.buffer, acceso.operacion, r);
            mutex.acquireUninterruptibly();
            try {
                if (r.getDespertado() != null) {
                    porDespertar.agregarAlFinal(r.getDespertado());
                }
                if (r.continua()) {
                    porDespertar.agregarAlFinal(acceso.proceso);
                }
            } finally {
                mutex.release();
            }
            if (r.bloqueado()) {
                nucleoDe(acceso.proceso.getIdComputador())
                        .cambiarMotivoBloqueo(acceso.proceso, r.getMotivo());
            }
        }
        despertarPendientes(numeroCiclo);
    }
 
    // ================= consulta =================
 
    public BufferAcotado getBuffer(int idBuffer) {
        mutex.acquireUninterruptibly();
        try {
            Iterador<BufferAcotado> it = buffers.iterador();
            while (it.tieneSiguiente()) {
                BufferAcotado b = it.siguiente();
                if (b.getId() == idBuffer) {
                    return b;
                }
            }
        } finally {
            mutex.release();
        }
        throw new IllegalArgumentException("No existe el buffer " + idBuffer);
    }
 
    public BufferAcotado[] getBuffers() {
        mutex.acquireUninterruptibly();
        try {
            BufferAcotado[] copia = new BufferAcotado[buffers.tamano()];
            Iterador<BufferAcotado> it = buffers.iterador();
            int i = 0;
            while (it.tieneSiguiente()) {
                copia[i++] = it.siguiente();
            }
            return copia;
        } finally {
            mutex.release();
        }
    }
 
    /** Cantidad de accesos remotos que todavia estan "viajando por la red". */
    public int cantidadEnViaje() {
        mutex.acquireUninterruptibly();
        try {
            return enViaje.tamano();
        } finally {
            mutex.release();
        }
    }
 
    // ================= privados =================
 
    /** Busca la asignacion de un proceso. El mutex ya debe estar tomado. */
    private Asignacion buscar(Pcb proceso) {
        Iterador<Asignacion> it = asignaciones.iterador();
        while (it.tieneSiguiente()) {
            Asignacion a = it.siguiente();
            if (a.proceso == proceso) {
                return a;
            }
        }
        return null;
    }
 
    private Nucleo nucleoDe(int idComputador) {
        mutex.acquireUninterruptibly();
        try {
            Iterador<Nucleo> it = nucleos.iterador();
            while (it.tieneSiguiente()) {
                Nucleo n = it.siguiente();
                if (n.getIdComputador() == idComputador) {
                    return n;
                }
            }
        } finally {
            mutex.release();
        }
        throw new IllegalArgumentException("El gestor no conoce el computador " + idComputador);
    }
 
    private AccesoRemoto[] extraerLlegados(long ciclo) {
        mutex.acquireUninterruptibly();
        try {
            Lista<AccesoRemoto> llegados = new Lista<>();
            int i = 0;
            while (i < enViaje.tamano()) {
                AccesoRemoto a = enViaje.obtener(i);
                if (a.cicloLlegada <= ciclo) {
                    enViaje.eliminar(a);
                    llegados.agregarAlFinal(a);
                } else {
                    i++;
                }
            }
            AccesoRemoto[] copia = new AccesoRemoto[llegados.tamano()];
            Iterador<AccesoRemoto> it = llegados.iterador();
            int j = 0;
            while (it.tieneSiguiente()) {
                copia[j++] = it.siguiente();
            }
            return copia;
        } finally {
            mutex.release();
        }
    }
 
    private void despertarPendientes(long ciclo) {
        Pcb[] lista;
        mutex.acquireUninterruptibly();
        try {
            lista = new Pcb[porDespertar.tamano()];
            Iterador<Pcb> it = porDespertar.iterador();
            int i = 0;
            while (it.tieneSiguiente()) {
                lista[i++] = it.siguiente();
            }
            porDespertar.vaciar();
        } finally {
            mutex.release();
        }
        for (Pcb proceso : lista) {
            if (proceso.getEstado() == EstadoProceso.BLOQUEADO) {
                nucleoDe(proceso.getIdComputador()).desbloquear(proceso);
            }
        }
    }
 
    private void registrarOperacion(long ciclo, Pcb proceso, BufferAcotado buffer,
                                    OperacionBuffer operacion, ResultadoOperacion r) {
        String texto = proceso.getEtiqueta() + " ejecuta " + operacion + " en " + buffer.getEtiqueta();
        if (r.bloqueado()) {
            texto += " y queda bloqueado (" + r.getMotivo() + ")";
        }
        if (r.getDespertado() != null) {
            texto += "; despierta a " + r.getDespertado().getEtiqueta();
        }
        bitacora.registrar(ciclo, texto);
    }
 
    private static void validarLatencia(int ciclos) {
        if (ciclos < 0) {
            throw new IllegalArgumentException("La latencia de red no puede ser negativa");
        }
    }
}