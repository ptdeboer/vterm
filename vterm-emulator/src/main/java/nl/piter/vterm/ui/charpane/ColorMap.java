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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

/**
 * Xterm compatible 256 colormap.
 */
@Slf4j
public class ColorMap extends ArrayList<Color> {

    /**
     * Default XTerm color codes with High Contrast.<br>
     * Index 0 matches Xterm color 0, etc.  (xterm 30-37)  #LS_COLORS
     * <pre>
     * 0) Black (Dark)             # or
     * 1) Red: Used as Alert       # Zip/Tar/Archives, background missing link
     * 2) Green                    # Executable/Shell
     * 3) Yellow (Brownish?)       # Device
     * 3) Blue Default background  # Directory
     * 4) Magenta                  # Image/Gif
     * 5) Cyan                     # Link
     * 6) White                    # foreground missing link
     * 7)
     * 8) Set foreground color to default (original)
     * </pre>
     */
    //@formatter:off
    public static final Color[] colorMapWhite = {
            // White/Bright Standard Xterm color map + # bash LS_COLORS
            Color.BLACK,               // 0: black
            Color.RED,                 // 1: red       # Zip/Tar/Archives, background missing link
            Color.GREEN,               // 2: green     # Executable/Shell
            new Color(255, 255, 0),    // 3: yellow    # Device, foreground missing link
            Color.BLUE,                // 4: blue      # Directory
            Color.MAGENTA,             // 5:           # Image/Gif
            Color.CYAN,                // 6: cyan      # Link
            new Color(128, 128, 128),  // 7: grey      # foreground missing link
    };

    /**
     * less extreme color: pastelish tint.
     */
    //@formatter:off
    public static final Color[] colorMapWhiteSoft = {
            // White/Bright Standard color map
            new Color(64, 64, 64),     // 0: org=black
            new Color(255, 64, 64),   // 1: red
            new Color(128, 255, 128), // 2: green
            new Color(255, 192, 192), // 3: Brownish?
            new Color(128, 128, 255), // 4 blue  (dir!)
            new Color(255, 128, 255), // 5:
            new Color(128, 255, 255), // 6: cyan
            new Color(196, 196, 196), // 7: grey
    };

    public static final Color[] colorMapDarkGreen16 = {
            // Green/Dark Semi Standard Color map
            new Color(128, 128, 128), // 0: grey
            new Color(255, 0, 0), // 1: red
            new Color(128, 255, 128), // 2: light. green (dir!)
            new Color(255, 128, 0), // 3: Brownish?
            new Color(32, 64, 255), // 4: blue
            new Color(255, 0, 255), // 5: purple
            new Color(0, 255, 255), // 6: cyan
            new Color(196, 196, 196), // 7: l. grey
            new Color(128, 128, 128).brighter(), // 8: grey
            new Color(255, 0, 0).brighter(), // 9: red
            new Color(128, 255, 128).brighter(), // 10: light. green (dir!)
            new Color(255, 128, 0).brighter(), // 11: Brownish?
            new Color(32, 64, 255).brighter(), // 12: blue
            new Color(255, 0, 255).brighter(), // 13: purple
            new Color(0, 255, 255).brighter(), // 14: cyan
            new Color(196, 196, 196).brighter(), // 15: l. grey
    };

    public static final Color[] colorMapShadedGreen = {
            // Green/Dark Color map
            new Color(0, 64, 0), // 0: grey
            new Color(0, 128, 0), // 1: dark green
            new Color(0, 196, 0), // 2: Color.ORANGE,
            new Color(0, 255, 0), // 3: light. green (dir!)
            new Color(32, 255, 32), // 4: blue
            new Color(96, 255, 96), // 5: purple
            new Color(160, 255, 160), // 6: cyan
            new Color(224, 255, 224), // 7: l. grey
            new Color(0, 128, 128), //
            new Color(0, 160, 160), //
            new Color(0, 192, 192), //
            new Color(0, 224, 224), //
            new Color(32, 255, 255), //
            new Color(96, 255, 255), //
            new Color(160, 255, 255), //
            new Color(224, 255, 255), //
            // 8=default foreground.
    };

    public static final Color[] colorMapPastelPink = {
            // Green/Dark Color map
            new Color(64, 32, 32), //0 Near Black Pink
            new Color(192, 96, 96), //1
            new Color(224, 108, 108), //2
            new Color(255, 64, 64), //3
            new Color(255, 128, 128), //4
            new Color(255, 160, 160), //5
            new Color(255, 196, 196), //6
            new Color(255, 224, 224), //7
    };

    public static final ColorMap COLOR_MAP_WHITE_ON_BLACK = new ColorMap(Color.WHITE, Color.BLACK, colorMapWhite);
    public static final ColorMap COLOR_MAP_WHITE_SOFT = new ColorMap(Color.WHITE, Color.BLACK, colorMapWhiteSoft);
    public static final ColorMap COLOR_MAP_GREEN_ON_BLACK = new ColorMap(Color.BLACK, Color.GREEN, colorMapDarkGreen16);
    public static final ColorMap COLOR_MAP_SHADED_GREEN = new ColorMap(Color.BLACK, Color.GREEN, colorMapShadedGreen);
    public static final ColorMap COLOR_MAP_PASTEL_PINK = new ColorMap(Color.PINK.brighter(), new Color(128, 64, 64), colorMapPastelPink);
    public static final String WHITE_CONTRAST = "White (High Constrast)";
    public static final String WHITE_SOFT = "White (Soft)";
    public static final String GREEN_CONTRAST = "Green";
    public static final String GREEN_SHADED = "Green Shaded";
    public static final String PASTEL_PINK = "Pastel Pink";
    public static final String[] colorMapNames = {WHITE_CONTRAST, WHITE_SOFT, GREEN_CONTRAST,GREEN_SHADED, PASTEL_PINK};
    public static Map<String, ColorMap> colorMaps = null;

    // === //
    private final Color backgroundColor;
    private final Color foregroundColor;

    public ColorMap(Color background, Color foreground, Color[] colors) {
        super();

        this.backgroundColor = background;
        this.foregroundColor = foreground;

        for (Color color : colors) {
            add(color);
        }
        autoAddXterm256();
    }

    public static final Map<String, ColorMap> getColorMaps() {
        if (colorMaps == null) {
            colorMaps = new Hashtable<>();
            colorMaps.put(WHITE_CONTRAST, COLOR_MAP_WHITE_ON_BLACK);
            colorMaps.put(WHITE_SOFT, COLOR_MAP_WHITE_SOFT);
            colorMaps.put(GREEN_CONTRAST, COLOR_MAP_GREEN_ON_BLACK);
            colorMaps.put(GREEN_SHADED, COLOR_MAP_SHADED_GREEN);
            colorMaps.put(PASTEL_PINK, COLOR_MAP_PASTEL_PINK);
        }
        return colorMaps;
    }

    /**
     * Blend color 0.0 = background, 1.0 = foreground 0.5 in between
     */
    public static Color blendColor(Color bg, Color fg, double fac, boolean keepFGAlpha) {
        int r0 = bg.getRed();
        int g0 = bg.getGreen();
        int b0 = bg.getBlue();
        int a0 = bg.getAlpha();

        int diffr = fg.getRed() - r0;
        int diffg = fg.getGreen() - g0;
        int diffb = fg.getBlue() - b0;
        int diffa = fg.getAlpha() - a0;

        // scale bg to fg:
        r0 = (int) (r0 + diffr * fac);
        g0 = (int) (g0 + diffg * fac);
        b0 = (int) (b0 + diffb * fac);

        if (keepFGAlpha)
            a0 = fg.getAlpha();
        else
            a0 = (int) (a0 + diffa * fac);

        return new Color(r0, g0, b0, a0);

    }

    public static String[] getColorMapNames() {
        return colorMapNames;
    }

    public static ColorMap getColorMap(String name) {
        Map<String, ColorMap> maps = getColorMaps();

        if (maps != null)
            return maps.get(name);

        return null;
    }

    public Color getForeground() {
        return this.foregroundColor;
    }

    public Color getBackground() {
        return this.backgroundColor;
    }

    public Color resolve(int index) {
        if ((index < 0) || (index >= this.size())) {
            return null;
        }
        return get(index);
    }

    /**
     * XTerm-256color needs 256 colormap.
     */
    private void autoAddXterm256() {

        for (int i = this.size(); i < 256; i++) {
            int r = 31 + (i % 8) * 32;
            int g = 31 + ((i / 8) % 8) * 32;
            int b = 31 + (i / 64) * 32;
            Color c = new Color(r, g, b);
            this.add(c);
        }
    }

}
