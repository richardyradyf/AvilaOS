/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.hardware;

import java.util.concurrent.Semaphore;

/**
 * @author richard
 * 
 * Memoria principal de un computador. Solo controla la memoria usada y la libre
 * (sin particiones ni asignacion contigua). La unidad es el megabyte.
 *
 * Varios hilos la usan (el nucleo y quien cree buffers desde la interfaz), asi que
 * la memoria usada se protege con un semaforo mutex.
 */
public class Ram {

    private final int capacidad;
    private int usada;
    private final Semaphore mutex = new Semaphore(1);

    public Ram(int capacidad) {
        if (capacidad < 1) {
            throw new IllegalArgumentException("La capacidad de la RAM debe ser mayor que 0");
        }
        this.capacidad = capacidad;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public int getUsada() {
        mutex.acquireUninterruptibly();
        try {
            return usada;
        } finally {
            mutex.release();
        }
    }

    public int getLibre() {
        mutex.acquireUninterruptibly();
        try {
            return capacidad - usada;
        } finally {
            mutex.release();
        }
    }

    public double getPorcentajeUso() {
        return 100.0 * getUsada() / capacidad;
    }


    public boolean hayEspacio(int cantidad) {
        validarCantidad(cantidad);
        return cantidad <= getLibre();
    }

    /**
     * Intenta reservar memoria de forma atomica (todo o nada).
     *
     * @return true si habia espacio y se reservo, false si no cabe
     */
    public boolean reservar(int cantidad) {
        validarCantidad(cantidad);
        mutex.acquireUninterruptibly();
        try {
            if (cantidad > capacidad - usada) {
                return false;
            }
            usada += cantidad;
            return true;
        } finally {
            mutex.release();
        }
    }

    /** Libera memoria previamente reservada. */
    public void liberar(int cantidad) {
        validarCantidad(cantidad);
        mutex.acquireUninterruptibly();
        try {
            if (cantidad > usada) {
                throw new IllegalStateException("Se intenta liberar " + cantidad
                        + " MB pero solo hay " + usada + " MB en uso");
            }
            usada -= cantidad;
        } finally {
            mutex.release();
        }
    }

    @Override
    public String toString() {
        return getUsada() + "/" + capacidad + " MB";
    }

    private void validarCantidad(int cantidad) {
        if (cantidad < 1) {
            throw new IllegalArgumentException("La cantidad de memoria debe ser mayor que 0");
        }
    }
}