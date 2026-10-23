package avilaos.demo;

import avilaos.model.EstadoProceso;
import avilaos.model.ModoEjecucion;
import avilaos.model.PoliticaPlanificacion;

/** Pruebas de los enums de estado, modo y politica. Ejecutar con Shift+F6. */
public class PruebaEnums {

    private static int fallos = 0;

    public static void main(String[] args) {
        verificar(EstadoProceso.values().length == 5, "EstadoProceso tiene 5 valores");
        verificar(EstadoProceso.valueOf("NUEVO") == EstadoProceso.NUEVO, "valueOf encuentra NUEVO");
        verificar(EstadoProceso.EJECUCION.getNombreVisible().equals("Ejecuci\u00f3n"),
                "nombre visible de EJECUCION");
        verificar(EstadoProceso.LISTO.toString().equals("Listo"), "toString devuelve el nombre visible");

        verificar(ModoEjecucion.values().length == 2, "ModoEjecucion tiene 2 valores");
        verificar(ModoEjecucion.SISTEMA_OPERATIVO.toString().equals("Sistema operativo"),
                "nombre visible de SISTEMA_OPERATIVO");

        verificar(PoliticaPlanificacion.values().length == 4, "PoliticaPlanificacion tiene 4 valores");
        verificar(PoliticaPlanificacion.ROUND_ROBIN.toString().equals("Round Robin"),
                "nombre visible de ROUND_ROBIN");

        System.out.println();
        System.out.println("Valores definidos (asi se mostraran en la interfaz):");
        for (EstadoProceso e : EstadoProceso.values()) {
            System.out.println("  Estado: " + e);
        }
        for (ModoEjecucion m : ModoEjecucion.values()) {
            System.out.println("  Modo: " + m);
        }
        for (PoliticaPlanificacion p : PoliticaPlanificacion.values()) {
            System.out.println("  Politica: " + p);
        }

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