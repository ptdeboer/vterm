/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.emulator;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class Util {

    /**
     * Mini byte buffer can be used as byte stack.
     */
    public static class MiniBuffer {
        // Use direct memory buffers?: ByteBuffer bytes = ByteBuffer.allocateDirect(200);

        private final byte[] bytes;
        private int index = 0;

        public MiniBuffer(int size) {
            bytes = new byte[size];
        }

        public void put(byte b) {
            bytes[index] = b;
            index++;
        }

        public byte pop() throws IOException {
            if (index <= 0) {
                throw new IOException("Byte Byffer is empty: can not pop");
            }

            index--;
            return bytes[index];
        }

        public String toString(String encoding) throws UnsupportedEncodingException {
            return new String(bytes, 0, index, encoding);
        }

        // set index to 0;
        public void reset() {
            index = 0;
        }

        /**
         * Returns duplicate of byte buffer
         */
        public byte[] getBytes() {
            byte[] b2 = new byte[index];
            System.arraycopy(bytes, 0, b2, 0, index);
            return b2;
        }

        public int size() {
            return index;
        }

        public int freeSpace() {
            return bytes.length - index;
        }

        /**
         * Auto casts integer to byte value. Uses lower 0x00ff value
         */
        public void put(int c) {
            put((byte) (c & 0x00ff));
        }

        public byte[] bytes() {
            return bytes;
        }

        public int index() {
            return index;
        }

        public void setBytes(byte[] newBytes, int num) {
            for (int i=0;i<num;i++) {
                this.bytes[i] = newBytes[i];
            }
            this.index=num;
        }
    }

    public static String byte2hexstr(int val) {
        char cs[]=new char[2];
        cs[0]=(char)(0x30+val%16);
        cs[1]=(char)(0x30+(val/16)%16);
        return new String(cs);
    }

    public static String prettyByteString(byte[] bytes) {
        return prettyByteString(bytes, bytes.length);
    }

    public static String prettyByteString(byte[] bytes, int nrb) {
        StringBuilder sb=new StringBuilder();

        sb.append("{");
        for (int i = 0; i < nrb; i++) {
            sb.append(Util.byte2hexstr(bytes[i] & 0x00ff));
            if (i + 1 < nrb)
                sb.append(",");
        }
        sb.append("} => {");
        for (int i = 0; i < nrb; i++) {
            char c = (char) bytes[i];
            sb.append(getSymbolicCharString(c));
            if (i + 1 < nrb)
                sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    public static String getSymbolicCharString(char c) {
        if (c == 0) {
            return Tokens.Token.NUL.toString();
        }
        // Use Token ENUM to get symbolic CHAR:
        Object[] tokenDef = VTxTokenDefs.findCharToken(c);
        if (tokenDef != null) {
            Object token = tokenDef[tokenDef.length - 1];
            return "<" + token + ">";
        }

        if ((c >= ' ') && (c <= 'z')) {
            return "'" + c + "'";
        }

        return Util.byte2hexstr(c);
    }
}
