package avilaos.gui;

import avilaos.util.Bitacora;
import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * 
 * Muestra los ultimos mensajes de la bitacora del sistema. 
 *
 * @author andre
 */





public class PanelLog extends JPanel {

    private final Bitacora bitacora;
    private final JTextArea area = new JTextArea();
    private String ultimoTexto = "";

    public PanelLog(Bitacora bitacora) {
        super(new BorderLayout());
        this.bitacora = bitacora;
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(new JScrollPane(area), BorderLayout.CENTER);
    }

    public void refrescar() {
        String[] mensajes = bitacora.ultimos();
        StringBuilder sb = new StringBuilder();
        for (String m : mensajes) {
            sb.append(m).append('\n');
        }
        String texto = sb.toString();
        if (!texto.equals(ultimoTexto)) {
            ultimoTexto = texto;
            area.setText(texto);
            area.setCaretPosition(area.getDocument().getLength());
        }
    }
}