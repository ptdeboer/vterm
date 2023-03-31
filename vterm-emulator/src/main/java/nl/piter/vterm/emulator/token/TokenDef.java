/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.emulator.token;

import lombok.ToString;
import nl.piter.vterm.emulator.Tokens;
import nl.piter.vterm.emulator.Util;

/**
 * Token definition, for single char and ESC Sequences.
 * UTF8 transcoding: the actual encoding, UT8 or not, is preserved in the char array.
 */
@ToString
public class TokenDef implements IToken {

    // factory
    public static IToken createFrom(byte[] chars,
                                    Tokens.TokenOption option,
                                    byte[] terminatorChars,
                                    Tokens.Token token,
                                    String tokenDescription) {
        return new TokenDef(chars, option, terminatorChars, token, tokenDescription);
    }

    // --- //

    protected final byte[] prefix;
    protected final Tokens.Token token;
    protected final Tokens.TokenOption option;
    protected final String tokenDescription;
    protected final byte[] terminator;
    // cached:
    private final byte[] fullSequence;

    public TokenDef(byte[] prefix, Tokens.TokenOption tokenOption, byte[] terminator, Tokens.Token token, String tokenDescription) {
        this.prefix = prefix;
        this.token = token;
        this.option = tokenOption;
        this.tokenDescription = tokenDescription;
        this.terminator = terminator;
        //
        if (terminator == null) {
            this.fullSequence = prefix;
        } else {
            this.fullSequence = Util.concat(prefix, terminator);
        }
    }

    public byte[] prefix() {
        return this.prefix;
    }

    public byte[] full() {
        return this.fullSequence;
    }

    public Tokens.Token token() {
        return this.token;
    }

    public String description() {
        return this.tokenDescription;
    }

    @Override
    public byte[] terminator() {
        return terminator;
    }

    @Override
    public Tokens.TokenOption option() {
        return option;
    }

}
