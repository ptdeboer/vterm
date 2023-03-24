/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.emulator;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static nl.piter.vterm.emulator.Tokens.Token.CHARSET_G0_DUTCH;
import static nl.piter.vterm.emulator.VTxTokenDefs.CTRL_ESC;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class VTXTokenizerTest {

    @Test
    public void chars() throws Exception {

        String source = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLNMOPQRSTUVWXYZ0123456789";

        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        InputStream inps = new ByteArrayInputStream(bytes);
        VTxTokenizer tokenizer = new VTxTokenizer(inps);
        Tokens.Token token = null;

        int index = 0;

        do {
            token = tokenizer.nextToken();

            // Text representation parse bytes sequence
            byte[] tokenBytes = tokenizer.getBytes();

            int arg1 = 0;
            int arg2 = 0;

            int numIntegers = tokenizer.args().numArgs();

            if (numIntegers > 0)
                arg1 = tokenizer.args().intArg(0);

            if (numIntegers > 1)
                arg2 = tokenizer.args().intArg(1);

            if (token != Tokens.Token.EOF) {
                // ASCII
                Assert.assertEquals("Char #" + index + " mismatch", source.charAt(index), (char) tokenBytes[0]);
                index++;
            } else {
                Assert.assertEquals("Wrong number of Char Tokens", source.length(), index);
            }

        } while ((token != null) && (token != Tokens.Token.EOF));

    }

    @Test
    public void charsetG0Dutch() throws IOException {
        byte[] source = new byte[]{CTRL_ESC, '(', '4'};
        testSequence(source, CHARSET_G0_DUTCH, new int[0]);
    }

    @Test
    public void cursors() throws IOException {
        testSequence(new byte[]{CTRL_ESC, '[', 'A'}, Tokens.Token.UP, new int[0]);
        testSequence(new byte[]{CTRL_ESC, '[', '1', ';', 'A'}, Tokens.Token.UP, new int[]{1});
        testSequence(new byte[]{CTRL_ESC, '[', 'B'}, Tokens.Token.DOWN, new int[0]);
        testSequence(new byte[]{CTRL_ESC, '[', '1', ';', 'B'}, Tokens.Token.DOWN, new int[]{1});
        testSequence(new byte[]{CTRL_ESC, '[', 'C'}, Tokens.Token.RIGHT, new int[0]);
        testSequence(new byte[]{CTRL_ESC, '[', '1', ';', 'C'}, Tokens.Token.RIGHT, new int[]{1});
        testSequence(new byte[]{CTRL_ESC, '[', 'D'}, Tokens.Token.LEFT, new int[0]);
        testSequence(new byte[]{CTRL_ESC, '[', '1', ';', 'D'}, Tokens.Token.LEFT, new int[]{1});
    }


    @Test
    public void decLedSetStartWithEmptyValue() throws IOException {
        byte[] source = new byte[]{CTRL_ESC, '[', ';', '2', ';', 'q'};
        testSequence(source, Tokens.Token.DEC_LED_SET, new int[]{0, 2});
    }

    @Test
    public void decLedSetEmptyList() throws IOException {
        byte[] source = new byte[]{CTRL_ESC, '[', ';', 'q'};
        testSequence(source, Tokens.Token.DEC_LED_SET, new int[]{0});
    }


    @Test
    public void decSetMode() throws IOException {
        //
        testSequence(new byte[]{CTRL_ESC, '[', '?', '0', 'h'}, Tokens.Token.DEC_SETMODE, new int[]{0});
        testSequence(new byte[]{CTRL_ESC, '[', '?', '0', 'l'}, Tokens.Token.DEC_RESETMODE, new int[]{0});
        //
        testSequence(new byte[]{CTRL_ESC, '[', '?', '1', 'h'}, Tokens.Token.DEC_SETMODE, new int[]{1});
        testSequence(new byte[]{CTRL_ESC, '[', '?', '1', 'l'}, Tokens.Token.DEC_RESETMODE, new int[]{1});
        testSequence(new byte[]{CTRL_ESC, '[', '?', '1', '2', 'h'}, Tokens.Token.DEC_SETMODE, new int[]{12});
        testSequence(new byte[]{CTRL_ESC, '[', '?', '1', '3', 'l'}, Tokens.Token.DEC_RESETMODE, new int[]{13});
        testSequence(new byte[]{CTRL_ESC, '[', '?', '1', '4', '5', 'h'}, Tokens.Token.DEC_SETMODE, new int[]{145});
        testSequence(new byte[]{CTRL_ESC, '[', '?', '9', '9', '9', 'l'}, Tokens.Token.DEC_RESETMODE, new int[]{999});

    }

    @Test
    public void decSetMode_empty() throws IOException {
        testSequence(new byte[]{CTRL_ESC, '[', '?', 'h'}, Tokens.Token.DEC_SETMODE, new int[0]);
        testSequence(new byte[]{CTRL_ESC, '[', '?', 'l'}, Tokens.Token.DEC_RESETMODE, new int[0]);
    }

    @Test
    public void graphMode_emptyList() throws IOException {
        // \[];\007
        testSequence(new byte[]{CTRL_ESC, ']', ';', 007}, Tokens.Token.OSC_GRAPHMODE, 0, "");
    }

    @Test
    public void graphMode_setTitle() throws IOException {
        // \[]0;XXXX\007
        testSequence(new byte[]{CTRL_ESC, ']', '0', ';', 'X', 'X', 'X', 'X', 007}, Tokens.Token.OSC_GRAPHMODE, 0, "XXXX");

    }

    @Test
    public void doubleGraphMode() throws IOException {
        byte[] seq = new byte[]{CTRL_ESC, ']', '0', ';', 'X', 007, CTRL_ESC, ']', '0', ';', 'Y', 007};
        List<Tokens.Token> tokens = Arrays.asList(Tokens.Token.OSC_GRAPHMODE, Tokens.Token.OSC_GRAPHMODE);
        List<Integer> values = Arrays.asList(0, 0);
        List<String> strValues = Arrays.asList("X", "Y");
        testSequence(seq, tokens, values, strValues);
    }

    @Test
    public void decSetModeMulti() throws IOException {
        byte[] seq = new byte[]{CTRL_ESC, '[', '?', '1', 'h', CTRL_ESC, '[', '?', '2', 'l'};
        List<Tokens.Token> tokens = Arrays.asList(Tokens.Token.DEC_SETMODE, Tokens.Token.DEC_RESETMODE);
        List<Integer> values = Arrays.asList(1, 2);
        List<String> strValues = List.of();
        testSequence(seq, tokens, values, strValues);
    }

    private void testSequence(byte[] seq, List<Tokens.Token> tokens, List<Integer> intValues, List<String> strValues) throws IOException {

        InputStream inps = new ByteArrayInputStream(seq);
        VTxTokenizer tokenizer = new VTxTokenizer(inps);

        for (int i = 0; i < tokens.size(); i++) {
            Tokens.Token expectedToken = tokens.get(i);
            Tokens.Token token = tokenizer.nextToken();
            log.debug("Verifying token #{}:{} vs {}", i, token, expectedToken);
            assertThat(token).isEqualTo(expectedToken);
            if (intValues.size() > 0) {
                assertThat(tokenizer.args().intArg(0)).isEqualTo(intValues.get(i));
            }
            if (strValues.size() > 0) {
                assertThat(tokenizer.args().strArg()).isEqualTo(strValues.get(i));
            }
        }

    }

    protected void testSequence(byte[] bytes, Tokens.Token expected, int[] expectedInts) throws IOException {
        InputStream inps = new ByteArrayInputStream(bytes);
        VTxTokenizer tokenizer = new VTxTokenizer(inps);

        Tokens.Token token = tokenizer.nextToken();
        Assert.assertEquals(expected, token);
        Assert.assertEquals(expectedInts.length, tokenizer.args().numArgs());
        for (int i = 0; i < expectedInts.length; i++) {
            Assert.assertEquals("Integer at index:#" + i + " mismatches", expectedInts[i], tokenizer.args().intArg(i));
            log.debug(" integer at #" + i + " matches:" + tokenizer.args().intArg(i));
        }
    }

    protected void testSequence(byte[] bytes, Tokens.Token expected, int graphmodeInt, String graphmodeStr) throws IOException {
        InputStream inps = new ByteArrayInputStream(bytes);
        VTxTokenizer tokenizer = new VTxTokenizer(inps);

        Tokens.Token token = tokenizer.nextToken();
        Assert.assertEquals(expected, token);
        Assert.assertEquals(graphmodeInt, tokenizer.args().intArg(0));
        Assert.assertEquals(graphmodeStr, tokenizer.args().strArg());
    }

}
