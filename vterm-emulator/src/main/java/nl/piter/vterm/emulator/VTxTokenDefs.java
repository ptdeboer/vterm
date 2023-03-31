/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.emulator;

import lombok.extern.slf4j.Slf4j;
import nl.piter.vterm.emulator.tokens.IToken;
import nl.piter.vterm.emulator.tokens.SearchTree;
import nl.piter.vterm.emulator.tokens.TokenDef;
import nl.piter.vterm.exceptions.VTxInvalidConfigurationException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static nl.piter.vterm.emulator.Tokens.Token;
import static nl.piter.vterm.emulator.Tokens.Token.*;
import static nl.piter.vterm.emulator.Tokens.TokenOption;
import static nl.piter.vterm.emulator.Tokens.TokenOption.*;
import static nl.piter.vterm.emulator.VTxCharDefs.*;

@Slf4j
public class VTxTokenDefs {

    /**
     * Simple token table. Store TOKEN as string together with char sequence (as string).
     * This list is searched linear so that the first match is used.<br>
     * The lines always end with a token, which is a TERMINATOR, or a SEQUENCE token.<br>
     * The following token definitions are supported<br>
     * <pre>
     *  { TERMINATOR_CHAR, CHAR_TOKEN}
     *  { UTF8_STRING, TERMINATOR_TOKEN}
     *  { UTF8_STRING, <OPTION>, SEQUENCE_TOKEN } -> CharSet only
     *  { UTF8_STRING, <OPTION>, TERMINATOR_CHAR, SEQUENCE_TOKEN [, COMMENT ]  }
     *  { UTF8_STRING, <OPTION>, TERMINATOR_UTF8_STRING, SEQUENCE_TOKEN [, COMMENT ] }
     * </pre>
     */
    private static final Object[][] tokenDefs = {
            // Single char tokens: 000 - 007 (octal)
            {CTRL_NUL, NUL}, // => Warning: 0x00 => Empty String
            {CTRL_SOH, SOH},
            {CTRL_ETX, ETX},
            {CTRL_STX, STX},
            {CTRL_EOT, EOT},
            {CTRL_ENQ, ENQ},
            {CTRL_ACK, ACK},
            {CTRL_BEL, BEL},
            // 010-017 (octal)
            {CTRL_BS, BS},
            {CTRL_HT, HT},
            {CTRL_LF, LF},
            {CTRL_VT, VT},
            {CTRL_FF, FF},
            {CTRL_CR, CR},
            {CTRL_SI, CHARSET_G0},
            {CTRL_SO, CHARSET_G1},
            //
            {CTRL_CAN, CAN},
            {CTRL_SUB, SUB},
            {CTRL_ESC, ESC}, // Careful ESC is NOT a terminating TOKEN !
            {CTRL_XON, XON},
            {CTRL_XOFF, XOFF},
            {CTRL_DEL, DEL},
            // Double Char Escape codes:
            {CTRL_ESC + "7", SAVE_CURSOR},
            {CTRL_ESC + "8", RESTORE_CURSOR},
            {CTRL_ESC + "D", IND_INDEX},
            {CTRL_ESC + "E", NEL_NEXT_LINE},
            {CTRL_ESC + "H", HTS_TAB_SET},
            {CTRL_ESC + "M", RI_REVERSE_INDEX}, // DELETE_LINE
            {CTRL_ESC + "N", UNSUPPORTED, "Single Shift Select G2 CharSet"},
            {CTRL_ESC + "O", UNSUPPORTED, "Single Shift Select G3 CharSet"},
            {CTRL_ESC + "V", SPA_START_OF_GUARDED_AREA},
            {CTRL_ESC + "W", EPA_END_OF_GUARDER_AREA},
            {CTRL_ESC + "X", SOS_START_OF_STRING},
            {CTRL_ESC + "Z", DECID_SEND_TERM_ID},
            {CTRL_ESC + "\\", ST_END_OF_STRING},
            {CTRL_ESC + "^", PM_PRIVACY_MESSAGE},
            {CTRL_ESC + "=", APPLICATION_KEYPAD}, // opposite NUMERIC
            {CTRL_ESC + ">", NUMERIC_KEYPAD}, // opposite of APPLICATION
            {CTRL_ESC + "<", EXIT_VT52_MODE},
            // Select G0-G3 character set:
            {CTRL_ESC + "(", PARAMETER_CHARSET, CHARSET_G0_DES},
            {CTRL_ESC + ")", PARAMETER_CHARSET, CHARSET_G1_DES},
            {CTRL_ESC + "*", PARAMETER_CHARSET, CHARSET_G2_DES},
            {CTRL_ESC + "+", PARAMETER_CHARSET, CHARSET_G3_DES},
            // Triple Char Escape Codes
            // Not implemented but add filter and detect them anyway
            {CTRL_ESC + "%@", UNSUPPORTED, "Select Default Charset ISO 8859-1"},
            {CTRL_ESC + "%G", UNSUPPORTED, "Select UTF8 Charset"},
            {CTRL_ESC + "#3", UNSUPPORTED, "DEC Double height, top half"},
            {CTRL_ESC + "#4", UNSUPPORTED, "DEC Double height, bottom half"},
            {CTRL_ESC + "#5", UNSUPPORTED, "DEC Single width line"},
            {CTRL_ESC + "#6", UNSUPPORTED, "DEC double width line"},
            {CTRL_ESC + " F", UNSUPPORTED, "7 Bits Controls"},
            {CTRL_ESC + " G", UNSUPPORTED, "8 Bits Controls"},
            {CTRL_ESC + " L", UNSUPPORTED, "Set ANSI conformance level 1 - vt100"},
            {CTRL_ESC + " M", UNSUPPORTED, "Set ANSI conformance level 2 - vt200"},
            {CTRL_ESC + " N", UNSUPPORTED, "Set ANSI conformance level 3 - vt300"},
            // DEC
            {CTRL_ESC + "#8", DEC_SCREEN_ALIGNMENT}, // "DEC Screen aligment Test"},
            // ----------------------------------------------------------------
            // APP, DCS, OSC (graphmode) adn CSI Sequences.
            // Optimization: Prefix must be first Escape + '[' token in token list so that the prefix
            // token is matched first.
            // The PREFIX token triggers the parseOptions() in nextToken() if 2d value is an OPTION.
            // ----------------------------------------------------------------
            // Device and App sequences:
            {CTRL_ESC + "P", PARAMETER_STRING, CTRL_ESC + "\\", DCS_DEVICE_CONTROL_STRING},
            {CTRL_ESC + "_", PARAMETER_STRING, CTRL_ESC + "\\", APP_PROGRAM_CMD},
            // OSC GRAPHMODE (xterm): <ESC>-]
            {CTRL_OSC_PREFIX, PARAMETER_GRAPHMODE, (char)-1, OSC_GRAPHMODE_PREFIX},
            {CTRL_OSC_PREFIX, PARAMETER_GRAPHMODE, CTRL_BEL, OSC_GRAPHMODE},
            {CTRL_OSC_PREFIX, PARAMETER_GRAPHMODE, CTRL_ESC + "\\", OSC_GRAPHMODE},
            // ----------------------------------------------------------------
            // CSI Sequences: <ESC>-[
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, (char)-1, CSI_PREFIX, "Start of CSI Prefix"},
            // 3 char CSIs:
            {CTRL_CSI_PREFIX + "?", PARAMETER_INTEGERS, 'h', DEC_SETMODE},
            {CTRL_CSI_PREFIX + "?", PARAMETER_INTEGERS, 'l', DEC_RESETMODE},
            // Devices/XTerm
            {CTRL_CSI_PREFIX + ">", PARAMETER_INTEGERS, 'c', REQ_SECONDARY_DA},
            {CTRL_CSI_PREFIX + ">", PARAMETER_INTEGERS, 'q', REQ_XTVERSION},
            {CTRL_CSI_PREFIX + "=", PARAMETER_INTEGERS, 'c', REQ_TERTIARY_DA},
            {CTRL_CSI_PREFIX + ">", PARAMETER_INTEGERS, 'm', XTERM_RESET_MODIFIERS},
            {CTRL_CSI_PREFIX + "?", PARAMETER_INTEGERS, 'm', XTERM_GET_MODIFIERS},
            // ----------------------------------------------------------------
            // Beta
            {CTRL_CSI_PREFIX + "?", PARAMETER_INTEGERS, 'S', XTERM_SETGET_GRAPHICS, "XTSMGRAPHICS"},
            {CTRL_CSI_PREFIX + ">", PARAMETER_INTEGERS, 'S', UNSUPPORTED},
            {CTRL_CSI_PREFIX + "?", PARAMETER_INTEGERS, "$p", UNSUPPORTED, "Request DEC private mode (DECRQM)"},
            {CTRL_CSI_PREFIX + "?", PARAMETER_INTEGERS, 'u', UNSUPPORTED},
            {CTRL_CSI_PREFIX + "=", PARAMETER_INTEGERS, 'u', UNSUPPORTED},
            {CTRL_CSI_PREFIX + ">", PARAMETER_INTEGERS, 'u', UNSUPPORTED},
            {CTRL_CSI_PREFIX + "?", PARAMETER_INTEGERS, 'r', UNSUPPORTED},
            {CTRL_CSI_PREFIX + "=", PARAMETER_INTEGERS, 'r', UNSUPPORTED},
            {CTRL_CSI_PREFIX + ">", PARAMETER_INTEGERS, 'r', UNSUPPORTED},
            {CTRL_CSI_PREFIX + "?", PARAMETER_INTEGERS, 's', UNSUPPORTED},
            {CTRL_CSI_PREFIX + "=", PARAMETER_INTEGERS, 's', UNSUPPORTED},
            {CTRL_CSI_PREFIX + ">", PARAMETER_INTEGERS, 's', UNSUPPORTED},
            // ----------------------------------------------------------------
            // 2 char CSIs:
            // Cursors: VT100 and/or XTerm:
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, '@', INSERT_BLANK_CHARS},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'A', UP},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'B', DOWN},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'C', RIGHT},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'D', LEFT},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'E', NEL_NEXT_LINE},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'F', PRECEDING_LINE},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'G', SET_COLUMN},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'H', SET_CURSOR},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'I', FORWARD_TABS},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'J', SCREEN_ERASE},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'K', LINE_ERASE},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'L', INSERT_LINES},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'M', DELETE_LINES},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'P', DEL_CHAR},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'S', SCROLL_UP},
            // one integer=scroll down, 5 integers=mouse track
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'T', SCROLL_DOWN_OR_MOUSETRACK},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'X', ERASE_CHARS},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'Z', BACKWARD_TABS},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, '^', SCROLL_DOWN},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, '`', UNSUPPORTED},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'a', UNSUPPORTED},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'b', CHARACTER_REPEAT},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'c', REQ_PRIMARY_DA},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'd', SET_ROW},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'f', SET_CURSOR},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'g', HTC_TAB_CLEAR},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'h', SET_MODE},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'i', UNSUPPORTED,"Media Copy"},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'l', RESET_MODE},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'm', CHARACTER_ATTRS},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'n', DEVICE_STATUS},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'r', SET_REGION, "DECSTBM – Set Top and Bottom Margin"},
            // Misc
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 't', XTERM_WIN_MANIPULATION, "Window-Manipulation"},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'q', DEC_LED_SET, "DEC_LED_SET"},
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, 'y', DEC_LED_TEST, "DEC_LED_TEST"},
            // Bug Filters
            {CTRL_CSI_PREFIX, PARAMETER_INTEGERS, "%m", UNSUPPORTED, "VI '%m' unknown"},
            {CTRL_OSC_PREFIX + CTRL_ESC + "(", PARAMETER_CHARSET, null, CHARSET_G0_DES, "YaST ncurses unknown"},
            {} // NILL
    };

    // === INSTANCE === //

    protected List<IToken> tokenPatterns = new ArrayList();
    protected SearchTree<IToken> searchTree=new SearchTree();

    public VTxTokenDefs() {
        compile();
    }

    // 'compile' = create SearchTree
    private void compile() {
        for (int i = 0; i < tokenDefs.length; i++) {
            addPattern(tokenDefs[i]);
        }
    }

    private void addPattern(Object[] def) {

        TokenOption option = null;
        byte[] terminatorChars = null;
        if ((def == null) || (def.length == 0)) {
            return;
        }

        Object lastObj = def[def.length - 1];
        // text OR enum representation:
        String tokenDescription = lastObj.toString();

        Token token;
        byte[] chars;


        if (def.length == 2) {
            // CHAR, TOKEN
            chars = getStringBytes(def[0]);
            token = (Token) def[1];
        } else if (def.length == 3) {
            // PREFIX, TOKEN, DESCRIPTION
            // PREFIX, OPTION, TOKEN
            chars = getStringBytes(def[0]);
            if (def[1] instanceof TokenOption) {
                option = (TokenOption) def[1];
                token = (Token) def[2];
            } else if (def[1] instanceof Token) {
                token = (Token) def[1];
                tokenDescription = (String) def[2];
            } else {
                log.error("Invalid token def: '{}'", Arrays.toString(def));
                token = null;
            }
        } else if ((def.length >= 4)) {
            // PREFIX, OPTION, TERMINATOR, TOKEN [, DESCRIPTION ]
            chars = getStringBytes(def[0]);
            option = (TokenOption) def[1];
            if (def[2] != null) {
                terminatorChars = getStringBytes(def[2]);
            } else {
                terminatorChars = null; // prefix token.
            }
            token = (Token) def[3];
        } else {
            log.debug("Skipping: {}", Arrays.toString(def));
            token = null;
            chars = null;
        }

        if (token == null) {
            throw new VTxInvalidConfigurationException("Couldn't parse pattern: " + Arrays.toString(def));
        }

        IToken tokenDef = TokenDef.createFrom(chars, option, terminatorChars, token, tokenDescription);
        tokenPatterns.add(tokenDef);
        searchTree.add(tokenDef);
    }

    private byte[] getStringBytes(Object obj) {
        return obj.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static Object[] findCharToken(char c) {

        for (int i = 0; i < tokenDefs.length; i++) {
            Object[] tokdef = tokenDefs[i];
            if (tokdef.length != 0) {
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
     * Order of matching:
     * <ul>
     * <li> Either match FULL token.
     * <li> Either match FULL PREFIX token
     * <li> If no FULL token or PREFIX token can be found, match first <em>partial</em> token.
     * </ul>
     */
    public IToken findFirst(byte[] pattern, int patternIndex) {

        // Partial state matching now functional?
        // IToken fullMatch = searchTree.findFull(pattern, patternIndex);
        IToken prefix = searchTree.findPartial(pattern, patternIndex);
        if(prefix!=null) {
            return prefix;
        }

        return null;
    }
}
