package avilaos.demo;

import avilaos.disco.Disco;
import avilaos.disco.PoliticaFifo;
import avilaos.disco.PoliticaSstf;
import avilaos.disco.SolicitudDisco;
import avilaos.metrics.MetricasDisco;
import avilaos.metrics.SerieTemporal;
import avilaos.model.PoliticaDisco;
import avilaos.model.TipoOperacionES;

/**
 * @author andre
 *
 * Pruebas unitarias de MetricasDisco:
 * - Desplazamiento acumulado del cabezal.
 * - Registro de puntos en SerieTemporal para la grafica (ciclo vs. desplazamiento).
 * - Tiempo promedio de espera en cola de E/S.
 * - Conteo exacto de solicitudes atendidas por cada politica (incluso con cambio en caliente).
 */
public class PruebaMetricasDisco {

    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        probarMetricasIniciales();
        probarDesplazamientoYSerieTemporal();
        probarTiempoEsperaYSolicitudesPorPolitica();

        System.out.println();
        System.out.println("Pruebas MetricasDisco: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar lineas FALLO");
    }

    private static void probarMetricasIniciales() {
        System.out.println("--- Prueba: Metricas iniciales ---");
        Disco disco = new Disco(200, new PoliticaFifo(), 0);
        MetricasDisco metricas = new MetricasDisco(disco);

        verificar(metricas.getDesplazamientoTotal() == 0, "Desplazamiento inicial 0");
        verificar(metricas.getTiempoEsperaPromedio() == 0.0, "Tiempo de espera inicial 0.0");
        verificar(metricas.getTotalAtendidas() == 0, "Total atendidas inicial 0");
        verificar(metricas.getAtendidasPorPolitica(PoliticaDisco.FIFO) == 0, "FIFO atendidas 0");
        verificar(metricas.getSerieDesplazamiento().tamano() == 0, "Serie temporal vacia");
    }

    private static void probarDesplazamientoYSerieTemporal() {
        System.out.println("--- Prueba: Desplazamiento y SerieTemporal ---");
        Disco disco = new Disco(200, new PoliticaFifo(), 10);
        MetricasDisco metricas = new MetricasDisco(disco, 1, 100);

        // Solicitud en bloque 12 (distancia 2), transfiere 1 bloque, llegada en ciclo 0
        SolicitudDisco s1 = new SolicitudDisco(null, TipoOperacionES.LEER, "/s1", 12, 1, 0);
        disco.encolar(s1);

        // Ciclo 1: cabezal avanza de 10 a 11
        disco.alCiclo(1);
        metricas.alCiclo(1);
        // Ciclo 2: cabezal avanza de 11 a 12
        disco.alCiclo(2);
        metricas.alCiclo(2);
        // Ciclo 3: transfiere y completa
        disco.alCiclo(3);
        metricas.alCiclo(3);

        verificar(metricas.getDesplazamientoTotal() == 2, "Desplazamiento total acumulado es 2");
        SerieTemporal serie = metricas.getSerieDesplazamiento();
        verificar(serie.tamano() == 3, "La serie tiene 3 puntos muestreados");
        long[] ciclos = serie.getCiclos();
        double[] valores = serie.getValores();
        verificar(ciclos[0] == 1 && valores[0] == 1.0, "Punto 1: ciclo 1, desp 1.0");
        verificar(ciclos[1] == 2 && valores[1] == 2.0, "Punto 2: ciclo 2, desp 2.0");
        verificar(ciclos[2] == 3 && valores[2] == 2.0, "Punto 3: ciclo 3, desp 2.0 (transfiriendo)");
    }

    private static void probarTiempoEsperaYSolicitudesPorPolitica() {
        System.out.println("--- Prueba: Tiempo de espera y solicitudes por politica ---");
        Disco disco = new Disco(200, new PoliticaFifo(), 0);
        MetricasDisco metricas = new MetricasDisco(disco);

        // s1: llegada en ciclo 0, destino en bloque 0 (0 mov, 1 ciclo transfiere). Inicio ciclo 1 -> espera 1.
        SolicitudDisco s1 = new SolicitudDisco(null, TipoOperacionES.LEER, "/a", 0, 1, 0);
        disco.encolar(s1);

        disco.alCiclo(1);
        verificar(metricas.getTotalAtendidas() == 1, "s1 atendida en ciclo 1");
        verificar(metricas.getAtendidasPorPolitica(PoliticaDisco.FIFO) == 1, "1 atendida con FIFO");
        verificar(metricas.getTiempoEsperaPromedio() == 1.0, "Espera de s1 es 1.0 ciclo");

        // Cambiamos a SSTF y encolamos s2 en ciclo 1. Destino bloque 0, 1 ciclo. Se atiende en ciclo 5 -> espera 4.
        disco.setPolitica(new PoliticaSstf());
        SolicitudDisco s2 = new SolicitudDisco(null, TipoOperacionES.LEER, "/b", 0, 1, 1);
        disco.encolar(s2);

        disco.alCiclo(5);
        verificar(metricas.getTotalAtendidas() == 2, "2 solicitudes atendidas en total");
        verificar(metricas.getAtendidasPorPolitica(PoliticaDisco.FIFO) == 1, "FIFO mantiene 1 atendida");
        verificar(metricas.getAtendidasPorPolitica(PoliticaDisco.SSTF) == 1, "SSTF tiene 1 atendida");
        // Esperas: s1 espero (1 - 0) = 1; s2 espero (5 - 1) = 4. Promedio: (1 + 4) / 2 = 2.5
        verificar(metricas.getTiempoEsperaPromedio() == 2.5, "Tiempo promedio de espera es (1+4)/2 = 2.5");
    }

    private static void verificar(boolean condicion, String descripcion) {
        total++;
        if (condicion) {
            System.out.println("OK    - " + descripcion);
        } else {
            fallos++;
            System.out.println("FALLO - " + descripcion);
        }
    }
}
