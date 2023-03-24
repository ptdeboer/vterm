/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.api;

public interface EmulatorListener {

    /**
     * When a XTerm (OSC) GraphMode update was received
     */
    void notifyGraphMode(int type, String arg);

    /**
     * When the CharacterSet has changed.
     */
    void notifyCharSet(String charSet);

    /**
     * Is send AFTER the terminal has been resized. CharPanel already has updated it size.
     */
    void notifyResized(int columns, int rows);

}
