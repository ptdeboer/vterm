/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.emulator;

public class Tokens {

    public enum TokenOption {
        // NON_PRINTABLE, //
        // NON_DIGIT, //
        OPTION_INTEGERS, //
        OPTION_GRAPHMODE //
    }

    // @formatter:off
    public enum Token {
        // ===
        // Single CTRL to tokens
        // ===
        EOF, // -1
        NUL,
        ETX,
        EOT,
        EOD,
        ENQ,
        BEEP,
        BS,
        HT,
        LF,
        VT,
        FF,
        CR,
        DC1,
        DC2,
        CAN,
        SUB,
        ESC,
        DEL,
        CHARSET_G0,
        CHARSET_G1,
        //===
        //Double Char Escape: ESC+CHAR:
        //===
        SAVE_CURSOR,
        RESTORE_CURSOR,
        APPLICATION_KEYPAD,
        NUMERIC_KEYPAD,
        INDEX,
        NEXT_LINE,
        TAB_SET,
        TABCLEAR,
        REVERSE_INDEX,
        SEND_TERM_ID,
        // Xterm graph mode
        OSC_GRAPHMODE,
        // SEND/RESPONSE
        SEND_PRIMARY_DA,
        RESPONSE_PRIMARY_DA,
        SEND_SECONDARY_DA,
        RESPONSE_SECONDARY_DA,
        // ===
        // Escape Sequences \E[ & \E[?
        // ===
        // screen manupilation:
        SET_REGION,
        SET_CURSOR,
        // SET_CURSORX,
        SET_FONT_STYLE,
        SET_MODE,
        RESET_MODE,
        LINE_ERASE,
        SCREEN_ERASE,
        SET_COLUMN,
        DEL_CHAR,
        ERASE_CHARS,
        SET_ROW,
        INSERT_BLANK_CHARS,
        PRECEDING_LINE,
        FORWARD_TABS,
        INSERT_LINES,
        SCROLL_UP,
        BACKWARD_TABS,
        SCROLL_DOWN_OR_MOUSETRACK,
        DELETE_LINES,
        // Character sequence
        CHAR, // sequence of one or more UTF-8 (ansi==false) characters !
        // movement tokens (Send only) (more are added as plain text, not as tokens)
        UP,
        DOWN,
        LEFT,
        RIGHT, //ENTER, TAB, BACKSPACE,
        DEVICE_STATUS,
        // ====================
        // Dec Privates ?
        // ======================
        DEC_SETMODE,
        DEC_RESETMODE,
        CHARSET_G0_UK,
        CHARSET_G0_US,
        CHARSET_G0_DUTCH,
        CHARSET_G0_FRENCH,
        CHARSET_G0_FINNISH,
        CHARSET_G0_SWEDISH,
        CHARSET_G0_GERMAN,
        CHARSET_G0_SPANISH,
        CHARSET_G0_OTHER, // Catch all see token argument for language ID.
        CHARSET_G0_GRAPHICS,
        CHARSET_G0_ALT_ROM_NORMAL,
        CHARSET_G0_ALT_ROM_SPECIAL,
        CHARSET_G1_UK,
        CHARSET_G1_US,
        CHARSET_G1_DUTCH,
        CHARSET_G1_FRENCH,
        CHARSET_G1_GERMAN,
        CHARSET_G1_SPANISH,
        CHARSET_G1_OTHER, // Catch all see token argument for language ID.
        CHARSET_G1_GRAPHICS,
        CHARSET_G1_ALT_ROM_NORMAL,
        CHARSET_G1_ALT_ROM_SPECIAL,
        DEC_SCREEN_ALIGNMENT,
        // prefix indicators, are NON-Terminating tokens (under construction)
        CSI_PREFIX_START(false),
        // todo:
        WINDOW_MANIPULATION,
        CHARACTER_ATTRS,
        XTERM_RES_RESET_MODIFIERS,
        XTERM_WIN_MANIPULATION,
        STRING_START,
        STRING_END,
        // Misc.
        DEC_LED_SET,
        DEC_LED_TEST,
        EXIT_VT52_MODE,
        UNSUPPORTED, // known, but not supported
        ERROR // ERROR sequence, tokenizer holds raw character sequence in buffer
        ;

        // terminators
        private final boolean terminator;

        Token() {
            this.terminator = true;
        }

        Token(boolean val) {
            this.terminator = val;
        }

        public boolean isTerminator() {
            return terminator;
        }
    }

}
