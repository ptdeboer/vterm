/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.api;

import nl.piter.vterm.ui.charpane.ColorMap;

import java.awt.*;

/**
 * Interface to a Character Terminal.
 */
public interface CharacterTerminal {

    // VT100 CharSets:
    String VT_CHARSET_US = "CHARSET_US";
    String VT_CHARSET_UK = "CHARSET_UK";
    String VT_CHARSET_GRAPHICS = "CHARSET_GRAPHICS";

    int getNumRows();

    int getNumColumns();

    void setCursor(int x, int y);

    int getCursorY();

    int getCursorX();

    /**
     * Write char at current cursor position. Auto moves cursor to the right.
     */
    void writeChar(byte[] bytes);

    /**
     * Put utf-8 character sequence, redrawing might occur later
     */
    void putChar(byte[] bytes, int x, int y);

    void move(int startX, int startY, int width, int height, int toX, int toY);

    /**
     * Clear text buffer(s), does not reset graphics
     */
    void clearText();

    void clearArea(int x1, int y1, int x2, int y2);

    /**
     * Reset graphics, internal state and clear text buffers
     */
    void reset();

    void beep();

    Color getForeground();

    /**
     * Default foreground color
     */
    void setForeground(Color color);

    Color getBackground();

    /**
     * Default background color
     */
    void setBackground(Color color);


    /**
     * Add style flags by performing a logical OR with current style and new style.
     */
    void addDrawStyle(int style);

    /**
     * Current used draw style.
     */
    int getDrawStyle();

    /**
     * Set draw style.
     */
    void setDrawStyle(int style);

    /**
     * Set styled color number from color map. If style==0 then no color from the styled colormap is
     * used
     */
    void setDrawBackground(int nr);

    /**
     * Set styled color number from color map. If style==0 then no color from the styled colormap is
     * used
     */
    void setDrawForeground(int nr);

    /**
     * Color map for indexed color codes. If draw style==0 then default background/foreground will
     * be used.
     */
    void setColorMap(ColorMap colorMap);

    /**
     * Scroll lines from startline(inclusive) to endline (exclusive)
     */
    void scrollRegion(int starline, int endline, int numlines, boolean scrollUp);

    /**
     * Switch to numbered charset
     */
    void setCharSet(int nr);

    /**
     * Set charset.
     */
    void setCharSet(int i, String str);

    /**
     * Enable cursor
     */
    void setEnableCursor(boolean value);

    /**
     * Set the nr of columns. This will initiate a resize.
     */
    void setColumns(int i);

    /**
     * Switch to alternate text buffer. Returns false if not supported or failed to do so.
     */
    boolean setAltScreenBuffer(boolean value);

    /**
     * Synchronized scrolling.
     */
    void setSlowScroll(boolean value);

    void setCursorOptions(boolean blink);

    // --- default interface ---

    default void putChar(byte cbyte, int x, int y) {
        putChar(new byte[]{cbyte},x,y);
    }


}
