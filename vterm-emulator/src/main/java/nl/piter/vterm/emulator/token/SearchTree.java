package nl.piter.vterm.emulator.token;

import nl.piter.vterm.api.ByteArray;
import nl.piter.vterm.exceptions.VTxInvalidConfigurationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed size 256-width (8-bit) minimal search tree.
 * Uses non-recursive find, and after init no dynamic memory is used.
 */
public class SearchTree<T extends ByteArray> {

    protected static class TreeEl<T> {
        protected TreeEl index[];
        protected List<T> values;

        public void add(T value) {
            if (values == null) {
                values = new ArrayList<>();
            }
            values.add(value);
        }

        protected TreeEl() {
            index = new TreeEl[256];
            values = new ArrayList<>();
        }
    }

    private final TreeEl<T> root;

    public SearchTree() {
        root = new TreeEl();
    }

    public void add(T tokenDef) {
        add(tokenDef.bytes(), tokenDef);
    }

    private void add(byte[] index, T value) {
        TreeEl node = root;
        int level = 0;

        // Use byte value as index to next child level.
        while (level < index.length) {
            int id = (index[level] & 0x00ff);

            if (node.index[id] == null) {
                node.index[id] = new TreeEl();
            }

            // Add both full and partial matching tokens.
            node = node.index[id];
            // Important: add matches in order, so that first added will be first matched (PREFIX tokens,etc)
            node.add(value);

            if (level == index.length - 1) {
                // At leaf: assert only one match:
                if (node.values.size()>1) {
                    throw new VTxInvalidConfigurationException("Leaf node has multiple (partial) definitions. This one won't be matched:" + value);
                }
                return;
            }
            level++;
        }
    }

    public T findFull(byte[] search, int indexLen) {
        return find(search, indexLen, true);
    }

    public T findPartial(byte[] pattern, int patternIndex) {
        return find(pattern, patternIndex, false);
    }

    /**
     * Tree Search using fixed size 256-width byte index.
     */
    public T find(byte[] search, int indexLen, boolean fullMatch) {
        TreeEl<T> node = root;
        int level = 0;

        while ((node != null) && (level < indexLen)) {

            if (node.index == null) {
                return null; // eot: end-of-tree
            }

            // Use byte value as index to next child level.
            int id = search[level] & 0x00ff;
            node = node.index[id];

            if (node==null) {
                return null; // child eot.
            }

            if ((node.values != null) && (node.values.size() > 0)) {
                //
                T token = node.values.get(0);

                // Last search level: check full or partial match.
                if (level == indexLen - 1) {
                    if (fullMatch && (token.length() == search.length)) {
                        // Full match.
                        return token;
                    } else {
                        // Prefix or Partial match.
                        return token;
                    }
                }
            }
            level++;
        } // wend;
        return null;
    }

}
