/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.api;

import nl.piter.vterm.ui.panels.charpane.ColorMap;

import java.awt.*;
import java.nio.charset.StandardCharsets;

/**
 * Interface to a Character Terminal.
 * Cursor logic and movement is now done at emulator level.
 */
public interface CharacterTerminal {

    int numRows();

    int numColumns();

    /**
     * Set the nr of columns. This will initiate a resize.
     */
    void setColumns(int cols);

    void setColumnsAndRows(int cols, int rows);

    void setCursor(int x, int y);

    int getCursorY();

    int getCursorX();

    /**
     * Write char at current cursor position. Does not move cursor.
     */
    void writeChar(byte[] bytes);

    /**
     * Put single char or utf-8 character sequence.
     */
    void putChar(byte[] bytes, int x, int y);

    void move(int startX, int startY, int width, int height, int toX, int toY);

    /**
     * Clear text buffer(s), does not reset graphics.
     */
    void clearArea();

    void clearArea(int x1, int y1, int x2, int y2);

    /**
     * Reset graphics, internal state and clear text buffers.
     */
    void reset();

    void beep();

    void setColor(int num, Color color);

    Color getColor(int num);

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

    void setDrawForeground(int r, int g, int b);

    void setDrawBackground(int r, int g, int b);

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

    int getCharSet();

    /**
     * Set charset.
     */
    void setCharSet(int i, String str);

    String getCharSetName(int i);

    void setEnableCursor(boolean value);

    void setCursorOptions(boolean blink);

    CursorOptions getCursorStatus();

    /**
     * Switch to alternate text buffer. Returns false if not supported or failed to do so.
     */
    boolean setAltScreenBuffer(boolean value);

    /**
     * 0=normal, 1 =alt
     */
    int getScreenBufferNr();

    void setReverseVideo(boolean value);

    /**
     * Character size in pixel. Chararacter height is total Line Height. Not Font Height.
     */
    Dimension getCharacterSize();

    // --- default interface ---

    default void putChar(char optUtf, int x, int y) {
        putChar(Character.toString(optUtf).getBytes(StandardCharsets.UTF_8), x, y);
    }

    default void setCharSet(int nr, TermConst.CharSet mapCharSet) {
        this.setCharSet(nr, mapCharSet.toString());
    }

    default void unsetDrawStyle(int style) {
        setDrawStyle(getDrawStyle() & ~style);
    }
}
