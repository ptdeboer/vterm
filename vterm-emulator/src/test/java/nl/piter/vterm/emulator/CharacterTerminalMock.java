package nl.piter.vterm.emulator;

import nl.piter.vterm.api.CharacterTerminal;
import nl.piter.vterm.api.CursorOptions;
import nl.piter.vterm.ui.panels.charpane.ColorMap;

import java.awt.*;

public class CharacterTerminalMock implements CharacterTerminal {

    protected int cursorX = 0;
    protected int cursorY = 0;
    protected int rows;
    protected int columns;
    protected int drawBackground;
    protected int drawForeground;
    protected int drawStyle;
    char[][] chars;

    protected int currentStyle;
    protected int currentCharSet;
    protected String[] charSets=new String[16];

    protected ColorMap colorMap = ColorMap.COLOR_MAP_GREEN_ON_BLACK;

    public CharacterTerminalMock() {
        cursorX = 0;
        cursorY = 0;
        resize(24,80);
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
        this.cursorX = x;
        this.cursorY = y;
    }

    @Override
    public int getCursorY() {
        return cursorY;
    }

    @Override
    public int getCursorX() {
        return cursorX;
    }

    @Override
    public void writeChar(byte[] bytes) {
        chars[cursorY][cursorX]=new String(bytes).charAt(0);
    }

    @Override
    public void putChar(byte[] bytes, int x, int y) {
        chars[y][x]=new String(bytes).charAt(0);
    }

    @Override
    public void move(int startX, int startY, int width, int height, int toX, int toY) {
    }

    @Override
    public void clearArea() {
        clearArea(0,0,columns,rows);
    }

    @Override
    public void clearArea(int x1, int y1, int x2, int y2) {
        for (int j=y1;j<y2;j++) {
            for (int i = x1; i < x2; i++) {
                chars[j][i] = 0;
            }
        }
    }

    @Override
    public void reset() {
        this.currentStyle=0;
    }

    @Override
    public void beep() {
    }

    @Override
    public void setColor(int num, Color color) {
        colorMap.set(num,color);
    }

    @Override
    public Color getColor(int num) {
        return colorMap.get(num);
    }

    @Override
    public void addDrawStyle(int style) {
        currentStyle=(currentStyle | style);
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
    public void setDrawForeground(int r, int g, int b) {

    }

    @Override
    public void setDrawBackground(int r, int g, int b) {

    }

    @Override
    public void setColorMap(ColorMap colorMap) {
    }

    @Override
    public void scrollRegion(int starline, int endline, int numlines, boolean scrollUp) {
        //dummy
    }

    @Override
    public void setCharSet(int nr) {
        currentCharSet=nr;
    }

    @Override
    public int getCharSet() {
        return currentCharSet;
    }

    @Override
    public void setCharSet(int nr, String str) {
        currentCharSet=nr;
        charSets[nr]=str;
    }

    @Override
    public String getCharSetName(int i) {
        return charSets[i];
    }

    @Override
    public void setEnableCursor(boolean value) {
    }

    @Override
    public CursorOptions getCursorStatus() {
        return CursorOptions.builder().build();
    }

    @Override
    public void setColumns(int cols) {
        resize(cols,this.rows);
    }

    @Override
    public void setColumnsAndRows(int cols, int rows) {
        resize(cols,rows);
    }

    @Override
    public boolean setAltScreenBuffer(boolean value) {
        return false;
    }

    @Override
    public int getScreenBufferNr() {
        return 0;
    }

    @Override
    public void setCursorOptions(boolean blink) {
    }

    @Override
    public void setReverseVideo(boolean value) {
    }

    @Override
    public Dimension getCharacterSize() {
        return new Dimension();
    }

}
