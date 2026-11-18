package avilaos.demo;

import avilaos.disco.DireccionCabezal;
import avilaos.disco.FabricaPoliticasDisco;
import avilaos.disco.PlanificacionDisco;
import avilaos.disco.PoliticaCScan;
import avilaos.disco.PoliticaFifo;
import avilaos.disco.PoliticaScan;
import avilaos.disco.PoliticaSstf;
import avilaos.disco.SolicitudDisco;
import avilaos.model.PoliticaDisco;
import avilaos.model.TipoOperacionES;
import avilaos.util.Lista;

/**
 * @author andre
 *
 * Pruebas de las politicas de disco con los casos del enunciado. Simula sin reloj:
 * solo importa el orden de atencion y el desplazamiento del cabezal.
 */
public class PruebaPoliticasDisco {

    private static final int TOTAL_BLOQUES = 200;
    private static final int CABEZAL_INICIAL = 50;

    /** Solicitudes del anexo del enunciado, en orden de llegada. */
    private static final int[] ANEXO = {82, 17, 43, 140, 24, 16, 190};

    /** Solicitudes de los casos P1 a P4, en orden de llegada. */
    private static final int[] CASOS_P = {95, 180, 34, 119, 11, 123, 62, 64};

    private static int total = 0;
    private static int fallos = 0;

    /** Resultado de una simulacion: bloques en el orden en que se atendieron. */
    private static class Resultado {
        final int[] orden;
        int desplazamiento = 0;
        int saltos = 0;

        Resultado(int cantidad) {
            orden = new int[cantidad];
        }
    }

    public static void main(String[] args) {
        probarFifo();
        probarSstf();
        probarScan();
        probarCScan();
        probarColaVacia();
        probarFabrica();

        System.out.println();
        System.out.println("Pruebas: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static void probarFifo() {
        System.out.println("--- FIFO: en orden de llegada ---");
        PlanificacionDisco fifo = new PoliticaFifo();

        Resultado anexo = simular(fifo, ANEXO);
        verificar(anexo.desplazamiento == 518, "anexo: desplazamiento 518 (dio " + anexo.desplazamiento + ")");
        verificar(mismoOrden(anexo.orden, ANEXO), "anexo: orden " + texto(anexo.orden));

        Resultado p1 = simular(fifo, CASOS_P);
        verificar(mismoOrden(p1.orden, CASOS_P), "P1: orden " + texto(p1.orden));
        verificar(p1.desplazamiento == 644, "P1: desplazamiento 644 (dio " + p1.desplazamiento + ")");
    }

    private static void probarSstf() {
        System.out.println("--- SSTF: la mas cercana al cabezal ---");
        PlanificacionDisco sstf = new PoliticaSstf();

        Resultado anexo = simular(sstf, ANEXO);
        verificar(anexo.desplazamiento == 208, "anexo: desplazamiento 208 (dio " + anexo.desplazamiento + ")");
        verificar(mismoOrden(anexo.orden, new int[]{43, 24, 17, 16, 82, 140, 190}),
                "anexo: orden " + texto(anexo.orden));

        Resultado p2 = simular(sstf, CASOS_P);
        verificar(mismoOrden(p2.orden, new int[]{62, 64, 34, 11, 95, 119, 123, 180}),
                "P2: orden " + texto(p2.orden));
        verificar(p2.desplazamiento == 236, "P2: desplazamiento 236 (dio " + p2.desplazamiento + ")");

        // Empate: 40 y 60 estan a 10 del cabezal; gana 60 porque llego primero.
        Resultado empate = simular(sstf, new int[]{60, 40});
        verificar(empate.orden[0] == 60, "empate: gana la que llego primero (60)");
    }

    private static void probarScan() {
        System.out.println("--- SCAN: sube hasta el extremo y regresa ---");
        PlanificacionDisco scan = new PoliticaScan();

        Resultado anexo = simular(scan, ANEXO);
        verificar(anexo.desplazamiento == 332, "anexo: desplazamiento 332 (dio " + anexo.desplazamiento + ")");
        verificar(mismoOrden(anexo.orden, new int[]{82, 140, 190, 43, 24, 17, 16}),
                "anexo: orden " + texto(anexo.orden));

        Resultado p3 = simular(scan, CASOS_P);
        verificar(mismoOrden(p3.orden, new int[]{62, 64, 95, 119, 123, 180, 34, 11}),
                "P3: orden " + texto(p3.orden));
        verificar(p3.saltos == 0, "P3: SCAN no salta al inicio");
    }

    private static void probarCScan() {
        System.out.println("--- C-SCAN: solo atiende subiendo y salta al 0 ---");
        PlanificacionDisco cscan = new PoliticaCScan();

        Resultado anexo = simular(cscan, ANEXO);
        verificar(anexo.desplazamiento == 192, "anexo: desplazamiento 192 sin el salto (dio "
                + anexo.desplazamiento + ")");
        verificar(mismoOrden(anexo.orden, new int[]{82, 140, 190, 16, 17, 24, 43}),
                "anexo: orden " + texto(anexo.orden));
        verificar(anexo.saltos == 1, "anexo: un salto al bloque 0 (dio " + anexo.saltos + ")");

        Resultado p4 = simular(cscan, CASOS_P);
        verificar(mismoOrden(p4.orden, new int[]{62, 64, 95, 119, 123, 180, 11, 34}),
                "P4: orden " + texto(p4.orden));
        verificar(p4.saltos == 1, "P4: un salto al bloque 0 (dio " + p4.saltos + ")");
    }

    private static void probarColaVacia() {
        System.out.println("--- Cola vacia ---");
        Lista<SolicitudDisco> vacia = new Lista<>();
        verificar(new PoliticaFifo().elegir(vacia, CABEZAL_INICIAL, DireccionCabezal.ASCENDENTE,
                TOTAL_BLOQUES) == null, "FIFO devuelve null con la cola vacia");
        verificar(new PoliticaSstf().elegir(vacia, CABEZAL_INICIAL, DireccionCabezal.ASCENDENTE,
                TOTAL_BLOQUES) == null, "SSTF devuelve null con la cola vacia");
        verificar(new PoliticaScan().elegir(vacia, CABEZAL_INICIAL, DireccionCabezal.ASCENDENTE,
                TOTAL_BLOQUES) == null, "SCAN devuelve null con la cola vacia");
        verificar(new PoliticaCScan().elegir(vacia, CABEZAL_INICIAL, DireccionCabezal.ASCENDENTE,
                TOTAL_BLOQUES) == null, "C-SCAN devuelve null con la cola vacia");
    }

    private static void probarFabrica() {
        System.out.println("--- Fabrica de politicas ---");
        PoliticaDisco[] tipos = {PoliticaDisco.FIFO, PoliticaDisco.SSTF, PoliticaDisco.SCAN,
                PoliticaDisco.C_SCAN};
        for (PoliticaDisco tipo : tipos) {
            PlanificacionDisco politica = FabricaPoliticasDisco.crear(tipo);
            verificar(politica != null && politica.getTipo() == tipo, "crear(" + tipo + ") da " + tipo);
        }
        verificar(FabricaPoliticasDisco.crear(PoliticaDisco.desdeJson("C-SCAN")) instanceof PoliticaCScan,
                "crear(desdeJson(\"C-SCAN\")) da PoliticaCScan");

        boolean lanzo = false;
        try {
            FabricaPoliticasDisco.crear(null);
        } catch (IllegalArgumentException e) {
            lanzo = true;
        }
        verificar(lanzo, "crear(null) lanza IllegalArgumentException");
    }

    /**
     * Atiende todas las solicitudes con la politica dada, sin reloj. El cabezal
     * empieza en 50 y subiendo, como en el enunciado.
     */
    private static Resultado simular(PlanificacionDisco politica, int[] bloques) {
        Lista<SolicitudDisco> cola = new Lista<>();
        for (int bloque : bloques) {
            cola.agregarAlFinal(new SolicitudDisco(null, TipoOperacionES.LEER, "/prueba", bloque, 1, 0));
        }

        Resultado r = new Resultado(bloques.length);
        int cabezal = CABEZAL_INICIAL;
        DireccionCabezal direccion = DireccionCabezal.ASCENDENTE;
        int atendidas = 0;

        while (!cola.estaVacia()) {
            SolicitudDisco s = politica.elegir(cola, cabezal, direccion, TOTAL_BLOQUES);
            int destino = s.getBloqueDestino();

            if (politica.vaAlExtremoAntes(cabezal, destino, direccion)) {
                int extremo = direccion == DireccionCabezal.ASCENDENTE ? TOTAL_BLOQUES - 1 : 0;
                r.desplazamiento += Math.abs(extremo - cabezal);
                cabezal = extremo;
                if (politica.saltaAlInicio()) {
                    // El salto al bloque 0 no se suma al desplazamiento; se cuenta aparte.
                    cabezal = 0;
                    r.saltos++;
                    direccion = DireccionCabezal.ASCENDENTE;
                } else {
                    direccion = direccion.invertir();
                }
            }

            if (destino != cabezal) {
                direccion = destino > cabezal ? DireccionCabezal.ASCENDENTE : DireccionCabezal.DESCENDENTE;
            }
            r.desplazamiento += Math.abs(destino - cabezal);
            cabezal = destino;

            cola.eliminar(s);
            r.orden[atendidas] = destino;
            atendidas++;
        }
        return r;
    }

    private static boolean mismoOrden(int[] obtenido, int[] esperado) {
        if (obtenido.length != esperado.length) {
            return false;
        }
        for (int i = 0; i < esperado.length; i++) {
            if (obtenido[i] != esperado[i]) {
                return false;
            }
        }
        return true;
    }

    private static String texto(int[] orden) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < orden.length; i++) {
            if (i > 0) {
                sb.append(" -> ");
            }
            sb.append(orden[i]);
        }
        return sb.toString();
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
