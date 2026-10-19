/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.demo;
import java.util.concurrent.Semaphore;

/**
 *
 * @author richard
 */
public class DemoSemaforoConteo {

    private static final Semaphore cupos = new Semaphore(2);

    public static void main(String[] args) throws InterruptedException {
        Thread[] hilos = new Thread[5];
        for (int i = 0; i < hilos.length; i++) {
            hilos[i] = new Thread(DemoSemaforoConteo::usarRecurso, "Proceso-" + (i + 1));
            hilos[i].start();
        }
        for (Thread h : hilos) {
            h.join();
        }
        System.out.println("Fin. Permisos disponibles: " + cupos.availablePermits());
    }

    private static void usarRecurso() {
        String nombre = Thread.currentThread().getName();
        try {
            System.out.println(nombre + " pide un cupo (disponibles: " + cupos.availablePermits() + ")");
            cupos.acquire();
            try {
                System.out.println(nombre + " ENTRO");
                Thread.sleep(500);
            } finally {
                System.out.println(nombre + " sale");
                cupos.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
