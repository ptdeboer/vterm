/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.ui.panels.charpane;

import lombok.EqualsAndHashCode;

import java.awt.*;

/**
 * Styled Character.
 * Holder for the character buffer. Keeps char, style color + charset (name).
 */
// Important: for the render cache to work, the StyleChar hashcode() and equals() must be defined.
@EqualsAndHashCode
public class StyleChar {

    public static final int STYLE_NONE = 0x0000;
    // ECMA order: bitnr 1-9 = SGR nr 1-9.
    public static final int STYLE_BOLD = 0x01 << 1;
    public static final int STYLE_FAINT = 0x01 << 2;
    public static final int STYLE_ITALIC = 0x01 << 3;
    public static final int STYLE_UNDERSCORE = 0x01 << 4;
    public static final int STYLE_SLOW_BLINK = 0x01 << 5;
    public static final int STYLE_FAST_BLINK = 0x01 << 6;
    public static final int STYLE_INVERSE = 0x01 << 7;
    public static final int STYLE_HIDDEN = 0x01 << 8;
    public static final int STYLE_STRIKETHROUGH = 0x01 << 9;
    // misc
    public static final int STYLE_UBERBOLD = 0x01 <<10;
    public static final int STYLE_DOUBLE_UNDERSCORE = 0x0001 << 11;
    public static final int STYLE_FRAKTUR = 0x0001 << 13;
    // =====================================================

    protected int MAX_BYTES = 8;
    protected byte[] charBytes = new byte[MAX_BYTES];
    protected int numBytes = 0;
    // color index -1 -> use default fore/back ground
    protected int style = 0;
    protected int foregroundColor = -1;
    protected int backgroundColor = -1;
    protected Color customForeground;
    protected Color customBackground;
    protected String charSet = null; // NAMED charSet ! (if null inheret)
    protected int alpha = 255; // 0=transparent,255=opaque
    // state
    protected boolean hasChanged = true; // when a redraw is needed

    public void copyFrom(StyleChar schar) {
        setBytes(schar.charBytes, schar.numBytes);
        this.style = schar.style;
        this.backgroundColor = schar.backgroundColor;
        this.foregroundColor = schar.foregroundColor;
        this.customForeground = schar.customForeground;
        this.customBackground = schar.customBackground;
        this.charSet = schar.charSet;
        this.alpha = schar.alpha;
        this.hasChanged = true;
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
        customBackground =null;
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


//    // --- Generated --- //
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        StyleChar styleChar = (StyleChar) o;
//        return MAX_BYTES == styleChar.MAX_BYTES && numBytes == styleChar.numBytes && style == styleChar.style && foregroundColor == styleChar.foregroundColor && backgroundColor == styleChar.backgroundColor && alpha == styleChar.alpha && hasChanged == styleChar.hasChanged && Arrays.equals(charBytes, styleChar.charBytes) && Objects.equals(customForeground, styleChar.customForeground) && Objects.equals(charSet, styleChar.charSet);
//    }
//
//    @Override
//    public int hashCode() {
//        int result = Objects.hash(MAX_BYTES, numBytes, style, foregroundColor, backgroundColor, customForeground, charSet, alpha, hasChanged);
//        result = 31 * result + Arrays.hashCode(charBytes);
//        return result;
//    }

}
