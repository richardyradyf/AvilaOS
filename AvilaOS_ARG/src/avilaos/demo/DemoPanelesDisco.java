package avilaos.demo;

import avilaos.config.ConfiguracionProcesos;
import avilaos.disco.Disco;
import avilaos.disco.PoliticaFifo;
import avilaos.disco.SolicitudDisco;
import avilaos.fs.CargadorSistemaArchivos;
import avilaos.fs.EstadoDiscoInicial;
import avilaos.fs.MapaBloques;
import avilaos.fs.SistemaArchivos;
import avilaos.model.Pcb;
import avilaos.model.SolicitudProceso;
import avilaos.model.TipoOperacionES;
import avilaos.model.TipoProceso;
import avilaos.gui.AccionesCarga;
import avilaos.gui.PaletaColores;
import avilaos.gui.PanelCarga;
import avilaos.gui.PanelColaDisco;
import avilaos.gui.PanelDisco;
import avilaos.gui.PanelGraficaDisco;
import avilaos.metrics.MetricasDisco;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * @author andre
 *
 * Ventana interactiva de demostracion de las vistas del disco (Proyecto 2 - Dia 7):
 * - PanelDisco: mapa visual de bloques de datos y swap con cabezal en movimiento.
 * - PanelColaDisco: tabla de solicitudes en espera, controles en caliente y metricas.
 * - PanelGraficaDisco: curva temporal de desplazamiento acumulado del cabezal.
 * - Barra de simulacion: avance paso a paso, ejecucion continua y traza del enunciado.
 */
public class DemoPanelesDisco extends JFrame {

    private final Disco disco;
    private final MetricasDisco metricas;
    private final MapaBloques mapa;

    private final PanelCarga panelCarga;
    private final PanelDisco panelDisco;
    private final PanelColaDisco panelCola;
    private final PanelGraficaDisco panelGrafica;

    private SistemaArchivos fsActual = null;

    private final JLabel lblCiclo;
    private final JButton btnPlayPausa;

    private long cicloActual = 0;
    private Timer timerSimulacion;
    private boolean enEjecucion = false;

    private static final int[] SOLICITUDES_ENUNCIADO = {98, 183, 37, 122, 14, 124, 65, 67};
    private int contadorProcesos = 1;
    private final Random random = new Random();

    public DemoPanelesDisco() {
        super("ÁvilaOS - Demostración de Vistas del Disco (Día 7)");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(900, 720));
        setPreferredSize(new Dimension(1020, 780));

        // 1. Instanciacion del subsistema de disco (200 bloques archivos + 50 swap = 250)
        mapa = new MapaBloques(200);
        mapa.configurarSwap(50);
        disco = new Disco(250, new PoliticaFifo(), 53, 1);
        metricas = new MetricasDisco(disco);

        // 2. Componentes graficos
        panelCarga = new PanelCarga(new AccionesCargaDemo());
        panelDisco = new PanelDisco(disco, mapa);
        panelCola = new PanelColaDisco(disco, metricas);
        panelGrafica = new PanelGraficaDisco(metricas);
        panelGrafica.setPreferredSize(new Dimension(400, 200));

        // 3. Barra superior de control y simulacion
        JPanel panelBarra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        panelBarra.setBackground(PaletaColores.FONDO_PANEL);
        panelBarra.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, PaletaColores.BORDE_SUAVE));

        lblCiclo = new JLabel("Ciclo: 0");
        lblCiclo.setFont(new Font("SansSerif", Font.BOLD, 13));
        panelBarra.add(lblCiclo);

        panelBarra.add(Box.createHorizontalStrut(10));

        JButton btnPaso = new JButton("Avanzar 1 ciclo");
        btnPaso.addActionListener(e -> avanzarCiclos(1));
        panelBarra.add(btnPaso);

        JButton btn10Pasos = new JButton("+10 ciclos");
        btn10Pasos.addActionListener(e -> avanzarCiclos(10));
        panelBarra.add(btn10Pasos);

        btnPlayPausa = new JButton("▶ Iniciar");
        btnPlayPausa.addActionListener(e -> alternarSimulacion());
        panelBarra.add(btnPlayPausa);

        panelBarra.add(Box.createHorizontalStrut(15));

        JButton btnEnunciado = new JButton("Encolar caso enunciado");
        btnEnunciado.setToolTipText("Carga la traza del enunciado: 98, 183, 37, 122, 14, 124, 65, 67");
        btnEnunciado.addActionListener(e -> encolarCasoEnunciado());
        panelBarra.add(btnEnunciado);

        JButton btnAleatoria = new JButton("+ Solicitud aleatoria");
        btnAleatoria.addActionListener(e -> encolarAleatoria());
        panelBarra.add(btnAleatoria);

        JButton btnReiniciar = new JButton("Reiniciar");
        btnReiniciar.addActionListener(e -> reiniciar());
        panelBarra.add(btnReiniciar);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(panelBarra, BorderLayout.NORTH);

        // 4. Distribucion de paneles
        JPanel panelContenedor = new JPanel();
        panelContenedor.setLayout(new BoxLayout(panelContenedor, BoxLayout.Y_AXIS));

        panelCarga.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        panelContenedor.add(panelCarga);

        panelDisco.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        panelContenedor.add(panelDisco);

        panelCola.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));
        panelContenedor.add(panelCola);

        panelContenedor.add(panelGrafica);

        JScrollPane scrollGeneral = new JScrollPane(panelContenedor);
        scrollGeneral.setBorder(null);
        scrollGeneral.getVerticalScrollBar().setUnitIncrement(16);
        getContentPane().add(scrollGeneral, BorderLayout.CENTER);

        // 5. Timer para simulacion continua
        timerSimulacion = new Timer(150, e -> avanzarCiclos(1));

        pack();
        setLocationRelativeTo(null);
    }

    private void avanzarCiclos(int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            cicloActual++;
            disco.alCiclo(cicloActual);
            metricas.alCiclo(cicloActual);
        }
        refrescarVistas();
    }

    private void alternarSimulacion() {
        if (enEjecucion) {
            timerSimulacion.stop();
            btnPlayPausa.setText("▶ Iniciar");
            enEjecucion = false;
        } else {
            timerSimulacion.start();
            btnPlayPausa.setText("⏸ Pausar");
            enEjecucion = true;
        }
    }

    private void encolarCasoEnunciado() {
        for (int bloque : SOLICITUDES_ENUNCIADO) {
            Pcb proceso = new Pcb("P" + (contadorProcesos++), 1, TipoProceso.IO_BOUND, 1, 10, 10, 0, cicloActual);
            SolicitudDisco s = new SolicitudDisco(
                    proceso,
                    TipoOperacionES.LEER,
                    "/archivo_" + bloque + ".bin",
                    bloque,
                    1,
                    cicloActual
            );
            disco.encolar(s);
        }
        refrescarVistas();
    }

    private void encolarAleatoria() {
        int bloque = random.nextInt(disco.getTotalBloques());
        TipoOperacionES op = random.nextBoolean() ? TipoOperacionES.LEER : TipoOperacionES.ACTUALIZAR;
        Pcb proceso = new Pcb("P" + (contadorProcesos++), 1, TipoProceso.IO_BOUND, 1, 10, 10, 0, cicloActual);
        SolicitudDisco s = new SolicitudDisco(
                proceso,
                op,
                "/datos_" + bloque + ".dat",
                bloque,
                1,
                cicloActual
        );
        disco.encolar(s);
        refrescarVistas();
    }

    private void reiniciar() {
        if (enEjecucion) {
            alternarSimulacion();
        }
        cicloActual = 0;
        contadorProcesos = 1;
        disco.reiniciar(53, new PoliticaFifo());
        metricas.reiniciar();
        refrescarVistas();
    }

    private void refrescarVistas() {
        lblCiclo.setText("Ciclo: " + cicloActual);
        panelDisco.actualizar();
        panelCola.actualizar();
        panelGrafica.actualizar();
    }

    private int buscarPrimerBloque(String ruta) {
        if (mapa != null) {
            for (int i = 0; i < mapa.getBloquesArchivos(); i++) {
                if (ruta.equals(mapa.getOcupante(i))) {
                    return i;
                }
            }
        }
        return random.nextInt(Math.max(1, disco.getTotalBloques()));
    }

    private class AccionesCargaDemo implements AccionesCarga {
        @Override
        public String cargarSistemaArchivos(String textoJson) {
            try {
                EstadoDiscoInicial estado = CargadorSistemaArchivos.desdeJson(textoJson);
                fsActual = estado.getSistemaArchivos();
                refrescarVistas();
                return null;
            } catch (Exception e) {
                return e.getMessage();
            }
        }

        @Override
        public String cargarProcesos(String textoJson) {
            try {
                ConfiguracionProcesos cfg = ConfiguracionProcesos.desdeJson(textoJson);
                SolicitudProceso[] procs = cfg.getProcesos();
                for (SolicitudProceso p : procs) {
                    disco.encolar(new SolicitudDisco(
                            new Pcb(p.getNombre(), 1, TipoProceso.IO_BOUND, p.getPrioridad(), p.getMemoria(), p.getDeadline(), 0, cicloActual),
                            p.getOperacion(),
                            p.getRuta(),
                            buscarPrimerBloque(p.getRuta()),
                            p.getBloques() > 0 ? p.getBloques() : 1,
                            cicloActual
                    ));
                }
                refrescarVistas();
                return null;
            } catch (Exception e) {
                return e.getMessage();
            }
        }

        @Override
        public String iniciar() {
            if (!enEjecucion) {
                alternarSimulacion();
            }
            return null;
        }

        @Override
        public String reiniciarConservandoDisco(String textoJson) {
            return cargarProcesos(textoJson);
        }

        @Override
        public String guardarSistemaArchivos(File destino) {
            try {
                if (fsActual == null) {
                    return "No hay un sistema de archivos cargado para guardar";
                }
                String json = CargadorSistemaArchivos.aJson(fsActual, disco.getCabezal());
                Files.writeString(destino.toPath(), json, StandardCharsets.UTF_8);
                return null;
            } catch (Exception e) {
                return e.getMessage();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DemoPanelesDisco demo = new DemoPanelesDisco();
            demo.setVisible(true);
        });
    }
}
