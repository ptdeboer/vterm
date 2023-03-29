/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.emulator;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static nl.piter.vterm.emulator.Tokens.Token.*;
import static nl.piter.vterm.emulator.VTxCharDefs.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Selection of sequences.
 */
@Slf4j
public class VTxTokenizerTest {

    @Test
    public void chars() throws Exception {
        String source = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLNMOPQRSTUVWXYZ0123456789";
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        InputStream inps = new ByteArrayInputStream(bytes);
        VTxTokenizer tokenizer = new VTxTokenizer(inps);
        Tokens.Token token;
        int index = 0;

        while ((token = tokenizer.nextToken()) != EOF) {
            // Text representation parse bytes sequence
            byte[] tokenBytes = tokenizer.getBytes();
            assertThat(token).as("character #%s", index).isEqualTo(CHAR);
            assertThat((char) tokenBytes[0]).as("character #%s", index).isEqualTo(source.charAt(index++));
        }
    }

    @Test
    public void charsetG0Dutch() throws IOException {
        byte[] source = new byte[]{CTRL_ESC, '(', '4'};
        testSequence(source, CHARSET_G0_DES, "4");
    }

    @Test
    public void charsetG0Graphics() throws IOException {
        byte[] source = new byte[]{CTRL_ESC, '(', '0'};
        testSequence(source, CHARSET_G0_DES, "0");
    }

    @Test
    public void charsetG1Dutch() throws IOException {
        byte[] source = new byte[]{CTRL_ESC, ')', '4'};
        testSequence(source, CHARSET_G1_DES, "4");
    }

    @Test
    public void charsetG1Graphics() throws IOException {
        byte[] source = new byte[]{CTRL_ESC, ')', '0'};
        testSequence(source, CHARSET_G1_DES, new int[0]);
    }

    @Test
    public void charsetG0() throws IOException {
        byte[] source = new byte[]{CTRL_SI, 'x'};
        testSequence(source, CHARSET_G0, new int[0]);
    }

    @Test
    public void charsetG1() throws IOException {
        byte[] source = new byte[]{CTRL_SO, 'x'};
        testSequence(source, CHARSET_G1, (String) null);
    }

    @Test
    public void cursorsSingle() throws IOException {
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
    public void cursorsMulti() throws IOException {
        byte[] seq = new byte[]{CTRL_ESC, '[', '1', ';', 'A',
                CTRL_ESC, '[', '2', ';', 'B',
                CTRL_ESC, '[', '3', ';', 'C',
                CTRL_ESC, '[', '4', ';', 'D'};
        List<Tokens.Token> tokens = Arrays.asList(UP, DOWN, RIGHT, LEFT);
        List<Integer> values = Arrays.asList(1, 2, 3, 4);
        List<String> strValues = new ArrayList<>();
        testSequence(seq, tokens, values, strValues);
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
        testSequence(new byte[]{CTRL_ESC, ']', ';', 007}, Tokens.Token.OSC_GRAPHMODE, 0, null);
    }

    @Test
    public void graphMode_setTitle() throws IOException {
        // \[]0;XXXX\007
        testSequence(new byte[]{CTRL_ESC, ']', '0', ';', 'X', 'X', 'X', 'X', 007}, Tokens.Token.OSC_GRAPHMODE, 0, "XXXX");
    }

    private final char[] c0chars = new char[]{CTRL_NUL, CTRL_BEL, CTRL_BS, CTRL_HT, CTRL_LF, CTRL_VT, CTRL_FF, CTRL_CR};
    private final Tokens.Token[] c0tokens = new Tokens.Token[]{NUL, BEL, BS, HT, LF, VT, FF, CR};

    @Test
    public void c0tokensMinimal() throws IOException {
        byte[] seq = new byte[c0chars.length];
        for (int i = 0; i < seq.length; seq[i] = (byte) c0chars[i], i++) ;
        //
        List<Integer> values = new ArrayList<>();
        List<String> strValues = new ArrayList<>();
        testSequence(seq, Arrays.asList(c0tokens), values, strValues);
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

    @Test
    public void dcsEmptyString() throws IOException {
        // Nice: ESC ending with ESC:
        byte[] seq = new byte[]{CTRL_ESC, 'P', CTRL_ESC, '\\', 0};
        List<Tokens.Token> tokens = List.of(DCS_DEVICE_CONTROL_STRING);
        List<Integer> values = Lists.emptyList();
        List<String> strValues = Lists.emptyList();
        testSequence(seq, tokens, values, strValues);
    }

    @Test
    public void dcsSingleString() throws IOException {
        // Nice: ESC ending with ESC:
        byte[] seq = new byte[]{CTRL_ESC, 'P', 'a', 'a', 'p', CTRL_ESC, '\\'};
        List<Tokens.Token> tokens = List.of(DCS_DEVICE_CONTROL_STRING);
        List<Integer> values = Lists.emptyList();
        List<String> strValues = List.of("aap");
        testSequence(seq, tokens, values, strValues);
    }

    // --- Helper methods --- //

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

    protected void testSequence(byte[] bytes, Tokens.Token expected, String expectedArg) throws IOException {
        InputStream inps = new ByteArrayInputStream(bytes);
        VTxTokenizer tokenizer = new VTxTokenizer(inps);

        Tokens.Token token = tokenizer.nextToken();
        assertThat(token).isEqualTo(expected);
        assertThat(tokenizer.args().strArg()).isEqualTo(expectedArg);
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
