package avilaos.demo;

import avilaos.disco.Disco;
import avilaos.disco.PoliticaFifo;
import avilaos.disco.PoliticaSstf;
import avilaos.disco.SolicitudDisco;
import avilaos.fs.MapaBloques;
import avilaos.model.TipoOperacionES;
import avilaos.gui.AccionesCarga;
import avilaos.gui.PaletaColores;
import avilaos.gui.PanelCarga;
import avilaos.gui.PanelColaDisco;
import avilaos.gui.PanelDisco;
import avilaos.gui.PanelGraficaDisco;
import avilaos.metrics.MetricasDisco;

/**
 * @author andre
 *
 * Pruebas unitarias y de integracion de las vistas del disco (Dias 6 y 7):
 * - PaletaColores: constantes de colores definidas y validas.
 * - PanelDisco: inicializacion correcta, dimensiones y refresco de estado.
 * - PanelColaDisco: tabla de cola, controles de politica/velocidad y metricas.
 * - PanelGraficaDisco: curva temporal y refresco con serie de desplazamiento.
 */
public class PruebaVistasDisco {

    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        probarPaletaColores();
        probarPanelDisco();
        probarPanelColaDisco();
        probarPanelGraficaDisco();
        probarPanelCarga();

        System.out.println();
        System.out.println("Pruebas Vistas Disco (Dias 6, 7 y 9): " + total + ", fallos: " + fallos);
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

        panel.actualizar();
        verificar(true, "actualizar() en PanelDisco ejecuta sin excepciones");
    }

    private static void probarPanelColaDisco() {
        System.out.println("--- Prueba: PanelColaDisco ---");
        Disco disco = new Disco(250, new PoliticaFifo(), 53);
        MetricasDisco metricas = new MetricasDisco(disco);
        PanelColaDisco panel = new PanelColaDisco(disco, metricas);

        verificar(panel != null, "PanelColaDisco instanciado correctamente");
        verificar(panel.getComponentCount() > 0, "PanelColaDisco contiene controles y tabla");

        // Encolar solicitudes y refrescar
        disco.encolar(new SolicitudDisco(null, TipoOperacionES.LEER, "/test.txt", 98, 1, 1));
        disco.encolar(new SolicitudDisco(null, TipoOperacionES.ACTUALIZAR, "/swap.bin", 210, 1, 1));
        panel.actualizar();
        verificar(true, "actualizar() con solicitudes encola ejecuta sin excepciones");

        // Cambio de politica
        disco.setPolitica(new PoliticaSstf());
        panel.actualizar();
        verificar(true, "actualizar() tras cambio de politica en caliente ejecuta correctamente");
    }

    private static void probarPanelGraficaDisco() {
        System.out.println("--- Prueba: PanelGraficaDisco ---");
        Disco disco = new Disco(250, new PoliticaFifo(), 53);
        MetricasDisco metricas = new MetricasDisco(disco);
        PanelGraficaDisco panel = new PanelGraficaDisco(metricas);

        verificar(panel != null, "PanelGraficaDisco instanciado correctamente");

        // Simular ciclos y actualizar
        disco.encolar(new SolicitudDisco(null, TipoOperacionES.LEER, "/a.bin", 100, 1, 1));
        for (long c = 1; c <= 10; c++) {
            disco.alCiclo(c);
            metricas.alCiclo(c);
        }
        panel.actualizar();
        verificar(metricas.getSerieDesplazamiento().tamano() > 0, "Serie temporal registro puntos");
        verificar(true, "actualizar() en PanelGraficaDisco ejecuta sin excepciones");
    }

    private static void probarPanelCarga() {
        System.out.println("--- Prueba: PanelCarga y AccionesCarga (Dia 9) ---");
        AccionesCarga accionesFalsas = new AccionesCarga() {
            @Override
            public String cargarSistemaArchivos(String textoJson) {
                return null;
            }
            @Override
            public String cargarProcesos(String textoJson) {
                return null;
            }
            @Override
            public String iniciar() {
                return null;
            }
            @Override
            public String reiniciarConservandoDisco(String textoJson) {
                return null;
            }
            @Override
            public String guardarSistemaArchivos(java.io.File destino) {
                return null;
            }
        };

        PanelCarga panel = new PanelCarga(accionesFalsas);
        verificar(panel != null, "PanelCarga instanciado correctamente");
        verificar(!panel.getBtnIniciar().isEnabled(), "Boton Iniciar inicia deshabilitado");
        verificar(!panel.isSistemaArchivosCargado(), "Sistema de archivos inicia sin cargar");
        verificar(!panel.isProcesosCargados(), "Procesos inician sin cargar");
        verificar(!panel.isSimulacionIniciada(), "Simulacion no iniciada al inicio");
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
