/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.hardware;

import avilaos.util.Iterador;
import avilaos.util.Lista;
import java.util.concurrent.Semaphore;

/**
 * 
 * @author richard
 * 
 * Reloj global que sincroniza todo el sistema distribuido. Corre en su propio hilo
 * ("Reloj-Global"): cada cierto tiempo avanza un ciclo y avisa, en el orden en que
 * se registraron, a todos los componentes Relojable.
 *
 * Tambien se puede avanzar a mano con avanzarCiclo (pruebas y modo paso a paso).
 */
public class RelojGlobal {

    private final Lista<Relojable> componentes = new Lista<>();
    private final Semaphore mutexComponentes = new Semaphore(1);
    private final Semaphore mutexCiclo = new Semaphore(1);

    private volatile long cicloActual = 0;
    private volatile int duracionCicloMs;
    private volatile boolean activo = false;
    private volatile boolean pausado = false;
    private Thread hilo;

    /** @param duracionCicloMs duracion de un ciclo en milisegundos (1 o mas) */
    public RelojGlobal(int duracionCicloMs) {
        validarDuracion(duracionCicloMs);
        this.duracionCicloMs = duracionCicloMs;
    }


    public void registrar(Relojable componente) {
        if (componente == null) {
            throw new IllegalArgumentException("El componente no puede ser nulo");
        }
        mutexComponentes.acquireUninterruptibly();
        try {
            componentes.agregarAlFinal(componente);
        } finally {
            mutexComponentes.release();
        }
    }

    public void iniciar() {
        if (hilo != null) {
            throw new IllegalStateException("El reloj ya fue iniciado");
        }
        activo = true;
        pausado = false;
        hilo = new Thread(this::bucle, "Reloj-Global");
        hilo.setDaemon(true);
        hilo.start();
    }

    public void pausar() {
        pausado = true;
    }

    public void reanudar() {
        pausado = false;
    }

    /** Detiene el hilo del reloj y espera a que termine. */
    public void detener() {
        activo = false;
        Thread h = hilo;
        if (h == null) {
            return;
        }
        h.interrupt();
        if (h != Thread.currentThread()) {
            try {
                h.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        hilo = null;
    }

    public void avanzarCiclo() {
        mutexCiclo.acquireUninterruptibly();
        try {
            long numero = cicloActual + 1;
            cicloActual = numero;
            Relojable[] copia = copiarComponentes();
            for (Relojable componente : copia) {
                try {
                    componente.alCiclo(numero);
                } catch (RuntimeException e) {
                    System.err.println("[Reloj] Error en un componente durante el ciclo " + numero);
                    e.printStackTrace();
                }
            }
        } finally {
            mutexCiclo.release();
        }
    }

    public void setDuracionCicloMs(int duracionCicloMs) {
        validarDuracion(duracionCicloMs);
        this.duracionCicloMs = duracionCicloMs;
    }

    public int getDuracionCicloMs() {
        return duracionCicloMs;
    }

    public long getCicloActual() {
        return cicloActual;
    }

    public boolean estaActivo() {
        return activo;
    }

    public boolean estaPausado() {
        return pausado;
    }

    // ---- privados ----

    private void bucle() {
        try {
            while (activo) {
                if (pausado) {
                    Thread.sleep(20);
                    continue;
                }
                long inicio = System.nanoTime();
                avanzarCiclo();
                long transcurridoMs = (System.nanoTime() - inicio) / 1_000_000;
                long espera = duracionCicloMs - transcurridoMs;
                if (espera > 0) {
                    Thread.sleep(espera);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Relojable[] copiarComponentes() {
        mutexComponentes.acquireUninterruptibly();
        try {
            Relojable[] copia = new Relojable[componentes.tamano()];
            Iterador<Relojable> it = componentes.iterador();
            int i = 0;
            while (it.tieneSiguiente()) {
                copia[i++] = it.siguiente();
            }
            return copia;
        } finally {
            mutexComponentes.release();
        }
    }

    private void validarDuracion(int ms) {
        if (ms < 1) {
            throw new IllegalArgumentException("La duracion del ciclo debe ser de 1 ms o mas");
        }
    }
}
