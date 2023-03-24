/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.ui;

import nl.piter.vterm.api.EmulatorListener;
import nl.piter.vterm.api.TermUI;
import nl.piter.vterm.ui.panel.Dialogs;

import java.awt.event.*;

import static nl.piter.vterm.util.StringUtil.isEmpty;

public class VTermController implements WindowListener, ComponentListener, EmulatorListener,
        ActionListener, TermUI {

    private final VTermJFrame vtermFrame;
    private String shortTitle;
    private String longTitle;

    public VTermController(VTermJFrame vtermFrame) {
        this.vtermFrame = vtermFrame;
    }

    public void componentHidden(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentResized(ComponentEvent e) {
        if (e.getSource() == vtermFrame.getTerminalPanel()) {
            vtermFrame.sendTermSize(vtermFrame.getTerminalPanel().getColumnCount(), vtermFrame.getTerminalPanel().getRowCount());
        }
    }

    public void componentShown(ComponentEvent e) {
    }

    public void notifyGraphMode(int type, String arg) {
        if (type == 1)
            this.shortTitle = arg;
        else
            this.longTitle = arg;

        if (shortTitle == null)
            shortTitle = "";

        if (longTitle == null)
            longTitle = "";

        StringBuilder sb = new StringBuilder();
        if (!isEmpty(shortTitle)) {
            sb.append("[" + shortTitle + "] ");
        }
        if (!isEmpty(longTitle)) {
            sb.append(longTitle);
        }
        vtermFrame.setTitle(sb.toString());
    }

    public void notifyCharSet(String charSet) {
    }

    public void actionPerformed(ActionEvent e) {
        vtermFrame.actionPerformed(e);
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
        vtermFrame.getTerminalPanel().activate(); // charPane.startRenderers();
    }

    public void windowIconified(WindowEvent e) {
        vtermFrame.getTerminalPanel().inactivate(); // charPane.stopRenderers();
    }

    public void windowOpened(WindowEvent e) {
        vtermFrame.getTerminalPanel().activate(); // charPane.startRenderers();
    }

    public void notifyResized(int columns, int rows) {
        // CharPane has already been resized: Update frame!
        vtermFrame.updateFrameSize();
    }

    @Override
    public void showMessage(String message) {
        vtermFrame.showMessage(message);
    }

    @Override
    public boolean askConfirmation(String message, DialogOption option) {
        if (option == DialogOption.DIALOG_YES_NO) {
            return new Dialogs(vtermFrame).askYesNo(message);
        } else {
            return new Dialogs(vtermFrame).askOkCancel(message);
        }
    }

    @Override
    public String askInput(String message, String type, boolean isSecret) {
        if (!isSecret) {
            return new Dialogs(vtermFrame).askInput("" + type, message);
        } else {
            return new Dialogs(vtermFrame).askForSecret("" + type, message);
        }
    }

}
