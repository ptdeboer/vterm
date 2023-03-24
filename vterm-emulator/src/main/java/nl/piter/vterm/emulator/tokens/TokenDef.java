/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.emulator.tokens;

import lombok.ToString;
import nl.piter.vterm.emulator.Tokens;

/**
 * Token definition, for single char and ESC Sequences.
 * UTF8 transcoding: the actual encoding, UT8 or not, is preserved in the char array.
 */
@ToString
public class TokenDef implements IToken {

    /**
     * Factory method.
     */
    public static IToken createFrom(char[] chars,
                                    Tokens.TokenOption option,
                                    Character terminatorCharm,
                                    Tokens.Token token,
                                    String tokenDescription) {
        // factory
        return new TokenDef(chars, option, terminatorCharm, token, tokenDescription);
    }

    // --- //

    protected final char[] chars;
    protected final Tokens.Token token;
    protected final Tokens.TokenOption option;
    protected final String tokenDescription;
    protected final Character terminatorChar;
    // cached:
    private final char[] fullSequence;

    public TokenDef(char[] chars, Tokens.TokenOption tokenOption, Character terminatorChar, Tokens.Token token, String tokenDescription) {
        this.chars = chars;
        this.token = token;
        this.option = tokenOption;
        this.tokenDescription = tokenDescription;
        this.terminatorChar = terminatorChar;
        //
        if (terminatorChar == null) {
            this.fullSequence = chars;
        } else {
            this.fullSequence = (new String(chars) + terminatorChar).toCharArray();
        }
    }

    public char[] chars() {
        return chars;
    }

    public char[] prefix() {
        return this.chars;
    }

    public char[] full() {
        return this.fullSequence;
    }

    public Tokens.Token token() {
        return this.token;
    }

    public String description() {
        return this.tokenDescription;
    }

    @Override
    public Character terminator() {
        return terminatorChar;
    }

    @Override
    public Tokens.TokenOption option() {
        return option;
    }

}
