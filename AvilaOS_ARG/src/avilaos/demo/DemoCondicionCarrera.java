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
public class DemoCondicionCarrera {

    private static final int ITERACIONES = 1_000_000;

    private static int contadorSinProteccion = 0;
    private static int contadorProtegido = 0;

    private static final Semaphore mutex = new Semaphore(1);

    public static void main(String[] args) throws InterruptedException {
        correr(() -> {
            for (int i = 0; i < ITERACIONES; i++) {
                contadorSinProteccion++;
            }
        });
        System.out.println("Sin proteccion: esperado " + (2 * ITERACIONES)
                + ", obtenido " + contadorSinProteccion);

        correr(() -> {
            for (int i = 0; i < ITERACIONES; i++) {
                try {
                    mutex.acquire();
                    try {
                        contadorProtegido++;
                    } finally {
                        mutex.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        System.out.println("Con semaforo:   esperado " + (2 * ITERACIONES)
                + ", obtenido " + contadorProtegido);
    }

    private static void correr(Runnable tarea) throws InterruptedException {
        Thread t1 = new Thread(tarea, "H1");
        Thread t2 = new Thread(tarea, "H2");
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }
}
