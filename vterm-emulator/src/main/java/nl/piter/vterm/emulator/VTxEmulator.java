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
import nl.piter.vterm.api.TermConst;
import nl.piter.vterm.emulator.Tokens.Token;
import nl.piter.vterm.ui.panels.charpane.EcmaMapping;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static nl.piter.vterm.emulator.Util.byte2hexstr;
import static nl.piter.vterm.emulator.VTxCharDefs.CTRL_ESC;
import static nl.piter.vterm.emulator.VTxCharDefs.CTRL_SI;

/**
 * Implementation of most VT100 codes, VT102, and some xterm/xterm-256-color;
 */
@Slf4j
public class VTxEmulator implements Emulator {

    public static class DecMode {
        // DEC MODE(s) (selection needed by vi)
        protected boolean lcf;
        protected boolean modeAutoWrap;
        protected boolean originMode;
        protected boolean bracketedPasteMode;
        protected boolean focusEvents;
        protected boolean applicationCursorKeys;
        protected boolean slowScroll;

        public void reset() {
            // todo: set default on, but actually this is configurable: (see: termcap).
            modeAutoWrap = true;
            lcf = false;
            originMode = false;
            bracketedPasteMode = false;
            focusEvents = true;
            applicationCursorKeys =false;
            slowScroll = false;
        }
    }

    public static class EmulatorState {

        // Region is INCLUSIVE from y1 to EXCLUSIVE y2;
        protected boolean hasRegion;
        protected int region_y1 = 0; // 1;
        protected int region_y2 = 0; //term_height;
        // tabs
        protected int tabSize = 8;
        // Cursors
        protected int savedCursorX;
        protected int savedCursorY;
        protected boolean savedLfc;
        protected int savedStyle;
        protected int savedCharSet;
        protected String savedCharSetName;
        protected DecMode decMode = new DecMode();
        protected byte[] lastChar; // for character repeat;
        public void reset() {
            hasRegion = false;
            tabSize = 8;
            decMode.reset();
        }
    }

    private InputStream errorInput;
    private final OutputStream outputStream;

    private boolean isConnected = false;
    private String termType;
    private Charset encoding = StandardCharsets.UTF_8;

    private final Object haltMutex = new Object();
    private final Object terminateMutex = new Object();

    private boolean signalHalt = false;
    private boolean signalTerminate = false;

    // Emulator reads from Tokenizer:
    private VTxTokenizer tokenizer = null;
    // Emulator outputs character to terminal:
    private CharacterTerminal term = null;

    private final List<EmulatorListener> listeners = new ArrayList();

    private final byte[] single = new byte[1];
    private final EmulatorState state = new EmulatorState();

    private long tokenCounter;

    /**
     * Construct new Terminal Emulator. Reads and writes from input- and output- streams and plots to CharacterTerminal.
     */
    public VTxEmulator(CharacterTerminal term, InputStream inputStream, OutputStream outputStream) {
        setTerm(term);
        setInputStream(inputStream);
        this.outputStream = outputStream;
        resetState();
    }

    void setTerm(CharacterTerminal term) {
        this.term = term;
    }

    void setInputStream(InputStream inps) {
        this.tokenizer = new VTxTokenizer(inps);
    }

    /**
     * Reset states, but does not disconnect.
     */
    public void resetState() {
        state.reset();
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
            log.error("(stderr:){}", errstr);
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

    public Charset getEncoding() {
        return this.encoding;
    }

    public void setEncoding(Charset encoding) {
        this.encoding = encoding;
    }

    @Override
    public void signalHalt(boolean val) {
        this.signalHalt = val;

        synchronized (haltMutex) {
            if (!val)
                haltMutex.notifyAll();
        }
    }

    @Override
    public void step() {
        // when halted, a notify will execute one step in the terminal
        synchronized (haltMutex) {
            haltMutex.notifyAll();
        }
    }

    @Override
    public void signalTerminate() {
        this.signalTerminate = true;

        synchronized (terminateMutex) {
            terminateMutex.notifyAll();
        }
    }

    public int numRows() {
        return term.numRows();
    }

    public int numColumns() {
        return term.numColumns();
    }

    @Override
    public int[] getRegion() {
        return new int[]{numColumns(), numRows(), this.state.region_y1, this.state.region_y2};
    }

    public void start() {
        log.info("<<<Session Started>>>");

        setConnected(true);
        fireStarted();
        while (!signalTerminate) {
            synchronized (haltMutex) {
                if (this.signalHalt) {
                    try {

                        this.haltMutex.wait();
                    } catch (InterruptedException e) {
                        log.warn("Interupted: {}", e.getMessage());
                    }
                }
            }

            try {
                nextToken();
                tokenCounter++;
//                if (tokenCounter> 5125) {
//                    Thread.sleep(500);
//                }
            } catch (Exception e) {
                log.error("nextToken():Exception >>>", e);
                signalTerminate = true;
            } // let RuntimeException pass here.
        }

        setConnected(false);
        fireStopped();
        log.info("<<<Session Ended>>>");
    }

    public String getType() {
        return "VTx";
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
        int num;

        int numIntegers = tokenizer.args().numArgs();

        if (numIntegers > 0)
            arg1 = tokenizer.args().arg1();

        if (numIntegers > 1)
            arg2 = tokenizer.args().arg2();

        int increment = 1;
        if (arg1 > 0) {
            increment = arg1;
        }
        log.debug("nextToken #{} {}:'{}' with args:{}", tokenCounter, token, tokenizer.getText(), tokenizer.getFormattedArguments());

        switch (token) {
            case NUL:
                //ignore
                break;
            case EOF:
                log.warn("EOF: Connection Closed.");
                signalTerminate = true;
                break;
            case EOT:
                log.warn("EOT: Connection Closed.");
                signalTerminate = true;
                break;
            case DECID_SEND_TERM_ID:
                this.sendPrimaryDA();
                break;
            case BEL:
                term.beep();
                break;
            case CHAR:
                // one or more characters: moves cursor !
                state.lastChar=bytes.clone();
                writeChar(bytes);
                break;
            case HT: { // HORIZONTAL TAB
                x = ((x / state.tabSize + 1) * state.tabSize);
                if (x >= numColumns()) {
                    //HT does not do autonewline:
                    x = numColumns() - 1;
                }
                setCursor(x, y);
                break;
            }
            case DEL:
            case BS: { // backspace, DEL is also used.
                x -= 1;
                if (x < 0) {
                    y -= 1;
                    x = numColumns() - 1;
                }
                setCursor(x, y);
                break;
            }
            case LF:
            case VT: // Vertical Tab
            case FF: {
                // MIN(nr_rows,region);
                int maxy = this.getRegionMaxY();
                int miny = this.getRegionMinY();
                // Auto LineFeed when y goes out of bounds (or region)
                if (y + 1 >= maxy) {
                    // scroll REGION
                    scrollRegion(miny, maxy, 1, true);
                    y = maxy - 1; // explicit keep cursor in region.
                } else {
                    y += 1;
                }
                setCursor(x, y);
                log.debug("FF: New Cursor (x,y)=[{},{}]", x, y);
                break;
            }
            case SCROLL_DOWN:
            case SCROLL_UP:
                boolean up=(token==Token.SCROLL_UP);
                this.scrollLines(arg1,up);
                break;
            case CR: // carriage return
                x = 0;
                setCursor(x, y);
                break;
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
                    state.region_y2 = numRows();
                    state.hasRegion = false;
                } else {
                    state.region_y1 = arg1 - 1; // inclusive ->inclusive (-1)
                    state.region_y2 = arg2; // inclusive -> exclusive (-1+1)
                    state.hasRegion = true;
                }
                setCursor(0, state.region_y1);
                log.debug("SET REGION: {}:{}", state.region_y1, state.region_y2);
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
                num = (arg1>0)?arg1:1;
                // multi delete is move chars to left
                term.move(x + num, y, numColumns() - x - num, 1, x, y);
                break;
            }
            case ERASE_CHARS: {
                num = (arg1>0)?arg1:1;
                for (int i = 0; i < num; i++)
                    term.putChar(' ', x + i, y);
                setCursor(x, y);
                break;
            }
            case DELETE_LINES: {
                num = (arg1>0)?arg1:1;
                for (int i = 0; i < num; i++)
                    for (int j = 0; j < numColumns() - 1; j++)
                        term.putChar(' ', j, y + i);
                setCursor(x, y);
                break;
            }
            case IND_INDEX: { // move down region
                int maxy = this.getRegionMaxY();
                int miny = this.getRegionMinY();
                if (y + 1 >= maxy) {
                    // move down scrollRegion up:
                    scrollRegion(miny, maxy, 1, true);
                } else {
                    y++;
                    setCursor(x, y);
                }
                break;
            }
            case NEL_NEXT_LINE: { // move down
                int maxy = this.getRegionMaxY();

                if (y + 1 >= maxy) {
                    // move down scrollRegion up:
                    scrollLines(1, true);
                    setCursor(0, y);
                } else {
                    y++;
                    setCursor(0, y);
                }
                break;
            }
            case RI_REVERSE_INDEX: { // move up
                int miny = this.getRegionMinY();
                int maxy = this.getRegionMaxY();

                if ((y - 1) < miny) {
                    // move up scrollRegion down:
                    term.scrollRegion(miny, maxy, 1, false);
                } else {
                    y--;
                    setCursor(x, y);
                }
                break;
            }
            case INSERT_LINES: {
                //default: one
                int numLines=(arg1>0)?arg1:1;
                // insert at current position: scroll down:
                int maxy = this.getRegionMaxY();
                scrollRegion(y, maxy, numLines, false);
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

                if (state.decMode.originMode) {
                    setCursor(x, y + state.region_y1);
                } else {
                    setCursor(x, y);
                }
                break;
            }
            case LINE_ERASE: {
                int mode = 0;
                if (numIntegers > 0)
                    mode = arg1;
                log.debug("LINE_ERASE: mode={}", mode);

                if (mode == 0) {
                    // cursor(inclusive) to end of line
                    term.clearArea(x, y, numColumns(), y + 1);
                } else if (mode == 1) {
                    // begin of line to cursor (inclusive)
                    term.clearArea(0, y, x + 1, y + 1);
                } else if (mode == 2) {
                    // complete line
                    term.clearArea(0, y, numColumns(), y + 1);
                } else {
                    log.warn("LINE_ERASE: unsupported mode:{}", mode);
                }
                break;
            }
            case SCREEN_ERASE: {
                int mode = 2; // no arg = full screen ? (VI does this!)
                if (numIntegers > 0)
                    mode = arg1;

                if (mode == 0) {
                    // cursor(inclusive) to end screen
                    term.clearArea(x, y, numColumns(), y); // rest of line
                    term.clearArea(0, y + 1, numColumns(), numRows());
                } else if (mode == 1) {
                    // begin of screen to cursor (inclusive)
                    term.clearArea(0, 0, numColumns(), y);
                    term.clearArea(0, y, x + 1, y);
                } else if (mode == 2) {
                    // complete screen
                    term.clearArea(0, 0, numColumns(), numRows());
                    setCursor(0, 0); //reset cursor ?
                }
                break;
            }
            case INSERT_BLANK_CHARS:
                num = (arg1>0)?arg1:1;
                // insert and move text from cursor to the right:
                for (int i = 0; i < num; i++) {
                    term.move(x, y, numColumns() - x, 1, x + 1, y);
                    term.putChar(' ', x, y);
                }
                break;
            case CHARACTER_REPEAT: {
                num = (arg1>0)?arg1:1;
                for (int i=0;i<num;i++) {
                    writeChar(state.lastChar);
                }
                break;
            }
            case CHARACTER_ATTRS:
                handleSetFontStyle(term, tokenizer.args().intArgs());
                break;
            case DEC_SETMODE:
            case DEC_RESETMODE:
                boolean decValue = (token.compareTo(Token.DEC_SETMODE) == 0);
                handleDecMode(term, tokenizer.args().intArgs(), decValue);
                break;
            case SET_MODE:
            case RESET_MODE:
                boolean modeValue = (token.compareTo(Token.SET_MODE) == 0);
                handleSetResetMode(term, tokenizer.args().intArgs(), modeValue);
                break;
            case DEVICE_STATUS: {
                if (arg1 == 6) {
                    this.sendCursor(x, y);
                } else {
                    log.debug("DEVICE_STATUS: Unknown device status mode request:{}", arg1);
                }
                break;
            }
            case CHARSET_G0:
                term.setCharSet(0);
                break;
            case CHARSET_G1:
                term.setCharSet(1);
                break;
            case CHARSET_G0_DES:
                term.setCharSet(0, mapCharSet(tokenizer.args().charSet()));
                break;
            case CHARSET_G1_DES:
                term.setCharSet(1, mapCharSet(tokenizer.args().charSet()));
                break;
            case CHARSET_G2_DES:
                term.setCharSet(2, mapCharSet(tokenizer.args().charSet()));
                break;
            case CHARSET_G3_DES:
                term.setCharSet(3, mapCharSet(tokenizer.args().charSet()));
                break;
            case OSC_GRAPHMODE:
                // Graph mode: 1 = short title, 2 = long title
                int type = arg1;
                handleGraphMode(type, tokenizer.args().strArg());
                break;
            case REQ_PRIMARY_DA:
                if (this.tokenizer.args().numArgs() > 0) {
                    log.debug("Fixme:SEND_PRIMARY_DA: has argument(s):{}", arg1);
                }
                sendPrimaryDA();
                break;
            case REQ_SECONDARY_DA:
                if (this.tokenizer.args().numArgs() > 0) {
                    log.debug("Fixme:SEND_SECONDARY_DA: has argument(s):{}", arg1);
                }
                sendSecondaryDA();
                break;
            case REQ_XTVERSION:
                sendXTVersion();
                break;
            case DEC_SCREEN_ALIGNMENT:
                decScreenAlignmentTest();
                break;
            case DCS_DEVICE_CONTROL_STRING:
                handleDCS(tokenizer.args().strArg());
                break;
            case XTERM_WIN_MANIPULATION:
                handleWindowManipulation(tokenizer.args().intArgs());
                log.debug("Fixme:Token error:{} with args:{}", token, Arrays.toString(tokenizer.args().intArgs()));
                break;
            case XTERM_SETGET_GRAPHICS:
                handleXtermSetGetGraphics(tokenizer.args().intArgs());
                break;
            // unsupported, misc:
            case ETX:
            case ENQ:
            case DC1:
            case DC2:
            case UNSUPPORTED:
            case ERROR: {
                String seqstr = Util.prettyByteString(bytes);
                // vt100 specifies to write checkerboard char:
                // drawChar('▒');
                log.debug("Fixme:Token error:{},{},sequence={}", token, tokenizer.getText(), seqstr);
                break;
            }
            default: {
                String seqstr = Util.prettyByteString(bytes);
                // vt100 specifies to write checkerboard char:
                // drawChar('▒');
                log.debug("Fixme: Unimplemented Token: {}:'{}' with args:'{}'; sequence={}; ('{}') ", token,
                        tokenizer.getText(),
                        tokenizer.getFormattedArguments(),
                        seqstr,
                        (tokenizer.getIToken() != null) ? tokenizer.getIToken().description() : "<null>");
                break;
            }
        }// switch (token)
    }

    private void handleXtermSetGetGraphics(int[] intArgs) {
        if (intArgs.length < 3) {
            log.debug("handleXtermSetGetGraphics(): not enough argument:{}", Arrays.toString(intArgs));
            return;
        }
        log.debug("handleXtermSetGetGraphics(): {}", Arrays.toString(intArgs));

        int id = intArgs[0];
        int mode = intArgs[1];
        int val = intArgs[2];
        boolean get = (mode == 1);
        boolean reset = (mode == 2);
        boolean set = (mode == 3);
        boolean max = (mode == 4);
        switch (id) {
            case 1:
                // number of color registers
                if (get) {
                    //  ok for 1024:
                    sendXTSMGRAPHICS(1, 0, 1024);
                } else if (set) {
                    // ok for val
                    sendXTSMGRAPHICS(1, 0, val);
                } else if (reset) {
                    // reset ok for val
                    sendXTSMGRAPHICS(1, 0, val);
                } else if (max) {
                    // could be more but 1024 is ok:
                    sendXTSMGRAPHICS(1, 0, 1024);
                }
                break;
            case 2:
                log.debug("unsupported Sixel graphics args :{}", Arrays.toString(intArgs));
                break;
            case 3:
                log.debug("unsupported ReGIS graphics args :{}", Arrays.toString(intArgs));
                break;
        }
    }

    private void handleWindowManipulation(int[] intArgs) {
        if (intArgs.length == 0)
            return;

        int rows = term.numRows();
        int cols = term.numColumns();

        int cmd = intArgs[0];
        switch (cmd) {
            case 8:
                if (intArgs.length > 2)
                    rows = intArgs[1];
                if (intArgs.length > 3)
                    cols = intArgs[2];
                log.warn("XTerm WinMan: setColumnsAndRows: {},{}", cols, rows);
                term.setColumnsAndRows(cols, rows);
                break;
            case 18:
                log.warn("XTerm, WinMan: request Size: {},{}", cols, rows);
                this.sendSize(cols, rows);
                break;
            default:
                log.debug("Unsupported Windows Manipulation: {}", Arrays.toString(intArgs));
        }

    }

    private void handleDCS(String strArg) {
        log.debug("handleDCS: not implemented: {}", strArg);

        if (strArg.startsWith("+q")) {
            String[] args = strArg.substring("+q".length()).split(";");
            for (String hexArg : args) {
                String val = new String(Util.hexstr2bytes(hexArg), StandardCharsets.UTF_8);
                log.debug("- got VAL: {}", val);
            }
        }

    }

    private void handleGraphMode(int type, String strArg) {
        log.debug("OSC_GRAPHMODE: '{}';'{}'", type, strArg);
        if (type ==0  || type == 1 || type == 2) {
            this.fireGraphModeEvent(type, tokenizer.args().strArg());
        }
        else if (type == 4 || type ==5) {
            log.debug("GraphMode color set/request: '{}'", strArg);
            setXtermColor(strArg,(type==5));
        } else {
            log.warn("Unsupported GraphMode:{}:{}", type, strArg);
        }
    }

    private void setXtermColor(String strArg, boolean special) {
        log.debug("setXtermColor: {}", strArg);

        String[] pars = strArg.split(";");
        if (pars.length < 2) {
            log.warn("Not enough arguments: '{}'", strArg);
            return;
        }

        for (int i=0;i<pars.length;i+=2)
          try {
            int num = Integer.parseInt(pars[i]);
            String option = pars[i+1];
            if ("?".equals(option)) {
                sendXtermColor(num, term.getColor(num));
            } else if (option.startsWith("rgb:")) {
                String rgb = option.substring("rgb:".length());
                String[] vals = rgb.split("/");
                if (vals.length < 3) {
                    log.warn("Not enough RGB arguments: '{}'", rgb);
                    return;
                }
                int r = Integer.parseInt(vals[0], 16);
                int g = Integer.parseInt(vals[1], 16);
                int b = Integer.parseInt(vals[2], 16);
                log.info("Setting new color: #{}:({},{},{})", num, r, g, b);
                this.term.setColor(num, new Color(r, g, b));
            } else {
                log.warn("Unknown graphmode option in argument: '{}'", strArg);
            }
          } catch (NumberFormatException e) {
            log.error("Failed to parse Color argument:" + strArg, e);
          }
    }

    /**
     * Legacy, UTF-8 is now default.
     */
    private TermConst.CharSet mapCharSet(Character charSet) {
        switch (charSet) {
            case CTRL_SI:
            case '0':
                return TermConst.CharSet.CHARSET_GRAPHICS;
            case 'A':
                return TermConst.CharSet.CHARSET_UK;
            case 'B':
                return TermConst.CharSet.CHARSET_US;
            case '4':
                return TermConst.CharSet.CHARSET_DUTCH;
            case 'C':
            case '5':
                return TermConst.CharSet.CHARSET_FINNISH;
            case 'E':
            case '6':
                return TermConst.CharSet.CHARSET_NORDANISH;
            case 'H':
            case '7':
                return TermConst.CharSet.CHARSET_SWEDISH;
            case 'K':
                return TermConst.CharSet.CHARSET_GERMAN;
            case 'R':
            case 'Q':
                return TermConst.CharSet.CHARSET_FRENCH;
            default:
                return TermConst.CharSet.CHARSET_OTHER;
        }
    }

    /**
     * Feature from 'vttest' (VTTEST).
     */
    private void decScreenAlignmentTest() {

        int y1 = 0;
        int y2 = this.numRows();
        if (this.state.hasRegion) {
            y1 = this.state.region_y1;
            y2 = this.state.region_y2;
        }
        byte[] bytes = new byte[1];
        bytes[0] = (byte) 'E';

        for (int y = y1; y < y2; y++) {
            for (int x = 0; x < numColumns(); x++) {
                this.term.putChar(bytes, x, y);
            }
        }
    }

    public void setCursor(int x, int y) {
        setCursor(x, y, false);
    }

    /**
     * Set absolute position, ignores offset, but keeps cursor in region by default
     */
    public void setCursor(int x, int y, boolean ignoreRegion) {
        if (!ignoreRegion && state.hasRegion) {
            if (y < state.region_y1) {
                y = state.region_y1;
            } else if (y >= state.region_y2) {
                y = state.region_y2;
            }
        }
        this.state.decMode.lcf = false;
        term.setCursor(x, y);
    }

    /**
     * Wrap around and boundary checks are now done at Emulator Level here, not at CharPane anymore:
     */
    public void moveCursor(int dx, int dy) {

        int xpos = term.getCursorX();
        int ypos = term.getCursorY();
        int numRs = term.numRows();
        int numCs = term.numColumns();
        int oldx = xpos;
        int oldy = ypos;

        int miny = 0;
        int maxy = numRs;
        if (state.hasRegion) {
            miny = state.region_y1; // inclusive:
            maxy = state.region_y2; // exclusive
        }

        xpos += dx;
        ypos += dy;

        // Limit cursors movement beyond screen: no autowrap when moving cursor:
        // Also: respect region here:
        if (xpos < 0) {
            xpos = 0;
        }
        if (xpos >= numCs) {
            xpos = numCs - 1;
        }
        if (ypos >= maxy) {
            ypos = maxy - 1; // exclusive
        }
        if (ypos < miny) {
            ypos = miny; // inclusive
        }

        log.trace("moveCursor(): {},{} + {},{} => {},{}", oldx, oldy, dx, dy, xpos, ypos);
        setCursor(xpos, ypos);
    }


    public int getRegionMinY() {
        if (state.hasRegion) {
            return state.region_y1;
        }
        return 0;
    }

    public int getRegionMaxY() {
        if (state.hasRegion) {
            return state.region_y2;
        }
        return numRows();
    }

    private void setSlowScroll(boolean value) {
        this.state.decMode.slowScroll = value;
    }

    private void scrollRegion(int y1, int y2, int numLines, boolean up) {
        long start = System.currentTimeMillis();
        term.scrollRegion(y1, y2, numLines, up);
        if (state.decMode.slowScroll) {
            long end = System.currentTimeMillis();
            long delta = end - start;
            if (delta < 100) {
                try {
                    Thread.sleep(100 - delta);
                } catch (InterruptedException e) {
                    log.warn("Interrupted:" + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Scroll lines in effective Region.
     *
     * @param numLines
     * @param up
     */
    private void scrollLines(int numLines, boolean up) {
        if (state.hasRegion) {
            scrollRegion(this.state.region_y1, this.state.region_y2, numLines, up);
        } else {
            scrollRegion(0, this.numRows(), numLines, up);
        }
    }

    private void autoNewline() {
        int xpos = 0;
        int ypos = this.term.getCursorY();
        ypos++;

        if (ypos >= getRegionMaxY()) {
            scrollLines(1, true);
            ypos = getRegionMaxY() - 1;
        }

        setCursor(xpos, ypos);
    }

    private void handleSetFontStyle(CharacterTerminal charTerm, int[] args) {
        log.trace("handleSetFontStyle(): {}", Arrays.toString(args));
        int numArgs = args.length;
        if (numArgs == 0) {
            // empty = clear
            charTerm.setDrawStyle(0);
            return;
        }

        // stream parse integers:
        Iterator<Integer> iterator = Arrays.stream(args).iterator();
        while (iterator.hasNext()) {
            int mode = iterator.next(); //#0

            if (((mode == 38) || (mode == 48))) {
                boolean isFG = (mode == 38);
                int subMode = safeNext(iterator, -1); //#1
                if (subMode == 2) {
                    if (numArgs >= 5) {
                        int r = safeNext(iterator, -1); //#2
                        int g = safeNext(iterator, -1); //#3
                        int b = safeNext(iterator, -1); //#4
                        log.error("RGB subMode: RGB: {},{},{}", r, g, b);
                        if (isFG) {
                            charTerm.setDrawForeground(r, g, b);
                        } else {
                            charTerm.setDrawBackground(r, g, b);
                        }
                    } else {
                        log.warn("RGB unknown subMode: {}", tokenizer.getFormattedArguments());
                    }
                } else if (subMode == 5) {
                    // XTERM 256 color mode:
                    int ccode = safeNext(iterator, -1);//#2
                    log.debug("RGB subMode: index: {}", ccode);
                    if (isFG) {
                        charTerm.setDrawForeground(ccode);
                    } else {
                        charTerm.setDrawBackground(ccode);
                    }
                } else {
                    log.debug("Unknown color submode: {}:{}", subMode, tokenizer.getFormattedArguments());
                }
            } else if (EcmaMapping.hasMode(mode)) {
                int style = charTerm.getDrawStyle();
                style = EcmaMapping.apply(style, mode);
                charTerm.setDrawStyle(style);
            } else if ((mode >= 30) && (mode <= 37))
                charTerm.setDrawForeground(mode - 30);
            else if ((mode >= 40) && (mode <= 47))
                charTerm.setDrawBackground(mode - 40);
            else if (mode == 39)
                charTerm.setDrawBackground(-1);
            else if (mode == 49)
                charTerm.setDrawForeground(-1);
            else if ((mode >= 90) && (mode <= 97))
                // 16 color mode:
                charTerm.setDrawForeground(mode - 90 + 8);
            else if ((mode >= 100) && (mode <= 107))
                // 16 color mode:
                charTerm.setDrawBackground(mode - 100 + 8);
            else {
                log.warn("Unknown font style: {}", mode);
            }
        } // wend

    }

    private void handleDecMode(CharacterTerminal charTerm, int[] args,
                               boolean value) {
        log.debug("DECSETMODE: {}:{}", value ? "SET" : "RESET", Arrays.toString(args));
        int numArgs = args.length;

        if (numArgs == 0) {
            return; //Reset all ?
        }

        for (int i = 0; i < numArgs; i++) {
            int mode = args[i];

            switch (mode) {
                case 1:
                    this.state.decMode.applicationCursorKeys = value;
                    break;
                case 3: {
                    charTerm.setColumns(value ? 132 : 80);
                    setCursor(0, 0);
                    clearText();
                    this.fireResizedEvent(numColumns(), numRows());
                    break;
                }
                case 4:
                    setSlowScroll(value);
                    break;
                case 5:
                    charTerm.setReverseVideo(value);
                    break;
                case 6:
                    state.decMode.originMode = value;
                    log.warn("DEC ORIGINMODE: {} for region: {}:{}", value, state.region_y1, state.region_y2);
                    if (value) {
                        setCursor(0, state.region_y1);
                    } else {
                        state.hasRegion = false;
                        setCursor(0, 0);
                    }
                    break;
                case 7:
                    log.warn("DECMODE: DECAWM wrapAround={}", value);
                    state.decMode.modeAutoWrap = value;
                    break;
                case 12: // Start Blinking
                    charTerm.setCursorOptions(value);
                    break;
                case 25:
                    charTerm.setEnableCursor(value);
                    break;
                case 45:
                    boolean result = charTerm.setAltScreenBuffer(value);
                    if ((value) && (!result))
                        log.warn("Fixme: DECMODE: ALT Text Buffer not supported by Character Terminal.");
                    break;
                case 1004:
                    state.decMode.focusEvents = value;
                    break;
                case 1034:
                    // P s = 1034 → Interpret "meta" key, sets eighth bit. (enables the eightBitInput resource).
                    log.debug("Fixme: Metakey/8bit?{}", value);
                    break;
                case 1048: {
                    if (value)
                        saveCursor();
                    else
                        restoreCursor();
                    break;
                }
                case 1049: {
                    // switch to als screen + use application cursor keys
                    if (value) {
                        saveCursor();
                        setUseApplicationKeys(value);
                        charTerm.setAltScreenBuffer(value);
                        clearText();
                    } else {
                        setUseApplicationKeys(value);
                        charTerm.setAltScreenBuffer(value);
                        restoreCursor();
                    }
                    break;
                }
                case 2004:
                    // vi feature(!):
                    state.decMode.bracketedPasteMode = value;
                    break;
                default:
                    log.warn("Fixme:Unknown DEC mode:set({},{})", mode, value);
                    break;
            }
        }
    }

    private void setUseApplicationKeys(boolean value) {
        this.state.decMode.applicationCursorKeys = value;
    }


    private void clearText() {
        term.clearArea();
    }

    private void handleSetResetMode(CharacterTerminal charTerm, int[] args,
                                    boolean value) {
        if (args.length == 0)
            return; //Reset all ?

        int mode = args[0];

        if (mode == 4) {
            if (value) {
                log.debug("Fixme:INSERT (true=insert, false=replace):{}; ", value);
            }
        } else {
            log.debug("Fixme:Unknown SET/RESET mode:{}={}", mode, value);
        }
    }

    private void restoreCursor() {
        this.setCursor(state.savedCursorX, state.savedCursorY);
        this.state.decMode.lcf = state.savedLfc;
        term.setDrawStyle(state.savedStyle);
        term.setCharSet(state.savedCharSet);
        term.setCharSet(state.savedCharSet, state.savedCharSetName);
    }

    private void saveCursor() {
        state.savedCursorX = term.getCursorX();
        state.savedCursorY = term.getCursorY();
        state.savedLfc = state.decMode.lcf;
        state.savedStyle = term.getDrawStyle();
        state.savedCharSet = term.getCharSet();
        state.savedCharSetName = term.getCharSetName(state.savedCharSet);
    }

    private void writeChar(byte[] bytes) {

        int oldx = term.getCursorX();
        int oldy = term.getCursorY();
        boolean oldLcf = this.state.decMode.lcf;

        if (term.getCursorX() == term.numColumns() - 1) {
            if (this.state.decMode.modeAutoWrap) {
                if (!this.state.decMode.lcf) {
                    term.writeChar(bytes);
                    this.state.decMode.lcf = true;
                } else {
                    autoNewline();
                    term.writeChar(bytes);
                    this.state.decMode.lcf = false;
                }
                log.trace("LCF GlitchMode: {},{} -> {},{} [lcf: {}=>{}", oldx, oldy, term.getCursorX(), term.getCursorY(), oldLcf, state.decMode.lcf);
            } else {
                term.writeChar(bytes);
                autoNewline();
            }
        } else {
            term.writeChar(bytes);
            moveCursor(1, 0);
        }

    }

    public byte[] getKeyCode(String keystr) {
        String prefix;
        if (this.state.decMode.applicationCursorKeys) {
            prefix = "APP";
        } else {
            prefix = termType;
        }

        byte[] bytes = KeyMappings.getKeyCode(prefix, keystr.toUpperCase());
        if (bytes == null) {
            log.debug("No keymapping for keycode:{}", keystr);
        }
        return bytes;
    }

    // ======================
    // Send responses:
    // ======================

    @Override
    public boolean sendSize(int cols, int rows) {
        log.debug("Verify me: sendSize: {},{}", cols, rows);
        String req = String.format("%c[8;%d;%dt", CTRL_ESC, rows, cols, CTRL_ESC);
        byte[] bytes = req.getBytes(StandardCharsets.UTF_8);
        log.debug("Sending xterm size: '{}'", Util.prettyByteString(bytes));
        try {
            this.send(bytes);
        } catch (IOException e) {
            checkIOException(e, true);
        }
        return true;
    }

    private void sendXTSMGRAPHICS(int id, int status, int val) {
        String req = String.format("%c[?%d;%d;%dS", CTRL_ESC, id, status, val);
        byte[] bytes = req.getBytes(StandardCharsets.UTF_8);
        log.debug("sendXTSMGRAPHICS: '{}'", Util.prettyByteString(bytes));
        try {
            this.send(bytes);
        } catch (IOException e) {
            checkIOException(e, true);
        }
    }


    private void sendXtermColor(int num, Color c) {
        // verified?
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();
        String req = String.format("%c]4;%d;rbg:%s/%s/%s%c\\", CTRL_ESC, num,
                byte2hexstr(r),
                byte2hexstr(g),
                byte2hexstr(b), CTRL_ESC);
        byte[] bytes = req.getBytes(StandardCharsets.UTF_8);
        log.debug("Sending xterm color: '{}'", Util.prettyByteString(bytes));
        try {
            this.send(bytes);
        } catch (IOException e) {
            checkIOException(e, true);
        }
    }

    private void sendCursor(int x, int y) {
        // offByOne(!) +x,y:
        String report = String.format("%c[%d;%dR", CTRL_ESC, y + 1, x + 1);
        byte[] bytes = report.getBytes(StandardCharsets.UTF_8);
        log.debug("sendCursor(): {}", Util.prettyByteString(bytes));
        try {
            this.send(bytes);
        } catch (IOException e) {
            checkIOException(e, true);
        }

    }

    /**
     * Send: { CSI, '>', Pp, ';', Pv, ';', Pc, 'c' }
     * Pp = Term Type
     * Pv = Version
     * Pc = firmware version
     */
    public void sendPrimaryDA() {

        // Report from 'vttest' when using it inside xterm.
        // Report is:    <27> [ ? 1 ; 2 c  -- means VT100 with AVO (could be a VT102)
        // Legend: AVO = Advanced Video Option

        // I am vt10x compatible:
        byte[] bytes = {CTRL_ESC, '[', '?', '1', ';', '2', 'c'};

        try {
            this.send(bytes);
        } catch (IOException e) {
            checkIOException(e, true);
        }
    }

    /**
     * Send: { CSI, '>', Pp, ';', Pv, ';', Pc, 'c' }
     * Pp = Term Type
     * Pv = Version
     * Pc = firmware version
     */
    public void sendSecondaryDA() {

        // From XTterm: 0 = VT100, version '115', 0= no firmware
        byte[] bytes = {CTRL_ESC, '[', '>', '0', ';', '1', '1', '5', ';', '0', 'c'};

        try {
            this.send(bytes);
        } catch (IOException e) {
            checkIOException(e, true);
        }
    }

    public void sendXTVersion() {
        log.debug("Fixme: sendXTVersion()");
    }

    public void send(byte b) throws IOException {
        single[0] = b;
        send(single);
    }

    public void send(byte[] code) throws IOException {
        if (code == null || code.length == 0) {
            log.error("Cowardly refusing to send " + ((code == null) ? "NULL" : "EMPTY") + " bytes");
            return;
        }

        // Beware: AWT calls emulator concurrently with emulator thread.
        synchronized (this.outputStream) {
            this.outputStream.write(code);
            this.outputStream.flush();
        }
    }

    protected void checkIOException(Exception e, boolean sendException) {
        log.error("Exception:" + e.getMessage(), e);
    }

    // ======================
    // Events
    // ======================

    @Override
    public void addListener(EmulatorListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeListener(EmulatorListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void setSlowScrolling(boolean val) {
        this.state.decMode.slowScroll = val;
    }

    @Override
    public boolean getSlowScrolling() {
        return this.state.decMode.slowScroll;
    }

    protected void fireGraphModeEvent(int type, String text) {
        for (EmulatorListener listener : listeners) {
            listener.notifyTermTitle(type, text);
        }
    }

    protected void fireStarted() {
        for (EmulatorListener listener : listeners) {
            listener.emulatorStarted();
        }
    }

    protected void fireStopped() {
        for (EmulatorListener listener : listeners) {
            listener.emulatorStopped();
        }
    }

    protected void fireResizedEvent(int columns, int rows) {
        for (EmulatorListener listener : listeners) {
            listener.notifyResized(columns, rows);
        }
    }

    public static <T> T safeNext(Iterator<T> itertr, T defaultVal) {
        if (itertr.hasNext()) {
            return itertr.next();
        }
        return defaultVal;
    }

}
