/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.emulator;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collection of String and Byte util methods.
 */
public class Util {

    public static char[] HEXVALUES = new char[]{
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    /**
     * Mini byte buffer can be used as byte stack.
     */
    public static class MiniBuffer {
        // Use direct memory buffers?: ByteBuffer bytes = ByteBuffer.allocateDirect(200);

        private final int[] values;
        private int index = 0;

        public MiniBuffer(int size) {
            values = new int[size];
        }

        public void put(int b) {
            values[index] = b;
            index++;
        }

        public int pop() throws EOFException {
            if (index <= 0) {
                throw new EOFException("Byte Byffer is empty: can not pop");
            }
            index--;
            return values[index];
        }

        // set index to 0;
        public void reset() {
            index = 0;
        }

        /**
         * Returns duplicate of byte buffer
         */
        public byte[] getBytes() {
            byte[] bts = new byte[index];
            for (int i = 0; i < index; i++)
                bts[i] = (byte) values[i];
            return bts;
        }

        public int size() {
            return index;
        }

        public int freeSpace() {
            return values.length - index;
        }

        public int[] values() {
            return values;
        }

        public int index() {
            return index;
        }

        public void setBytes(byte[] newBytes, int num) {
            for (int i = 0; i < newBytes.length; i++) {
                this.values[i] = newBytes[i];
            }
            this.index = num;
        }
    }

    public static boolean isEmpty(String str) {
        return ((str == null) || (str.equals("")));
    }

    public static String object2string(Object objectOrNull) {
        return (objectOrNull != null) ? objectOrNull.toString() : null;
    }

    public static String object2stringOrEmpty(Object objectOrNull) {
        return (objectOrNull != null) ? objectOrNull.toString() : "";
    }

    /**
     * Parse hexadecimal byte string WITHOUT '0x' prefix.
     *
     * @param hexString for example "DEADBEAF" as String.
     * @return byte array, for example: byte[]{0xDE,0xAD,0xBE,0xAF}
     */
    public static byte[] hexstr2bytes(String hexString) {
        if (hexString == null)
            return null;

        int n = hexString.length() / 2;
        byte[] bytes = new byte[n];

        for (int i = 0; i < n; i++) {
            int b = Integer.parseInt(hexString.substring(i * 2, i * 2 + 2), 16);
            bytes[i] = (byte) b;
        }

        return bytes;
    }

    public static String bytes2hexstr(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(byte2hexstr(b));
        }
        return sb.toString();
    }

    // Autopromotion: captures byte argument as well:
    public static String byte2hexstr(int val) {
        val = (val & 0x00ff); // byte wraparound:
        char[] cs = new char[2];
        cs[0] = HEXVALUES[((val / 16) % 16)];
        cs[1] = HEXVALUES[(val % 16)];
        return new String(cs);
    }

    public static String prettyByteString(byte[] bytes) {
        return prettyByteString(bytes, bytes.length);
    }

    public static String prettyByteString(byte[] bytes, int nrb) {
        StringBuilder sb = new StringBuilder();

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


    public static String null2empty(String str) {
        return (str != null) ? str : "";
    }

    public static int min(int v1, int v2) {
        return (v1 < v2) ? v1 : v2;
    }

    public static int max(int v1, int v2) {
        return (v1 > v2) ? v1 : v2;
    }

    /**
     * Match various combinations of quoted and unquoted patterns.
     * Pattern keeps quotes for example if they are needed as shell arguments.
     */
    public static final Pattern spacesWithQuotesRE = Pattern.compile(
            "(([^\"\\s]+)|(\"[^\"]*\"))+"
    );

    /**
     * Split arguments but keeps quoted arguments in quotes.
     * Works for not to complicated argument constructions. For example:
     * <pre>
     *   <em>Split: 'arg1 arg1' => 'arg1', 'arg2'</em>
     *   <em>Split: 'arg1 "a quote" arg3' => 'arg1', '"a quote"', 'arg3'</em>
     * For example:
     *  <em>Split: '-i -script="long path to file' => '-i','-script="long path to file"'.
     * </pre>
     * For 'bash' it is important that quoted argument are kept in quotes.
     */
    public static List<String> splitQuotedArgs(String source) {
        Matcher matcher = spacesWithQuotesRE.matcher(source);
        List<String> args = new ArrayList();

        while (matcher.find()) {
            args.add(source.substring(matcher.start(), matcher.end()));
        }

        return args;
    }

    public static byte[] concat(byte[] arr1, byte[] arr2) {
        byte[] result=new byte[arr1.length+arr2.length];
        System.arraycopy(arr1,0,result,0,arr1.length);
        System.arraycopy(arr2,0,result,arr1.length,arr2.length);
        return result;
    }

}
