/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.ui.charpane;

import lombok.extern.slf4j.Slf4j;
import nl.piter.vterm.api.CharacterTerminal;
import nl.piter.vterm.ui.fonts.FontInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;

/**
 * Character Terminal Render Engine.
 */
@Slf4j
public class CharPane extends JComponent implements CharacterTerminal, ActionListener {

    public static final String OPTION_ALWAYS_SYNCHRONIZED_SCROLLING = "optionAlwaysSynchronizedScrolling";
    public static final int MAX_CHARSETS = 16;

    // ========================================================================

    // Terminal
    private int numColumns = 80;
    private int numRows = 24;
    private String characterEncoding = "UTF-8";
    private final String[] charSets = new String[MAX_CHARSETS];
    private int charSet = 0;
    // Cursor
    private int cursorX = 0;
    private int cursorY = 0;
    private boolean showCursor = true;
    private boolean cursorBlink = false;
    private Color cursorBlinkColor;
    // Control
    private boolean mustStop = false;

    // === Image Character Buffer ==== //
    private Image background; // background image
    private Image currentImage;
    private TextBuffer currentBuffer;
    private TextBuffer altTextBuffer;
    private TextBuffer fullBuffer;

    // === Current Draw Style ===
    private int drawStyle = 0;
    private int drawForegroundIndex = -1; // no index -> use default !
    private int drawBackgroundIndex = -1;

    //
    private CharRenderer charRenderer;

    // Animations & Threads:
    private int animationCounter = 0;
    private Timer refreshTimer;
    private Runnable renderTask;
    private Thread renderThread;

    // ===============================
    // === MUTEX and Paint Control ===
    // ===============================

    // Paint mutex: can also be use to wait for a repaint()
    private final Object paintImageMutex = new Object();
    // Render mutex do not resize or swap text buffers between rendering attempts
    private final Object textBufferMutex = new Object();
    // Whether whole text buffer should be painted or only the characters which have changed
    private boolean paintCompleteTextBuffer = false;
    // Whether after each scroll the scrolling should wait until the image is displayed
    private boolean optionAlwaysSynchronizedScrolling = false;
    // --- Text Rendering options ---
    // Whether textBuffer Renderer should wait for the Swing paint thread
    private final boolean optionRendererWaitForPaint = true;
    // VI demands this
    private final boolean optionSupportAltScreenBuffer = true;
    // Set to false: Delta character renderer seems to work now
    private final boolean optionAlwaysPaintCompleteTextBuffer = false;

    // ========================================================================
    // INIT
    // ========================================================================

    public CharPane() {
        init();
    }

    private void init() {
        this.setLayout(null);
        this.setFocusable(true);
        // unset focuskeys, must get TAB chars:
        this.setFocusTraversalKeysEnabled(false);

        // startRefresher();
        charSets[0] = CharSets.CHARSET_US;
        charSets[1] = CharSets.CHARSET_GRAPHICS;

        // Move to parent container ! : enableEvents(AWTEvent.KEY_EVENT_MASK);
        this.setBackground(Color.BLACK);
        this.setForeground(Color.GREEN);

        this.charRenderer = new CharRenderer();
        this._resizeTextBuffers(this.numColumns, this.numRows);
    }

    /**
     * Start timers and renderer thread is not running
     */
    public void startRenderers() {
        log.debug("startRenderers(): Started...");

        this.mustStop = false;

        if (renderTask == null) {
            // background render thread: must not use Swing Event Thread !
            renderTask = () -> doRender();
        }

        if ((renderThread == null) || (!renderThread.isAlive())) {
            renderThread = new Thread(renderTask);
            renderThread.start();
        }

        if ((refreshTimer == null) || (!refreshTimer.isRunning())) {
            // timer
            refreshTimer = new Timer(10, this);
            refreshTimer.setInitialDelay(100); // animate 10 per second (blink)
            refreshTimer.start();
        }
    }

    private void doRender() {
        log.debug("doRender(): >>> start");
        while (!this.mustStop) {
            synchronized (this.textBufferMutex) {
                if (this.currentBuffer != null) {
                    // check whether complete buffer need to be renderED or a part:
                    if (this.paintCompleteTextBuffer || this.currentBuffer.hasChanged()) {
                        // change *before* repaint
                        // to detect new changes while painting
                        currentBuffer.setChanged(false);

                        boolean succesfull = false;
                        try {
                            // at startup image might not be displayed yet
                            succesfull = paintTextBuffer();
                        } catch (Throwable e) {
                            // log.
                            succesfull = false;
                        }

                        // schedule repaint:
                        if (!succesfull)
                            this.currentBuffer.setChanged(true);

                        // call repaint anyway
                        repaint();
                    }
                }
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                log.debug("***Interrupted***", e);
            }
        }
        log.debug("doRender(): >>> end.");
    }

    /**
     * Initialize Backing Image.
     */
    private boolean initTextBufferImage() {

        // block paint: will be back for breakfast:
        synchronized (paintImageMutex) {
            // image = new BufferedImage(getTermWidth(), getTermHeight(),BufferedImage.TYPE_INT_ARGB);
            Container parent = this.getParent();

            if ((parent == null) || (!parent.isDisplayable())) {
                log.warn("Parent NULL or Image source not displayable (yet).");
                return false;
            }

            if (!parent.isVisible()) {
                log.warn("Image source not Visible (yet)!");
                return false;
            }

            // offscreen buffered image:
            currentImage = new BufferedImage(getImageWidth(), getImageHeight(), BufferedImage.TYPE_INT_ARGB);
        }

        return true;
    }

    public void drawTestScreen() {
        this.setDrawStyle(0);

        for (int i = 0; i < 2; i++)
            for (int y = 0; y < 4; y++)
                for (int x = 0; x < 8; x++) {
                    this.setDrawForeground(x);
                    this.setDrawBackground(y + i * 4);
                    putString("VTx", 16 + (x + i * 8) * 3, 1 + y);
                }

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 64; j++) {
                this.setDrawForeground(1);
                this.setDrawBackground(j + i * 64);
                putString("X", 8 + j, 6 + i);
            }
        }

        int n = 64; //
        int m = 13;
        for (int c = 0; c < n; c++) {
            this.setDrawStyle(c);
            for (int i = 0; i < m; i++) {
                int index = c * m + i;
                int x = 1 + index % 78; // 6x13=78
                int y = 9 + index / 78;
                putChar('A' + index % 26, x, y);
            }
        }

        this.setDrawStyle(0);

        for (int y = 0; y < 4; y++)
            for (int x = 0; x < 64; x++)
                putChar(x + y * 64, 8 + x, 20 + y);

        // graphics
        this.setCharSet(1, VT_CHARSET_GRAPHICS);
        this.setCharSet(1);
        for (int y = 0; y < 2; y++) {
            if (y % 2 == 1)
                this.addDrawStyle(StyleChar.STYLE_BOLD);

            for (int x = 0; x < 16; x++) {
                int c = 'j' + x;
                if (x == 15)
                    c = 'a';// checkered
                putChar(c, 8 + x, 20 + y * 2);
            }
        }

        this.setCursor(0, 0);
        this.setDrawStyle(0);
        this.setCharSet(0);

        //
        this.paintTextBuffer();
        repaint();
    }

    /**
     * Clear text buffer, doesn't do repaint
     */
    public void clearText() {
        synchronized (textBufferMutex) {
            this.currentBuffer.clearAll();
        }
    }

    /**
     * Clear text, state and Reset Graphics
     */
    public void reset() {
        this.stopRenderers();
        setDrawStyle(0);
        setCharSet(0);
        setCharSet(0, CharSets.CHARSET_US);
        setCharSet(1, CharSets.CHARSET_GRAPHICS);
        this.setCursor(0, 0);
        this.showCursor = true;
        this.clearText();
        resetGraphics();
        this.startRenderers();
    }

    /**
     * Clear text + reset graphics
     */
    public void clear() {
        // clear text + reset graphics
        this.clearText();
        this.resetGraphics();
        this.setCursor(0, 0);
    }

    /**
     * Reset Graphics and recalculate font metrics, image size and repaint complete text buffer.
     * Keeps text in text buffer. Must be invoked when Font or Character attributes are changed.
     */
    public void resetGraphics() {
        charRenderer.initFonts();

//        // DANGEROUS: check double mutex locking !
//        synchronized (paintImageMutex) {
//        }
        synchronized (textBufferMutex) {
            // resize does refresh contents and initializes text image!
            this.resizeTextBuffers(this.numColumns, this.numRows);
        }
    }

    public void update(Graphics g) {
        paint(g);
    }

    public void paint(Graphics g) {
        // Claim Paint mutex to block other threads if applicable...
        synchronized (paintImageMutex) {
            // paint text image:
            Rectangle clip = g.getClipBounds();

            // check clip...
            int w = currentImage.getWidth(null);
            int h = currentImage.getHeight(null);
            int imageOffsetx = 0;
            int imageOffsety = 0;
            // Note: Component offset has already been added into graphics context by Parent!
            g.drawImage(currentImage, 0, 0, w, h, imageOffsetx, imageOffsety, w, h, this);
        }

        // Post painting: Notify waiting threads (for synchronized scrolling).
        synchronized (paintImageMutex) {
            paintImageMutex.notifyAll();
        }
    }

    /**
     * Paint textBuffer in offscreen image buffer
     */
    public boolean paintTextBuffer() {
        return paintTextBuffer(0, 0, this.numColumns, this.numRows);
    }

    /**
     * Paints region [x1,y1] to [x2,y2] - includes x2 if x1==x1 - includes y2 if y1==y2
     */
    protected boolean paintTextBuffer(int x1, int y1, int x2, int y2) {
        if (currentImage == null) {
            // at startup the window might not be visible yet.
            if (!initTextBufferImage())
                return false;
        }

        int charwidth = this.charRenderer.getCharWidth();
        int lineHeight = this.charRenderer.getLineHeight();

        // ===========
        // Option: rendererWaitForPaint
        // Wait for the current Swing Thread to paint the image.
        // This speed up the Swing painting thread but slows down the rendering.
        // ===========

        if (this.optionRendererWaitForPaint) {
            synchronized (paintImageMutex) {
                //
            }
        }

        synchronized (textBufferMutex) {
            // Concurrency!
            // keep current: reset global
            boolean paintAll = (this.paintCompleteTextBuffer || this.optionAlwaysPaintCompleteTextBuffer);

            this.paintCompleteTextBuffer = false;

            // single column mode:
            if (x2 == x1)
                x2++;

            // single line mode:
            if (y2 == y1)
                y2++;

            // don't draw past buffer
            if (x2 > numColumns)
                x2 = numColumns;

            if (y2 > numRows)
                y2 = numRows;

            if (x1 < 0)
                x1 = 0;

            if (y1 < 0)
                y1 = 0;

            Graphics graphics = currentImage.getGraphics();

            // Graphics for all:
            if (graphics instanceof Graphics2D) {
                charRenderer.updateRenderingHints((Graphics2D) graphics);
            }
            graphics.setFont(charRenderer.getFontPlain());
            graphics.setColor(charRenderer.getColorMap().getForeground());

            for (int y = y1; y < y2; y++) {
                // whether next character already has been cleared:
                boolean paintBackgroundAheadDone = false;

                for (int x = x1; x < x2; x++) {
                    int xpos = x * charwidth;
                    int ypos = y * lineHeight;

                    StyleChar sChar = currentBuffer.get(x, y);

                    if (sChar == null) {
                        log.error("NULL char at:{},{}", x, y);
                        continue;
                        // return false;
                    }

                    // no redraw needed
                    if (!paintAll && !sChar.hasChanged)
                        continue;

                    // ====
                    // Italics Clear Ahead mode:
                    // ===
                    // Clear next char before drawing current
                    // This because an italics character can 'lean' into the next character.
                    // So clear next character first, draw current and when rendering neighbour
                    // character, do not clear background, but just draw the character.
                    // ====

                    boolean paintBackground = true;

                    // previous char was italic: current background has already been draw: don't clear current;
                    if (paintBackgroundAheadDone) {
                        paintBackground = false;
                        paintBackgroundAheadDone = false; // reset (!)
                    }

                    // clear next char
                    boolean paintBackgroundAhead = sChar.isItalic() || sChar.isUberBold();

                    // first clear next:
                    if (paintBackgroundAhead) {
                        StyleChar nextChar = currentBuffer.get(x + 1, y);

                        if (nextChar != null) {
                            // clear neighbour background:
                            charRenderer.renderChar(graphics, nextChar, xpos + charwidth, ypos, true, false);
                            // form next drawing that field already has been cleared.
                            this.currentBuffer.needsRepaint(x + 1, y, true); // update draw field !
                            paintBackgroundAheadDone = true;
                        }
                    }

                    // draw current:
                    charRenderer.renderChar(graphics, sChar, xpos, ypos, paintBackground, true);
                    this.currentBuffer.needsRepaint(x, y, false); // has been drawn

                    // check/update cursor:
                    if ((showCursor) && (isCursor(x, y))) {
                        charRenderer.renderCursor(graphics, xpos, ypos, cursorBlinkColor);
                    }
                } // for y
            } // for x
        } // synchronized

        // notify waiting threads:
        synchronized (textBufferMutex) {
            textBufferMutex.notifyAll();
        }

        return true;
    }

    private ColorMap colorMap() {
        return charRenderer.getColorMap();
    }

    public boolean isCursor(int x, int y) {
        return (cursorX == x && cursorY == y);
    }

    public Dimension getPreferredSize() {
        // make sure layout manager respects current image size.
        return getImageSize();
    }

    public Dimension getMaximumSize() {
        // make sure layout manager respects current image size.
        return getImageSize();
    }

    public Dimension getImageSize() {
        return new Dimension(getImageWidth(), getImageHeight());
    }

    public int getImageWidth() {
        return charRenderer.getFontCharWidth() * numColumns;
    }

    public int getImageHeight() {
        return charRenderer.getLineHeight() * numRows;
    }

    public int getNumColumns() {
        return numColumns;
    }

    public int getNumRows() {
        return numRows;
    }

    public void setCursor(int x, int y) {
        if (x < 0) {
            log.error("new cursor X:{}<0", x);
        }

        if (y < 0) {
            log.error("new cursor Y:{}<0", y);
        }

        if (x >= numColumns) {
            log.error("new cursor X:{}>{}", x, numColumns);
        }

        if (y >= numRows) {
            log.error("new cursor Y:{}>{}", y, numRows);
        }

        int prefx = cursorX;
        int prefy = cursorY;

        this.cursorX = x;
        this.cursorY = y;

        // Request repaints: !
        characterChanged(cursorX, cursorY);
        characterChanged(prefx, prefy);
    }

    /**
     * Request and schedule repaint. Actual repaint might occur later. Multiple repaint requests are
     * gathered and the hasChanged field is set to true to merge/combine repaints.
     *
     * @param x position of char which needs to be repainted
     * @param y position of char which needs to be repainted
     */
    private void characterChanged(int x, int y) {
        this.currentBuffer.needsRepaint(x, y, true);
    }

    /**
     * Clear area from [x1,y1] (inclusive) to [x2,y2] (exclusive)
     */
    public void clearArea(int x1, int y1, int x2, int y2) {
        // avoid empty lines:
        if (y2 == y1)
            y2++;

        // avoid empty lines:
        if (x2 == x1)
            x2++;

        if (x2 > this.numColumns) {
            log.debug("***Overflow: x2 > nr_columns:{}>{}\n", x2, numColumns);
            x2 = numColumns;
        }

        if (y2 > this.numRows) {
            log.debug("***Overflow: y2 > nr_rows:{}>{}\n", y2, numRows);
            y2 = numRows;
        }

        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                // clear means: put space char using current draw style:
                this.putChar(' ', x, y);
            }
        }
    }

    public void move(int startX, int startY, int width, int height, int toX, int toY) {
        move(startX, startY, width, height, toX, toY, null);
    }

    // not fully tested yet. If filler==null => clear values
    public void move(int startX, int startY, int width, int height, int toX, int toY,
                     StyleChar filler) {
        int beginx = 0;
        int stepx = 1;
        int endx = width;

        int beginy = 0;
        int stepy = 1;
        int endy = height;

        // reverse horizontal move
        if (toX > startX) {
            beginx = width - 1;
            stepx = -1;
            endx = -1;
        }

        // reverse vertical move
        if (toY > startY) {
            beginy = height - 1;
            stepy = -1;
            endy = -1;
        }

        synchronized (textBufferMutex) {
            for (int j = beginy; j != endy; j += stepy)
                // rows
                for (int i = beginx; i != endx; i += stepx) // columns
                {
                    currentBuffer.copy(toX + i, toY + j, startX + i, startY + j);
                    if (filler == null)
                        currentBuffer.clear(startX + i, startY + j);// clear source
                    else
                        currentBuffer.set(startX + i, startY + j, filler);// clear source

                }
        }
    }

    public void scrollRegion(int startline, int endline, int lines, boolean up) {
        if (up)
            move(0, startline + lines, numColumns, endline - startline - lines, 0, startline);
        else
            move(0, startline, numColumns, endline - startline - lines, 0, startline + lines);
    }

    /**
     * String Encoding
     */
    public String getEncoding() {
        return this.characterEncoding;
    }

    /**
     * String Encoding: Does not reset graphics
     */
    public void setEncoding(String encoding) {
        this.characterEncoding = encoding;
    }

    public void putChar(int c, int x, int y) {
        String str = "" + (char) c;
        this.putChar(getBytes(str), x, y);
    }

    public byte[] getBytes(String str) {
        try {
            return str.getBytes(getEncoding());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return str.getBytes();
    }

    public void putString(String str, int x, int y) {
        setCursor(x, y);
        for (int i = 0; i < str.length(); i++) {
            putChar(str.charAt(i), x + i, y);
        }
    }

    public void writeChar(byte[] bytes) {
        putChar(bytes, getCursorX(), getCursorY());
    }

    // Master method. Write single UTF encoded char at position x,y
    public void putChar(byte[] bytes, int x, int y) {

        if (!currentBuffer.checkBounds(x, y)) {
            log.warn("putChar(): out of bounds (x,y)={},{} >< {},{}n", x, y, numColumns, numRows);
            return;
        }

        int xpos = x;
        int ypos = y;

        // synchronize per put to allow paint event to come between puts.
        synchronized (textBufferMutex) {

            StyleChar sChar;
            if ((sChar = this.currentBuffer.get(xpos, ypos)) == null) {
                // happens during asynchronize resize events!
                // Swing resize the current text buffer but the emulator still appends chars.
                log.warn("No character at position: {},{}\n", xpos, ypos);
            } else {
                sChar.setBytes(bytes);
                sChar.charSet = getCharSet();
                sChar.setDrawStyle(getDrawStyle());
                sChar.foregroundColor = this.drawForegroundIndex;
                sChar.backgroundColor = this.drawBackgroundIndex;
                sChar.alpha = -1; // reset;
                characterChanged(xpos, ypos);
            }
        }
    }

    public String getCharSet() {
        return charSets[charSet]; // may be null;
    }

    public void setCharSet(int nr) {
        log.debug("setCharSet:#{}", nr);
        this.charSet = nr;
    }

    public void beep() {
        Toolkit.getDefaultToolkit().beep();
    }

    public int getDrawStyle() {
        return drawStyle;
    }

    public void setDrawStyle(int style) {
        this.drawStyle = style;

        // style=0 is reset colors as well.
        if (style == 0) {
            this.drawBackgroundIndex = -1;
            this.drawForegroundIndex = -1;
        }
    }

    public int getCursorY() {
        return this.cursorY;
    }

    public int getCursorX() {
        return this.cursorX;
    }

    public void addDrawStyle(int style) {
        setDrawStyle(getDrawStyle() | style);
    }

    public void setDrawBackground(int nr) {
        this.drawBackgroundIndex = nr;
    }

    public void setDrawForeground(int nr) {
        this.drawForegroundIndex = nr;
    }

    /**
     * Set Font size: Does not reset graphics
     */
    public void setFontSize(int i) {
        this.charRenderer.getFontInfo().setFontSize(i);
    }

    /**
     * Set Font type: Does not reset graphics
     */
    public void setFontType(String type) {
        this.charRenderer.getFontInfo().setFontFamily(type);
    }

    /**
     * Set Character Set: Does not reset graphics
     */
    public void setCharSet(int i, String str) {
        log.debug("setCharSet: #{}={}", i, str);
        charSets[i] = str;
    }

    /**
     * Set Colormap: Does not reset graphics.
     */
    public void setColorMap(ColorMap colorMap) {
        this.charRenderer.setColorMap(colorMap);
    }

    public FontInfo getFontInfo() {
        return this.charRenderer.getFontInfo();
    }

    public void dispose() {
        stopRenderers();
        if (this.currentImage != null) {
            this.currentImage.flush();
            this.currentImage = null;
        }
        this.renderTask = null;
        this.renderThread = null;

        if (this.fullBuffer != null) {
            this.fullBuffer.dispose();
            this.fullBuffer = null;
        }
        if (this.altTextBuffer != null) {
            this.altTextBuffer.dispose();
            this.altTextBuffer = null;
        }

        this.currentBuffer = null;
    }

    public void stopRenderers() {
        log.debug(">>> STOPPED <<<");
        this.mustStop = true;
        if (refreshTimer != null)
            this.refreshTimer.stop();
        if (renderThread != null)
            this.renderThread.interrupt();
        renderThread = null;
        refreshTimer = null;
        renderTask = null;
    }

    /**
     * After a resize by the parent container, this method can be called to update the internal size
     * to match the actual component size. If not the charPane will keep its current size and the
     * contents will be clipped.
     */
    public void resizeTextBuffersToAWTSize(Dimension size) {
        int cols = size.width / this.charRenderer.getCharWidth();
        int rows = size.height / this.charRenderer.getLineHeight();
        resizeTextBuffers(cols, rows);
    }

    /**
     * Update character pane size given the new column and row count.
     */
    public void resizeTextBuffers(int cols, int rows) {
        this.numRows = rows;
        this.numColumns = cols;

        this._resizeTextBuffers(numColumns, numRows);

        if (this.cursorY >= numRows)
            cursorY = numRows - 1;

        this.paintCompleteTextBuffer = true;
        this.paintTextBuffer();
        this.revalidate();
        this.repaint();
    }

    public void setColumns(int columns) {
        resizeTextBuffers(columns, this.numRows);
    }

    // actual resize:
    private void _resizeTextBuffers(int numCs, int numRs) {
        log.debug("resizeTextBuffer:{},{}", numCs, numRs);

        synchronized (this.textBufferMutex) {
            if (fullBuffer == null)
                this.fullBuffer = new TextBuffer(numCs, numRs);// add virtual buffer
            else
                this.fullBuffer.resize(numCs, numRs, true);

            if (altTextBuffer == null)
                this.altTextBuffer = new TextBuffer(numCs, numRs, numRs); // no virtual buffer
            else
                this.altTextBuffer.resize(numCs, numRs, true);

            if (this.currentBuffer == null)
                this.currentBuffer = fullBuffer;
        }

        // block paints:
        synchronized (paintImageMutex) {
            this.initTextBufferImage();
        }
    }

    public void pageUp() {
        // todo: virtual page up!
    }

    public void pageDown() {
        // todo: virtual page down
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == refreshTimer) {
            doAnimation();
        }
    }

    public void doAnimation() {
        Dimension size = this.currentBuffer.getSize();

        int div = 8; // slowdown
        int numSteps = 32;
        // smooth cosinus

        Color fg = colorMap().getForeground();

        for (int x = 0; x < size.width; x++)
            for (int y = 0; y < size.height; y++) {
                StyleChar c = currentBuffer.get(x, y);

                double phase0 = (((animationCounter + x * 16) / 7) % numSteps);
                double phase1 = (((animationCounter + x * 16) / 11) % numSteps);
                double phase2 = (((animationCounter + x * 16) / 17) % numSteps);

                double cos0 = Math.cos((phase0 / numSteps) * Math.PI * 2);
                double cos1 = Math.cos((phase1 / numSteps) * Math.PI * 2);
                double cos2 = Math.cos((phase2 / numSteps) * Math.PI * 2);

                int alpha = (int) (128 + 127 * cos0);
                int val0 = (int) (128 + 127 * cos0);
                int val1 = (int) (128 + 127 * cos1);
                int val2 = (int) (128 + 127 * cos2);

                if ((c != null) && (c.hasStyle(StyleChar.STYLE_BLINK))) {
                    if (c.alpha != alpha) {
                        currentBuffer.needsRepaint(x, y, true);// update
                    }
                    c.customForeground = new Color(val0, val1, val2);
                }

                if (isCursor(x, y)) {
                    if (!this.cursorBlink) {
                        cursorBlinkColor = null;
                    } else {
                        int r = (int) (fg.getRed() * (0.5 + cos0 / 2.0));
                        int g = (int) (fg.getGreen() * (0.5 + cos0 / 2.0));
                        int b = (int) (fg.getBlue() * (0.5 + cos0 / 2.0));
                        cursorBlinkColor = new Color(r, g, b, alpha);
                        currentBuffer.needsRepaint(x, y, true);// update
                    }
                }
            }

        this.animationCounter++;
    }

    /**
     * Options
     */
    public String getOption(String optStr) {
        if (optStr == null)
            return null;

        if (optStr.compareTo(CharPane.OPTION_ALWAYS_SYNCHRONIZED_SCROLLING) == 0)
            return "" + this.optionAlwaysSynchronizedScrolling;

        return null;
    }

    public void setOption(String name, String value) {
        if ((name == null) || (value == null))
            return;

        if (name.compareTo(CharPane.OPTION_ALWAYS_SYNCHRONIZED_SCROLLING) == 0) {
            this.optionAlwaysSynchronizedScrolling = Boolean.valueOf(value);
        }

    }

    public void setEnableCursor(boolean value) {
        this.showCursor = value;
    }

    public boolean setAltScreenBuffer(boolean useAlt) {
        // extra buffer can mess up painting thread -> check mutex handling.
        if (!this.optionSupportAltScreenBuffer)
            return false;

        // only swap between render events !
        synchronized (this.textBufferMutex) {
            if (useAlt)
                this.currentBuffer = this.altTextBuffer;
            else
                this.currentBuffer = this.fullBuffer;

            this.paintCompleteTextBuffer = true;
        }

        return true;
    }

    public void setSlowScroll(boolean value) {
        log.error("FIXME: setSlowScroll():" + value);
    }

    public void setCursorOptions(boolean blink) {
        this.cursorBlink = blink;
    }

    public static class CharSets {
        public final static String CHARSET_US = VT_CHARSET_US;
        public final static String CHARSET_UK = VT_CHARSET_UK;
        public final static String CHARSET_GRAPHICS = VT_CHARSET_GRAPHICS;
    }

}
