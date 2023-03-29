/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.emulator;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class UtilStringTest {

    @Test
    public void byte2hexstr() {
        assertThat(Util.byte2hexstr(0)).isEqualTo("00");
        assertThat(Util.byte2hexstr(15)).isEqualTo("0f");
        assertThat(Util.byte2hexstr(127)).isEqualTo("7f");
        assertThat(Util.byte2hexstr(128)).isEqualTo("80");
        assertThat(Util.byte2hexstr(255)).isEqualTo("ff");
        // autowrap byte values:
        assertThat(Util.byte2hexstr(-1)).isEqualTo("ff");
    }

    @Test
    public void hexstr2bytes() {
        assertThat(Util.hex2bytes("")).isEqualTo(new byte[0]);
        assertThat(Util.hex2bytes("00")).isEqualTo(new byte[]{(byte) 0});
        assertThat(Util.hex2bytes("1234")).isEqualTo(new byte[]{(byte) 0x12, (byte) 0x34});
        assertThat(Util.hex2bytes("DEADBEEF")).isEqualTo(new byte[]{(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF});
    }

    @Test
    public void splitQuotedArgs() {
        assertThat(Util.splitQuotedArgs("simple")).isEqualTo(List.of("simple"));
        assertThat(Util.splitQuotedArgs("a b")).isEqualTo(Arrays.asList("a", "b"));
        assertThat(Util.splitQuotedArgs("\"quoted\"")).isEqualTo(List.of("\"quoted\""));
        assertThat(Util.splitQuotedArgs("a \"quoted\" arg")).isEqualTo(Arrays.asList("a", "\"quoted\"", "arg"));
        assertThat(Util.splitQuotedArgs("a \"quoted with space\" arg")).isEqualTo(Arrays.asList("a", "\"quoted with space\"", "arg"));
        assertThat(Util.splitQuotedArgs("with \" spaces \" surrounded")).isEqualTo(Arrays.asList("with", "\" spaces \"", "surrounded"));
        assertThat(Util.splitQuotedArgs("-i -script 1 -D='a' aap=${noot}"))
                .isEqualTo(Arrays.asList("-i", "-script", "1", "-D='a'", "aap=${noot}"));
    }

    @Test
    public void splitQuotedVariableArgs() {

        assertThat(Util.splitQuotedArgs("-Dsimple ${simple}")).isEqualTo(Arrays.asList("-Dsimple", "${simple}"));
        assertThat(Util.splitQuotedArgs("-Dsimple \"${simple}\"")).isEqualTo(Arrays.asList("-Dsimple", "\"${simple}\""));

        // -Dopt "value",  -Dopt="value"
        assertThat(Util.splitQuotedArgs("-script \"/path/to/script\"")).isEqualTo(Arrays.asList("-script", "\"/path/to/script\""));
        assertThat(Util.splitQuotedArgs("-script=\"/path/to/script\"")).isEqualTo(List.of("-script=\"/path/to/script\""));

        // -Dopt "a spaced value", -Dopt="a spaced value"
        assertThat(Util.splitQuotedArgs("-i -script \"/path/subdir with space/script\""))
                .isEqualTo(Arrays.asList("-i", "-script", "\"/path/subdir with space/script\""));
        assertThat(Util.splitQuotedArgs("-i -script=\"/path/subdir with space/script\""))
                .isEqualTo(Arrays.asList("-i", "-script=\"/path/subdir with space/script\""));

        // -Dopt="quoted"+postfix, -Dopt=prefix:"quoted"
        assertThat(Util.splitQuotedArgs("-i -opt=\"a quote\"+postfix"))
                .isEqualTo(Arrays.asList("-i", "-opt=\"a quote\"+postfix"));
        assertThat(Util.splitQuotedArgs("-i -opt=prefix:\"a quote\""))
                .isEqualTo(Arrays.asList("-i", "-opt=prefix:\"a quote\""));

        // -Dopt=var-"quoted"-and-"more"
        assertThat(Util.splitQuotedArgs("-i -Dopt=var-\"2B quoted\"-and-\"not 2B quoted\""))
                .isEqualTo(Arrays.asList("-i", "-Dopt=var-\"2B quoted\"-and-\"not 2B quoted\""));

        // arg1 a"b"_c_"d"e
        assertThat(Util.splitQuotedArgs("arg1 a\"b\"_c_\"d\"end --"))
                .isEqualTo(Arrays.asList("arg1", "a\"b\"_c_\"d\"end", "--"));

        // arg1 "a"_b_"c"_d_"e"
        assertThat(Util.splitQuotedArgs("arg1 \"a\"_b_\"c\"_d_\"e\" --"))
                .isEqualTo(Arrays.asList("arg1", "\"a\"_b_\"c\"_d_\"e\"", "--"));
    }

}
