/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.emulator;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static nl.piter.vterm.emulator.VTxCharDefs.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Some 'vttest' inspired tests.
 */
@Slf4j
public class VTxTokenizerVtTest {

    @Test
    public void c0tokenInCursorBeforeParameters() throws IOException {
        byte[] seq = new byte[]{CTRL_ESC, '[', CTRL_LF, '1', ';', 'A'};
        List<Tokens.Token> tokens = Arrays.asList(Tokens.Token.LF, Tokens.Token.UP);
        // here options are not parsed before LF:
        List<Integer> values = Arrays.asList(-1, 1);
        testSequence(seq, tokens, values, new ArrayList<>());
    }

    @Test
    public void c0tokenInCursorAfterParameters() throws IOException {
        byte[] seq2 = new byte[]{CTRL_ESC, '[', '2', ';', CTRL_FF, 'B'};
        List<Tokens.Token> tokens2 = Arrays.asList(Tokens.Token.FF, Tokens.Token.DOWN);
        // here options are parsed before LF:
        List<Integer> values2 = Arrays.asList(2, 2);
        testSequence(seq2, tokens2, values2, new ArrayList<>());
    }

    @Test
    public void zeroInCursorSequenceBeforeParameters() throws IOException {
        byte[] seq = new byte[]{CTRL_ESC, '[', 0, '1', ';', 'C'};
        List<Tokens.Token> tokens = Arrays.asList(Tokens.Token.NUL, Tokens.Token.RIGHT);
        // here options are not parsed before NUL:
        List<Integer> values = Arrays.asList(-1, 1);
        testSequence(seq, tokens, values, new ArrayList<>());
    }

    @Test
    public void zeroInCursorSequenceAfterParameters() throws IOException {
        byte[] seq2 = new byte[]{CTRL_ESC, '[', '2', ';', 0, 'D'};
        List<Tokens.Token> tokens2 = Arrays.asList(Tokens.Token.NUL, Tokens.Token.LEFT);
        // here options ARE parsed but duplicated:
        List<Integer> values2 = Arrays.asList(2, 2);
        testSequence(seq2, tokens2, values2, new ArrayList<>());
    }

    // --- helpers --- //

    private void testSequence(byte[] seq, List<Tokens.Token> tokens, List<Integer> intValues, List<String> strValues) throws IOException {

        InputStream inps = new ByteArrayInputStream(seq);
        VTxTokenizer tokenizer = new VTxTokenizer(inps);

        for (int i = 0; i < tokens.size(); i++) {
            Tokens.Token expectedToken = tokens.get(i);
            Tokens.Token token = tokenizer.nextToken();
            log.debug("Verifying token #{}:{} vs {} with args: {}", i, token, expectedToken, tokenizer.getFormattedArguments());
            assertThat(token).isEqualTo(expectedToken);
            if (intValues.size() > 0) {
                assertThat(tokenizer.args().intArg(0)).as("Integer argument #%s", i).isEqualTo(intValues.get(i));
            }
            if (strValues.size() > 0) {
                assertThat(tokenizer.args().strArg()).as("String argument #%s").isEqualTo(strValues.get(i));
            }
        }
    }
}
