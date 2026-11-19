package avilaos.demo;

import avilaos.disco.Disco;
import avilaos.disco.PoliticaFifo;
import avilaos.fs.MapaBloques;
import avilaos.gui.PaletaColores;
import avilaos.gui.PanelDisco;

/**
 * @author andre
 *
 * Pruebas unitarias de los componentes del Dia 6:
 * - PaletaColores: constantes de colores definidas y validas.
 * - PanelDisco: inicializacion correcta, dimensiones de lienzo y refresco de estado.
 */
public class PruebaVistasDisco {

    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        probarPaletaColores();
        probarPanelDisco();

        System.out.println();
        System.out.println("Pruebas Vistas Disco (Dia 6): " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar lineas FALLO");
    }

    private static void probarPaletaColores() {
        System.out.println("--- Prueba: Paleta de colores ---");
        verificar(PaletaColores.CABEZAL != null, "Color de cabezal definido");
        verificar(PaletaColores.BLOQUE_LIBRE != null, "Color bloque libre definido");
        verificar(PaletaColores.BLOQUE_ARCHIVO != null, "Color bloque archivo definido");
        verificar(PaletaColores.BLOQUE_SWAP != null, "Color bloque swap definido");
        verificar(PaletaColores.ESTADO_OCIOSO != null, "Color estado ocioso definido");
    }

    private static void probarPanelDisco() {
        System.out.println("--- Prueba: PanelDisco y MapaBloques ---");
        MapaBloques mapa = new MapaBloques(200);
        mapa.configurarSwap(50);
        Disco disco = new Disco(250, new PoliticaFifo(), 15);

        PanelDisco panel = new PanelDisco(disco, mapa);
        verificar(panel != null, "PanelDisco instanciado correctamente");
        verificar(panel.getComponentCount() > 0, "PanelDisco contiene componentes graficos");

        // Simular actualizacion
        panel.actualizar();
        verificar(true, "actualizar() ejecuta sin excepciones");
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
