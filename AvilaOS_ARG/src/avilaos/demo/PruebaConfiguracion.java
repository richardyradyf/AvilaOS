/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.demo;

import avilaos.app.Sistema;
import avilaos.config.ConfiguracionSimulacion;
import avilaos.config.EntradaInvalidaException;
import avilaos.metrics.ReporteMetricas;
import avilaos.model.PoliticaPlanificacion;

/**
 * @author richard
 * 
 * Pruebas de la configuracion JSON y del armado completo del sistema. 
 */
public class PruebaConfiguracion {

    private static int total = 0;
    private static int fallos = 0;

    private static void verificar(String descripcion, boolean condicion) {
        total++;
        if (condicion) {
            System.out.println("OK    " + descripcion);
        } else {
            fallos++;
            System.out.println("FALLO " + descripcion);
        }
    }

    private static String errorAl(String json) {
        try {
            ConfiguracionSimulacion.desdeJson(json);
            return null;
        } catch (EntradaInvalidaException e) {
            return e.getMessage();
        }
    }

    private static final String BASE_PCS = "\"computadores\":[{\"memoriaMb\":100,\"politica\":\"FCFS\"},"
            + "{\"memoriaMb\":100,\"politica\":\"EDF\",\"quantum\":3}]";

    public static void main(String[] args) {
        probarIdaYVuelta();
        probarErrores();
        probarSistemaCompleto();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
        System.exit(fallos == 0 ? 0 : 1);
    }

    private static void probarIdaYVuelta() {
        System.out.println("--- JSON: escribir y leer ---");
        ConfiguracionSimulacion original = ConfiguracionSimulacion.porDefecto();
        String json = original.aJson();
        ConfiguracionSimulacion leida = ConfiguracionSimulacion.desdeJson(json);
        verificar("la duracion del ciclo se conserva", leida.getDuracionCicloMs() == original.getDuracionCicloMs());
        verificar("los computadores se conservan", leida.getComputadores().length == 2
                && leida.getComputadores()[0].politica == PoliticaPlanificacion.ROUND_ROBIN
                && leida.getComputadores()[0].quantum == 4 && leida.getComputadores()[1].memoriaMb == 256);
        verificar("los procesos se conservan", leida.getProcesos().length == original.getProcesos().length
                && leida.getProcesos()[1].getCiclosEs() == 3);
        verificar("escribir de nuevo da el mismo texto", leida.aJson().equals(json));
        verificar("acepta politicas en minusculas", errorAl("{\"duracionCicloMs\":100,"
                + "\"computadores\":[{\"memoriaMb\":100,\"politica\":\"fcfs\"},{\"memoriaMb\":100,\"politica\":\"edf\"}]}") == null);
        verificar("ignora campos viejos como latenciaRed y buffers", errorAl("{\"duracionCicloMs\":100,\"latenciaRed\":3,"
                + "\"buffers\":[]," + BASE_PCS + "}") == null);
    }

    private static void probarErrores() {
        System.out.println("--- JSON: errores con mensaje claro ---");
        String e = errorAl("{\"duracionCicloMs\": 100,");
        verificar("JSON incompleto indica linea y columna (" + e + ")", e != null && e.contains("línea"));
        verificar("texto que no es objeto", errorAl("[1,2]") != null);
        e = errorAl("{" + BASE_PCS + "}");
        verificar("falta la duracion del ciclo", e != null && e.contains("duracionCicloMs") || e != null && e.contains("Duración"));
        e = errorAl("{\"duracionCicloMs\":5," + BASE_PCS + "}");
        verificar("duracion fuera de rango", e != null && e.contains("entre 10 y 10000"));
        e = errorAl("{\"duracionCicloMs\":100,\"computadores\":[{\"memoriaMb\":\"mucha\",\"politica\":\"FCFS\"},"
                + "{\"memoriaMb\":100,\"politica\":\"FCFS\"}]}");
        verificar("memoria que no es numero", e != null && e.contains("número entero"));
        e = errorAl("{\"duracionCicloMs\":100,\"computadores\":[{\"memoriaMb\":100,\"politica\":\"FCFS\"}]}");
        verificar("menos de 2 computadores", e != null && e.contains("entre 2 y 6"));
        e = errorAl("{\"duracionCicloMs\":100,\"computadores\":[{\"memoriaMb\":100,\"politica\":\"XYZ\"},"
                + "{\"memoriaMb\":100,\"politica\":\"FCFS\"}]}");
        verificar("politica desconocida", e != null && e.contains("FCFS, EDF"));
        e = errorAl("{\"duracionCicloMs\":100," + BASE_PCS
                + ",\"procesos\":[{\"nombre\":\"X\",\"tipo\":\"CPU_BOUND\",\"instrucciones\":0,\"memoria\":10,\"prioridad\":1,\"deadline\":10}]}");
        verificar("proceso con 0 instrucciones (" + e + ")", e != null && e.contains("Instrucciones"));
        e = errorAl("{\"duracionCicloMs\":100," + BASE_PCS
                + ",\"procesos\":[{\"nombre\":\"X\",\"tipo\":\"PRODUCTOR\",\"memoria\":10,\"prioridad\":1,\"deadline\":10}]}");
        verificar("los productores ya no se aceptan (" + e + ")", e != null && e.contains("CPU_BOUND o IO_BOUND"));
        e = errorAl("{\"duracionCicloMs\":100,\"duracionCicloMs\":200," + BASE_PCS + "}");
        verificar("clave repetida", e != null && e.contains("repetida"));
    }

    private static void probarSistemaCompleto() {
        System.out.println("--- Sistema armado con la configuracion de ejemplo ---");
        Sistema s = Sistema.construir(ConfiguracionSimulacion.porDefecto());
        verificar("2 computadores", s.getCluster().cantidadComputadores() == 2);
        for (int i = 0; i < 600; i++) {
            s.getCluster().avanzarCiclo();
        }
        ReporteMetricas r = s.getRecolector().generarReporte();
        int terminados = s.getCluster().contarTerminados();
        verificar("los 4 procesos terminaron (" + terminados + ")", terminados == 4);
        verificar("hay procesos completados y metricas del sistema", r.getSistema().getCompletados() >= 4);
        s.getCluster().detener();

        String sinCabida = "{\"duracionCicloMs\":100," + BASE_PCS
                + ",\"procesos\":[{\"nombre\":\"Gigante\",\"tipo\":\"CPU_BOUND\",\"instrucciones\":5,\"memoria\":500,"
                + "\"prioridad\":1,\"deadline\":50,\"computador\":1}]}";
        boolean rechazado = false;
        try {
            Sistema.construir(ConfiguracionSimulacion.desdeJson(sinCabida));
        } catch (EntradaInvalidaException e) {
            rechazado = e.getMessage().contains("Gigante");
        }
        verificar("un proceso que no cabe en la RAM se rechaza con el nombre del proceso", rechazado);
    }
}