/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.emulator;

import lombok.extern.slf4j.Slf4j;

import static nl.piter.vterm.emulator.Tokens.Token.*;

/**
 * Match symbolic (AWT) Key Code String to actual Control Sequence to send.<br>
 * Optional prefixed with TERM Type for Terminal specific control sequences.
 */
@Slf4j
public class KeyMappings {

    public static String[][] keyMappings = {
            //Default key mapping. Applies for most VT100/XTerms
            {"F1", VTxCharDefs.CTRL_ESC + "OP"}, //
            {"F2", VTxCharDefs.CTRL_ESC + "OQ"}, //
            {"F3", VTxCharDefs.CTRL_ESC + "OR"}, //
            {"F4", VTxCharDefs.CTRL_ESC + "OS"}, //
            {"F5", VTxCharDefs.CTRL_ESC + "[15~"}, //
            {"F6", VTxCharDefs.CTRL_ESC + "[17~"}, //
            {"F7", VTxCharDefs.CTRL_ESC + "[18~"}, //
            {"F8", VTxCharDefs.CTRL_ESC + "[19~"}, //
            {"F9", VTxCharDefs.CTRL_ESC + "[20~"}, //
            {"F10", VTxCharDefs.CTRL_ESC + "[21~"},//
            {"F11", VTxCharDefs.CTRL_ESC + "[23~"},//
            {"F12", VTxCharDefs.CTRL_ESC + "[24~"},//
            {"PAGE_UP", VTxCharDefs.CTRL_ESC + "[5~"},  //
            {"PAGE_DOWN", VTxCharDefs.CTRL_ESC + "[6~"},//
            {"INSERT", VTxCharDefs.CTRL_ESC + "[2~"},   //
            {"DELETE", VTxCharDefs.CTRL_ESC + "[3~"},   //
            {"PAGE_UP", VTxCharDefs.CTRL_ESC + "[5~"},  //
            {"PAGE_DOWN", VTxCharDefs.CTRL_ESC + "[6~"},//
            {"ENTER", VTxCharDefs.CTRL_CR + ""},        //
            {"BACKSPACE", VTxCharDefs.CTRL_BS + "",},  //
            {"TAB", VTxCharDefs.CTRL_HT + ""},
            {"UP", VTxCharDefs.CTRL_ESC + "[A"},   //
            {"DOWN", VTxCharDefs.CTRL_ESC + "[B"}, //
            {"RIGHT", VTxCharDefs.CTRL_ESC + "[C"},//
            {"LEFT", VTxCharDefs.CTRL_ESC + "[D"}, //
            // Application keys
            {"APP_" + UP, VTxCharDefs.CTRL_ESC + "OA"},     //
            {"APP_" + DOWN, VTxCharDefs.CTRL_ESC + "OB"},   //
            {"APP_" + RIGHT, VTxCharDefs.CTRL_ESC + "OC"},  //
            {"APP_" + LEFT, VTxCharDefs.CTRL_ESC + "OD"},   //
            // Terminal specifics. of other than TERM_XTERM/VT100 default: specify here:
            {"XTERM_" + UP, VTxCharDefs.CTRL_ESC + "[A"},   //
            {"XTERM_" + DOWN, VTxCharDefs.CTRL_ESC + "[B"}, //
            {"XTERM_" + RIGHT, VTxCharDefs.CTRL_ESC + "[C"},//
            {"XTERM_" + LEFT, VTxCharDefs.CTRL_ESC + "[D"}, //
            {"XTERM_F1", VTxCharDefs.CTRL_ESC + "OP"}, //
            {"XTERM_F2", VTxCharDefs.CTRL_ESC + "OQ"}, //
            {"XTERM_F3", VTxCharDefs.CTRL_ESC + "OR"}, //
            {"XTERM_F4", VTxCharDefs.CTRL_ESC + "OS"}, //
            {"XTERM_F5", VTxCharDefs.CTRL_ESC + "[15~"}, //
            {"XTERM_F6", VTxCharDefs.CTRL_ESC + "[17~"}, //
            {"XTERM_F7", VTxCharDefs.CTRL_ESC + "[18~"}, //
            {"XTERM_F8", VTxCharDefs.CTRL_ESC + "[19~"}, //
            {"XTERM_F9", VTxCharDefs.CTRL_ESC + "[20~"}, //
            {"XTMER_F10", VTxCharDefs.CTRL_ESC + "[21~"},//
            {"XTMER_F11", VTxCharDefs.CTRL_ESC + "[23~"},//
            {"XTMER_F12", VTxCharDefs.CTRL_ESC + "[24~"},//
            {"VT100_" + UP, VTxCharDefs.CTRL_ESC + "OA"},   //
            {"VT100_" + DOWN, VTxCharDefs.CTRL_ESC + "OB"}, //
            {"VT100_" + RIGHT, VTxCharDefs.CTRL_ESC + "OC"},//
            {"VT100_" + LEFT, VTxCharDefs.CTRL_ESC + "OD"}, //
            {"VT52_" + UP, VTxCharDefs.CTRL_ESC + "A"},     //
            {"VT52_" + DOWN, VTxCharDefs.CTRL_ESC + "B"},   //
            {"VT52_" + RIGHT, VTxCharDefs.CTRL_ESC + "C"},  //
            {"VT52_" + LEFT, VTxCharDefs.CTRL_ESC + "D"},   //
            {"VT52_F1", VTxCharDefs.CTRL_ESC + "P"},        //
            {"VT52_F2", VTxCharDefs.CTRL_ESC + "Q"},        //
            {"VT52_F3", VTxCharDefs.CTRL_ESC + "R"},        //
            {"VT52_F4", VTxCharDefs.CTRL_ESC + "S"},        //
            {"HOME", VTxCharDefs.CTRL_ESC + "[H"},
            {"END", VTxCharDefs.CTRL_ESC + "[F"}
            //
    };

    /**
     * Look in token table to match key/token string and return escape sequence
     */
    public static byte[] getKeyCode(String termType, String keystr) {
        if (termType != null) {
            String termKeystr = (termType + "_" + keystr);

            for (int i = 0; i < keyMappings.length; i++)
                if (keyMappings[i][0].compareToIgnoreCase(termKeystr) == 0) {
                    log.trace("Mapping keystr '{}' -> '{}'", keystr, keyMappings[i][1]);
                    return keyMappings[i][1].getBytes();
                }

        }

        // default:
        for (int i = 0; i < keyMappings.length; i++)
            if (keyMappings[i][0].compareToIgnoreCase(keystr) == 0) {
                log.trace("Mapping keystr '{}' -> '{}'", keystr, keyMappings[i][1]);
                return keyMappings[i][1].getBytes();
            }

        return null;
    }
}
