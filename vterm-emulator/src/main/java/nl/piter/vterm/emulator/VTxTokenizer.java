/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.emulator;

import lombok.extern.slf4j.Slf4j;
import nl.piter.vterm.emulator.Tokens.Token;
import nl.piter.vterm.emulator.Tokens.TokenOption;
import nl.piter.vterm.emulator.Util.MiniBuffer;
import nl.piter.vterm.emulator.tokens.IToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Simple tokenizer class. Issue nextToken() to parse inputStream.
 * getBytes() returns parsed byte sequence.
 */
@Slf4j
public class VTxTokenizer {

    private static final int MAX_MINIBUF = 256;

    // =======================================================================

    private final VTxTokenDefs tokenDefs;
    private final boolean ansi_mode = false;

    // Mini buffer for read ahead and pattern.
    // Must be able to hold long (XTERM) string arguments. (title, etc.):
    private final MiniBuffer readAheadBuffer = new MiniBuffer(MAX_MINIBUF);
    private final MiniBuffer patternBuffer = new MiniBuffer(MAX_MINIBUF);

    // input
    private final InputStream inputStream;

    //
    private final Logger vtermTokenRecorder = LoggerFactory.getLogger("VTERM-RECORDER");

    /**
     * Options/Arguments for CSI sequences.
     */
    public class Arguments {
        private final int[] integerArgs = new int[16];
        private int numIntegerArgs;
        private int dummyND;
        private String stringArg;
        private int dummyNP;

        public int numArgs() {
            return numIntegerArgs;
        }

        public int arg1() {
            return integerArgs[0];
        }

        public int arg2() {
            return integerArgs[1];
        }

        public String strArg() {
            return stringArg;
        }

        public int[] intArgs() {
            if (this.numIntegerArgs == 0) {
                return new int[]{};
            }
            return Arrays.copyOfRange(this.integerArgs, 0, this.numIntegerArgs);
        }

        public int intArg(int i) {
            if (i < this.numIntegerArgs) {
                return integerArgs[i];
            }
            return -1;
        }

        public void clear() {
            this.numIntegerArgs = 0;
            for (int i = 0; i < integerArgs.length; i++) {
                this.integerArgs[i] = 0;
            }
            this.stringArg = null;
            this.dummyND = -1;
            this.dummyNP = -1;
        }

        public int[] ints() {
            return integerArgs;
        }
    }

    public class State {
        protected boolean escSequence;
        protected boolean optIntegersParsed;
        protected boolean optGraphModeParsed;
        protected int errorChar;
        protected Token matchedToken;
        protected IToken matchedIToken;
        //
        protected char[] charBuffer = new char[16];
        protected char[] stringBuffer = new char[256];

        public void reset() {
            this.escSequence = false;
            this.optIntegersParsed = false;
            this.optGraphModeParsed = false;
            this.errorChar = 0;
            this.matchedToken = null;
            this.matchedIToken = null;
        }
    }

    private final State state = new State();
    private final Arguments arguments = new Arguments();

    public VTxTokenizer(InputStream inps) {
        this.inputStream = inps;
        this.tokenDefs = new VTxTokenDefs();
    }

    public Arguments args() {
        return this.arguments;
    }

    public int readChar() throws IOException {
        if (readAheadBuffer.size() > 0) {
            return readAheadBuffer.pop();
        } else {
            return inputStream.read();
        }
    }

    public void ungetChar(int c) throws IOException {
        if (readAheadBuffer.freeSpace() <= 0) {
            throw new IOException("ungetChar(): buffer overflow with capacity: " + readAheadBuffer.size());
        }
        readAheadBuffer.put((byte) c);
    }

    public byte[] getBytes() {
        return this.patternBuffer.getBytes();
    }

    protected void reset() {
        patternBuffer.reset();
        this.arguments.clear();
        this.state.reset();
    }

    /**
     * Ad Hoc tokenizer, need to use proper scanner.
     */
    public Token nextToken() throws IOException {

        VTxTokenizer tokenizer = this;
        reset();

        // Central State Machine Parser
        do {
            // get char -> buffer:
            int c = tokenizer.readChar();
            patternBuffer.put(c);

            // log character bugger only at finest logging level !
            if (log.isTraceEnabled()) {
                log.trace("+ appending [{}]'{}': buffer='{}'", String.format("%02x", c), (char) c, Util.prettyByteString(patternBuffer.getBytes()));
            }

            //===============================================================
            // From the Web:
            //> Hmmm... I didn't realize LF was ignored in a CSI.  I had assumed
            //> that *all* characters (except flow control) were expected *literally*
            //> in these sequences!
            //
            //No, it's not at all ignored, nor is it treated as a literal.  If you
            //send, for example, the sequence "ESC [ LF C", then the terminal will
            //move the cursor down one line (scrolling if necessary) and then to the
            //right one position (stopping at the right margin if necessary).

            Token c0Token = matchC0Token(c);
            if (c0Token != null) {
                // ----------------------------
                // <ESC> -> start of sequence:
                // ----------------------------
                if (c0Token == Token.ESC) {
                    if (this.state.escSequence) {
                        // PARSE error: received ESC wil in an ESC sequence.
                        this.state.errorChar = c;
                        ungetChar(c); // Keep start of next ESC sequence.
                        return fullMatch(Token.ERROR);
                    } else {
                        state.escSequence = true;
                        continue;
                    }
                }

                // Escape mode ? => match C0 Token
                if (!state.escSequence) {
                    return fullMatch(c0Token);
                }

                // C0 char while in Escape Sequence Mode !
                // Keep current pattern, handle C0 first:
                log.warn("Received C0 during ESCAPE Sequence. C0: '{}'", c0Token);

                if (log.isDebugEnabled()) {
                    log.debug("> Pattern={}", Util.prettyByteString(patternBuffer.getBytes()));
                }

                patternBuffer.pop(); // Eat C0, but preserve rest of buffer.

                // Dump current pattern in readahead buffer without (filtered) C0:
                {
                    while (patternBuffer.size() > 0) {
                        ungetChar(patternBuffer.pop());
                    }
                    patternBuffer.reset();
                }
                // Handle superfluous C0 first:
                return fullMatch(c0Token);
            }

            // ================
            // Check Char
            // ================

            if (!state.escSequence) {
                Token charToken = matchCharToken(c);
                if (charToken != null) {
                    return fullMatch(charToken);
                }

                charToken = matchUTF8Token(c);
                if (charToken != null) {
                    return fullMatch(charToken);
                }

                state.errorChar = c;
                break;
            }

            // =================
            // Sequences
            // =================

            boolean fullMatch;
            boolean prefixMatch = false;
            boolean partialPrefixMatch = false;

            IToken itoken = this.tokenDefs.findFirst(patternBuffer.bytes(), patternBuffer.index());
            if (itoken != null) {
                fullMatch = itoken.full().length == patternBuffer.index();
                if (!fullMatch) {
                    prefixMatch = (patternBuffer.index() == itoken.prefix().length);
                    partialPrefixMatch = (patternBuffer.index() < itoken.prefix().length);
                }
            } else {
                log.warn("- unrecognized pattern: {}", Util.prettyByteString(patternBuffer.getBytes()));
                state.errorChar = c;
                break;
            }

            log.trace(">>> sequenceToken :'{}':'{}'", itoken.token(), Util.prettyByteString(new String(itoken.chars()).getBytes()));
            log.trace(">>> fullmatch     :'{}'", fullMatch);
            log.trace(">>> prefixMatch   :'{}'", prefixMatch);
            log.trace(">>> partialPrefix :'{}'", partialPrefixMatch);

            // Need better state matcher!!!
            if (partialPrefixMatch) {
                // Still in 'prefix' mode. No exact prefix nor full match.
                continue;
            }

            if ((!fullMatch) && (!prefixMatch)) {
                state.errorChar = c;
                break;
            }

            // ---
            // Dirty tokenizer, check lookahead if there is an integer (or osc-graphmode parameter list) option, so
            // either integer list or graphmode arguments can be parsed.
            // Then continue to find complete token matching against patternBuffer !
            // ---
            if ((itoken.option() == TokenOption.OPTION_GRAPHMODE)) {
                // Careful: LOOKAHEAD for digit but ALLOW empty parameter ';':
                if ((isDigit(lookahead()) || (lookahead() == ';'))) {
                    this.state.optGraphModeParsed = parseGraphModeArguments();
                }
                log.debug("state.optGraphModeParsed:{}: ({},'{}')", state.optGraphModeParsed, arguments.integerArgs[0], arguments.stringArg);
            }

            if (prefixMatch && (itoken.option() == TokenOption.OPTION_INTEGERS)) {
                // Careful: LOOKAHEAD for digit but ALLOW empty parameter ';':
                if ((isDigit(lookahead()) || (lookahead() == ';'))) {
                    arguments.numIntegerArgs = parseIntegerList(arguments.integerArgs);
                    this.state.optIntegersParsed = (arguments.numIntegerArgs > 0);
                }
            }

            if (fullMatch) {
                log.debug("FULL MATCH: '{}' => sequence: {} with args: {}", itoken.token(),
                        Util.prettyByteString(patternBuffer.getBytes()),
                        getFormattedArguments());
                tokenizer.state.matchedIToken = itoken;
                return fullMatch(itoken.token());
            }
            // Fall through;
            this.state.errorChar = c;
        } while (state.escSequence);

        // === ERROR FALL THROUGH ===
        log.error("- error pattern Sequence={}", Util.prettyByteString(patternBuffer.getBytes()));
        log.error("*** Unexpected char at #{}:0x{}='{}'", patternBuffer.size(), Util.byte2hexstr(this.state.errorChar), this.state.errorChar);
        return fullMatch(Token.ERROR);
    }

    private Token matchC0Token(int c) {
        if (c <= -1) {
            return Token.EOF;
        }

        switch (c) {
            case 0x00:
                return Token.NUL;
            case VTxTokenDefs.CTRL_ETX:
                return Token.ETX;
            case VTxTokenDefs.CTRL_EOT:
                return Token.EOT;
            case VTxTokenDefs.CTRL_ENQ:
                return Token.ENQ;
            case VTxTokenDefs.CTRL_BS:
                return Token.BS;
            case VTxTokenDefs.CTRL_HT:
                return Token.HT;
            case VTxTokenDefs.CTRL_CR:
                return Token.CR;
            case VTxTokenDefs.CTRL_LF:
                return Token.LF;
            case VTxTokenDefs.CTRL_VT:
                return Token.VT;
            case VTxTokenDefs.CTRL_FF:
                return Token.FF;
            case VTxTokenDefs.CTRL_CAN:
                return Token.CAN;
            case VTxTokenDefs.CTRL_SUB:
                return Token.SUB;
            case VTxTokenDefs.CTRL_ESC:
                return Token.ESC;
            case VTxTokenDefs.CTRL_BEL:
                return Token.BEEP;
            case VTxTokenDefs.CTRL_SI:
                return Token.CHARSET_G0;
            case VTxTokenDefs.CTRL_SO:
                return Token.CHARSET_G1;
            default: {
                if (c < 0x20) {
                    log.error("Unknown C0 Character:#{}\n", c);
                    return Token.ERROR;
                }
                return null;
            }
        }
    }

    private Token fullMatch(Token token) {

        vtermTokenRecorder.debug("{x},{},[{},'{}'])",
                patternBuffer.getBytes(),
                token, args().intArgs(),
                (args().stringArg != null ? args().stringArg : ""));

        this.state.matchedToken = token;
        if (log.isTraceEnabled()) {
            log.trace("MATCHED:{},args={}", this.state.matchedToken, getFormattedArguments());
        }
        return token;
    }

    private Token matchCharToken(int c) {
        // default 7-bit range:
        if (isChar(c)) {
            return Token.CHAR;
        }
        return null;
    }

    private Token matchUTF8Token(int c) throws IOException {
        // check utf-8
        if (((c & 0x80) > 0) && (!ansi_mode)) {
            int num = 1; //already have first byte

            // utf-8 can exist of 6 bytes length (32bits encoded)
            // binary prefix are:
            //  110xxxxx (c0) for 2 bytes
            //  1110xxxx (e0) for 3 bytes
            //  11110xxx (f0) for 4 bytes
            //  111110xx (f8) for 5 bytes
            //  1111110x (fc) for 6 bytes

            if ((c & 0xe0) == 0xc0) {
                num = 2;
            } else if ((c & 0xf0) == 0xe0) {
                num = 3;
            } else if ((c & 0xf8) == 0xf0) {
                num = 4;
            } else if ((c & 0xfc) == 0xf8) {
                num = 5;
            } else if ((c & 0xfd) == 0xfc) {
                num = 6;
            }

            byte[] utfBytes = new byte[num];
            utfBytes[0] = (byte) c;

            // read bytes as-is:
            for (int i = 1; i < num; i++) {
                utfBytes[i] = readUByte(); // put into byte buffer
            }

            String utf8 = new String(utfBytes, StandardCharsets.UTF_8);
            log.info("Is this UTf8? : '{}' => '{}'", Util.prettyByteString(utfBytes), utf8);
            this.patternBuffer.setBytes(utfBytes, num);
            return Token.CHAR;
        }
        return null;
    }

    /**
     * Read Unsigned Byte value: 0<= value <=255. This method does NOT return values < 0 ! If this
     * is the case an IOEception is thrown. This contrary to getChar(), which may return -1 in the
     * case of an EOF.
     */
    private byte readUByte() throws IOException {
        int c = readChar();

        if (c < 0)
            throw new IOException("EOF: End of stream");
        // cast unsigned byte value:
        return (byte) (c & 0x00ff);
    }

    private boolean isChar(int c) {
        return (c >= 0x20) && (c <= 0x7f);
    }

    private String parseInt() throws IOException {
        boolean cont = true;
        int index = 0;
        while (cont && index < state.charBuffer.length) {
            int digit = this.readChar();
            if (isDigit(digit)) {
                state.charBuffer[index++] = (char) digit;
                cont = true;
            } else {
                this.ungetChar(digit);
                cont = false;
            }
        }
        if (index == 0) {
            return null;
        }
        return new String(state.charBuffer, 0, index);
    }

    private String parseString() throws IOException {

        int index = 0;
        while (index < state.stringBuffer.length) {
            int c = this.readChar();
            if (isPrintable(c)) {
                state.stringBuffer[index++] = (char) c;
            } else {
                this.ungetChar(c);
                break;
            }
        }
        if (index == 0) {
            return "";
        }
        return new String(state.stringBuffer, 0, index);
    }

    /**
     * Read one char put it into the read ahead buffer
     * and return it.
     */
    private int lookahead() throws IOException {
        int c = this.readChar();
        this.ungetChar(c);
        return c;
    }

    /**
     * parse (optional) arguments: [ <INT> ] [ ; <INT> ]*
     */
    private int parseIntegerList(int[] array) throws IOException {
        int numInts = 0;
        //
        while (true) {
            String intstr = parseInt();

            if (intstr != null) {
                array[numInts++] = Integer.valueOf(intstr);
            }

            int digit = readChar();

            if (digit == ';') {
                // check for empty value, but only after ';';
                if (intstr == null) {
                    array[numInts++] = 0;
                }
                continue; // parse next integer
            } else {
                // unknown char: put back and return list
                ungetChar(digit);
                break; // end of argument list
            }
        }
        log.debug("- parseIntegerList(): '{}'", Arrays.toString(Arrays.copyOfRange(array, 0, numInts)));
        return numInts;
    }

    /**
     * parse graph mode: <Int> <ND> <String> <NP>
     */
    private boolean parseGraphModeArguments() throws IOException {
        String intstr = parseInt();
        if (intstr != null) {
            arguments.integerArgs[0] = Integer.valueOf(intstr);
            arguments.numIntegerArgs = 1;
        }

        // ND: any non-digit char, typically ';'.
        arguments.dummyND = readChar();
        if (intstr == null) {
            arguments.integerArgs[0] = 0;
            arguments.numIntegerArgs = 1;
        }

        String argStr = parseString();
        arguments.stringArg = argStr;

        // NP: any non-printable char: typically BEEP (\007)
        arguments.dummyNP = readChar();
        return true;
    }

    private boolean isDigit(int digit) {
        return (('0' <= digit) && (digit <= '9'));
    }

    // allowed char set ?
    private boolean isPrintable(int c) {
        return isChar(c);
    }

    /**
     * return byte buffer as text using specified encoding
     */
    public String getText(String encoding) {
        try {
            return patternBuffer.toString(encoding);
        } catch (UnsupportedEncodingException e) {
            return new String(patternBuffer.getBytes()); // defualt !
        }
    }

    public String getFormattedArguments() {
        String str = "[";

        if (args().numIntegerArgs > 0) {
            for (int i = 0; i < args().numIntegerArgs; i++) {
                str += args().integerArgs[i];
                if (i < args().numIntegerArgs - 1) {
                    str += ";";
                }
            }
            // for graph mode
            if (args().stringArg != null) {
                str += ",'" + args().stringArg + "'";
            }
        }
        str = str + "]";
        return str;
    }

    public IToken getIToken() {
        return state.matchedIToken;
    }

}
