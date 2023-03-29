/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.examples;

import nl.piter.vterm.api.EmulatorListener;
import nl.piter.vterm.api.TermUI;

/**
 * Minimal UI and Controller adaptor.
 */
public class ControllerAdaptor implements TermUI, EmulatorListener {

    @Override
    public void showMessage(String message) {
        System.out.println(message);
    }

    @Override
    public boolean askConfirmation(String message, DialogOption option) {
        return false;
    }

    @Override
    public String askInput(String message, String type, boolean isSecret) {
        return null;
    }

    @Override
    public void showError(Throwable ex) {
        System.err.println(ex.getMessage());
        ex.printStackTrace();
    }

    @Override
    public void emulatorStarted() {
        System.out.println("Emulator started");
    }

    @Override
    public void emulatorStopped() {
        System.out.println("Emulator started");
        System.exit(0);
    }

    @Override
    public void notifyTermTitle(int type, String arg) {
        System.out.printf("graphmode: %d:%s\n", type, arg);
    }

    @Override
    public void notifyResized(int columns, int rows) {
        System.out.printf("resized: %d,%d\n", columns, rows);
    }

}
