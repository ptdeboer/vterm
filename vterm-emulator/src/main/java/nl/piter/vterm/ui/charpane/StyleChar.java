/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.ui.charpane;

import java.awt.*;

/**
 * Styled Character.
 * Holder for the character buffer. Keeps char, style color + charset (name).
 */
public class StyleChar {

    public static final int STYLE_NONE = 0x0000;
    public static final int STYLE_BOLD = 0x0001 << 1;
    public static final int STYLE_ITALIC = 0x0001 << 2;
    public static final int STYLE_INVERSE = 0x0001 << 3;
    public static final int STYLE_UNDERSCORE = 0x0001 << 4;
    public static final int STYLE_UBERBOLD = 0x0001 << 05;
    public static final int STYLE_BLINK = 0x0001 << 06;
    public static final int STYLE_HIDDEN = 0x0001 << 07;

    // =====================================================

    protected int MAX_BYTES = 8;
    protected byte[] charBytes = new byte[MAX_BYTES];
    protected int numBytes = 0;
    // color index -1 -> use default fore/back ground
    protected int style = 0;
    protected  int foregroundColor = -1;
    protected int backgroundColor = -1;
    protected  Color customForeground;
    protected String charSet = null; // NAMED charSet ! (if null inheret)
    protected int alpha = 255; // 0=transparent,255=opaque
    // state
    protected boolean hasChanged = true; // when a redraw is needed

    public void copyFrom(StyleChar schar) {
        setBytes(schar.charBytes, schar.numBytes);
        this.style = schar.style;
        this.backgroundColor = schar.backgroundColor;
        this.foregroundColor = schar.foregroundColor;
        this.charSet = schar.charSet;
        this.hasChanged = true; //
        this.alpha = schar.alpha;
    }

    public void setBytes(byte[] bytes) {
        System.arraycopy(bytes, 0, this.charBytes, 0, bytes.length);
        numBytes = bytes.length;
    }

    public void setBytes(byte[] bytes, int len) {
        System.arraycopy(bytes, 0, this.charBytes, 0, len);
        numBytes = len;
    }

    public void setChar(byte c) {
        charBytes[0] = c;
        numBytes = 1;
    }

    public void clear() {
        setChar((byte) ' ');
        style = 0;
        foregroundColor = -1;
        backgroundColor = -1;
        customForeground = null;
        hasChanged = true; // needs redraw
        charSet = null;
        alpha = -1;
    }

    /**
     * Alpha Blending ! 0-255
     */
    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    public boolean hasStyle(int styleFlag) {
        return (this.style & styleFlag) > 0;
    }

    public void setDrawStyle(int drawStyle) {
        this.style = drawStyle;
    }

    public boolean isItalic() {
        return hasStyle(STYLE_ITALIC);
    }

    public boolean isBold() {
        return hasStyle(STYLE_BOLD);
    }

    public boolean isInverse() {
        return hasStyle(STYLE_INVERSE);
    }

    public boolean hasUnderscore() {
        return hasStyle(STYLE_UNDERSCORE);
    }

    /**
     * My custom style.
     */
    public boolean isUberBold() {
        return hasStyle(STYLE_UBERBOLD);
    }

    /**
     * Compare with single char
     */
    public boolean isChar(char c) {
        return this.numBytes == 1 && (charBytes[0] == c);
    }

}
