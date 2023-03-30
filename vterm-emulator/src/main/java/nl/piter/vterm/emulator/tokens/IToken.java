/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.emulator.tokens;

import nl.piter.vterm.api.ByteArray;
import nl.piter.vterm.emulator.Tokens;

public interface IToken extends ByteArray {

    /**
     * Prefix part BEFORE options part.
     */
    byte[] prefix();

    /**
     * Full sequence: Prefix + Terminator Char.
     */
    byte[] full();

    Tokens.Token token();

    Tokens.TokenOption option();

    byte[] terminator();

    String description();

    // -- defaults -- //
    default boolean hasOption() {
        return (option() != null);
    }

    default boolean hasTerminator() {
        return (terminator() != null);
    }

    // -- ByteArray
    default int length() {
        return full().length;
    }

    default byte[] bytes() {
        return full();
    }
}
