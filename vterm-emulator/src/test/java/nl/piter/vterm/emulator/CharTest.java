/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.emulator;

import org.junit.Test;

import static nl.piter.vterm.emulator.VTxTokenDefs.CTRL_ESC;

public class CharTest {

    @Test
    public void testCharToBytes() {
        char[] chars = {CTRL_ESC, '[', '?', '1', ';', '2', 'c'};

        byte[] bytes = new byte[chars.length];

        for (int i = 0; i < chars.length; i++) {
            bytes[i] = (byte) chars[i];
            System.out.printf(" - #%d:'%c' -> '%x'\n", i, chars[i], bytes[i]);
        }

        byte[] cbytes = {CTRL_ESC, '[', '?', '1', ';', '2', 'c'};

        for (int i = 0; i < chars.length; i++) {
            System.out.printf(" - #%d: '%x'\n", i, cbytes[i]);
        }

    }

}
