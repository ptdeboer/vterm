/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.ui.fonts;

public class FontConst {

    /**
     * Logical name, used a key in font database.
     */
    public static final String FONT_ALIAS = "fontAlias";

    /**
     * Most specific font type. Might be equal to "font family" or more specific.
     */
    @Deprecated
    public static final String FONT_TYPE = "fontType";

    /**
     * Less specific font type or font "family"
     */
    public static final String FONT_FAMILY = "fontFamily";

    /**
     * Italic,Bold,Underlined, etc.
     */
    public static final String FONT_STYLE = "fontStyle";

    /**
     * Size in screen pixels.
     */
    public static final String FONT_SIZE = "fontSize";

    public static final String FONT_HAS_ANTIALIASING = "fontHasAntiAliasing";

    // ---
    // Some default font types:
    // ---

    public static final String FONT_ICON_LABEL = "iconlabel";

    public static final String FONT_MONO_SPACED = "monospaced";

    public static final String FONT_DIALOG = "dialog";

    public static final String FONT_TERMINAL = "terminal";

}
