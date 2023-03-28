package nl.piter.vterm.ui.panels;

import nl.piter.vterm.api.CursorOptions;

import javax.swing.*;
import java.awt.*;

public class StatusBar extends JPanel {

    private JLabel statusInfo;
    private JLabel cursorInfo;
    private JLabel sizeInfo;

    public StatusBar() {
        super();
        initGui();
    }

    private void initGui() {
        this.setLayout(new FlowLayout());
        this.statusInfo = new JLabel("status:");
        this.add(statusInfo);
        this.cursorInfo = new JLabel("cursor:");
        this.add(cursorInfo);
        this.sizeInfo = new JLabel("size:");
        this.add(sizeInfo);
    }

    public void updateCursor(CursorOptions options) {
        this.cursorInfo.setText("Cursor:" + (options.enabled ? "[ON]" : "[off]")
                + " " + options.x + "," + options.y + " : "
                + (options.blink ? "[BLINK]" : "")
        );
    }

    public void setStatus(String msg) {
        this.statusInfo.setText("Status:" + msg);
    }

    public void setStatusSize(int columns, int rows) {
        this.sizeInfo.setText("size: ["+columns+"x"+ rows+"]");
    }
}
