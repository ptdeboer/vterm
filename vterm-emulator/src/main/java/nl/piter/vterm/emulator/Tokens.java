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
        PARAMETER_CHARSET,
        PARAMETER_INTEGERS, //
        PARAMETER_STRING,
        PARAMETER_GRAPHMODE //
    }

    // @formatter:off
    public enum Token {
        // Single CTRL to tokens
        EOF, // -1
        NUL,
        SOH,
        STX,
        ETX,
        EOT,
        ENQ,
        ACK,
        BEL,
        //
        BS,
        HT,
        LF,
        VT,
        FF,
        CR,
        //
        XON,
        XOFF,
        DC1,
        DC2,
        CAN,
        SUB,
        ESC,
        DEL,
        // Double Char Escape: ESC+CHAR:
        SAVE_CURSOR,
        RESTORE_CURSOR,
        APPLICATION_KEYPAD,
        NUMERIC_KEYPAD,
        IND_INDEX,
        NEL_NEXT_LINE,
        HTS_TAB_SET,
        HTC_TAB_CLEAR,
        RI_REVERSE_INDEX,
        // Device, Xterm, App
        DCS_DEVICE_CONTROL_STRING,
        SOS_START_OF_STRING,
        ST_END_OF_STRING,
        SPA_START_OF_GUARDED_AREA,
        EPA_END_OF_GUARDER_AREA,
        DECID_SEND_TERM_ID,
        PM_PRIVACY_MESSAGE,
        REQ_PRIMARY_DA,
        RESPONSE_PRIMARY_DA,
        REQ_SECONDARY_DA,
        RESPONSE_SECONDARY_DA,
        REQ_TERTIARY_DA,
        REQ_XTVERSION,
        DEVICE_STATUS,
        // Misc (used?)
        STRING_END,
        // Screen, Cursor, Lines, Character (font)
        CHARACTER_ATTRS,
        SET_REGION,
        SET_CURSOR,
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
        SCROLL_DOWN,
        BACKWARD_TABS,
        SCROLL_DOWN_OR_MOUSETRACK,
        DELETE_LINES,
        CHARACTER_REPEAT,
        // Actual Character
        CHAR,
        // movement tokens
        UP,
        DOWN,
        LEFT,
        RIGHT,
        // CharSets
        CHARSET_G0,
        CHARSET_G1,
        CHARSET_G0_DES,
        CHARSET_G1_DES,
        CHARSET_G2_DES,
        CHARSET_G3_DES,

        // ====================
        // Dec modes
        // ======================
        DEC_SETMODE,
        DEC_RESETMODE,
        DEC_SCREEN_ALIGNMENT,
        // ---
        // prefix indicators, are NON-Terminating tokens (under construction)
        // ---
        CSI_PREFIX(false),
        OSC_GRAPHMODE_PREFIX(false),
        OSC_GRAPHMODE,
        // todo:
        WINDOW_MANIPULATION,
        XTERM_RESET_MODIFIERS,
        XTERM_GET_MODIFIERS,
        XTERM_SETGET_GRAPHICS,
        XTERM_WIN_MANIPULATION,
        // Legacy not applicable or defunct.
        PRIVACY_MESSAGE,
        APP_PROGRAM_CMD,
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
