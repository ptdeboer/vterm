/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.ui.fonts;

import org.junit.Test;

import java.util.Properties;
import static org.assertj.core.api.Assertions.assertThat;

public class FontInfoTest {

    @Test
    public void loadProperties() {
        Properties props=new Properties();
        props.put(FontConst.FONT_FAMILY,"family");
        props.put(FontConst.FONT_SIZE,"42");
        props.put(FontConst.FONT_STYLE,"13");
        props.put(FontConst.FONT_HAS_ANTIALIASING,true);

        FontInfo info = new FontInfo(props);
        assertThat(info.getFontFamily()).isEqualTo("family");
        assertThat(info.getFontSize()).isEqualTo(42);
        assertThat(info.getFontStyle()).isEqualTo(13);
        assertThat(info.hasAntiAliasing()).isEqualTo(true);
    }

    @Test
    public void saveProperties() {

        FontInfo info = new FontInfo();
        info.setFontFamily("family");
        info.setFontSize(42);
        info.setFontStyle(13);
        info.setAntiAliasing(true);

        Properties props=info.getFontProperties();
        assertThat(props.get(FontConst.FONT_FAMILY)).isEqualTo("family");
        assertThat(props.get(FontConst.FONT_SIZE)).isEqualTo(42);
        assertThat(props.get(FontConst.FONT_STYLE)).isEqualTo(13);
        assertThat(props.get(FontConst.FONT_HAS_ANTIALIASING)).isEqualTo(true);
    }

}
