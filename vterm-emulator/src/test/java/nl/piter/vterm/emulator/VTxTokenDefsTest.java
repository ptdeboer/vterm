/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.emulator;

import lombok.extern.slf4j.Slf4j;
import nl.piter.vterm.emulator.token.IToken;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static nl.piter.vterm.emulator.Tokens.Token.*;
import static nl.piter.vterm.emulator.VTxCharDefs.CTRL_ESC;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class VTxTokenDefsTest {

    @Test
    public void testParsing() {
        VTxTokenDefs tokenDefs = new VTxTokenDefs();
        List<IToken> patterns = tokenDefs.getPatterns();

        patterns.stream().forEach(pat -> {
                    log.info("- token:<{}>:[option={},terminator='{}',description='{}'",
                            pat.token(),
                            pat.option(),
                            pat.terminator(),
                            pat.description());
                }
        );
    }

    @Test
    public void findPartialPrefix() {
        VTxTokenDefs tokenDefs = new VTxTokenDefs();

        // <ESC>,'?';
        byte[] bytes1b = new byte[]{0x1b, '#'};
        IToken def = tokenDefs.findFirst(bytes1b, 2);
        log.error(" - def^[# = {}:{}", def.token(), def.description());
        Assert.assertNotNull(def);

        // <ESC>,'?';
        byte[] bytesCsi = new byte[]{0x1b, '['};
        IToken defCsi = tokenDefs.findFirst(bytes1b, 2);
        log.debug(" - def^[[ = {}:{}", defCsi.token(), defCsi.description());
        Assert.assertNotNull(defCsi);
    }

    @Test
    public void findPrefix() {
        VTxTokenDefs tokenDefs = new VTxTokenDefs();
        log.debug("Char '0x5b' = {}", String.format("%c", 0x5b));

        {
            // <ESC>,'[';
            byte[] bytes1 = new byte[]{0x1b, 0x5b};
            IToken def1 = tokenDefs.findFirst(bytes1, 2);
            log.debug("def1 ={}:{}", def1.token(), def1.description());
            Assert.assertNotNull(def1);
            assertThat(def1.token()).isEqualTo(CSI_PREFIX);

        }

        {
            // <ESC>,'[';
            byte[] bytes2 = new byte[]{0x1b, '['};
            IToken def2 = tokenDefs.findFirst(bytes2, 2);
            log.debug("def2 ={}:{}", def2.token(), def2.description());
            Assert.assertNotNull(def2);
            assertThat(def2.token()).isEqualTo(CSI_PREFIX);
        }

        {
            // <ESC>,']';
            byte[] bytes3 = new byte[]{0x1b, ']'};
            IToken def3 = tokenDefs.findFirst(bytes3, 2);
            log.debug("def3 ={}:{}", def3.token(), def3.description());
            Assert.assertNotNull(def3);
            assertThat(def3.token()).isEqualTo(OSC_GRAPHMODE_PREFIX);

        }

        {
            byte[] bytes4 = new byte[]{0x1b, '[', '?'};
            IToken def4 = tokenDefs.findFirst(bytes4, 3);
            log.debug("def4 ={}:{}", def4.token(), def4.description());
            Assert.assertNotNull(def4);
            assertThat(def4.token()).isEqualTo(DEC_SETMODE);
        }

    }


    // DCS is nasty as it ends with and ESC-\ sequence...
    @Test
    public void findDcsPrefix() {
        VTxTokenDefs tokenDefs = new VTxTokenDefs();

        {
            byte[] bytes1 = new byte[]{CTRL_ESC, 'P'};
            IToken def1 = tokenDefs.findFirst(bytes1, 2);
            Assert.assertNotNull(def1);
            log.debug("def1 ={}:{}", def1.token(), def1.description());
            assertThat(def1.token()).isEqualTo(DCS_DEVICE_CONTROL_STRING);
        }
        {
            byte[] bytes2 = new byte[]{CTRL_ESC, 'P', CTRL_ESC,};
            IToken def2 = tokenDefs.findFirst(bytes2, 3);
            Assert.assertNotNull(def2);
            log.debug("def2 ={}:{}", def2.token(), def2.description());
            assertThat(def2.token()).isEqualTo(DCS_DEVICE_CONTROL_STRING);
        }
        {
            byte[] bytes3 = new byte[]{CTRL_ESC, 'P', CTRL_ESC, '\\'};
            IToken def3 = tokenDefs.findFirst(bytes3, 4);
            Assert.assertNotNull(def3);
            log.debug("def2 ={}:{}", def3.token(), def3.description());
            assertThat(def3.token()).isEqualTo(DCS_DEVICE_CONTROL_STRING);
        }
    }

}
