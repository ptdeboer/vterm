/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.api;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public interface TerminalKeyListener extends KeyListener {

    void keyPressed(KeyEvent e);

    void keyReleased(KeyEvent e);

    void keyTyped(KeyEvent e);

}
