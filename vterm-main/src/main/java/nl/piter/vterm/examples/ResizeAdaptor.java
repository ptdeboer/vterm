package nl.piter.vterm.examples;

import nl.piter.vterm.ui.VTermSessionManager;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

public class ResizeAdaptor implements ComponentListener {

    private final VTermSessionManager manager;

    public ResizeAdaptor(VTermSessionManager vtermManager) {
        this.manager=vtermManager;
    }

    @Override
    public void componentResized(ComponentEvent e) {
            manager.resizeTerminalToAWT();
    }

    @Override
    public void componentMoved(ComponentEvent e) {
    }

    @Override
    public void componentShown(ComponentEvent e) {
    }

    @Override
    public void componentHidden(ComponentEvent e) {
    }

}
