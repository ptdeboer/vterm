/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.ui.fonts;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.RenderingHints.Key;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static nl.piter.vterm.ui.fonts.FontConst.*;

/**
 * Simple Font Information holder class. FontInfo is used by the FontToolbar.<br>
 * Use createFont() to instantiate a new Font object using the specified Font information.
 */
@ToString
@Slf4j
public class FontInfo {

    // Static registry (auto init).
    private static Hashtable<String, FontInfo> fontRegistry;

    // Store FontInfo into registry
    public static void store(FontInfo info) {
        info.store();
    }

    public static FontInfo getFontInfo(String alias) {

        if (fontRegistry == null) {
            if (fontRegistry == null)
                fontRegistry = new Hashtable<>();
        }

        FontInfo info = fontRegistry.get(alias);

        if (info != null) {
            return info;
        }

        // Current hardcoded ones:
        if (alias.compareToIgnoreCase(FONT_ICON_LABEL) == 0) {
            Font font = new Font("dialog", 0, 14);
            return store(font, FONT_ICON_LABEL);
        } else if (alias.compareToIgnoreCase(FONT_DIALOG) == 0) {
            Font font = new Font("dialog", 0, 14);
            return store(font, FONT_DIALOG);
        } else if (alias.compareToIgnoreCase(FONT_MONO_SPACED) == 0) {
            Font font = new Font("monospaced", 0, 16);
            return store(font, FONT_MONO_SPACED);
        } else if (alias.compareToIgnoreCase(FONT_TERMINAL) == 0) {
            Font font = new Font("txmonospaced", 0, 16);
            FontInfo newInfo = store(font, FONT_TERMINAL);
            newInfo.setAntiAliasing(true);
            return newInfo;
        } else if (alias.compareToIgnoreCase(FontConst.FONT_FRAKTUR) == 0) {
            Font font = new Font("Chomsky", 0, 16);
            FontInfo newInfo = store(font, FontConst.FONT_FRAKTUR);
            newInfo.setAntiAliasing(true);
            return newInfo;
        }
        return null;
    }

    // --- Font Attributes: family, alias, size, style. ---/

    protected String fontFamily = "Monospaced";
    protected String fontAlias = null;
    protected Integer fontSize = 13;
    protected Integer fontStyle = 0;

    // Not used:
    protected Map<Key, Object> renderingHints;

    protected FontInfo() {
    }

    public FontInfo(Properties props) {
        this.setFontProperties(props);
        // backward compatibility: add alias name
        if (fontAlias == null)
            fontAlias = fontFamily;
    }

    public FontInfo(Font font) {
        init(font);
    }

    /**
     * Store FontInfo under (new) alias.
     */
    protected static FontInfo store(Font font, String alias) {
        FontInfo info = new FontInfo(font);
        info.fontAlias = alias;
        info.store();
        return info;
    }

    protected void init(Font font) {
        fontSize = font.getSize();
        fontStyle = font.getStyle();
        fontFamily = font.getFamily();
        // alias default to fontName
        fontAlias = fontFamily;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public int getFontStyle() {
        return fontStyle;
    }

    public void setFontStyle(int fontStyle) {
        this.fontStyle = fontStyle;
    }

    /**
     * @return Returns the font family, for example "Monospaced" or "Arial"
     */
    public String getFontFamily() {
        return fontFamily;
    }

    /**
     * @param family Font Family name. For example "Monospaced" or "Arial".
     */
    public void setFontFamily(String family) {
        this.fontFamily = family;
    }

    /**
     * Create Font using this Font Information.
     */
    public Font createFont() {
        return new Font(fontFamily, fontStyle, fontSize);
    }

    public boolean isBold() {
        return (fontStyle & Font.BOLD) == Font.BOLD;
    }

    public void setBold(boolean val) {
        fontStyle = setFlag(fontStyle, Font.BOLD, val);
    }

    public boolean isItalic() {
        return (fontStyle & Font.ITALIC) == Font.ITALIC;
    }

    public void setItalic(boolean val) {
        fontStyle = setFlag(fontStyle, Font.ITALIC, val);
    }

    private int setFlag(int orgvalue, int flag, boolean val) {
        if (val) {
            orgvalue = orgvalue | flag;
        } else if ((orgvalue & flag) == flag) {
            orgvalue -= flag;
        }

        return orgvalue;
    }

    /**
     * Return font properties as Properties Set (for persistence).
     *
     * @return Properties set with this font information.
     */
    public Properties getFontProperties() {
        Properties props = new Properties();

        if (fontAlias == null)
            fontAlias = fontFamily;

        props.put(FONT_ALIAS, fontAlias);
        props.put(FONT_FAMILY, fontFamily);
        props.put(FONT_SIZE, fontSize);
        props.put(FONT_STYLE, fontStyle);
        props.put(FONT_HAS_ANTIALIASING, hasAntiAliasing());

        return props;
    }

    /**
     * Uses Font properties and updates info (persistence).
     */
    public void setFontProperties(Properties props) {
        String valstr;

        valstr = (String) props.get(FONT_ALIAS);
        if (valstr != null)
            this.fontAlias = valstr;

        // Old Type name => renamed to FAMILY
        valstr = (String) props.get(FONT_TYPE);
        if (valstr != null)
            setFontFamily(valstr);

        // new Correct 'family' i.s.o. generic 'type'
        valstr = (String) props.get(FONT_FAMILY);
        if (valstr != null)
            setFontFamily(valstr);

        Object intVal = props.get(FONT_SIZE);
        if (intVal != null)
            setFontSize(Integer.valueOf(intVal.toString()));

        valstr = (String) props.get(FONT_STYLE);
        if (valstr != null)
            setFontStyle(Integer.valueOf(valstr));

        Object val = props.get(FONT_HAS_ANTIALIASING);
        if (val != null)
            setAntiAliasing(Boolean.valueOf(val.toString()));

    }

    private void store() {
        if (fontRegistry == null) {
            fontRegistry = new Hashtable<>();
        }
        fontRegistry.put(this.fontAlias, this);
    }

    /**
     * Return optional Rendering Hints for this font.
     * Defaults should be determined by actual screen device.
     */
    public Map<Key, ?> getRenderingHints() {
        return this.renderingHints;
    }

    /**
     * Explicit Set Anti Aliasing Rendering Hints to "On" or "Off". If useAA==null the settings will
     * be set to "Default".
     *
     * @param useAA - Anti Aliasing Rendering Hint: Use true to turn On, use false to run Off, null to
     *              set to "Default".
     */
    public void setAntiAliasing(Boolean useAA) {
        if (this.renderingHints == null) {
            renderingHints = new HashMap<>();
        }

        if (useAA == null) {
            renderingHints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
        } else if (useAA) {
            renderingHints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            renderingHints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }
    }

    /**
     * @return - true if AntiAliasing Rendering Hint has NOT been set to 'off'.
     */
    public boolean hasAntiAliasing() {
        if (renderingHints != null) {
            if (renderingHints.get(RenderingHints.KEY_ANTIALIASING) != null) {
                return (renderingHints.get(RenderingHints.KEY_ANTIALIASING) != RenderingHints.VALUE_ANTIALIAS_OFF);
            }
        }

        return true; // default
    }

    /**
     * Update font settings of specified Component with this font.
     */
    public void updateComponentFont(JComponent jcomp) {
        jcomp.setFont(createFont());
    }

}
