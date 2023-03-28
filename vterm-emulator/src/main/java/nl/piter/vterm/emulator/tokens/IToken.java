/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.emulator.tokens;

import nl.piter.vterm.emulator.Tokens;

import java.nio.charset.StandardCharsets;

public interface IToken {

    /**
     * Single char or char sequence from Terminator or Prefix Sequence.
     */
    char[] chars();

    /**
     * Prefix part BEFORE options part.
     */
    char[] prefix();

    /**
     * Full sequence: Prefix + Terminator Char.
     */
    char[] full();

    Tokens.Token token();

    Tokens.TokenOption option();

    char[] terminator();

    String description();

    // -- defaults -- //
    default boolean hasOption() {
        return (option() != null);
    }

    default boolean hasPostFix() {
        return (terminator() != null);
    }

    default byte[] getBytes() {
        return new String(chars()).getBytes(StandardCharsets.UTF_8);
    }

}
