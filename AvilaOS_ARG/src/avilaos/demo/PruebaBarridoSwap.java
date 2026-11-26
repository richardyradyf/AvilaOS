/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.demo;

import avilaos.disco.Disco;
import avilaos.disco.PlanificacionDisco;
import avilaos.disco.PoliticaCScan;
import avilaos.disco.PoliticaScan;
import avilaos.disco.SolicitudDisco;
import avilaos.model.TipoOperacionES;

/**
 *
 * @author richard
 * 
 * Pruebas del barrido con swap: SCAN y C-SCAN llegan hasta el ultimo bloque de archivos
 * y el cabezal solo entra al swap cuando una solicitud lo necesita.
 */
public class PruebaBarridoSwap {

    private static final int[] ANEXO = {82, 17, 43, 140, 24, 16, 190};

    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        probarAnexoConSwap();
        probarSinLimite();
        probarSolicitudEnSwap();
        probarLimiteInvalido();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static void probarAnexoConSwap() {
        System.out.println("--- Anexo del enunciado con swap ---");
        Disco scan = crear(new PoliticaScan(), 199);
        String ordenScan = atender(scan, ANEXO);
        verificar(ordenScan.equals("82 140 190 43 24 17 16 "), "SCAN atiende en el orden del anexo");
        verificar(scan.getTotalDesplazamiento() == 332, "SCAN recorre 332 bloques");

        Disco cscan = crear(new PoliticaCScan(), 199);
        String ordenCscan = atender(cscan, ANEXO);
        verificar(ordenCscan.equals("82 140 190 16 17 24 43 "), "C-SCAN atiende en el orden del anexo");
        verificar(cscan.getTotalDesplazamiento() == 192, "C-SCAN recorre 192 bloques");
        verificar(cscan.getTotalSaltos() == 1, "C-SCAN hace un salto");
    }

    private static void probarSinLimite() {
        System.out.println("--- Sin limite ---");
        Disco disco = new Disco(300, new PoliticaScan(), 50, 5);
        atender(disco, ANEXO);
        verificar(disco.getTotalDesplazamiento() == 532, "sin limite, SCAN llega al ultimo bloque del disco");
    }

    private static void probarSolicitudEnSwap() {
        System.out.println("--- Solicitudes en el swap ---");
        Disco scan = crear(new PoliticaScan(), 199);
        String ordenScan = atender(scan, new int[]{250, 20});
        verificar(ordenScan.equals("250 20 "), "SCAN entra al swap y regresa");
        verificar(scan.getTotalDesplazamiento() == 430, "SCAN no sigue hasta el bloque 299");

        Disco cscan = crear(new PoliticaCScan(), 199);
        String ordenCscan = atender(cscan, new int[]{250, 20});
        verificar(ordenCscan.equals("250 20 "), "C-SCAN entra al swap y salta");
        verificar(cscan.getTotalDesplazamiento() == 220 && cscan.getTotalSaltos() == 1,
                "C-SCAN salta desde el bloque del swap");
    }

    private static void probarLimiteInvalido() {
        System.out.println("--- Limite invalido ---");
        Disco disco = new Disco(300, new PoliticaScan(), 50, 5);
        boolean rechazado = false;
        try {
            disco.setLimiteBarrido(300);
        } catch (IllegalArgumentException e) {
            rechazado = true;
        }
        verificar(rechazado, "rechaza un limite fuera del disco");
    }

    private static Disco crear(PlanificacionDisco politica, int limite) {
        Disco disco = new Disco(300, politica, 50, 5);
        disco.setLimiteBarrido(limite);
        return disco;
    }

    private static String atender(Disco disco, int[] destinos) {
        StringBuilder orden = new StringBuilder();
        disco.agregarOyente((solicitud, ciclo) -> orden.append(solicitud.getBloqueDestino()).append(' '));
        for (int destino : destinos) {
            disco.encolar(new SolicitudDisco(null, TipoOperacionES.LEER, "/prueba", destino, 1, 0));
        }
        for (int ciclo = 1; ciclo <= 1000 && disco.getTotalSolicitudesAtendidas() < destinos.length; ciclo++) {
            disco.alCiclo(ciclo);
        }
        return orden.toString();
    }

    private static void verificar(boolean condicion, String descripcion) {
        total++;
        if (!condicion) {
            fallos++;
        }
        System.out.println((condicion ? "OK    - " : "FALLO - ") + descripcion);
    }
}
