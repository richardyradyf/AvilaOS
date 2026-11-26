package avilaos.gui;

import avilaos.fs.MapaBloques;
import avilaos.fs.NodoFs;
import avilaos.fs.SistemaArchivos;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * @author Gianm
 * Tabla de asignacion de archivos: una fila por archivo con su ruta, los bloques
 * asignados, el primer bloque, el dueno, el proceso que lo creo y su color. El color
 * es el mismo que usa el mapa del disco (PaletaColores.colorDe), asi se ve que bloques
 * del mapa son de cada archivo.
 *
 * Solo observa: se refresca sola cada 500 ms, pero solo vuelve a llenar las filas si
 * la version del sistema de archivos cambio. Asi queda al dia con cada operacion CRUD.
 */
public class PanelTablaAsignacion extends JPanel {

    private static final String[] COLUMNAS = {
        "Nombre (ruta)", "Bloques asignados", "Primer bloque", "Dueño", "Proceso creador", "Color"
    };
    private static final int COLUMNA_COLOR = 5;

    private final SistemaArchivos fs;
    private final DefaultTableModel modelo;
    private final JTable tabla;
    private final JLabel resumen = new JLabel(" ");
    private final Timer temporizador;
    private int versionDibujada = -1;

    public PanelTablaAsignacion(SistemaArchivos fs) {
        super(new BorderLayout(4, 4));
        if (fs == null) {
            throw new IllegalArgumentException("El sistema de archivos es obligatorio");
        }
        this.fs = fs;

        modelo = new DefaultTableModel(COLUMNAS, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        tabla = new JTable(modelo);
        tabla.setRowHeight(22);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(200);
        tabla.getColumnModel().getColumn(COLUMNA_COLOR).setPreferredWidth(60);
        tabla.getColumnModel().getColumn(COLUMNA_COLOR).setCellRenderer(new RendererColor());

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new Dimension(640, 220));
        resumen.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        add(scroll, BorderLayout.CENTER);
        add(resumen, BorderLayout.SOUTH);

        refrescar();
        temporizador = new Timer(500, e -> refrescar());
        temporizador.start();
    }

    /** Vuelve a llenar la tabla solo si el sistema de archivos cambio desde la ultima vez. */
    public void refrescar() {
        int version = fs.getVersion();
        if (version == versionDibujada) {
            return;
        }
        NodoFs[] archivos = fs.archivos();
        modelo.setRowCount(0);
        int usados = 0;
        for (NodoFs a : archivos) {
            String ruta = a.getRuta();
            modelo.addRow(new Object[]{ruta, a.getCantidadBloques(), a.getPrimerBloque(),
                a.getDueno(), a.getProcesoCreador(), ruta});
            usados += a.getCantidadBloques();
        }
        MapaBloques mapa = fs.getMapa();
        resumen.setText(archivos.length + " archivos  |  bloques usados: " + usados + " de "
                + mapa.getBloquesArchivos() + "  |  libres: " + mapa.libres());
        versionDibujada = version;
    }

    /** Detiene el refresco automatico (al cerrar la ventana o en pruebas). */
    public void detener() {
        temporizador.stop();
    }

    /** Pinta la celda de la columna "Color" con el color del archivo en el mapa del disco. */
    private static final class RendererColor extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada,
                                                       boolean conFoco, int fila, int columna) {
            super.getTableCellRendererComponent(t, "", seleccionada, conFoco, fila, columna);
            String ruta = valor == null ? null : valor.toString();
            Color color = PaletaColores.colorDe(ruta);
            setBackground(color);
            setToolTipText("Color de " + ruta + " en el mapa del disco");
            return this;
        }
    }
}