package avilaos.demo;

import avilaos.disco.Disco;
import avilaos.disco.EstadoDisco;
import avilaos.disco.OyenteDisco;
import avilaos.disco.PoliticaCScan;
import avilaos.disco.PoliticaFifo;
import avilaos.disco.PoliticaSstf;
import avilaos.disco.SolicitudDisco;
import avilaos.model.PoliticaDisco;
import avilaos.model.TipoOperacionES;

/**
 * @author andre
 *
 * Pruebas unitarias de la clase Disco:
 * - Ciclo de vida: OCIOSO -> MOVIENDO -> TRANSFIRIENDO -> OCIOSO.
 * - Desplazamiento y velocidad configurable (1 o mas bloques por ciclo).
 * - Transferencia (1 bloque por ciclo).
 * - Salto de C-SCAN al bloque 0 contado aparte sin sumarse al desplazamiento.
 * - Notificacion correcta al OyenteDisco con el numero de ciclo.
 * - Sincronizacion y cambio de politica en caliente (la actual termina con la
 *   politica anterior y la siguiente usa la nueva).
 */
public class PruebaDisco {

    private static int total = 0;
    private static int fallos = 0;

    private static class OyenteContador implements OyenteDisco {
        SolicitudDisco ultimaAtendida = null;
        long ultimoCiclo = -1;
        int llamadas = 0;

        @Override
        public void solicitudAtendida(SolicitudDisco solicitud, long ciclo) {
            this.ultimaAtendida = solicitud;
            this.ultimoCiclo = ciclo;
            this.llamadas++;
        }
    }

    public static void main(String[] args) {
        probarEstadoInicialYOcioso();
        probarMovimientoYTransferenciaSimple();
        probarMultiplesSolicitudesConSstf();
        probarSaltoCScanYVelocidad();
        probarMultiplesOyentes();
        probarCambioDePoliticaEnCaliente();

        System.out.println();
        System.out.println("Pruebas Disco: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar lineas FALLO");
    }

    private static void probarEstadoInicialYOcioso() {
        System.out.println("--- Prueba: Estado inicial y ocioso ---");
        Disco disco = new Disco(200, new PoliticaFifo(), 0);

        verificar(disco.getEstado() == EstadoDisco.OCIOSO, "El disco inicia en OCIOSO");
        verificar(disco.getCabezal() == 0, "El cabezal inicia en 0");
        verificar(disco.getVelocidad() == 1, "La velocidad por defecto es 1 bloque/ciclo");
        verificar(disco.getTotalDesplazamiento() == 0, "Desplazamiento inicial 0");

        // Al avanzar ciclos sin solicitudes, debe permanecer OCIOSO
        disco.alCiclo(1);
        disco.alCiclo(2);
        verificar(disco.getEstado() == EstadoDisco.OCIOSO, "Permanece OCIOSO tras 2 ciclos sin solicitudes");
    }

    private static void probarMovimientoYTransferenciaSimple() {
        System.out.println("--- Prueba: Movimiento y transferencia simple ---");
        Disco disco = new Disco(200, new PoliticaFifo(), 10);
        OyenteContador oyente = new OyenteContador();
        disco.setOyente(oyente);

        // Solicitud al bloque 12, transfiere 2 bloques, llegada en ciclo 0
        SolicitudDisco s1 = new SolicitudDisco(null, TipoOperacionES.LEER, "/archivo.txt", 12, 2, 0);
        disco.encolar(s1);

        // Ciclo 1: el disco toma s1. Bloque 10 -> mueve hacia 12. Cabezal pasa a 11.
        disco.alCiclo(1);
        verificar(disco.getEstado() == EstadoDisco.MOVIENDO, "Ciclo 1: estado MOVIENDO");
        verificar(disco.getCabezal() == 11, "Ciclo 1: cabezal en 11");
        verificar(s1.getCicloInicio() == 1 && s1.getEspera() == 1, "Ciclo 1: inicio marcado en ciclo 1");

        // Ciclo 2: cabezal pasa a 12 (llega al destino).
        disco.alCiclo(2);
        verificar(disco.getCabezal() == 12, "Ciclo 2: cabezal en 12");

        // Ciclo 3: empieza a transferir (bloque 1 de 2).
        disco.alCiclo(3);
        verificar(disco.getEstado() == EstadoDisco.TRANSFIRIENDO, "Ciclo 3: estado TRANSFIRIENDO");
        verificar(oyente.llamadas == 0, "Ciclo 3: aun no finaliza");

        // Ciclo 4: termina de transferir (bloque 2 de 2). Notifica al oyente.
        disco.alCiclo(4);
        verificar(oyente.llamadas == 1, "Ciclo 4: oyente notificado");
        verificar(oyente.ultimaAtendida == s1, "Ciclo 4: la solicitud atendida es s1");
        verificar(oyente.ultimoCiclo == 4 && s1.getCicloFin() == 4, "Ciclo 4: ciclo fin registrado es 4");
        verificar(disco.getEstado() == EstadoDisco.OCIOSO, "Tras finalizar s1 vuelve a OCIOSO");
        verificar(disco.getTotalDesplazamiento() == 2, "Desplazamiento total fue de 2 bloques (10 a 12)");
        verificar(disco.getTotalSolicitudesAtendidas() == 1, "1 solicitud atendida");
    }

    private static void probarMultiplesSolicitudesConSstf() {
        System.out.println("--- Prueba: Multiples solicitudes con SSTF ---");
        Disco disco = new Disco(200, new PoliticaSstf(), 50);
        OyenteContador oyente = new OyenteContador();
        disco.setOyente(oyente);

        // Encolar sA en 100 y sB en 60. Con cabezal en 50, SSTF debe elegir primero sB (distancia 10 vs 50).
        SolicitudDisco sA = new SolicitudDisco(null, TipoOperacionES.LEER, "/a", 100, 1, 0);
        SolicitudDisco sB = new SolicitudDisco(null, TipoOperacionES.LEER, "/b", 60, 1, 0);

        disco.encolar(sA);
        disco.encolar(sB);

        // Al ciclo 1 toma una solicitud: debe ser sB (60)
        disco.alCiclo(1);
        verificar(disco.getSolicitudActual() == sB, "SSTF eligio primero sB (bloque 60)");
        verificar(disco.getCabezal() == 51, "Cabezal avanzando hacia 60");
    }

    private static void probarSaltoCScanYVelocidad() {
        System.out.println("--- Prueba: C-SCAN con salto y velocidad configurable ---");
        // Disco de 200 bloques, cabezal en 190, velocidad 10 bloques/ciclo
        Disco disco = new Disco(200, new PoliticaCScan(), 190, 10);
        OyenteContador oyente = new OyenteContador();
        disco.setOyente(oyente);

        // Solicitud en bloque 15 (por debajo de 190): C-SCAN debe ir a 199, saltar a 0 y subir a 15
        SolicitudDisco s = new SolicitudDisco(null, TipoOperacionES.LEER, "/cscan", 15, 1, 0);
        disco.encolar(s);

        // Ciclo 1: avanza de 190 al extremo 199 (9 bloques <= velocidad 10) y salta a 0
        disco.alCiclo(1);
        verificar(disco.getCabezal() == 0, "Ciclo 1: llego a 199 y salto a 0");
        verificar(disco.getTotalDesplazamiento() == 9, "Ciclo 1: desplazamiento 9 (sin sumar el salto)");
        verificar(disco.getTotalSaltos() == 1, "Ciclo 1: 1 salto al inicio registrado");

        // Ciclo 2: avanza de 0 a 10 (velocidad 10)
        disco.alCiclo(2);
        verificar(disco.getCabezal() == 10, "Ciclo 2: cabezal en 10");

        // Ciclo 3: avanza de 10 a 15 (5 bloques)
        disco.alCiclo(3);
        verificar(disco.getCabezal() == 15, "Ciclo 3: cabezal llega a 15");
        verificar(disco.getTotalDesplazamiento() == 24, "Ciclo 3: desplazamiento acumulado 9 + 15 = 24");

        // Ciclo 4: transfiere 1 bloque y termina
        disco.alCiclo(4);
        verificar(oyente.llamadas == 1 && disco.getEstado() == EstadoDisco.OCIOSO,
                "Ciclo 4: solicitud C-SCAN completada");
    }

    private static void probarMultiplesOyentes() {
        System.out.println("--- Prueba: Multiples oyentes en Disco (Nucleo + Metricas) ---");
        Disco disco = new Disco(200, new PoliticaFifo(), 0);
        OyenteContador oyenteNucleo = new OyenteContador();
        OyenteContador oyenteMetricas = new OyenteContador();

        disco.agregarOyente(oyenteNucleo);
        disco.agregarOyente(oyenteMetricas);

        SolicitudDisco s = new SolicitudDisco(null, TipoOperacionES.LEER, "/compartido", 0, 1, 0);
        disco.encolar(s);

        // Ciclo 1: atiende y finaliza
        disco.alCiclo(1);

        verificar(oyenteNucleo.llamadas == 1, "Oyente nucleo recibio 1 aviso");
        verificar(oyenteMetricas.llamadas == 1, "Oyente metricas recibio 1 aviso");
        verificar(oyenteNucleo.ultimaAtendida == s && oyenteMetricas.ultimaAtendida == s,
                "Ambos oyentes recibieron la misma solicitud");
    }

    private static void probarCambioDePoliticaEnCaliente() {
        System.out.println("--- Prueba: Cambio de politica en caliente ---");
        Disco disco = new Disco(200, new PoliticaCScan(), 198, 1);

        // s1 en bloque 2 (elegida con C-SCAN: debe ir a 199 y saltar a 0 aunque cambiemos a FIFO a mitad)
        SolicitudDisco s1 = new SolicitudDisco(null, TipoOperacionES.LEER, "/s1", 2, 1, 0);
        SolicitudDisco s2 = new SolicitudDisco(null, TipoOperacionES.LEER, "/s2", 100, 1, 0);
        SolicitudDisco s3 = new SolicitudDisco(null, TipoOperacionES.LEER, "/s3", 5, 1, 0);
        disco.encolar(s1);

        // Ciclo 1: C-SCAN elige s1, mueve de 198 a 199 y salta a 0
        disco.alCiclo(1);
        // Cambiamos en caliente a FIFO mientras s1 sigue en curso y encolamos s2 (100) y s3 (5)
        disco.setPolitica(new PoliticaFifo());
        disco.encolar(s2);
        disco.encolar(s3);
        verificar(disco.getTipoPolitica() == PoliticaDisco.FIFO, "Politica cambiada en caliente a FIFO");

        // Ciclos 2 y 3: s1 avanza 0 -> 1 -> 2; ciclo 4: transfiere s1
        disco.alCiclo(2);
        disco.alCiclo(3);
        disco.alCiclo(4);
        verificar(disco.getTotalSolicitudesAtendidas() == 1, "s1 termino su atencion");

        // Ciclo 5: la nueva eleccion usa FIFO (debe elegir s2 en 100 aunque s3 en 5 este mas cerca)
        disco.alCiclo(5);
        verificar(disco.getSolicitudActual() == s2, "Siguiente eleccion ya usa la nueva politica (FIFO -> s2)");
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
