/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.ui.charpane;

import lombok.extern.slf4j.Slf4j;

import java.awt.*;

/**
 * UNsynchronized text buffer contains Matrix of StyleChars
 */
@Slf4j
public class TextBuffer {

    private StyleChar[][] textBuffer;
    private int virtualSize = 100;
    private final int virtualOffset = 0;
    private boolean bufferChanged;
    private int nrColumns;
    private int nrRows;

    public TextBuffer(int cols, int rows) {
        init(cols, rows, 100);
    }

    public TextBuffer(int numCs, int numRs, int numVRs) {
        init(numCs, numRs, numVRs);
    }

    private void init(int cols, int rows, int virtualSize) {
        this.virtualSize = virtualSize;
        this.nrColumns = cols;
        this.nrRows = rows;

        if (virtualSize < rows)
            ;
        virtualSize = rows;

        this.textBuffer = new StyleChar[rows][cols];

        for (int y = 0; y < rows; y++)
            for (int x = 0; x < cols; x++)
                put(x, y, new StyleChar());

        this.bufferChanged = true;
    }

    public void clearAll() {
        for (int y = 0; y < nrRows; y++)
            for (int x = 0; x < nrColumns; x++)
                textBuffer[y][x].clear();

        this.bufferChanged = true;
    }

    /**
     * Puts actual object into text array. Doesn't copy object !
     */
    protected void put(int x, int y, StyleChar newChar) {
        textBuffer[y][x] = newChar;
        newChar.hasChanged = true;
        this.bufferChanged = true;
    }

    public boolean checkBounds(int x, int y) {
        boolean val = true;

        if (textBuffer == null)
            val = false;
        else if ((y < 0) || (x < 0))
            val = false;
        else if (y >= textBuffer.length)
            val = false;
        else if (textBuffer[y] == null)
            val = false;
        else if (x >= textBuffer[y].length)
            val = false;
        else if (textBuffer[y][x] == null) {
            val = false;
        }

        return val;
    }

    /**
     * Returns actual object in text array
     */
    public StyleChar get(int x, int y) {
        if (!checkBounds(x, y)) {
            return null;
        }

        return textBuffer[y][x];
    }

    /**
     * Copies values from StyleChar. Does not store object
     */
    public void set(int x, int y, StyleChar schar) {
        if (!checkBounds(x, y)) {
            log.error("set(): outofbounds (x,y): ({},{})", x, y);
        }

        textBuffer[y][x].copyFrom(schar);
    }

    public void resize(int newCs, int newRs, boolean keepContents) {
        StyleChar[][] oldbuffer = this.textBuffer;

        int offset = 0;
        // move current contents up:
        if (newRs < this.nrRows)
            offset = nrRows - newRs;

        init(newCs, newRs, virtualSize);
        // int offy=textBuffer.length-numRs;
        if (keepContents) {
            for (int y = 0; y < newCs; y++) {
                for (int x = 0; x < newCs; x++) {
                    // copy values from old buffer:
                    if (oldbuffer != null) {
                        int sourcey = y + offset;
                        if ((sourcey < oldbuffer.length) && (x < oldbuffer[sourcey].length))
                            textBuffer[y][x].copyFrom(oldbuffer[sourcey][x]);
                    }
                }
            }
        }

        this.bufferChanged = true;
    }

    public void needsRepaint(int x, int y, boolean val) {
        if (!checkBounds(x, y)) {
            return;
        }

        textBuffer[y][x].hasChanged = val;

        if (val)
            this.bufferChanged = true;
    }

    public void clear(int x, int y) {
        if (!checkBounds(x, y)) {
            log.error("clear(): outofbounds (x,y): ({},{})", x, y);
            return;
        }

        textBuffer[y][x].clear();
        this.bufferChanged = true;
    }

    public void copy(int destx, int desty, int sourcex, int sourcey) {
        if (!checkBounds(sourcex, sourcey)) {
            log.error("copy(): source outofbounds (x,y): ({},{})", sourcex, sourcey);
            return;
        }
        if (!checkBounds(destx, desty)) {
            log.error("copy(): outofbounds (x,y): ({},{})", destx, desty);
            return;
        }
        // copy values !
        // do not copy object reference.
        textBuffer[desty][destx].copyFrom(textBuffer[sourcey][sourcex]);
        this.bufferChanged = true;
    }

    public void setChanged(boolean val) {
        this.bufferChanged = val;
    }

    public boolean hasChanged() {
        return bufferChanged;
    }

    public Dimension getSize() {
        return new Dimension(nrColumns, nrRows);
    }

    public void dispose() {
        this.textBuffer = null; // nullify object references.
    }
}
