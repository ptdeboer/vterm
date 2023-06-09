/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.ui.panels.charpane;

import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ColorMapTest {

    @Test
    public void initColorMaps() {
        // Just test parsing. All maps now should have 256 colors.
        Map<String, ColorMap> maps = ColorMap.getColorMaps();
        for (ColorMap map : maps.values()) {
            assertThat(map.size()).isEqualTo(256);
        }
    }
}

