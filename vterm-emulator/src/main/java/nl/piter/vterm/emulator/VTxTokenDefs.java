/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.emulator;

import nl.piter.vterm.emulator.tokens.IToken;
import nl.piter.vterm.emulator.tokens.TokenDef;
import nl.piter.vterm.exceptions.VTxInvalidConfigurationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static nl.piter.vterm.emulator.Tokens.Token;
import static nl.piter.vterm.emulator.Tokens.Token.*;
import static nl.piter.vterm.emulator.Tokens.TokenOption;
import static nl.piter.vterm.emulator.Tokens.TokenOption.OPTION_GRAPHMODE;
import static nl.piter.vterm.emulator.Tokens.TokenOption.OPTION_INTEGERS;

public class VTxTokenDefs {

    /*
     * VT102 Control characters (octal)
     * 000 = Null (fill character)
     * 003 = ETX (Can be selected half-duplex turnaround char)
     * 004 = EOT (Can be turnaround or disconnect char, if turn, then DLE-EOT=disc.)
     * 005 = ENQ (Transmits answerback message)
     * 007 = BEL (Generates bell tone)
     * 010 = BS  (Moves cursor left)
     * 011 = HT  (Moves cursor to next tab)
     * 012 = LF  (Linefeed or New line operation)
     * 013 = VT  (Processed as LF)
     * 014 = FF  (Processed as LF, can be selected turnaround char)
     * 015 = CR  (Moves cursor to left margin, can be turnaround char)
     * 016 = SO  (Selects G1 charset)
     * 017 = SI  (Selects G0 charset)
     * 021 = DC1 (XON, causes terminal to continue transmit)
     * 023 = DC3 (XOFF, causes terminal to stop transmitting)
     * 030 = CAN (Cancels escape sequence)
     * 032 = SUB (Processed as CAN)
     * 033 = ESC (Processed as sequence indicator)
     */

    final public static char CTRL_NUL = 0x00; // Ignored on input; not stored in buffer
    final public static char CTRL_ETX = 0x03; // CTRL-C:
    final public static char CTRL_EOT = 0x04; // CTRL-D: End Of Transission ?
    final public static char CTRL_ENQ = 0x05; // CTRL_E Transmit ANSWERBACK message
    final public static char CTRL_BEL = 0x07; // CTRL-G: BEEEEEEEEEEEEEEEEP
    final public static char CTRL_BS = 0x08; // CTRL-H: Backspace
    final public static char CTRL_HT = 0x09; // CTRL-I: next stabstop
    final public static char CTRL_LF = 0x0a; // CTRL-J: line feed/new line (depends on line feed mode)
    final public static char CTRL_VT = 0x0b; // CTRL-K/LF : line feed/new line
    final public static char CTRL_FF = 0x0c; // CTRL-L/LF : line feed/new line
    final public static char CTRL_CR = 0x0d; // CTRL-M/CR : Carriage Return
    final public static char CTRL_SO = 0x0e; // CTRL-N: G1 character set
    final public static char CTRL_SI = 0x0f; // CTRL-O: G0 character set
    final public static char CTRL_XON = 0x11; // CTRL-Q: XON (only XON/XOFF are allowed)
    final public static char CTRL_XOFF = 0x13;// CTRL-S: XOFF (turn off XON/XOFF mode)
    final public static char CTRL_CAN = 0x18; // Abort CTRL sequence, output ERROR Char
    final public static char CTRL_SUB = 0x1a; // Same as CAN
    final public static char CTRL_ESC = 0x1b; // New Escape Sequence  (aborts previous)
    final public static char CTRL_DEL = 0x7f; // Ignored
    // 8 bit characters:                     // <name> (7 bit equivalent-> doesn't match)
    final public static char IND = 0x84; // Index (ESC D)
    final public static char NEL = 0x85; // Next Line  = ESC E
    final public static char HTS = 0x88; // Tab Set ESC H
    final public static char RI = 0x8d; //
    final public static char SS2 = 0x8e; //
    final public static char SS3 = 0x8f; //
    final public static char DCS = 0x90; //
    final public static char SPA = 0x96; //
    final public static char EPA = 0x97; //
    final public static char SOS = 0x98; //
    final public static char DECID = 0x9a; //
    final public static char CSI = 0x9b; //
    final public static char ST = 0x9c; //
    final public static char OSC = 0x9d; //
    final public static char PM = 0x9e; //
    final public static char APC = 0x9f; //
    // Prefix characters sequences:
    final public static String CTRL_CSI_PREFIX = CTRL_ESC + "[";
    final public static String CTRL_DEC_PRIVATE_PREFIX = CTRL_ESC + "[?";
    final public static String CTRL_SECONDARY_DA_PREFIX = CTRL_ESC + "[>";

    /**
     * Simple token table. Store TOKEN as string together with char sequence (as string).
     * This list is searched linear so that the first match is used.<br>
     * The lines always end with a token, which is a TERMINATOR, or a SEQUENCE token.<br>
     * The following token definitions are supported<br>
     * <pre>
     *  { TERMINATOR_CHAR, CHAR_TOKEN}
     *  { UTF8_STRING, TERMINATOR_TOKEN}
     *  { UTF8_STRING, <OPTION>, SEQUENCE_TOKEN }  -> OSC graphmode only
     *  { UTF8_STRING, <OPTION>, TERMINATOR_CHAR, SEQUENCE_TOKEN [, COMMENT ]  }
     *  { UTF8_STRING, <OPTION>, TERMINATOR_UTF8_STRING, SEQUENCE_TOKEN [, COMMENT ] }
     * </pre>
     */
    private static final Object[][] tokenDefs = {
            // === Single Char Tokens === //
            {CTRL_NUL, NUL}, // => Warning: 0x00 => Empty String
            {CTRL_ETX, ETX},
            {CTRL_EOT, EOT},
            {CTRL_ENQ, ENQ},
            {CTRL_BS, BS},
            {CTRL_HT, HT},
            {CTRL_CR, CR},
            {CTRL_LF, LF},
            {CTRL_VT, VT},
            {CTRL_FF, FF},
            {CTRL_CAN, CAN},
            {CTRL_SUB, SUB},
            {CTRL_ESC, ESC}, // Careful ESC is NOT Terminating TOKEN !
            {CTRL_BEL, BEEP}, // Beep
            {CTRL_XON, UNSUPPORTED, "XON (Not Implementend)"},
            {CTRL_XOFF, UNSUPPORTED, "XOFF (Not Implemented)"},
            // charsets:
            {CTRL_SI, CHARSET_G0},
            {CTRL_SO, CHARSET_G1},
            // === Double Char Escape codes === //
            {CTRL_ESC + "7", SAVE_CURSOR},
            {CTRL_ESC + "8", RESTORE_CURSOR},
            {CTRL_ESC + "=", APPLICATION_KEYPAD}, // opposite NUMERIC
            {CTRL_ESC + ">", NUMERIC_KEYPAD}, // opposite of APPLICATION
            {CTRL_ESC + "<", EXIT_VT52_MODE},
            {CTRL_ESC + "D", INDEX},
            {CTRL_ESC + "E", NEXT_LINE},
            {CTRL_ESC + "H", TAB_SET},
            {CTRL_ESC + "M", REVERSE_INDEX}, // DELETE_LINE
            {CTRL_ESC + "N", UNSUPPORTED},
            {CTRL_ESC + "O", UNSUPPORTED},
            {CTRL_ESC + "P", UNSUPPORTED},
            {CTRL_ESC + "V", UNSUPPORTED},
            {CTRL_ESC + "W", UNSUPPORTED},
            {CTRL_ESC + "X", STRING_START},
            {CTRL_ESC + "Z", SEND_TERM_ID},
            {CTRL_ESC + "\\", STRING_END},
            // Select G0 character set: (there are more)
            {CTRL_ESC + "(0", CHARSET_G0_GRAPHICS},
            {CTRL_ESC + "(A", CHARSET_G0_UK},
            {CTRL_ESC + "(B", CHARSET_G0_US},
            {CTRL_ESC + "(4", CHARSET_G0_DUTCH},
            {CTRL_ESC + "(C", CHARSET_G0_FINNISH},
            {CTRL_ESC + "(5", CHARSET_G0_FINNISH},
            {CTRL_ESC + "(R", CHARSET_G0_FRENCH},
            {CTRL_ESC + "(f", CHARSET_G0_FRENCH},
            {CTRL_ESC + "(K", CHARSET_G0_GERMAN},
            {CTRL_ESC + "(Z", CHARSET_G0_SPANISH},
            {CTRL_ESC + "(H", CHARSET_G0_SWEDISH},
            {CTRL_ESC + "(7", CHARSET_G0_SWEDISH},
            {CTRL_ESC + "(1", CHARSET_G0_ALT_ROM_NORMAL},
            {CTRL_ESC + "(2", CHARSET_G0_ALT_ROM_SPECIAL},
            // Select G1 character set: (there are more)
            // ESC + '*' and ESC + '+' are Charset G2, and G3.
            {CTRL_ESC + ")A", CHARSET_G1_UK},
            {CTRL_ESC + ")B", CHARSET_G1_US},
            {CTRL_ESC + ")4", CHARSET_G1_DUTCH},
            {CTRL_ESC + ")R", CHARSET_G1_FRENCH},
            {CTRL_ESC + ")f", CHARSET_G1_FRENCH},
            {CTRL_ESC + ")K", CHARSET_G1_GERMAN},
            {CTRL_ESC + ")Z", CHARSET_G1_SPANISH},
            {CTRL_ESC + ")C", CHARSET_G1_OTHER},
            {CTRL_ESC + ")5", CHARSET_G1_OTHER},
            {CTRL_ESC + ")0", CHARSET_G1_GRAPHICS},
            {CTRL_ESC + ")1", CHARSET_G1_ALT_ROM_NORMAL},
            {CTRL_ESC + ")2", CHARSET_G1_ALT_ROM_SPECIAL},
            //
            {CTRL_ESC + "%@", UNSUPPORTED, "Select Default Charset ISO 8859-1"},
            {CTRL_ESC + "%G", UNSUPPORTED, "Select UTF8 Charset"},
            // Not implemented but add filter and detect them anyway
            {CTRL_ESC + "#3", UNSUPPORTED, "DEC Double height, top half"},
            {CTRL_ESC + "#4", UNSUPPORTED, "DEC Double height, bottom half"},
            {CTRL_ESC + "#5", UNSUPPORTED, "DEC Single width line"},
            {CTRL_ESC + "#6", UNSUPPORTED, "DEC double width line"},
            //
            {CTRL_ESC + " F", UNSUPPORTED, "7 Bits Controls"},
            {CTRL_ESC + " G", UNSUPPORTED, "8 Bits Controls"},
            {CTRL_ESC + " L", UNSUPPORTED, "Set ANSI conformance level 1 - vt100"},
            {CTRL_ESC + " M", UNSUPPORTED, "Set ANSI conformance level 2 - vt200"},
            {CTRL_ESC + " N", UNSUPPORTED, "Set ANSI conformance level 3 - vt300"},
            // DEC
            {CTRL_ESC + "#3", UNSUPPORTED, "DEC double height, top half"},
            {CTRL_ESC + "#4", UNSUPPORTED, "DEC double height, bottom half"},
            {CTRL_ESC + "#5", UNSUPPORTED, "DEC single width line"},
            {CTRL_ESC + "#6", UNSUPPORTED, "DEC double width line"},
            {CTRL_ESC + "#8", DEC_SCREEN_ALIGNMENT}, // "DEC Screen aligment Test"},
            // --------------------------------------------------------------------------------
            // CSI Escape Sequences "^[[" Or: <ESC> '[' OR <ESC> + ']' for XGraphMode.
            // Optimization: Prefix must be first Escape + '[' token in token list so that the prefix
            // token is matched first or else all tokens with options trigger read aheads.
            // The PREFIX token triggers the parseOptions() in nextToken() if 2d value is an OPTION.
            // --------------------------------------------------------------------------------
            // XGRAPHMODE: No terminator only 'Prefix':
            {CTRL_ESC + "]", OPTION_GRAPHMODE, OSC_GRAPHMODE},
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, -1, CSI_PREFIX_START, "Start of CSI Prefix"},
            // DEC_PRIVATE terminators: 3 char prefix, must be after 2 char CSI prefix!
            {CTRL_CSI_PREFIX + "?", OPTION_INTEGERS, 'h', DEC_SETMODE},
            {CTRL_CSI_PREFIX + "?", OPTION_INTEGERS, 'l', DEC_RESETMODE},
            {CTRL_CSI_PREFIX + "?", OPTION_INTEGERS, 'm', CHARACTER_ATTRS},
            // no 3rd char here:
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'c', SEND_PRIMARY_DA},
            {CTRL_CSI_PREFIX + "?", OPTION_INTEGERS, 'c', RESPONSE_PRIMARY_DA},
            {CTRL_CSI_PREFIX + ">", OPTION_INTEGERS, 'c', SEND_SECONDARY_DA},
            {CTRL_CSI_PREFIX + ">", OPTION_INTEGERS, 'c', RESPONSE_SECONDARY_DA}, // Duplicate!
            // XTERM ?
            {CTRL_CSI_PREFIX + ">", OPTION_INTEGERS, 'm', XTERM_RES_RESET_MODIFIERS},
            // --------------------------------------------------------------------------------
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, "%m", UNSUPPORTED, "VI '0%m' GREMLIN"},
            // --------------------------------------------------------------------------------
            // CSI TERMINATORS
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, '@', INSERT_BLANK_CHARS}, // \E[K
            // Cursors:
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'A', UP}, // vt100/xterm cursor control
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'B', DOWN}, //vt100/xterm cursor control
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'C', RIGHT}, //vt100/xterm cursor control
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'D', LEFT}, //vt100/xterm cursor control
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'E', NEXT_LINE},
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'F', PRECEDING_LINE},
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'G', SET_COLUMN},
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'H', SET_CURSOR}, // \[[<rows>;<columns>H
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'I', FORWARD_TABS}, // \[[<y>;<x>H
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'J', SCREEN_ERASE}, // \[[...,J
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'K', LINE_ERASE}, // \[[...,K
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'L', INSERT_LINES}, // \[[...,L
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'M', DELETE_LINES}, // \[[...,M
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'P', DEL_CHAR},
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'S', SCROLL_UP},
            // one integer=scroll down, 5 integers=mouse track
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'T', SCROLL_DOWN_OR_MOUSETRACK},
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'X', ERASE_CHARS},
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'Z', BACKWARD_TABS},
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'd', SET_ROW},
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'f', SET_CURSOR}, // double -> see \E[H
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'g', TABCLEAR},
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'h', SET_MODE}, // \E[<c>;..;<c>h
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'l', RESET_MODE}, // \E[<c>;..;<c>l
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'm', SET_FONT_STYLE}, // \E[<c>;..;<c>m
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'n', DEVICE_STATUS},
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'r', SET_REGION}, // \E[<y1>;<y2>r
            // misc
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 't', XTERM_WIN_MANIPULATION, "Window-Manipulation"}, // \E[<y1>;<y2>r
            // Led Control: has integer beteen '[' and 'q'.
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'q', DEC_LED_SET, "DEC_LED_SET"}, // [0q,[1q,[2q,[3q,[4q
            // DECTEST: has integer between '['  and 'y'
            {CTRL_CSI_PREFIX, OPTION_INTEGERS, 'y', DEC_LED_TEST, "DEC_LED_TEST"}, // [0q,[1q,[2q,[3q,[4q
            {} // NILL
    };

    // === INSTANCE === //

    protected List<IToken> tokenPatterns = new ArrayList();

    public VTxTokenDefs() {
        compile();
    }

    // 'compile' he says.
    private void compile() {
        for (int i = 0; i < tokenDefs.length; i++) {
            addPattern(tokenDefs[i]);
        }
    }

    private void addPattern(Object[] def) {

        TokenOption option = null;
        Character terminatorChar = null;
        if ((def == null) || (def.length == 0)) {
            return;
        }

        Object lastObj = def[def.length - 1];
        // text OR enum resprentation:
        String tokenDescription = lastObj.toString();

        Token token;
        String chars;

        if (def.length == 2) {
            chars = def[0].toString();
            token = (Token) def[1];
        } else if (def.length == 3) {
            chars = def[0].toString();
            if (def[1] instanceof TokenOption) {
                option = (TokenOption) def[1];
                token = (Token) def[2];
            } else if (def[1] instanceof Token) {
                token = (Token) def[1];
                tokenDescription = (String) def[2];
            } else {
                token = null;
            }
        } else if ((def.length == 4) || (def.length == 5)) {
            chars = def[0].toString();
            option = (TokenOption) def[1];
            if (def[2] != null) {
                terminatorChar = def[2].toString().charAt(0);
            } else {
                terminatorChar = null; // prefix token.
            }
            token = (Token) def[3];
        } else {
            token = null;
            chars = null;
        }

        if (token == null) {
            throw new VTxInvalidConfigurationException("Couldn't parse pattern: " + Arrays.toString(def));
        }

        tokenPatterns.add(TokenDef.createFrom(chars.toCharArray(), option, terminatorChar, token, tokenDescription));
    }

    public boolean matchPartial(char[] sequence, byte[] pattern, int index) {

        // sequence already to long?
        if ((sequence == null) || index > sequence.length)
            return false;

        for (int i = 0; i < index; i++)
            if (sequence[i] != pattern[i])
                return false;

        return true;
    }

    public boolean matchFully(char[] sequence, byte[] pattern, int index) {
        if (sequence == null)
            return false;

        // Length must match:
        if (index != sequence.length)
            return false;

        for (int i = 0; i < index; i++) {
            if (sequence[i] != pattern[i]) {
                return false;
            }
        }

        return true;
    }

    public static Object[] findCharToken(char c) {

        for (int i = 0; i < tokenDefs.length; i++) {
            Object[] tokdef = tokenDefs[i];
            if (tokdef.length == 0) {
                continue;
            } else {
                // Single Char Token
                if (tokdef[0].toString().length() == 1) {
                    if (tokdef[0].toString().charAt(0) == c) {
                        return tokdef;
                    }
                }
            }
        }
        return null;
    }

    public List<IToken> getPatterns() {
        return this.tokenPatterns;
    }

    /**
     * Either match FULL token.
     * Either match FULL PREFIX token
     * If no FULL token or PREFIX token can be found, match first <em>partial></em> token.
     */
    public IToken findFirst(byte[] pattern, int patternIndex) {

        int index = 0;
        List<IToken> partials = new ArrayList<>();

        do {
            IToken tokenDef = tokenPatterns.get(index);

            // I) Shortcut: Full match will work for both: Token + option (and options parsed) and Token + NO option:
            if (matchFully(tokenDef.full(), pattern, patternIndex)) {
                return tokenDef;
            }

            // II) Match against FULL or PREFIX Token with OPTION
            if (tokenDef.hasOption()) {
                if (matchFully(tokenDef.prefix(), pattern, patternIndex)) {
                    return tokenDef;
                }
            }
            // III) Partial match against Full or Prefix Token:
            if (matchPartial(tokenDef.full(), pattern, patternIndex)) {
                partials.add(tokenDef);
            }

            index++;
        } while (index < tokenPatterns.size());

        // Optionally sort for priority here:
        if (partials.size() > 0) {
            return partials.get(0);
        }
        return null;
    }
}
