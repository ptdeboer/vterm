/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.ui.panels.charpane;

import lombok.extern.slf4j.Slf4j;
import nl.piter.vterm.api.CharacterTerminal;
import nl.piter.vterm.api.CursorOptions;
import nl.piter.vterm.api.TermConst;
import nl.piter.vterm.emulator.Util;
import nl.piter.vterm.ui.fonts.FontInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static nl.piter.vterm.api.TermConst.CharSet.CHARSET_GRAPHICS;

/**
 * Character Terminal Render Engine.
 */
@Slf4j
public class CharPane extends JComponent implements CharacterTerminal, ActionListener {

    public static final int MAX_CHARSETS = 16;

    // ========================================================================

    // Terminal
    private int numColumns = 80;
    private int numRows = 25;
    private Charset characterEncoding = StandardCharsets.UTF_8;
    private final TermConst.CharSet[] charSets = new TermConst.CharSet[MAX_CHARSETS];
    private int charSet = 0;
    // Cursor
    private int cursorX = 0;
    private int cursorY = 0;
    private boolean showCursor = true;
    private boolean cursorBlink = false;
    private Color cursorBlinkColor;

    // === Image Character Buffer ==== //
    private Image currentImage;
    private TextBuffer currentBuffer;
    private TextBuffer altTextBuffer;
    private TextBuffer fullBuffer;

    // === Current Draw Style ===
    private int drawStyle = 0;
    private int drawForegroundIndex = -1; // no index -> use default !
    private int drawBackgroundIndex = -1;
    private boolean reverseVideo = false;
    //
    private CharRenderer charRenderer;
    private Color customBackground;
    private Color customForeground;
    private int screenBufferNr;

    // Animations & Threads:
    private int animationCounter = 0;
    private Timer animationTimer;

    // ===============================
    // === MUTEX and Paint Control ===
    // ===============================
    // Whether after each scroll the scrolling should wait until the image is displayed
    private final boolean optionAlwaysSynchronizedScrolling = false;
    // VI demands this
    private final boolean optionSupportAltScreenBuffer = true;
    // used when resizing component
    private final Object bufferMutex = new Object();
    private final Object paintMutex = new Object();

    // ========================================================================
    // INIT
    // ========================================================================

    public CharPane() {
        super();
        init();
    }

    private void init() {
        this.setLayout(null);
        this.setFocusable(true);
        // unset focuskeys, must get TAB chars:
        this.setFocusTraversalKeysEnabled(false);

        // startRefresher();
        charSets[0] = TermConst.CharSet.CHARSET_US;
        charSets[1] = TermConst.CharSet.CHARSET_GRAPHICS;

        // Move to parent container ! : enableEvents(AWTEvent.KEY_EVENT_MASK);
        this.setBackground(Color.BLACK);
        this.setForeground(Color.GREEN);

        this.charRenderer = new CharRenderer();
        this._resizeTextBuffers(this.numColumns, this.numRows, false, true);
    }

    /**
     * Start timers and renderer thread is not running
     */
    public void startRenderers() {
        log.debug("startRenderers(): Started...");

        if ((animationTimer == null) || (!animationTimer.isRunning())) {
            // timer
            animationTimer = new Timer(10, this);
            animationTimer.setInitialDelay(100); // animate 10 per second (blink)
            animationTimer.start();
        }
    }

    /**
     * Initialize Backing Image.
     */
    private void initTextBufferImage() {
        // offscreen buffered image:
        currentImage = new BufferedImage(getImageWidth(), getImageHeight(), BufferedImage.TYPE_INT_ARGB);
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

        int offy = 6;
        int marginx = 8;
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                int index = j + i * 16;
                int x = index % 64;
                int y = index / 64;
                this.setDrawForeground(i);
                this.setDrawBackground(j);
                putString("X", marginx + x, offy + y);
            }
        }
        offy++;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 72; j++) {
                this.setDrawForeground(1);
                this.setDrawBackground(16 + j + i * 72);
                putString("X", 4 + j, offy + i);
            }
        }

        offy += 4;

        int n = 234; //
        int m = 2;
        for (int c = 0; c < n; c++) {
            this.setDrawStyle(c);
            for (int i = 0; i < m; i++) {
                int index = c * m + i;
                int x = 1 + index % 78; // 6x13=78
                int y = offy + index / 78;
                putChar('A' + index % 26, x, y);
            }
        }

        offy += 7;
        this.setDrawStyle(0);

        for (int y = 0; y < 4; y++)
            for (int x = 0; x < 64; x++)
                putChar(x + y * 64, marginx + x, offy + y);

        offy += 4;
        // graphics
        n = CharRenderer.graphOrg0.length();
        this.setCharSet(1, CHARSET_GRAPHICS.toString());
        this.setCharSet(1);
        for (int d = 0; d < 2; d++) {
            if (d % 2 == 1)
                this.addDrawStyle(StyleChar.STYLE_BOLD);

            for (int x = 0; x < n; x++) {
                putChar(CharRenderer.graphOrg0.charAt(x), marginx + x + d * n, offy);
            }
        }

        this.setCursor(0, 0);
        this.setDrawStyle(0);
        this.setCharSet(0);

        //
        this.renderTextBuffer(true);
        repaint();
    }

    /**
     * Clear text buffer, doesn't do repaint.
     */
    public void clearArea() {
        currentBuffer().clearRegion();
    }

    /**
     * Clear text, state and Reset Graphics
     */
    public void reset() {
        this.stopRenderers();
        setDrawStyle(0);
        setCharSet(0);
        setCharSet(0, TermConst.CharSet.CHARSET_US);
        setCharSet(1, TermConst.CharSet.CHARSET_GRAPHICS);
        this.setCursor(0, 0);
        this.showCursor = true;
        this.clearArea();
        resetGraphics();
        this.startRenderers();
    }

    /**
     * Clear text + reset graphics
     */
    public void clear() {
        // clear text + reset graphics
        this.clearArea();
        this.resetGraphics();
        this.setCursor(0, 0);
    }

    /**
     * Reset Graphics and recalculate font metrics, image size and repaint complete text buffer.
     * Keeps text in text buffer. Must be invoked when Font or Character attributes are changed.
     */
    public void resetGraphics() {
        charRenderer.initFonts();

        // resize does refresh contents and initializes text image!
        this.resizeTextBuffers(this.numColumns, this.numRows, true, true);
    }

    public void update(Graphics g) {
        paint(g);
    }

    public void paint(Graphics g) {
        // Claim Paint mutex to block other threads if applicable...
        // paint text image:
        Rectangle clip = g.getClipBounds();
        // check clip...
        int w = currentImage.getWidth(null);
        int h = currentImage.getHeight(null);
        int imageOffsetx = 0;
        int imageOffsety = 0;
        // Note: Component offset has already been added into graphics context by Parent!
        g.drawImage(currentImage, 0, 0, w, h, imageOffsetx, imageOffsety, w, h, this);

        synchronized (paintMutex) {
            paintMutex.notify();
        }
    }

    /**
     * Paint textBuffer in offscreen image buffer.
     * paintAll==false => incremental paint.
     */
    public boolean renderTextBuffer(boolean paintAll) {
        return renderTextBuffer(0, 0, this.numColumns, this.numRows, paintAll);
    }

    public void renderChar(int xpos, int ypos) {
        renderTextBuffer(xpos, ypos, xpos + 1, ypos, true);
        repaint();
    }

    /**
     * Paints region [x1,y1] to [x2,y2] - includes x2 if x1==x1 - includes y2 if y1==y2
     */
    protected boolean renderTextBuffer(int x1, int y1, int x2, int y2, boolean paintAll) {
        if (currentImage == null) {
            // at startup the window might not be visible yet.
            initTextBufferImage();
        }

        int charwidth = this.charRenderer.getCharWidth();
        int lineHeight = this.charRenderer.getLineHeight();

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

        Graphics2D graphics = (Graphics2D) currentImage.getGraphics();
        charRenderer.updateRenderingHints(graphics);

        graphics.setFont(charRenderer.getFontPlain());
        graphics.setColor(getEffectiveForeground());


        for (int y = y1; y < y2; y++) {
            // whether next character already has been cleared:
            boolean paintBackgroundAheadDone = false;

            for (int x = x1; x < x2; x++) {
                int xpos = x * charwidth;
                int ypos = y * lineHeight;

                StyleChar sChar = currentBuffer().get(x, y);

                if (sChar == null) {
                    log.error("NULL char at:{},{}", x, y);
                    continue;
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
                    StyleChar nextChar = currentBuffer().get(x + 1, y);

                    if (nextChar != null) {
                        // clear neighbour background:
                        charRenderer.renderChar(graphics, nextChar, xpos + charwidth, ypos, true, false);
                        // form next drawing that field already has been cleared.
                        currentBuffer().needsRepaint(x + 1, y, true); // update draw field !
                        paintBackgroundAheadDone = true;
                    }
                }

                // draw current:
                charRenderer.renderChar(graphics, sChar, xpos, ypos, paintBackground, true);
                currentBuffer().needsRepaint(x, y, false); // has been drawn

                // check/update cursor:
                if ((showCursor) && (isCursor(x, y))) {
                    charRenderer.renderCursor(graphics, xpos, ypos, cursorBlinkColor);
                }

            } // for y
        } // for x

        return true;
    }

    private Color getEffectiveForeground() {
        return charRenderer.getColorMap().getForeground();
    }

    private Color getEffectiveBackground() {
        return charRenderer.getColorMap().getBackground();
    }

    private ColorMap colorMap() {
        return charRenderer.getColorMap();
    }

    public boolean isCursor(int x, int y) {
        return (cursorX == x && cursorY == y);
    }

    public Dimension getPreferredSize() {
        // Make sure layout manager respects current image size.
        return getImageSize();
    }

    public Dimension getMaximumSize() {
        // Make sure layout manager respects current image size.
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
        } else if (x >= numColumns) {
            log.error("new cursor X:{}>{}", x, numColumns);
        }
        if (y < 0) {
            log.error("new cursor Y:{}<0", y);
        } else if (y >= numRows) {
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
        currentBuffer().needsRepaint(x, y, true);
        renderChar(x, y);
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

        for (int y = beginy; y != endy; y += stepy) { // rows
            for (int x = beginx; x != endx; x += stepx) { // columns
                //
                currentBuffer().copy(toX + x, toY + y, startX + x, startY + y);
                if (filler == null)
                    currentBuffer().clear(startX + x, startY + y);// clear source
                else
                    currentBuffer().set(startX + x, startY + y, filler);// clear source

            }
        }

        int x1 = Util.min(startX, toX);
        int y1 = Util.min(startY, toY);
        int x2 = Util.max(startX + width, toX + width);
        int y2 = Util.max(startY + height, toY + height);
        this.renderTextBuffer(x1, y1, x2, y2, false);
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
    public Charset getEncoding() {
        return this.characterEncoding;
    }

    /**
     * String Encoding: Does not reset graphics.
     */
    public void setEncoding(Charset encoding) {
        this.characterEncoding = encoding;
    }

    public void putChar(int c, int x, int y) {
        String str = "" + (char) c;
        this.putChar(getBytes(str), x, y);
    }

    public byte[] getBytes(String str) {
        return str.getBytes(getEncoding());
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

    /**
     * Master character update method.
     */
    public void putChar(byte[] bytes, int x, int y) {

        // synchronize per put to allow paint event to come between puts.
        if (!currentBuffer().checkBounds(x, y)) {
            log.warn("putChar(): out of bounds (x,y)={},{} >< {},{}", x, y, numColumns, numRows);
            return;
        }

        int xpos = x;
        int ypos = y;


        StyleChar sChar;
        if ((sChar = currentBuffer().get(xpos, ypos)) == null) {
            // happens during asynchronize resize events!
            // Swing resize the current text buffer but the emulator still appends chars.
            log.warn("No character at position: {},{}\n", xpos, ypos);
        } else {
            sChar.setBytes(bytes);
            sChar.charSet = getCharSetName();
            sChar.setDrawStyle(getDrawStyle());
            sChar.foregroundColor = this.drawForegroundIndex;
            sChar.backgroundColor = this.drawBackgroundIndex;
            sChar.customForeground = this.customForeground;
            sChar.customBackground = this.customBackground;
            sChar.alpha = -1; // reset;
            characterChanged(xpos, ypos);
        }
        renderChar(xpos, ypos);
    }

    public String getCharSetName() {
        return Util.toStringOrNull(charSets[charSet]); // may be null;
    }

    public String getCharSetName(int index) {
        return Util.toStringOrNull(charSets[index]); // may be null;
    }

    public void setCharSet(int nr) {
        log.debug("setCharSet:#{}", nr);
        this.charSet = nr;
    }

    public int getCharSet() {
        return this.charSet;
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
        this.customBackground = null;
    }

    public void setDrawForeground(int nr) {
        this.drawForegroundIndex = nr;
        this.customForeground = null;
    }

    @Override
    public void setDrawForeground(int r, int g, int b) {
        this.customForeground = new Color(r, g, b);
    }

    @Override
    public void setDrawBackground(int r, int g, int b) {
        this.customBackground = new Color(r, g, b);
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
        charSets[i] = TermConst.CharSet.valueOf(str);
    }

    /**
     * Set Colormap: Does not reset graphics.
     */
    public void setColorMap(ColorMap colorMap) {
        this.charRenderer.setColorMap(colorMap.duplicate());
    }

    public void setColor(int num, Color c) {
        this.charRenderer.getColorMap().set(num, c);
        // repaint ALL
        this.renderTextBuffer(true);
    }

    public Color getColor(int num) {
        return this.charRenderer.getColorMap().get(num);
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
//        this.renderTask = null;
//        this.renderThread = null;

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
        log.debug("stopRenderers()");
        if (animationTimer != null) {
            this.animationTimer.stop();
        }
        animationTimer = null;
    }

    public void setColumns(int columns) {
        resizeTextBuffers(columns, getNumRows(), false, false);
    }


    public void setColumnsAndRows(int columns, int rows) {
        resizeTextBuffers(columns, rows, false, false);
    }

    /**
     * After a resize by the parent container, this method can be called to update the internal size
     * to match the actual component size. If not the charPane will keep its current size and the
     * contents will be clipped.
     */
    public void resizeTextBuffersToAWTSize() {
        Dimension actual = this.getSize();
        log.info("resizeTextBuffersToAWTSize(): {}", actual);

        int cols = actual.width / this.charRenderer.getCharWidth();
        int rows = actual.height / this.charRenderer.getLineHeight();
        resizeTextBuffers(cols, rows, true, false);
    }


    /**
     * Update character pane size given the new column and row count.
     */
    public void resizeTextBuffers(int cols, int rows, boolean copyContent, boolean initAll) {

        this._resizeTextBuffers(cols, rows, copyContent, initAll);

        if (this.cursorY >= numRows) {
            cursorY = numRows - 1;
        }
        // Full repaint:
        this.renderTextBuffer(true);
        // Will trigger AWT event *here*:
        this.revalidate();
        this.repaint();
    }

    // actual resize:
    private boolean _resizeTextBuffers(int newCs, int newRs, boolean copyContent, boolean initAll) {

        // Concurrent invocations by both AWT and Emulator thread(!):
        synchronized (bufferMutex) {

            // Filter out resize events from AWT *after* a resize already has been triggered by the Emulator (!):
            if ((!initAll) && currentBuffer != null) {
                if ((this.currentBuffer.columns() == newCs) && (this.currentBuffer.rows() == newRs)) {
                    log.warn("resizeTextBuffers(): ignoring double resize event: {},{} => {},{}", this.numColumns, this.numRows, newCs, newRs);
                    return false;
                }
            }

            TextBuffer oldBuffer = this.currentBuffer;

            this.fullBuffer = new TextBuffer(newCs, newRs, newCs, newRs);
            this.altTextBuffer = new TextBuffer(newCs, newRs, newCs, newRs);

            int offsety = 0;
            if ((oldBuffer != null) && newRs < oldBuffer.rows()) {
                // auto move content up, if cursor is below
                if (this.getCursorY() >= newRs) {
                    offsety = getCursorY() - newRs + 1;
                }
            }
            if (copyContent) {
                this.fullBuffer.copyFrom(oldBuffer, 0, offsety);
            }
            this.currentBuffer = fullBuffer;
            //
            log.info("resizeTextBuffers(): DONE resizing: {},{} => {},{}", this.numColumns, this.numRows, newCs, newRs);
            this.numColumns = newCs;
            this.numRows = newRs;
        }

        this.initTextBufferImage();

        return true;
    }

    protected TextBuffer currentBuffer() {
        return this.currentBuffer;
    }

    public void pageUp() {
        // todo: virtual page up!
    }

    public void pageDown() {
        // todo: virtual page down
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == animationTimer) {
            doAnimation();
        }
    }

    public void doAnimation() {
        TextBuffer buffer;
        Dimension size;

        // work asynchronous on reference:
        size = currentBuffer().getSize();
        buffer = currentBuffer();

        int div = 8; // slowdown
        int numSteps = 32;
        // smooth cosinus

        Color fg = getEffectiveForeground();

        for (int x = 0; x < size.width; x++)
            for (int y = 0; y < size.height; y++) {
                StyleChar c = buffer.get(x, y);

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

                if ((c != null) && (c.hasStyle(StyleChar.STYLE_SLOW_BLINK))) {
                    if (c.alpha != alpha) {
                        buffer.needsRepaint(x, y, true);// update
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
                        buffer.needsRepaint(x, y, true);// update
                    }
                }
            }
        this.animationCounter++;
        this.renderTextBuffer(false);
        this.repaint();
    }

    public void setEnableCursor(boolean value) {
        this.showCursor = value;
        if (value) {
            this.renderChar(cursorX, cursorY);
        }
    }

    public CursorOptions getCursorStatus() {
        return CursorOptions.builder()
                .x(cursorX)
                .y(cursorY)
                .enabled(this.showCursor)
                .blink(this.cursorBlink)
                .build();
    }

    public boolean setAltScreenBuffer(boolean useAlt) {
        if (!this.optionSupportAltScreenBuffer)
            return false;

        // extra buffer can mess up painting thread -> check mutex handling.
        synchronized (this.bufferMutex) {
            if (useAlt)
                currentBuffer = this.altTextBuffer;
            else
                currentBuffer = this.fullBuffer;
            this.screenBufferNr = (useAlt) ? 1 : 0;
        }
        this.renderTextBuffer(true);
        return true;
    }

    public int getScreenBufferNr() {
        return screenBufferNr;
    }

    public void setCursorOptions(boolean blink) {
        this.cursorBlink = blink;
    }

    public void setReverseVideo(boolean value) {
        this.reverseVideo = value;
    }

    @Override
    public Dimension getCharacterSize() {
        return new Dimension(this.charRenderer.getCharWidth(), this.charRenderer.getLineHeight());
    }

}
