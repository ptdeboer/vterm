/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.examples;

import nl.piter.vterm.ui.VTermSessionManager;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

public class ResizeAdaptor implements ComponentListener {

    private final VTermSessionManager manager;

    public ResizeAdaptor(VTermSessionManager vtermManager) {
        this.manager = vtermManager;
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
