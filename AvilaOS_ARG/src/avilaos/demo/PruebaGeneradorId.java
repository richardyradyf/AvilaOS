package avilaos.demo;

import avilaos.model.GeneradorId;
import avilaos.model.MotivoBloqueo;
import avilaos.model.TipoProceso;

/**
 * Prueba de GeneradorId con dos hilos pidiendo IDs a la vez, y de los enums
 * TipoProceso y MotivoBloqueo. Ejecutar con Shift+F6.
 */
public class PruebaGeneradorId {

    private static int fallos = 0;

    public static void main(String[] args) throws InterruptedException {
        // --- enums ---
        verificar(TipoProceso.values().length == 4, "TipoProceso tiene 4 valores");
        verificar(TipoProceso.IO_BOUND.toString().equals("I/O bound"), "nombre visible de IO_BOUND");
        verificar(MotivoBloqueo.values().length == 6, "MotivoBloqueo tiene 6 valores");
        verificar(MotivoBloqueo.BUFFER_VACIO.toString().equals("Buffer vac\u00edo"),
                "nombre visible de BUFFER_VACIO");

        // --- generador de IDs ---
        GeneradorId.reiniciar();
        verificar(GeneradorId.siguienteId() == 1, "el primer ID es 1");
        verificar(GeneradorId.siguienteId() == 2, "el segundo ID es 2");

        GeneradorId.reiniciar();
        final int porHilo = 1000;
        final int[] veces = new int[2 * porHilo + 1];   // indice = ID

        Runnable tarea = () -> {
            for (int i = 0; i < porHilo; i++) {
                int id = GeneradorId.siguienteId();
                veces[id]++;
            }
        };
        Thread a = new Thread(tarea, "Gen-A");
        Thread b = new Thread(tarea, "Gen-B");
        a.start();
        b.start();
        a.join();
        b.join();

        boolean unicos = true;
        for (int id = 1; id <= 2 * porHilo; id++) {
            if (veces[id] != 1) {
                unicos = false;
                System.out.println("ID " + id + " aparecio " + veces[id] + " veces");
                break;
            }
        }
        verificar(unicos, "con dos hilos, los 2000 IDs son unicos y consecutivos");

        System.out.println();
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static void verificar(boolean condicion, String descripcion) {
        if (condicion) {
            System.out.println("OK    - " + descripcion);
        } else {
            fallos++;
            System.out.println("FALLO - " + descripcion);
        }
    }
}
