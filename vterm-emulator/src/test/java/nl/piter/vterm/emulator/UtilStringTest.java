package nl.piter.vterm.emulator;

import org.junit.Test;

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
        assertThat(Util.hex2bytes("DEADBEEF")).isEqualTo(new byte[]{(byte)0xDE,(byte)0xAD,(byte)0xBE,(byte)0xEF});
    }

}
