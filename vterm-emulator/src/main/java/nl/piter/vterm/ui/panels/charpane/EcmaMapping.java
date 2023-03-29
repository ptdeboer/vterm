/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.ui.panels.charpane;

import java.util.HashMap;
import java.util.Map;

public class EcmaMapping {

    private static EcmaMapping mapping;

    static {
        init();
    }

    private static void init() {
        mapping = new EcmaMapping();
    }

    public static int apply(int style, int mode) {
        return mapping.applyStyle(style, mode);
    }

    public static boolean hasMode(int mode) {
        return mapping.onMask.containsKey(mode) || mapping.offMask.containsKey(mode);
    }

    // ---

    private final Map<Integer, Integer> onMask = new HashMap<>();
    private final Map<Integer, Integer> offMask = new HashMap<>();

    public EcmaMapping() {
        registerAll();
    }

    public void register(int nr, int onMask, int offMask) {
        this.onMask.put(nr, onMask);
        this.offMask.put(nr, offMask);
    }

    int applyStyle(int style, int mode) {
        if (offMask.containsKey(mode)) {
            style &= ~offMask.get(mode);
        }

        if (onMask.containsKey(mode)) {
            style |= onMask.get(mode);
        }
        return style;
    }

    private void registerAll() {
        register(0, 0, 0x7fffffff);
        register(1, StyleChar.STYLE_BOLD, 0);
        register(2, StyleChar.STYLE_FAINT, 0);
        register(3, StyleChar.STYLE_ITALIC, 0);
        register(4, StyleChar.STYLE_UNDERSCORE, 0);
        register(5, StyleChar.STYLE_SLOW_BLINK, 0);
        register(6, StyleChar.STYLE_FAST_BLINK | StyleChar.STYLE_UBERBOLD, 0);
        register(7, StyleChar.STYLE_INVERSE, 0);
        register(8, StyleChar.STYLE_HIDDEN, 0);
        register(9, StyleChar.STYLE_STRIKETHROUGH, 0);
        // extended:
        register(20, StyleChar.STYLE_FRAKTUR, 0);
        register(21, StyleChar.STYLE_DOUBLE_UNDERSCORE, 0);
        // unmask:
        register(22, 0, StyleChar.STYLE_BOLD | StyleChar.STYLE_FAINT);
        register(23, 0, StyleChar.STYLE_ITALIC | StyleChar.STYLE_FRAKTUR);
        register(24, 0, StyleChar.STYLE_UNDERSCORE | StyleChar.STYLE_DOUBLE_UNDERSCORE);
        register(25, 0, StyleChar.STYLE_SLOW_BLINK | StyleChar.STYLE_FAST_BLINK);
        // 26 - reserved
        register(27, 0, StyleChar.STYLE_INVERSE);
        register(28, 0, StyleChar.STYLE_HIDDEN);
        register(29, 0, StyleChar.STYLE_STRIKETHROUGH);
    }

}
