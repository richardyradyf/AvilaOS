/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.util;

import java.util.concurrent.Semaphore;

/**
 * @author richard
 * 
 * Log de eventos del sistema, compartido por todos los computadores. Guarda solo
 * los ultimos mensajes (el resto se descarta) y es seguro entre hilos. La interfaz
 * grafica lo lee para mostrar el registro de eventos.
 */
public class Bitacora {

    private final int maximo;
    private final Lista<String> mensajes = new Lista<>();
    private final Semaphore mutex = new Semaphore(1);
    private volatile boolean eco = false;

    /** @param maximo cantidad maxima de mensajes que se conservan */
    public Bitacora(int maximo) {
        if (maximo < 1) {
            throw new IllegalArgumentException("La bitacora debe guardar al menos 1 mensaje");
        }
        this.maximo = maximo;
    }

    public void setEco(boolean eco) {
        this.eco = eco;
    }

    public void registrar(long ciclo, String texto) {
        String linea = "[Ciclo " + ciclo + "] " + texto;
        mutex.acquireUninterruptibly();
        try {
            mensajes.agregarAlFinal(linea);
            if (mensajes.tamano() > maximo) {
                mensajes.eliminarPrimero();
            }
        } finally {
            mutex.release();
        }
        if (eco) {
            System.out.println(linea);
        }
    }

    public String[] ultimos() {
        mutex.acquireUninterruptibly();
        try {
            String[] copia = new String[mensajes.tamano()];
            Iterador<String> it = mensajes.iterador();
            int i = 0;
            while (it.tieneSiguiente()) {
                copia[i++] = it.siguiente();
            }
            return copia;
        } finally {
            mutex.release();
        }
    }

    public boolean contiene(String fragmento) {
        mutex.acquireUninterruptibly();
        try {
            Iterador<String> it = mensajes.iterador();
            while (it.tieneSiguiente()) {
                if (it.siguiente().contains(fragmento)) {
                    return true;
                }
            }
            return false;
        } finally {
            mutex.release();
        }
    }
}