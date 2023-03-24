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
import nl.piter.vterm.ui.charpane.ColorMap;
import org.junit.Assert;
import org.junit.Test;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static nl.piter.vterm.emulator.VTxTokenDefs.CTRL_ESC;

@Slf4j
public class VTXEmulatorTest {

    @Test
    public void testSend() throws IOException {

        InputStream inps;
        ByteArrayOutputStream outps = new ByteArrayOutputStream(1024);
        VTxEmulator emulator = new VTxEmulator(createCharacterTerm(), null, outps);

        byte[] bytes = {CTRL_ESC, '[', '?', '1', ';', '2', 'c'};

        emulator.send(bytes);

        byte[] sendBytes = outps.toByteArray();

        for (int i = 0; i < bytes.length; i++) {
            Assert.assertEquals("Integer at index:#" + i + " mismatches", bytes[i], sendBytes[i]);
            log.debug(" integer at #{}, matches:{}", i, bytes[i]);
        }
    }

    private CharacterTerminal createCharacterTerm() {
        return new DummyCharacterTerminal();
    }

    public static class DummyCharacterTerminal implements CharacterTerminal {

        int x = 0;
        int y = 0;
        int rows;
        int columns;
        int drawBackground;
        int drawForeground;
        int drawStyle;
        char[][] chars;

        public DummyCharacterTerminal() {
            x = 0;
            y = 0;
            rows = 24;
            columns = 80;
            resize(rows, columns);
        }

        public void resize(int rows, int cols) {
            this.rows = rows;
            this.columns = cols;
            chars = new char[rows][cols];
        }

        @Override
        public int getNumRows() {
            return rows;
        }

        @Override
        public int getNumColumns() {
            return columns;
        }

        @Override
        public void setCursor(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int getCursorY() {
            return y;
        }

        @Override
        public int getCursorX() {
            return x;
        }

        @Override
        public void writeChar(byte[] bytes) {
        }

        @Override
        public void putChar(byte[] bytes, int x, int y) {
        }

        @Override
        public void move(int startX, int startY, int width, int height, int toX, int toY) {
        }

        @Override
        public void clearText() {
        }

        @Override
        public void clearArea(int x1, int y1, int x2, int y2) {
        }

        @Override
        public void reset() {
        }

        @Override
        public void beep() {
        }

        @Override
        public Color getForeground() {
            return null;
        }

        @Override
        public void setForeground(Color color) {
        }

        @Override
        public Color getBackground() {
            return null;
        }

        @Override
        public void setBackground(Color color) {
        }

        @Override
        public void addDrawStyle(int style) {
        }

        @Override
        public int getDrawStyle() {
            return drawStyle;
        }

        @Override
        public void setDrawStyle(int style) {
            this.drawStyle = style;
        }

        @Override
        public void setDrawBackground(int nr) {
            this.drawBackground = nr;
        }

        @Override
        public void setDrawForeground(int nr) {
            this.drawForeground = nr;
        }

        @Override
        public void setColorMap(ColorMap colorMap) {
        }

        @Override
        public void scrollRegion(int starline, int endline, int numlines, boolean scrollUp) {
        }

        @Override
        public void setCharSet(int nr) {
        }

        @Override
        public void setCharSet(int i, String str) {
        }

        @Override
        public void setEnableCursor(boolean value) {
        }

        @Override
        public void setColumns(int cols) {
            this.columns = cols;
        }

        @Override
        public boolean setAltScreenBuffer(boolean value) {
            return false;
        }

        @Override
        public void setSlowScroll(boolean value) {

        }

        @Override
        public void setCursorOptions(boolean blink) {

        }
    }
}

