package nl.piter.vterm.emulator.tokens;

import lombok.extern.slf4j.Slf4j;
import nl.piter.vterm.emulator.Tokens;
import nl.piter.vterm.exceptions.VTxInvalidConfigurationException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class SearchTreeTest {

    @Test
    public void rootLevel() {
        SearchTree<IToken> tree = new SearchTree();

        byte[] index = new byte[]{(byte) 1};
        TokenDef tokenMock = new TokenDef(index, null, null, Tokens.Token.NUL, "test1a");

        tree.add(tokenMock);
        IToken match = tree.findFull(index, index.length);
        assertThat(match).isEqualTo(tokenMock);

        byte[] index13 = new byte[]{(byte) 13};
        TokenDef tokenMock13 = new TokenDef(index13, null, null, Tokens.Token.NUL, "test1b");
        tree.add(tokenMock13);
        IToken match13 = tree.findFull(index13, index.length);
        assertThat(match13).isEqualTo(tokenMock13);
    }

    @Test
    public void rootLevelNoMatch() {
        SearchTree<IToken> tree = new SearchTree();
        byte[] index1 = new byte[]{(byte) 1};
        TokenDef tokenMock = new TokenDef(index1, null, null, Tokens.Token.NUL, "test2a");
        tree.add(tokenMock);

        byte[] noMatch2 = new byte[]{(byte) 2};
        IToken match = tree.findFull(noMatch2, noMatch2.length);
        assertThat(match).isNull();

        byte[] index13 = new byte[]{(byte) 13};
        TokenDef tokenMock13 = new TokenDef(index13, null, null, Tokens.Token.NUL, "test2b");
        tree.add(tokenMock13);

        byte[] index14 = new byte[]{(byte) 14};
        IToken match14 = tree.findFull(index14, index14.length);
        assertThat(match14).isNull();
    }

    @Test
    public void level2() {
        SearchTree<IToken> tree = new SearchTree();
        byte[] index = new byte[]{(byte) 1, (byte) 2};
        TokenDef tokenMock = new TokenDef(index, null, null, Tokens.Token.NUL, "test3a");

        tree.add(tokenMock);
        IToken match = tree.findFull(index, index.length);
        assertThat(match).isEqualTo(tokenMock);

        byte[] index13 = new byte[]{(byte) 13, (byte) 42};
        TokenDef tokenMock13 = new TokenDef(index13, null, null, Tokens.Token.NUL, "test3b");
        tree.add(tokenMock13);
        IToken match13 = tree.findFull(index13, index.length);
        assertThat(match13).isEqualTo(tokenMock13);
    }

    @Test
    public void level2noMatch() {
        SearchTree<IToken> tree = new SearchTree();
        byte[] index = new byte[]{(byte) 1, (byte) 2};
        TokenDef tokenMock = new TokenDef(index, null, null, Tokens.Token.NUL, "test4a");

        tree.add(tokenMock);
        byte[] indexNoMatch = new byte[]{(byte) 1, (byte) 3};
        IToken match = tree.findFull(indexNoMatch, index.length);
        assertThat(match).isNull();

        byte[] index13 = new byte[]{(byte) 13, (byte) 42};
        TokenDef tokenMock13 = new TokenDef(index13, null, null, Tokens.Token.NUL, "test4b");
        tree.add(tokenMock13);
        byte[] index13noMatch = new byte[]{(byte) 13, (byte) 43};
        IToken match13 = tree.findFull(index13noMatch, index.length);
        assertThat(match13).isNull();
    }

    @Test
    public void level3partial() {
        SearchTree<IToken> tree = new SearchTree();
        byte[] index123 = new byte[]{(byte) 1, (byte) 2, (byte) 3};
        TokenDef tokenMock123 = new TokenDef(index123, null, null, Tokens.Token.NUL, "test5");

        tree.add(tokenMock123);
        byte[] partial = new byte[]{(byte) 1, (byte) 2};
        IToken match123 = tree.findPartial(partial, partial.length);
        assertThat(match123).isEqualTo(tokenMock123);


        byte[] index13 = new byte[]{(byte) 13, (byte) 14, (byte) 42};
        TokenDef tokenMock13 = new TokenDef(index13, null, null, Tokens.Token.NUL, "test5b");
        tree.add(tokenMock13);
        byte[] partial13 = new byte[]{(byte) 13, (byte) 14};
        IToken match13 = tree.findPartial(partial13, partial13.length);
        assertThat(match13).isEqualTo(tokenMock13);
    }

    @Test
    public void doubleDefinitionException() {

        SearchTree<IToken> tree = new SearchTree();

        byte[] index123 = new byte[]{(byte) 1, (byte) 2, (byte) 3};
        TokenDef tokenMock = new TokenDef(index123, null, null, Tokens.Token.NUL, "test-error");
        tree.add(tokenMock);
        // add same:
        try {
            tree.add(tokenMock);
        } catch (VTxInvalidConfigurationException e) {
            log.debug("Got expected Exception: {}:{}", e.getClass().getCanonicalName(), e.getMessage());
        }
        // add similar:
        try {
            TokenDef otherMock = new TokenDef(index123, null, null, Tokens.Token.NUL, "test-other");
            tree.add(otherMock);
        } catch (VTxInvalidConfigurationException e) {
            log.debug("Got expected Exception: {}:{}", e.getClass().getCanonicalName(), e.getMessage());
        }
    }

}
