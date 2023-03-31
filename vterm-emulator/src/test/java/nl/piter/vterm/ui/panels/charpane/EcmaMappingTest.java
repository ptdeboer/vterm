/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.ui.panels.charpane;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EcmaMappingTest {

    @Test
    public void mapModes() {
        // ECMA minimal:
        assertThat(EcmaMapping.apply(0, 1)).isEqualTo(StyleChar.STYLE_BOLD);
        assertThat(EcmaMapping.apply(0, 2)).isEqualTo(StyleChar.STYLE_FAINT);
        assertThat(EcmaMapping.apply(0, 3)).isEqualTo(StyleChar.STYLE_ITALIC);
        assertThat(EcmaMapping.apply(0, 4)).isEqualTo(StyleChar.STYLE_UNDERSCORE);
        assertThat(EcmaMapping.apply(0, 5)).isEqualTo(StyleChar.STYLE_SLOW_BLINK);
        assertThat(EcmaMapping.apply(0, 6)).isEqualTo(StyleChar.STYLE_FAST_BLINK | StyleChar.STYLE_UBERBOLD);
        assertThat(EcmaMapping.apply(0, 7)).isEqualTo(StyleChar.STYLE_INVERSE);
        assertThat(EcmaMapping.apply(0, 8)).isEqualTo(StyleChar.STYLE_HIDDEN);
        assertThat(EcmaMapping.apply(0, 9)).isEqualTo(StyleChar.STYLE_STRIKETHROUGH);
    }

    @Test
    public void mapUnmask() {
        // Used by BASH/LS_COLORS?:
        assertThat(EcmaMapping.apply(StyleChar.STYLE_BOLD | StyleChar.STYLE_FAINT, 22)).isEqualTo(0);
        assertThat(EcmaMapping.apply(StyleChar.STYLE_ITALIC | StyleChar.STYLE_FRAKTUR, 23)).isEqualTo(0);
        assertThat(EcmaMapping.apply(StyleChar.STYLE_UNDERSCORE | StyleChar.STYLE_DOUBLE_UNDERSCORE, 24)).isEqualTo(0);
        assertThat(EcmaMapping.apply(StyleChar.STYLE_SLOW_BLINK | StyleChar.STYLE_FAST_BLINK, 25)).isEqualTo(0);
        // no 26
        assertThat(EcmaMapping.apply(StyleChar.STYLE_INVERSE, 27)).isEqualTo(0);
        assertThat(EcmaMapping.apply(StyleChar.STYLE_HIDDEN, 28)).isEqualTo(0);
        assertThat(EcmaMapping.apply(StyleChar.STYLE_STRIKETHROUGH, 29)).isEqualTo(0);
    }

}
