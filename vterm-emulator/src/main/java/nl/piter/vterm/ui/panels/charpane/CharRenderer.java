/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.ui.panels.charpane;

import lombok.extern.slf4j.Slf4j;
import nl.piter.vterm.api.TermConst;
import nl.piter.vterm.ui.fonts.FontConst;
import nl.piter.vterm.ui.fonts.FontInfo;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * Character and graphics renderer for a single cell (character).
 */
@Slf4j
public class CharRenderer {

    // Graphics Set:Reverse engineered from xterm codes:
    // echo <ESC>")0"<CTRL-N>"abcdefghijklmnopqrstuvwxyz"<CTRL-O>
    // Note, java uses 16-bit chars, following strings are utf-8 (!)
    public final static String graphOrg0 = "_`abcdefghijklmnopqrstuvwxyz";
    public final static String graphSet1 = " ◆▒␉␌␍␊°±␤␋┘┐┌└┼⎺⎻─⎼⎽├┤┴┬│≤≥";

    // --- instance --- //

    // Whether to use own graphics charset renderer instead of the default fonts graphics
    private final boolean optionUseOwnGraphicsCharsetRenderer = true;
    private final boolean optionFatGraphicPipes = true;
    private ColorMap colorMap = ColorMap.COLOR_MAP_GREEN_ON_BLACK;

    // === Font Metrics and Renderings ===
    private final FontInfo fontInfo;
    private java.awt.Font fontPlain;
    private int fontCharWidth;
    private int fontDescent;
    private int fontAscent;
    private int fontCharHeight;
    private Font fontBold;
    private Font fontItalic;
    private Font fontItalicBold;
    private final int lineLeading = 0; // pixels
    private Map<RenderingHints.Key, ?> renderingHints;


    public CharRenderer() {
        // initialize font metrics:
        this.fontInfo = FontInfo.getFontInfo(FontConst.FONT_TERMINAL);
        initFont(fontInfo);
    }

    public void setColorMap(ColorMap map) {
        this.colorMap = map;
    }

    /**
     * Initialize font&font metrics
     */
    private void initFont(FontInfo finfo) {
        String fontType = finfo.getFontFamily();
        int fontStyle = finfo.getFontStyle();
        int fontSize = finfo.getFontSize();

        // dummy image for font metrics:
        Image dummyImage = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);

        // metrics:
        fontPlain = finfo.createFont();
        fontBold = new Font(fontType, Font.BOLD, fontSize);
        fontItalic = new Font(fontType, Font.ITALIC, fontSize);
        fontItalicBold = new Font(fontType, Font.BOLD | Font.ITALIC, fontSize);
        //
        Graphics graphics = dummyImage.getGraphics();
        graphics.setFont(fontPlain);
        graphics.dispose();
        FontMetrics metrics = graphics.getFontMetrics();
        this.fontDescent = metrics.getDescent();
        this.fontAscent = metrics.getAscent();
        this.fontCharHeight = metrics.getHeight();

        // biggest char on the block:
        this.fontCharWidth = metrics.charWidth('W');
        renderingHints = finfo.getRenderingHints();
        dummyImage.flush();
    }

    void renderChar(Graphics2D imageGraphics, StyleChar sChar, int xpos, int ypos, boolean paintBackground, boolean paintForeground) {
        renderTemplate(imageGraphics, sChar, xpos, ypos, paintBackground, paintForeground);
    }

    protected BufferedImage duplicate(BufferedImage image) {
        ColorModel cm = image.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = image.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    protected int createHash(StyleChar sChar, boolean paintBackground, boolean paintForeground) {
        return Objects.hash(sChar, paintBackground, paintForeground);
    }

    /**
     * Render single character (cell) in imageGraphics
     */
    void renderTemplate(Graphics2D imageGraphics, StyleChar sChar, int xpos, int ypos, boolean paintBackground, boolean paintForeground) {
        int style = sChar.style;

        // check indexed colors!
        Color fg = colorMap.resolve(sChar.foregroundColor);
        Color bg = colorMap.resolve(sChar.backgroundColor);

        if (sChar.customForeground != null) {
            fg = sChar.customForeground;
        }

        if (sChar.customBackground != null) {
            bg = sChar.customBackground;
        }

        if (fg == null)
            fg = this.getEffectiveForeground();

        if (bg == null)
            bg = this.getEffectiveBackground();

        if ((style & StyleChar.STYLE_INVERSE) > 0) {
            // swap bg/fg
            Color c = fg;
            fg = bg;
            bg = c;
        }

        // optional alpha level:
        int alpha = sChar.alpha;

        // HIDDEN / FAINT
        if ((style & StyleChar.STYLE_FAINT) > 0)
            alpha = 128;
        // Hidden => fixed alpha of 25%;
        if ((style & StyleChar.STYLE_HIDDEN) > 0)
            alpha = 64;

        // alpha=-1 don't care/inherit from foreground
        // alpha=255 is opaque which means also inherit from foreground !
        // alpha blended color!
        if ((alpha >= 0) && (alpha < 255))
            fg = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), alpha);

        int lineHeight = getLineHeight();
        int charWidth = getCharWidth();

        // Paint background
        if (paintBackground) {
            imageGraphics.setColor(bg);
            // can use background image here:
            imageGraphics.fillRect(xpos, ypos, charWidth, lineHeight);
        }

        if (!paintForeground)
            return;

        if (sChar.isChar(' ')) {
            // space already drawn as background has been cleared.
        } else {
            boolean isGraphicsChar = isGraphicsCharSet(sChar.charSet);
            if (isGraphicsChar && this.optionUseOwnGraphicsCharsetRenderer)
                renderGraphicsChar(imageGraphics, sChar, fg, bg, xpos, ypos);
            else
                renderPlainChar(imageGraphics, sChar, fg, bg, xpos, ypos);
        }

    }

    public Color getEffectiveForeground() {
        return colorMap.getForeground();
    }

    public Color getEffectiveBackground() {
        return colorMap.getBackground();
    }

    private void renderGraphicsChar(Graphics2D imageGraphics, StyleChar schar,
                                    Color fg, Color bg, int xpos, int ypos) {
        // 8-bits to 16-bits:
        char c = (char) schar.charBytes[0];

        boolean leftLine = false;
        boolean rightLine = false;
        boolean upperLine = false;
        boolean lowerLine = false;

        int charWidth = this.getCharWidth();
        int lineHeight = this.getLineHeight();
        // middle pixels or pixel near middle in the case of an even char height/width
        // the real middle if between two pixels.
        int midlx = xpos + (charWidth / 2); // middle pixel or the pixel to the left of the middle.
        int midrx = xpos + ((charWidth + 1) / 2); // if charWidth is even right middle pixel != left middel pixel
        int midhy = ypos + (lineHeight / 2);
        int midly = ypos + ((lineHeight + 1) / 2);
        // use one center point left and above logical middle (if even size!).
        midrx = midlx;
        midly = midhy;
        int endx = xpos + charWidth;
        int endy = ypos + lineHeight;

        double[][] alphaBlends = {//
                {},
                {1.0},
                {1.0, 0.5},
                {1.0, 0.75, 0.5},
                {1.0, 0.9, 0.5, 0.25}
        };
        // size and attributes
        int size = 1;
        // Fat Pipes instead of thin lines;

        if (optionFatGraphicPipes)
            if (schar.isBold())
                size = 4;
            else
                size = 2;
        else if (schar.isBold())
            size = 2;
        else
            size = 1;

        boolean notSupported = false;

        switch (c) {
            case 'u': // ┤
                lowerLine = true;
            case 'j': // ┘
                leftLine = true;
                upperLine = true;
                break;
            case 'k': // ┐
                lowerLine = true;
                leftLine = true;
                break;
            case 'w': // ┬
                leftLine = true;
            case 'l': // ┌
                lowerLine = true;
                rightLine = true;
                break;
            case 'v':
                leftLine = true;
            case 'm': // └
                upperLine = true;
                rightLine = true;
                break;
            case 'n':
                upperLine = true;
                rightLine = true;
                lowerLine = true;
                leftLine = true;
                break;
            case 'x':
                upperLine = true;
                lowerLine = true;
                break;
            case 'q': // ─
                leftLine = true;
                rightLine = true;
                break;
            case 't': // ├
                upperLine = true;
                lowerLine = true;
                rightLine = true;
                break;
            // "opqrs" => "⎺⎻─⎼⎽"
            case 'o':
                leftLine = true;
                rightLine = true;
                midhy = ypos + 1 + size;
                midly = ypos + 1 + size;
                break;
            case 'p':
                leftLine = true;
                rightLine = true;
                midhy -= lineHeight / 4;
                midly -= lineHeight / 4;
                break;
            // case q already done
            case 'r':
                leftLine = true;
                rightLine = true;
                midhy += lineHeight / 4;
                midly += lineHeight / 4;
                break;
            case 's':
                leftLine = true;
                rightLine = true;
                midhy = endy - 1 - size;
                midly = endy - 1 - size;
                break;
            default:
                notSupported = true;
                break;
        }

        if (notSupported)
            renderPlainChar(imageGraphics, schar, fg, bg, xpos, ypos);

        for (int i = 0; i < size; i++) {
            Color drawFG = fg;

            if (optionFatGraphicPipes) {
                drawFG = ColorMap.blendColor(bg, fg, alphaBlends[size][i], true);
            }

            imageGraphics.setColor(drawFG);

            // todo: beter logic
            if (leftLine) {
                if (upperLine)
                    imageGraphics.drawLine(xpos, midhy - i, midlx - i, midhy - i);
                else
                    imageGraphics.drawLine(xpos, midhy - i, midlx, midhy - i);

                if (lowerLine)
                    imageGraphics.drawLine(xpos, midly + i, midlx - i, midly + i);
                else
                    imageGraphics.drawLine(xpos, midly + i, midlx, midly + i);
            }

            if (rightLine) {
                if (upperLine)
                    imageGraphics.drawLine(midrx + i, midhy - i, endx, midhy - i);
                else
                    imageGraphics.drawLine(midrx, midhy - i, endx, midhy - i);

                if (lowerLine)
                    imageGraphics.drawLine(midrx + i, midly + i, endx, midly + i);
                else
                    imageGraphics.drawLine(midrx, midly + i, endx, midly + i);
            }

            if (upperLine) {
                if (leftLine)
                    imageGraphics.drawLine(midlx - i, ypos, midlx - i, midhy - i);
                else
                    imageGraphics.drawLine(midlx - i, ypos, midlx - i, midhy);

                if (rightLine)
                    imageGraphics.drawLine(midrx + i, ypos, midrx + i, midhy - i);
                else
                    imageGraphics.drawLine(midrx + i, ypos, midrx + i, midhy);

            }
            if (lowerLine) {
                if (leftLine)
                    imageGraphics.drawLine(midlx - i, midly + i, midlx - i, endy);
                else
                    imageGraphics.drawLine(midlx - i, midly, midlx - i, endy);

                if (rightLine)
                    imageGraphics.drawLine(midrx + i, midly + i, midrx + i, endy);
                else
                    imageGraphics.drawLine(midrx + i, midly, midrx + i, endy);
            }
        }
    }

    private void renderPlainChar(Graphics2D imageGraphics, StyleChar schar,
                                 Color fg, Color bg, int xpos, int ypos) {
        //
        int style = schar.style;
        byte[] bytes = schar.charBytes;
        int numBytes = schar.numBytes;

        // lower left corner to start drawing (above descent):
        int imgx = xpos;
        int basey = ypos + getLineHeight() - fontDescent;// text center start above lower border
        int midy = basey - fontAscent / 3;
        String encoded;
        encoded = new String(bytes, 0, numBytes, StandardCharsets.UTF_8);

        encoded = mapCharsetChars(schar.charSet, encoded);

        // Render Character;
        // Blink ? => currently done in animation thread
        boolean blink = ((style & StyleChar.STYLE_SLOW_BLINK) > 0);
        boolean faint = ((style & StyleChar.STYLE_FAINT) > 0);
        boolean bold = ((style & StyleChar.STYLE_BOLD) > 0);
        boolean italic = ((style & StyleChar.STYLE_ITALIC) > 0);
        boolean uberbold = ((style & StyleChar.STYLE_UBERBOLD) > 0);

        imageGraphics.setColor(fg);

        if (bold && !italic)
            imageGraphics.setFont(fontBold);
        else if (!bold && italic)
            imageGraphics.setFont(fontItalic);
        else if (bold && italic)
            imageGraphics.setFont(fontItalicBold);
        else
            imageGraphics.setFont(fontPlain);

        Graphics2D g2d = imageGraphics;

        if (this.fontInfo.getRenderingHints() != null) {
            g2d.setRenderingHints(fontInfo.getRenderingHints());
        }

        // =========================
        // Actual Rendering
        // =========================

        // slow :
        if (uberbold) {
            Color shadedFG = ColorMap.blendColor(bg, fg, 0.5, true);
            imageGraphics.setColor(shadedFG);
            imageGraphics.drawString(encoded, imgx - 1, basey);
            imageGraphics.drawString(encoded, imgx + 1, basey);
            imageGraphics.drawString(encoded, imgx, basey - 1);
            imageGraphics.drawString(encoded, imgx, basey + 1);
            imageGraphics.setColor(fg);
            imageGraphics.drawString(encoded, imgx, basey);
        } else {
            imageGraphics.drawString(encoded, imgx, basey);
        }

        // add line:
        if ((style & StyleChar.STYLE_UNDERSCORE) > 0) {
            imageGraphics.drawLine(imgx, basey + 1, imgx + getCharWidth(), basey + 1);
        }
        if ((style & StyleChar.STYLE_STRIKETHROUGH) > 0) {
            imageGraphics.drawLine(imgx, midy, imgx + getCharWidth(), midy);
        }
    }

    private String mapCharsetChars(String charset, String org) {
        if (!isGraphicsCharSet(charset)) {
            return org;
        }

        char[] chars = new char[org.length()];
        for (int i = 0; i < org.length(); i++) {
            chars[i] = this.mapGraphicsChar(org.charAt(i));
        }

        return new String(chars);
    }

    private static boolean isGraphicsCharSet(String charset) {
        if (charset == null)
            return false;

        return charset.compareTo(TermConst.CharSet.CHARSET_GRAPHICS.toString()) == 0;
    }

    public void initFonts() {
        initFont(this.fontInfo);
    }

    private char mapGraphicsChar(char c) {
        // linear lookup, could use mapping tables:
        for (int i = 0; i < graphOrg0.length(); i++) {
            if (graphOrg0.charAt(i) == c) {
                return graphSet1.charAt(i);
            }
        }
        return c;
    }

    public ColorMap getColorMap() {
        return colorMap;
    }

    public Font getFontPlain() {
        return fontPlain;
    }

    void updateRenderingHints(Graphics2D graphics) {
        if (this.renderingHints == null)
            return;

        RenderingHints.Key[] keys = renderingHints.keySet().toArray(new RenderingHints.Key[0]);
        for (RenderingHints.Key key : keys) {
            Object value = renderingHints.get(key);
            graphics.setRenderingHint(key, value);
        }
    }

    public FontInfo getFontInfo() {
        return fontInfo;
    }

    public void renderCursor(Graphics graphics, int xpos, int ypos, Color cursorBlinkColor) {
        Color fg = getEffectiveForeground();
        if (cursorBlinkColor != null) {
            fg = cursorBlinkColor;
        }

        graphics.setXORMode(fg);
        graphics.setColor(getEffectiveBackground());
        graphics.fillRect(xpos, ypos, getCharWidth() - 1, getLineHeight() - 1);
        graphics.setPaintMode();
    }

    private ColorMap colorMap() {
        return colorMap;
    }

    // Space between 'lowest descenders on the glyphs' and baseline.
    public int getFontDescent() {
        return fontDescent;
    }

    /**
     * Full Line Height =  Character Height + Line Spacing + Font Descent.
     */
    public int getLineHeight() {
        return this.fontCharHeight + this.lineLeading;
    }

    public int getFontCharWidth() {
        return fontCharWidth;
    }

    public int getFontCharHeight() {
        return fontCharHeight;
    }

    public int getCharWidth() {
        return this.fontCharWidth;
    }

    public int getCharHeight() {
        return this.fontCharHeight;
    }

}
