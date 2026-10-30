package avilaos.gui;

import avilaos.cluster.Cluster;
import avilaos.cluster.Computador;
import avilaos.config.EntradaInvalidaException;
import avilaos.config.ValidadorEntradas;
import avilaos.hardware.Cpu;
import avilaos.hardware.Ram;
import avilaos.model.EstadoProceso;
import avilaos.model.MotivoBloqueo;
import avilaos.model.Pcb;
import avilaos.model.PoliticaPlanificacion;
import avilaos.scheduler.FabricaPlanificadores;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Vista de un computador: politica (modificable en caliente), estado de la CPU, uso de
 * RAM y todos sus procesos con su estado, motivo de bloqueo, PC, MAR y deadline.
 * 
 * @author andre
 */
public class PanelComputador extends JPanel {

    private static final String[] COLUMNAS = {
        "ID", "Nombre", "Tipo", "Estado", "Motivo de bloqueo", "PC", "MAR", "Prioridad",
        "Memoria", "Deadline restante"
    };

    private final Cluster cluster;
    private final Computador computador;
    private final ControlSimulacion control;
    private final JLabel etiquetaCpu = new JLabel(" ");
    private final JProgressBar barraRam = new JProgressBar(0, 100);
    private final JComboBox<PoliticaPlanificacion> comboPolitica = new JComboBox<>(PoliticaPlanificacion.values());
    private final JTextField campoQuantum = new JTextField(String.valueOf(FabricaPlanificadores.QUANTUM_POR_DEFECTO), 3);
    private final JLabel etiquetaMensaje = new JLabel(" ");
    private final DefaultTableModel modelo = new DefaultTableModel(COLUMNAS, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    public PanelComputador(Cluster cluster, Computador computador, ControlSimulacion control) {
        super(new BorderLayout(4, 4));
        this.cluster = cluster;
        this.computador = computador;
        this.control = control;
        setBorder(BorderFactory.createTitledBorder("Computador " + computador.getId()));

        JPanel cabecera = new JPanel(new BorderLayout());
        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        izquierda.add(new JLabel("Política:"));
        izquierda.add(comboPolitica);
        izquierda.add(new JLabel("Quantum:"));
        izquierda.add(campoQuantum);
        JButton aplicar = new JButton("Cambiar política");
        aplicar.addActionListener(e -> cambiarPolitica());
        izquierda.add(aplicar);
        barraRam.setStringPainted(true);
        barraRam.setPreferredSize(new Dimension(220, 18));
        izquierda.add(new JLabel("RAM:"));
        izquierda.add(barraRam);
        cabecera.add(izquierda, BorderLayout.NORTH);
        cabecera.add(etiquetaCpu, BorderLayout.CENTER);
        etiquetaMensaje.setForeground(new Color(0xB00020));
        cabecera.add(etiquetaMensaje, BorderLayout.SOUTH);

        add(cabecera, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(new JTable(modelo));
        scroll.setPreferredSize(new Dimension(700, 150));
        add(scroll, BorderLayout.CENTER);
        refrescar();
    }

    public void refrescar() {
        long ciclo = cluster.getCicloActual();
        Cpu cpu = computador.getCpu();
        Pcb enCpu = cpu.getProceso();
        etiquetaCpu.setText("CPU: " + (enCpu == null ? "ociosa" : "ejecutando " + enCpu.getEtiqueta()
                + " (" + enCpu.getNombre() + ")") + " | modo: " + cpu.getModo().getNombreVisible()
                + " | utilización: " + String.format("%.1f %%", cpu.getUtilizacion() * 100)
                + " | política activa: " + computador.getNucleo().getPlanificador().getClass()
                        .getSimpleName().replace("Planificador", ""));

        Ram ram = computador.getRam();
        barraRam.setValue((int) Math.round(ram.getPorcentajeUso()));
        barraRam.setString(ram.getUsada() + " / " + ram.getCapacidad() + " MB");

        modelo.setRowCount(0);
        Pcb[] todos = unir(enCpu, computador.getNucleo().getNuevos(), computador.getNucleo().getListos(),
                computador.getNucleo().getBloqueados(), computador.getNucleo().getTerminados());
        for (Pcb p : todos) {
            String motivo = p.getEstado() == EstadoProceso.BLOQUEADO && p.getMotivoBloqueo() != MotivoBloqueo.NINGUNO
                    ? p.getMotivoBloqueo().getNombreVisible() : "-";
            String deadline = p.getEstado() == EstadoProceso.TERMINADO
                    ? (p.isTerminadoPorDeadline() ? "vencido" : "cumplido")
                    : String.valueOf(p.deadlineRestante(ciclo));
            modelo.addRow(new Object[]{p.getEtiqueta(), p.getNombre(), p.getTipo(), p.getEstado(), motivo,
                p.getPc() + "/" + p.getInstruccionesTotales(), p.getMar(), p.getPrioridad(),
                p.getMemoria() + " MB", deadline});
        }
    }

    private void cambiarPolitica() {
        try {
            int quantum = ValidadorEntradas.entero(campoQuantum.getText(), "Quantum", 1, 1000);
            String error = control.cambiarPolitica(computador.getId(),
                    (PoliticaPlanificacion) comboPolitica.getSelectedItem(), quantum);
            etiquetaMensaje.setText(error == null ? " " : error);
        } catch (EntradaInvalidaException e) {
            etiquetaMensaje.setText(e.getMessage());
        }
    }

    /** Une el proceso de la CPU y las colas sin repetir procesos. */
    private static Pcb[] unir(Pcb enCpu, Pcb[]... colas) {
        int max = enCpu == null ? 0 : 1;
        for (Pcb[] c : colas) {
            max += c.length;
        }
        Pcb[] mezcla = new Pcb[max];
        int n = 0;
        if (enCpu != null) {
            mezcla[n++] = enCpu;
        }
        for (Pcb[] cola : colas) {
            for (Pcb p : cola) {
                boolean repetido = false;
                for (int i = 0; i < n && !repetido; i++) {
                    repetido = mezcla[i] == p;
                }
                if (!repetido) {
                    mezcla[n++] = p;
                }
            }
        }
        Pcb[] exacto = new Pcb[n];
        for (int i = 0; i < n; i++) {
            exacto[i] = mezcla[i];
        }
        return exacto;
    }
}