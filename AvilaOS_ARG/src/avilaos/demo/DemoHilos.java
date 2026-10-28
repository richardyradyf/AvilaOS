/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.demo;

/**
 *
 * @author richard
 */
public class DemoHilos {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("main corre en el hilo: " + Thread.currentThread().getName());

        Runnable tarea = () -> {
            String nombre = Thread.currentThread().getName();
            for (int i = 1; i <= 3; i++) {
                System.out.println(nombre + " -> paso " + i);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        };

        Thread a = new Thread(tarea, "Hilo-A");
        Thread b = new Thread(tarea, "Hilo-B");
        a.start();
        b.start();

        a.join();
        b.join();
        System.out.println("main: ambos hilos terminaron");
    }
}
