/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.emulator;

import lombok.extern.slf4j.Slf4j;
import nl.piter.vterm.emulator.tokens.IToken;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

@Slf4j
public class TokenDefsParsing {

    @Test
    public void testParsing() {
        VTxTokenDefs tokenDefs = new VTxTokenDefs();

        List<IToken> patterns = tokenDefs.getPatterns();

        patterns.stream().forEach(pat -> {
                    log.info(" -token:<{}>:[option={},terminator='{}',description='{}'",
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
        log.error(" - def^[[ = {}:{}", defCsi.token(), defCsi.description());

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
            Assert.assertNotNull(def1);
            log.error("def1 ={}:{}", def1.token(), def1.description());
        }

        {
            // <ESC>,'[';
            byte[] bytes2 = new byte[]{0x1b, '['};
            IToken def2 = tokenDefs.findFirst(bytes2, 2);
            Assert.assertNotNull(def2);
            log.error("def2 ={}:{}", def2.token(), def2.description());
        }

        {
            // <ESC>,']';
            byte[] bytes3 = new byte[]{0x1b, ']'};
            IToken def3 = tokenDefs.findFirst(bytes3, 2);
            Assert.assertNotNull(def3);
            log.error("def3 ={}:{}", def3.token(), def3.description());
        }

        {
            byte[] bytes4 = new byte[]{0x1b, '[', '?'};
            IToken def4 = tokenDefs.findFirst(bytes4, 3);
            Assert.assertNotNull(def4);
            log.error("def4 ={}:{}", def4.token(), def4.description());
        }

    }

}
