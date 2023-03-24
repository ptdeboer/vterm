/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.emulator;

import lombok.extern.slf4j.Slf4j;
import nl.piter.vterm.api.CharacterTerminal;
import nl.piter.vterm.api.EmulatorListener;
import nl.piter.vterm.emulator.Tokens.Token;
import nl.piter.vterm.ui.charpane.StyleChar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static nl.piter.vterm.emulator.Util.prettyByteString;
import static nl.piter.vterm.emulator.VTxTokenDefs.CTRL_ESC;

/**
 * Implementation of most VT100 codes, VT102, and some xterm/xterm-256-color;
 */
@Slf4j
public class VTxEmulator implements Emulator {

    public static class EmulatorState {

        // Region is INCLUSIVE from y1 to EXCLUSIVE y2;
        protected boolean hasRegion;
        protected int region_y1 = 0; // 1;
        protected int region_y2 = 0; //term_height;

        // tabs
        protected int tabSize = 8;

        // size
        protected int nr_columns;
        protected int nr_rows;

        // Cursors
        private boolean applicationCursorKeys;
        private int savedCursorX;
        private int savedCursorY;
        private boolean savedLfc;

        // DEC MODE(s)
        protected boolean decModeAutoWrap = false;
        protected boolean lfc;

    }

    private InputStream errorInput;
    private final OutputStream outputStream;

    private boolean isConnected = false;
    private String termType;
    private String encoding = "UTF-8";

    private Object haltMutex = new Object();
    private Object terminateMutex = new Object();

    private boolean signalHalt = false;
    private boolean signalTerminate = false;

    // Emulator reads from Tokenizer:
    private VTxTokenizer tokenizer = null;
    // Emulator outputs character to terminal:
    private CharacterTerminal term = null;

    private final List<EmulatorListener> listeners = new ArrayList();

    private final byte[] single = new byte[1];
    private final EmulatorState state = new EmulatorState();

    /**
     * Construct new Terminal Emulator. Reads and writes from input- and output- streams and plots to CharacterTerminal.
     */
    public VTxEmulator(CharacterTerminal term, InputStream inputStream, OutputStream outputStream) {
        setTerm(term);
        setInputStream(inputStream);
        this.outputStream = outputStream;
        state.nr_columns = term.getNumColumns();
        state.nr_rows = term.getNumRows();
    }

    void setTerm(CharacterTerminal term) {
        this.term = term;
    }

    void setInputStream(InputStream inps) {
        this.tokenizer = new VTxTokenizer(inps);
    }

    /**
     * Reset states, but do NO disconnect.
     */
    public void reset() {
        state.nr_columns = term.getNumColumns();
        state.nr_rows = term.getNumRows();
    }

    public void send(byte b) throws IOException {
        single[0] = b;
        send(single);
    }

    public void send(byte[] code) throws IOException {
        if (code == null) {
            log.error("Cowardly refusing to send NULL bytes");
            return;
        }

        synchronized (this.outputStream) {
            this.outputStream.write(code);
            this.outputStream.flush();
        }
    }

    /**
     * Check whether there is text from stderr which is connected to the Terminal implementation.
     */
    protected void readErrorStream() throws IOException {
        if (errorInput == null)
            return;

        int MAX = 1024;
        byte[] buf = new byte[MAX + 1];

        if (this.errorInput.available() > 0) {
            int size = this.errorInput.available();

            if (size > MAX)
                size = 1024;

            int numread = errorInput.read(buf, 0, size);

            buf[numread] = 0;

            String errstr = new String(buf, 0, numread);
            log.error("(stderr:){}",errstr);
        }

    }

    public void setErrorInput(InputStream errorStream) {
        this.errorInput = errorStream;
    }

    public boolean isConnected() {
        return isConnected;
    }

    protected void setConnected(boolean val) {
        isConnected = val;
    }

    public String getTermType() {
        return termType;
    }

    /**
     * Set/send new TERM type.
     */
    public void setTermType(String type) {
        this.termType = type;
    }

    public String getEncoding() {
        return this.encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    // ======================
    //
    // ======================

    @Override
    public void addListener(EmulatorListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeListener(EmulatorListener listener) {
        this.listeners.remove(listener);
    }

    protected void fireGraphModeEvent(int type, String text) {
        for (EmulatorListener listener : listeners) {
            listener.notifyGraphMode(type, text);
        }
    }

    protected void fireResizedEvent(int columns, int rows) {
        for (EmulatorListener listener : listeners) {
            listener.notifyResized(columns, rows);
        }
    }

    public void signalHalt(boolean val) {
        this.signalHalt = val;

        synchronized (haltMutex) {
            if (!val)
                haltMutex.notifyAll();
        }
    }

    public void step() {
        // when halted, a notify will execute one step in the terminal
        synchronized (haltMutex) {
            haltMutex.notifyAll();
        }
    }

    public void signalTerminate() {
        this.signalTerminate = true;

        synchronized (terminateMutex) {
            terminateMutex.notifyAll();
        }
    }

    public boolean sendSize(int cols, int rows) {
        state.nr_columns = cols;
        state.nr_rows = rows;
        this.state.region_y1 = 0;
        this.state.region_y2 = rows;
        log.error("FIXME: sendSize(): Doesn't work");
        return false;
    }

    /**
     * Update terminal size and region without sending control sequences.
     */
    public boolean updateRegion(int cols, int rows, int y1, int y2) {
        state.nr_columns = cols;
        state.nr_rows = rows;
        this.state.region_y1 = y1;
        this.state.region_y2 = y2;
        return true;
    }

    public int[] getRegion() {
        return new int[]{state.nr_columns, state.nr_rows, this.state.region_y1, this.state.region_y2};
    }

    public boolean sendTermSize() {
        log.warn("sendTermSize():[{},{}]", state.nr_columns, state.nr_rows);

//        int r = state.nr_rows;
//        int c= state.nr_columns;
//
//        byte pr1 = (byte) ('0' + ((r / 10) % 10));
//        byte pr2 = (byte) ('0' + (r % 10));
//        byte pc1 = (byte) ('0' + (c / 100) % 10);
//        byte pc2 = (byte) ('0' + (c / 10) % 10);
//        byte pc3 = (byte) ('0' + (c % 10));
//
//
//        byte[] bytes = {(byte) CTRL_ESC, '[', '1','8',';', pr1, pr2, ';', pc1, pc2,pc3, 't'};
//
//
//        try {
//            this.send(bytes);
//        } catch (IOException e) {
//            checkIOException(e, true);
//        }

        return false;

    }

    public void sendTermType() {

        // *** Report from 'vttest' when using it inside xterm.
        // For some reason when connecting through ssh, this works, but not using
        // the homebrew 'ptty.lxe'  file. (is does when forking a 'bin/csh' ).
        // Report is:    <27> [ ? 1 ; 2 c  -- means VT100 with AVO (could be a VT102)
        // Legend:  AVO = Advanced Video Option

        // I am vt10x compatible:
        byte[] bytes = {CTRL_ESC, '[', '?', '1', ';', '2', 'c'};

        try {
            this.send(bytes);
        } catch (IOException e) {
            checkIOException(e, true);
        }
    }


    public void start() {
        state.nr_columns = term.getNumColumns();
        state.nr_rows = term.getNumRows();
        this.state.region_y1 = 0;
        this.state.region_y2 = state.nr_rows;

        log.info("<<<Session Started>>>");
        setConnected(true);
        while (!signalTerminate) {
            synchronized (haltMutex) {
                if (this.signalHalt) {
                    try {
                        this.haltMutex.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                nextToken();
            }
            // Catch ALL and continue!!
            catch (Exception e) {
                log.error("nextToken():Exception >>>", e);
            }
        }// while

        setConnected(false);
        log.info("<<<Session Ended>>>");
    }

    protected void nextToken() throws IOException {

        readErrorStream();

        int x = term.getCursorX();
        int y = term.getCursorY();

        Token token = tokenizer.nextToken();

        // Text representation parse bytes sequence
        byte[] bytes = tokenizer.getBytes();

        int arg1 = 0;
        int arg2 = 0;

        int numIntegers = tokenizer.args().numArgs();

        if (numIntegers > 0)
            arg1 = tokenizer.args().arg1();

        if (numIntegers > 1)
            arg2 = tokenizer.args().arg2();

        int increment = 1;
        if (arg1 > 0) {
            increment = arg1;
        }

        switch (token) {
            case EOF:
                log.debug("EOF: Connection Closed.");
                signalTerminate = true;
                break;
            case EOT:
                log.debug("EOT: Connection Closed.");
                signalTerminate = true;
                break;
            case NUL:
            case DEL:
                //ignore
                break;
            case SEND_TERM_ID:
                // supported ?
                log.warn("***Fixme: Request Identify not tested");
                this.sendTermType();
                break;
            case BEEP:
                term.beep();
                break;
            case HT: { // HORIZONTAL TAB
                x = ((x / state.tabSize + 1) * state.tabSize);
                if (x >= state.nr_columns) {
                    x = 0;
                    y += 1;
                }
                setCursor(x, y);
                break;
            }
            case BS: { // backspace
                x -= 1;
                if (x < 0) {
                    y -= 1;
                    x = state.nr_columns - 1;
                }
                setCursor(x, y);
                break;
            }
            case LF:
            case VT: // Vertical Tab
            case FF: {
                // MIN(nr_rows,region);
                int maxy = state.nr_rows;
                if (state.region_y2 < maxy) {
                    maxy = state.region_y2;
                }
                // Auto LineFeed when y goes out of bounds (or region)
                if (y + 1 >= maxy) {
                    // scroll REGION
                    term.scrollRegion(this.state.region_y1, maxy, 1, true);
                    y = maxy - 1; // explicit keep cursor in region.
                } else {
                    y += 1;
                }
                setCursor(x, y);
                log.debug("FF: New Cursor (x,y)=[{},{}]", x, y);
                break;
            }
            case CR: { // carriage return
                x = 0;
                setCursor(x, y);
                break;
            }
            case UP:
                moveCursor(0, -increment);
                break;
            case DOWN:
                moveCursor(0, +increment);
                break;
            case LEFT:
                moveCursor(-increment, 0);
                break;
            case RIGHT:
                moveCursor(+increment, 0);
                break;
            case SAVE_CURSOR:
                saveCursor();
                break;
            case RESTORE_CURSOR:
                restoreCursor();
                break;
            case SET_REGION: {
                if (numIntegers == 0) {
                    //reset
                    state.region_y1 = 0;
                    state.region_y2 = state.nr_rows;
                    state.hasRegion = false;
                } else {
                    state.region_y1 = arg1 - 1; // inclusive ->inclusive (-1)
                    state.region_y2 = arg2; // inclusive -> exclusive (-1+1)
                    state.hasRegion = true;
                }
                break;
            }
            case SET_COLUMN: {
                setCursor(arg1 - 1, y);
                break;
            }
            case SET_ROW: {
                setCursor(x, arg1 - 1);
                break;
            }
            case DEL_CHAR: {
                // delete char under cursor, shift characters right of cursor to the left !
                int num = 1;
                if (numIntegers > 0)
                    num = arg1;
                // multi delete is move chars to left
                term.move(x + num, y, state.nr_columns - x - num, 1, x, y);
                break;
            }
            case ERASE_CHARS: {
                int n = arg1;
                for (int i = 0; i < n; i++)
                    term.putChar(" ".getBytes(StandardCharsets.UTF_8), x + i, y);
                setCursor(x, y);
                break;
            }
            case DELETE_LINES: {
                int n = arg1;
                for (int i = 0; i < n; i++)
                    for (int j = 0; j < state.nr_columns - 1; j++)
                        term.putChar((byte)0x20, j, y + i);
                setCursor(x, y);
                break;
            }
            case INDEX: { // move down
                if (y + 1 >= this.state.region_y2) {
                    // move down scrollRegion up:
                    term.scrollRegion(this.state.region_y1, this.state.region_y2, 1, true);
                } else {
                    y++;
                    setCursor(x, y);
                }
                break;
            }
            case NEXT_LINE: { // move down
                if (y + 1 >= this.state.region_y2) {
                    // move down scrollRegion up:
                    scrollLines(1, true);
                    setCursor(0, y);
                } else {
                    y++;
                    setCursor(0, y);
                }
                break;
            }
            case REVERSE_INDEX: { // move up
                if ((y - 1) < this.state.region_y1) {
                    // move up scrollRegion down:
                    term.scrollRegion(this.state.region_y1, this.state.region_y2, 1, false);
                } else {
                    y--;
                    setCursor(x, y);
                }
                break;
            }
            case INSERT_LINES: {
                //default: one
                int numlines = 1;

                if (arg1 > 0)
                    numlines = arg1 + 1;

                // insert at current position: scroll down:
                term.scrollRegion(y, this.state.region_y2, numlines, false);
                break;
            }
            case SET_CURSOR: {
                if (numIntegers > 0)
                    y = arg1 - 1;
                else
                    y = 0;

                if (numIntegers > 1)
                    x = arg2 - 1;
                else
                    x = 0;

                setCursor(x, y);
                break;
            }
            case LINE_ERASE: {
                int mode = 0;
                if (numIntegers > 0)
                    mode = arg1;
                log.debug("LINE_ERASE: mode={}", mode);

                if (mode == 0) {
                    // cursor(inclusive) to end of line
                    term.clearArea(x, y, state.nr_columns, y + 1);
                } else if (mode == 1) {
                    // begin of line to cursor (inclusive)
                    term.clearArea(0, y, x + 1, y + 1);
                } else if (mode == 2) {
                    // complete line
                    term.clearArea(0, y, state.nr_columns, y + 1);
                } else {
                    log.error("LINE_ERASE: unsupported mode:{}", mode);
                }
                break;
            }
            case SCREEN_ERASE: {
                int mode = 2; // no arg = full screen ? (VI does this!)
                if (numIntegers > 0)
                    mode = arg1;

                if (mode == 0) {
                    // cursor(inclusive) to end screen
                    term.clearArea(x, y, state.nr_columns, y); // rest of line
                    term.clearArea(0, y + 1, state.nr_columns, state.nr_rows);
                } else if (mode == 1) {
                    // begin of screen to cursor (inclusive)
                    term.clearArea(0, 0, state.nr_columns, y);
                    term.clearArea(0, y, x + 1, y);
                } else if (mode == 2) {
                    // complete screen
                    term.clearArea(0, 0, state.nr_columns, state.nr_rows);
                    setCursor(0, 0); //reset cursor ?
                }
                break;
            }
            case SET_FONT_STYLE:
                handleSetFontStyle(term, tokenizer.args().numArgs(), tokenizer.args().ints());
                break;
            case DEC_SETMODE:
            case DEC_RESETMODE:
                boolean decValue = (token.compareTo(Token.DEC_SETMODE) == 0);
                handleDecMode(term, tokenizer.args().numArgs(), tokenizer.args().ints(), decValue);
                break;
            case SET_MODE:
            case RESET_MODE:
                boolean modeValue = (token.compareTo(Token.SET_MODE) == 0);
                handleSetResetMode(term, tokenizer.args().numArgs(), tokenizer.args().ints(), modeValue);
                break;
            case DEVICE_STATUS: {
                if (arg1 == 6) {
                    log.warn("FIXME: Verify Request Cursor Report");
                    x = 120;
                    y = 30;

                    byte px1 = (byte) ('0' + (x / 10) % 10);
                    byte px2 = (byte) ('0' + x % 10);
                    byte py1 = (byte) ('0' + (y / 10) % 10);
                    byte py2 = (byte) ('0' + (y % 10));

                    byte[] sbytes = {(byte) CTRL_ESC, '[', py1, py2, ';', px1, px2, 'R'};

                    this.send(sbytes);
                } else {
                    log.warn("DEVICE_STATUS: Unknown device status mode:{}", arg1);
                }
                break;
            }
            case CHARSET_G0_UK:
                term.setCharSet(0, CharacterTerminal.VT_CHARSET_UK);
                break;
            case CHARSET_G1_UK:
                term.setCharSet(1, CharacterTerminal.VT_CHARSET_UK);
                break;
            case CHARSET_G0_US:
                term.setCharSet(0, CharacterTerminal.VT_CHARSET_US);
                break;
            case CHARSET_G1_US:
                term.setCharSet(1, CharacterTerminal.VT_CHARSET_US);
                break;
            case CHARSET_G0_GRAPHICS:
                term.setCharSet(0, CharacterTerminal.VT_CHARSET_GRAPHICS);
                break;
            case CHARSET_G1_GRAPHICS:
                term.setCharSet(1, CharacterTerminal.VT_CHARSET_GRAPHICS);
                break;
            case CHARSET_G0:
                term.setCharSet(0);
                break;
            case CHARSET_G1:
                term.setCharSet(1);
                break;
            case CHAR:
                // one or more characters: moves cursor !
                writeChar(bytes);
                break;
            case OSC_GRAPHMODE: {
                // Graph mode
                // 1 short title
                // 2 long title
                int type = arg1;
                log.info("OSC_GRAPHMODE: '{}';'{}'", type, tokenizer.args().strArg());
                this.fireGraphModeEvent(type, tokenizer.args().strArg());
                //System.err.println("XGRAPH type="+type+"np="+token.np+","+token.nd+"-"+token.strArg);
                break;
            }
            case SEND_PRIMARY_DA:
                if (this.tokenizer.args().numArgs() > 0) {
                    log.warn("FIXME:SEND_PRIMARY_DA: has argument(s):{}", arg1);
                }
                sendTermType();
                break;
            case SEND_SECONDARY_DA:
                if (this.tokenizer.args().numArgs() > 0) {
                    log.warn("FIXME:SEND_SECONDARY_DA: has argument(s):{}", arg1);
                }
                sendTermType();
                break;
            case XTERM_WIN_MANIPULATION: {
                log.warn("FIXME:Token error:{} with args:{}",token,Arrays.toString(tokenizer.args().intArgs()));
                break;
            }
            case ERROR: {
                String seqstr = Util.prettyByteString(bytes);
                // vt100 specifies to write checkerboard char:
                // drawChar('▒');
                log.warn("FIXME:Token error:{},{},sequence={}", token, tokenizer.getText(encoding + ":"), seqstr);
                break;
            }
            // unsupported, misc:
            case ETX:
            case ENQ:
            case DC1:
            case DC2:
            case UNSUPPORTED:
            default: {
                String seqstr = Util.prettyByteString(bytes);
                // vt100 specifies to write checkerboard char:
                // drawChar('▒');
                log.warn("FIXME: Unimplemented Token: {}:'{}'; sequence={}; ('{}') ", token, tokenizer.getText(encoding + ":"), seqstr, tokenizer.getIToken().description());
                break;
            }
        }// switch (token)
    }

    public void setCursor(int x, int y) {
        log.trace("setCursor(): {},{}", x, y);
        term.setCursor(x, y);
    }

    /**
     * Wrap around and boundary checks are now done at Emulator Level (here), not CharPane anymore:
     */
    public void moveCursor(int dx, int dy) {
        moveCursor(dx, dy, false);
    }

    /**
     * Wrap around and boundary checks are now done at Emulator Level here, not at CharPane anymore:
     */
    public void moveCursor(int dx, int dy, boolean afterWrite) {

        int xpos = term.getCursorX();
        int ypos = term.getCursorY();
        int numRs = term.getNumRows();
        int numCs = term.getNumColumns();
        int oldx = xpos;
        int oldy = ypos;

        // ----------------
        // LFC Glitch Mode at End-Of-Line:
        // ----------------
        if (afterWrite) {
            if ((state.decModeAutoWrap) && (xpos == numCs - 1)) {
                if (!this.state.lfc) {
                    this.state.lfc = true;
                    xpos = xpos; // glitch mode: keep at end-of-line (!)
                } else if (this.state.lfc) {
                    this.state.lfc = false;
                    xpos = 0;
                    ypos++;
                }

                if (ypos >= numRs) {
                    ypos--;
                    // AUTO-SCROLL
                    scrollLines(1, true);
                }
                log.debug("moveCursor(): GLITCH-MODE: {},{} + {},{} => {},{}", oldx, oldy, dx, dy, xpos, ypos);
                setCursor(xpos, ypos);
                return;
            }
        }

        if (afterWrite) {
            dx = 1;
            dy = 0;
        } else {
            this.state.lfc = false;
            // limit cursors movement beyond screen: no autowrap when moving cursors:
            if (xpos + dx >= numCs) {
                dx = numCs - xpos - 1;
            }
            if (ypos + dy >= numRs) {
                dy = numRs - ypos - 1;
            }
        }

        xpos += dx;
        ypos += dy;

        //X
        if (xpos < 0) {
            xpos = 0;
        } else if (xpos >= numCs) {
            // autowrap
            xpos = xpos % numCs;
            ypos++;
        }
        // Y
        if (ypos < 0) {
            ypos = 0;
        } else if (ypos >= numRs) {
            ypos--;
            // AUTO-SCROLL
            scrollLines(1, true);
        }

        log.trace("moveCursor(): {},{} + {},{} => {},{}", oldx, oldy, dx, dy, xpos, ypos);
        setCursor(xpos, ypos);
    }

    private void scrollLines(int numLines, boolean up) {
        term.scrollRegion(this.state.region_y1, this.state.region_y2, numLines, up);
    }

    private void handleSetFontStyle(CharacterTerminal charTerm, int numIntegers, int[] integers) {
        log.debug("handle fonstyle:{}", Arrays.toString(Arrays.copyOfRange(integers, 0, numIntegers)));
        if (numIntegers == 0) {
            // empty = clear
            charTerm.setDrawStyle(0);
            return;
        }

        int mode = integers[0];

        if (((mode == 38) || (mode == 48))) {
            //XTERM 256 color mode:
            int ccode = tokenizer.args().intArg(2);
            if (mode == 38) {
                charTerm.setDrawForeground(ccode);
            } else if (mode == 48) {
                charTerm.setDrawBackground(ccode);
            }
            return;
        }

        for (int i = 0; i < numIntegers; i++) {
            mode = tokenizer.args().intArg(i);

            if (mode == 0)
                charTerm.setDrawStyle(0); // reset
            else if (mode == 1)
                charTerm.addDrawStyle(StyleChar.STYLE_BOLD);
            else if (mode == 4)
                charTerm.addDrawStyle(StyleChar.STYLE_UNDERSCORE);
            else if (mode == 5) {
                // blink supported ?
                charTerm.addDrawStyle(StyleChar.STYLE_BLINK);
                charTerm.addDrawStyle(StyleChar.STYLE_UBERBOLD);
            } else if (mode == 7)
                charTerm.addDrawStyle(StyleChar.STYLE_INVERSE);
            else if (mode == 8)
                charTerm.addDrawStyle(StyleChar.STYLE_HIDDEN);
            else if ((mode >= 30) && (mode <= 37))
                charTerm.setDrawForeground(mode - 30);
            else if ((mode >= 40) && (mode <= 47))
                charTerm.setDrawBackground(mode - 40);
            else if (mode == 39)
                charTerm.setDrawBackground(-1);
            else if (mode == 49)
                charTerm.setDrawForeground(-1);
            else if ((mode >= 90) && (mode <= 97))
                charTerm.setDrawForeground(mode - 90 + 8);
            else if ((mode >= 100) && (mode <= 107))
                charTerm.setDrawBackground(mode - 100 + 8);
        }

    }

    private void handleDecMode(CharacterTerminal charTerm, int numIntegers, int[] integers,
                               boolean value) {
        if (numIntegers == 0)
            return; //Reset all ?

        int mode = integers[0];

        switch (mode) {
            case 1:
                this.state.applicationCursorKeys = value;
                break;
            case 3: {
                if (value)
                    state.nr_columns = 132;
                else
                    state.nr_columns = 80;
                charTerm.setColumns(state.nr_columns);
                this.fireResizedEvent(state.nr_columns, state.nr_rows);
                break;
            }
            case 4:
                charTerm.setSlowScroll(value);
                break;
            case 7:
                log.warn("DECMODE: DECAWM wrapAround={}", value);
                state.decModeAutoWrap = value;
                break;
            case 12: // Start Blinking
                charTerm.setCursorOptions(value);
                break;
            case 25:
                charTerm.setEnableCursor(value);
                break;
            case 45:
                log.warn("Received unsupported DECMODE:Set Alt Screen={}", value);
                boolean result = charTerm.setAltScreenBuffer(value);
                if ((value) && (!result))
                    log.warn("FIXME: DECMODE: Alternative Text Buffer not supported by Character Terminal.");
                break;
            case 1034:
                // P s = 1034 → Interpret "meta" key, sets eighth bit. (enables the eightBitInput resource).
                log.warn("FIXME: Metakey/8bit?{}", value);
                break;
            case 1048:
                if (value)
                    saveCursor();
                else
                    restoreCursor();
                break;
            case 1049: {
                // switch to als screen + use application cursor keys
                if (value)
                    saveCursor();
                else
                    restoreCursor();
                this.state.applicationCursorKeys = value;
                charTerm.setAltScreenBuffer(value);
                if (value)
                    charTerm.clearText();
                break;
            }
            default:
                log.warn("FIXME:Unknown DEC mode:set({},{})", mode, value);
                break;
        }
    }

    private void handleSetResetMode(CharacterTerminal charTerm, int numIntegers, int[] integers,
                                    boolean value) {
        if (numIntegers == 0)
            return; //Reset all ?

        int mode = integers[0];

        if (mode == 4) {
            if (value) {
                log.warn("FIXME:INSERT (true=insert, false=replace):{}; ", value);
            }
        } else {
            log.warn("FIXME:Unknown SET/RESET mode:{}={}", mode, value);
        }
    }

    private void restoreCursor() {
        this.setCursor(state.savedCursorX, state.savedCursorY);
        this.state.lfc = state.savedLfc;
    }

    private void saveCursor() {
        state.savedCursorX = term.getCursorX();
        state.savedCursorY = term.getCursorY();
        state.savedLfc = state.lfc;
    }

    private void writeChar(byte[] bytes) {
        term.writeChar(bytes);
        moveCursor(1, 0, true);
    }

    public byte[] getKeyCode(String keystr) {
        byte[] bytes = KeyMappings.getKeyCode(termType, keystr.toUpperCase());
        if (bytes == null) {
            log.debug("No keymapping for keycode:{}", keystr);
        }
        return bytes;
    }

    protected void checkIOException(Exception e, boolean sendException) {
        log.error("Exception:" + e.getMessage(), e);
    }
}
