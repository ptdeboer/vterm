package nl.piter.vterm.ui.panels.charpane;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class StyleCharTest {

    @Test
    public void styleFlags1_8() {

        int mode1234=StyleChar.STYLE_BOLD | StyleChar.STYLE_FAINT | StyleChar.STYLE_ITALIC |  StyleChar.STYLE_UNDERSCORE;
        assertThat(mode1234).isEqualTo(15);
        int mode5678=StyleChar.STYLE_SLOW_BLINK | StyleChar.STYLE_FAST_BLINK | StyleChar.STYLE_INVERSE | StyleChar.STYLE_HIDDEN;
        assertThat(mode5678).isEqualTo(240);
    }
}
