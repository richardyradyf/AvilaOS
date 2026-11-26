package avilaos.disco;

import avilaos.hardware.Relojable;
import avilaos.model.PoliticaDisco;
import avilaos.util.Iterador;
import avilaos.util.Lista;
import java.util.concurrent.Semaphore;

/**
 * @author andre
 *
 * Disco magnetico simulado para AvilaOS.
 *
 * Caracteristicas principales:
 * - Implementa Relojable: avanza ciclo a ciclo con el reloj del sistema.
 * - Maquina de estados: OCIOSO, MOVIENDO, TRANSFIRIENDO.
 * - Desplazamiento del cabezal: avanza hasta 'velocidad' bloques por ciclo de reloj (por defecto 1).
 * - Transferencia: 1 bloque por cada ciclo de reloj.
 * - Sincronizacion: Semaphore(1) protege el acceso concurrente a la cola,
 *   la solicitud actual y la politica activa entre el hilo del reloj y el de Swing.
 * - Cambio de politica en caliente: la solicitud en curso termina con la politica
 *   que la eligio; la siguiente eleccion usa la nueva politica.
 * - Notificacion: al terminar una solicitud, invoca a OyenteDisco fuera del semaforo.
 */
public class Disco implements Relojable {

    public static final int CABEZAL_INICIAL_DEFAULT = 0;
    public static final int VELOCIDAD_DEFAULT = 1;

    private int totalBloques;
    private int limiteBarrido;
    private boolean retenido;
    private int velocidad;
    private final Lista<SolicitudDisco> cola;
    private final Lista<OyenteDisco> oyentes;
    private final Semaphore semaforo;

    private PlanificacionDisco politica;
    private PlanificacionDisco politicaSolicitudActual;
    private PoliticaDisco politicaUltimaAtendida;

    private int cabezal;
    private DireccionCabezal direccion;
    private EstadoDisco estado;

    private SolicitudDisco solicitudActual;
    private int bloquesRestantesTransferencia;

    // Metricas acumuladas basicas
    private int totalDesplazamiento;
    private int totalSaltos;
    private int totalSolicitudesAtendidas;

    public Disco(int totalBloques, PlanificacionDisco politicaInicial) {
        this(totalBloques, politicaInicial, CABEZAL_INICIAL_DEFAULT, VELOCIDAD_DEFAULT);
    }

    public Disco(int totalBloques, PlanificacionDisco politicaInicial, int cabezalInicial) {
        this(totalBloques, politicaInicial, cabezalInicial, VELOCIDAD_DEFAULT);
    }

    public Disco(int totalBloques, PlanificacionDisco politicaInicial, int cabezalInicial, int velocidad) {
        if (totalBloques <= 0) {
            throw new IllegalArgumentException("La cantidad de bloques debe ser mayor a 0");
        }
        if (politicaInicial == null) {
            throw new IllegalArgumentException("La política de planificación no puede ser nula");
        }
        if (cabezalInicial < 0 || cabezalInicial >= totalBloques) {
            throw new IllegalArgumentException("Posición inicial del cabezal inválida: " + cabezalInicial);
        }
        if (velocidad <= 0) {
            throw new IllegalArgumentException("La velocidad del cabezal debe ser mayor a 0");
        }

        this.totalBloques = totalBloques;
        this.limiteBarrido = totalBloques - 1;
        this.velocidad = velocidad;
        this.politica = politicaInicial;
        this.politicaSolicitudActual = null;
        this.cabezal = cabezalInicial;
        this.direccion = DireccionCabezal.ASCENDENTE;
        this.estado = EstadoDisco.OCIOSO;
        this.cola = new Lista<>();
        this.oyentes = new Lista<>();
        this.semaforo = new Semaphore(1);
        this.solicitudActual = null;
        this.politicaUltimaAtendida = null;
        this.bloquesRestantesTransferencia = 0;
        this.totalDesplazamiento = 0;
        this.totalSaltos = 0;
        this.totalSolicitudesAtendidas = 0;
    }

    /**
     * Agrega un nuevo oyente para recibir avisos cuando el disco termine solicitudes.
     * Permite multiples oyentes (por ejemplo el nucleo y metricas simultaneamente).
     */
    public void agregarOyente(OyenteDisco oyente) {
        if (oyente == null) {
            throw new IllegalArgumentException("El oyente no puede ser nulo");
        }
        semaforo.acquireUninterruptibly();
        try {
            oyentes.agregarAlFinal(oyente);
        } finally {
            semaforo.release();
        }
    }

    /**
     * Define o reemplaza el oyente principal (mantiene compatibilidad, agrega a la lista).
     */
    public void setOyente(OyenteDisco oyente) {
        if (oyente != null) {
            agregarOyente(oyente);
        }
    }

    /**
     * Agrega una nueva solicitud de E/S a la cola del disco (hilo seguro).
     */
    public void encolar(SolicitudDisco solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        semaforo.acquireUninterruptibly();
        try {
            cola.agregarAlFinal(solicitud);
        } finally {
            semaforo.release();
        }
    }

    /**
     * Cambia la politica activa del planificador de disco en caliente (hilo seguro).
     * Si hay una solicitud en curso, termina con la politica que la eligio y la
     * siguiente eleccion usa la nueva politica.
     */
    public void setPolitica(PlanificacionDisco nuevaPolitica) {
        if (nuevaPolitica == null) {
            throw new IllegalArgumentException("La política no puede ser nula");
        }
        semaforo.acquireUninterruptibly();
        try {
            this.politica = nuevaPolitica;
        } finally {
            semaforo.release();
        }
    }

    /**
     * Cambia la velocidad del cabezal (bloques por ciclo).
     */
    public void setVelocidad(int nuevaVelocidad) {
        if (nuevaVelocidad <= 0) {
            throw new IllegalArgumentException("La velocidad del cabezal debe ser mayor a 0");
        }
        semaforo.acquireUninterruptibly();
        try {
            this.velocidad = nuevaVelocidad;
        } finally {
            semaforo.release();
        }
    }

    /**
     * Actualiza el total de bloques del disco (por ejemplo al configurar el area de swap).
     */
    public void setTotalBloques(int nuevoTotalBloques) {
        if (nuevoTotalBloques <= 0) {
            throw new IllegalArgumentException("La cantidad de bloques debe ser mayor a 0");
        }
        semaforo.acquireUninterruptibly();
        try {
            if (cabezal >= nuevoTotalBloques) {
                throw new IllegalArgumentException("El nuevo total de bloques deja al cabezal fuera de rango");
            }
            if (limiteBarrido == totalBloques - 1 || limiteBarrido >= nuevoTotalBloques) {
                limiteBarrido = nuevoTotalBloques - 1;
            }
            this.totalBloques = nuevoTotalBloques;
        } finally {
            semaforo.release();
        }
    }

    /**
     * Ultimo bloque al que llegan SCAN y C-SCAN cuando no hay solicitudes mas arriba.
     * Con swap es el ultimo bloque de archivos: el cabezal solo entra al swap si una
     * solicitud lo necesita.
     */
    public void setLimiteBarrido(int ultimoBloque) {
        semaforo.acquireUninterruptibly();
        try {
            if (ultimoBloque < 0 || ultimoBloque >= totalBloques) {
                throw new IllegalArgumentException("Límite de barrido inválido: " + ultimoBloque);
            }
            this.limiteBarrido = ultimoBloque;
        } finally {
            semaforo.release();
        }
    }
    
    /** Mientras esta retenido, el disco recibe solicitudes pero no elige ninguna. */
    public void setRetenido(boolean retenido) {
        semaforo.acquireUninterruptibly();
        try {
            this.retenido = retenido;
        } finally {
            semaforo.release();
        }
    }

    public boolean isRetenido() {
        semaforo.acquireUninterruptibly();
        try {
            return retenido;
        } finally {
            semaforo.release();
        }
    }

    /**
     * Reinicia la cola, el estado y los contadores del disco para una nueva corrida.
     */
    public void reiniciar(int cabezalInicial, PlanificacionDisco politicaInicial) {
        if (politicaInicial == null) {
            throw new IllegalArgumentException("La política de planificación no puede ser nula");
        }
        semaforo.acquireUninterruptibly();
        try {
            if (cabezalInicial < 0 || cabezalInicial >= totalBloques) {
                throw new IllegalArgumentException("Posición inicial del cabezal inválida: " + cabezalInicial);
            }
            while (!cola.estaVacia()) {
                cola.eliminarPrimero();
            }
            this.politica = politicaInicial;
            this.politicaSolicitudActual = null;
            this.politicaUltimaAtendida = null;
            this.cabezal = cabezalInicial;
            this.direccion = DireccionCabezal.ASCENDENTE;
            this.estado = EstadoDisco.OCIOSO;
            this.solicitudActual = null;
            this.bloquesRestantesTransferencia = 0;
            this.totalDesplazamiento = 0;
            this.totalSaltos = 0;
            this.totalSolicitudesAtendidas = 0;
        } finally {
            semaforo.release();
        }
    }

    /**
     * Ciclo del reloj global del sistema. En cada ciclo el disco avanza exactamente
     * un ciclo de trabajo (movimiento o transferencia de 1 bloque).
     */
    @Override
    public void alCiclo(long numeroCiclo) {
        SolicitudDisco finalizada = null;

        semaforo.acquireUninterruptibly();
        try {
            if (solicitudActual == null) {
                if (retenido || cola.estaVacia()) {
                    estado = EstadoDisco.OCIOSO;
                    return;
                }
                // Elegir la proxima solicitud segun la politica activa en este momento
                solicitudActual = politica.elegir(cola, cabezal, direccion, totalBloques);
                if (solicitudActual == null) {
                    estado = EstadoDisco.OCIOSO;
                    return;
                }
                politicaSolicitudActual = politica;
                cola.eliminar(solicitudActual);
                solicitudActual.setCicloInicio(numeroCiclo);
                bloquesRestantesTransferencia = solicitudActual.getBloquesATransferir();
            }

            int destino = solicitudActual.getBloqueDestino();

            // Si la solicitud no requiere mover el cabezal (destino == -1 o ya esta en destino), transfiere
            if (destino == -1 || cabezal == destino) {
                estado = EstadoDisco.TRANSFIRIENDO;
                bloquesRestantesTransferencia--;
                if (bloquesRestantesTransferencia <= 0) {
                    solicitudActual.setCicloFin(numeroCiclo);
                    totalSolicitudesAtendidas++;
                    finalizada = solicitudActual;
                    politicaUltimaAtendida = (politicaSolicitudActual != null)
                            ? politicaSolicitudActual.getTipo() : politica.getTipo();
                    solicitudActual = null;
                    politicaSolicitudActual = null;
                    estado = EstadoDisco.OCIOSO;
                }
            } else {
                // Requiere mover el cabezal usando la politica que eligio esta solicitud
                estado = EstadoDisco.MOVIENDO;
                PlanificacionDisco polActiva = (politicaSolicitudActual != null) ? politicaSolicitudActual : politica;

                if (polActiva.vaAlExtremoAntes(cabezal, destino, direccion)) {
                    int extremo = (direccion == DireccionCabezal.ASCENDENTE)
                            ? Math.max(limiteBarrido, cabezal) : 0;
                    if (cabezal == extremo) {
                        // Si ya estaba parado en el extremo al iniciar el movimiento, voltea o salta y avanza
                        aplicarLlegadaAlExtremo(polActiva);
                        moverHacia(destino);
                    } else {
                        moverHacia(extremo);
                        if (cabezal == extremo) {
                            aplicarLlegadaAlExtremo(polActiva);
                        }
                    }
                } else {
                    moverHacia(destino);
                }
            }

        } finally {
            semaforo.release();
        }

        // Notificar fuera del cerrojo a todos los oyentes para evitar interbloqueos
        if (finalizada != null) {
            notificarOyentes(finalizada, numeroCiclo);
        }
    }

    private void notificarOyentes(SolicitudDisco finalizada, long numeroCiclo) {
        Lista<OyenteDisco> copiaOyentes = new Lista<>();
        semaforo.acquireUninterruptibly();
        try {
            Iterador<OyenteDisco> it = oyentes.iterador();
            while (it.tieneSiguiente()) {
                copiaOyentes.agregarAlFinal(it.siguiente());
            }
        } finally {
            semaforo.release();
        }

        Iterador<OyenteDisco> it = copiaOyentes.iterador();
        while (it.tieneSiguiente()) {
            it.siguiente().solicitudAtendida(finalizada, numeroCiclo);
        }
    }

    private void aplicarLlegadaAlExtremo(PlanificacionDisco polActiva) {
        if (polActiva.saltaAlInicio()) {
            cabezal = 0;
            totalSaltos++;
            direccion = DireccionCabezal.ASCENDENTE;
        } else {
            direccion = direccion.invertir();
        }
    }

    private void moverHacia(int objetivo) {
        int distancia = Math.abs(objetivo - cabezal);
        int paso = Math.min(velocidad, distancia);
        if (paso <= 0) {
            return;
        }
        if (cabezal < objetivo) {
            cabezal += paso;
            direccion = DireccionCabezal.ASCENDENTE;
        } else {
            cabezal -= paso;
            direccion = DireccionCabezal.DESCENDENTE;
        }
        totalDesplazamiento += paso;
    }

    // Getters sincronizados para la interfaz de usuario y las metricas

    public int getCabezal() {
        semaforo.acquireUninterruptibly();
        try {
            return cabezal;
        } finally {
            semaforo.release();
        }
    }

    public int getVelocidad() {
        semaforo.acquireUninterruptibly();
        try {
            return velocidad;
        } finally {
            semaforo.release();
        }
    }

    public DireccionCabezal getDireccion() {
        semaforo.acquireUninterruptibly();
        try {
            return direccion;
        } finally {
            semaforo.release();
        }
    }

    public EstadoDisco getEstado() {
        semaforo.acquireUninterruptibly();
        try {
            return estado;
        } finally {
            semaforo.release();
        }
    }

    public PoliticaDisco getTipoPolitica() {
        semaforo.acquireUninterruptibly();
        try {
            return politica.getTipo();
        } finally {
            semaforo.release();
        }
    }

    /**
     * Devuelve la politica con la que se atendio la ultima solicitud finalizada.
     */
    public PoliticaDisco getPoliticaUltimaAtendida() {
        semaforo.acquireUninterruptibly();
        try {
            return politicaUltimaAtendida;
        } finally {
            semaforo.release();
        }
    }

    public SolicitudDisco getSolicitudActual() {
        semaforo.acquireUninterruptibly();
        try {
            return solicitudActual;
        } finally {
            semaforo.release();
        }
    }

    public int getTotalBloques() {
        semaforo.acquireUninterruptibly();
        try {
            return totalBloques;
        } finally {
            semaforo.release();
        }
    }

    public int getTotalDesplazamiento() {
        semaforo.acquireUninterruptibly();
        try {
            return totalDesplazamiento;
        } finally {
            semaforo.release();
        }
    }

    public int getTotalSaltos() {
        semaforo.acquireUninterruptibly();
        try {
            return totalSaltos;
        } finally {
            semaforo.release();
        }
    }

    public int getTotalSolicitudesAtendidas() {
        semaforo.acquireUninterruptibly();
        try {
            return totalSolicitudesAtendidas;
        } finally {
            semaforo.release();
        }
    }

    public int getCantidadEnCola() {
        semaforo.acquireUninterruptibly();
        try {
            return cola.tamano();
        } finally {
            semaforo.release();
        }
    }

    public Lista<SolicitudDisco> clonarCola() {
        semaforo.acquireUninterruptibly();
        try {
            Lista<SolicitudDisco> copia = new Lista<>();
            Iterador<SolicitudDisco> it = cola.iterador();
            while (it.tieneSiguiente()) {
                copia.agregarAlFinal(it.siguiente());
            }
            return copia;
        } finally {
            semaforo.release();
        }
    }
}
