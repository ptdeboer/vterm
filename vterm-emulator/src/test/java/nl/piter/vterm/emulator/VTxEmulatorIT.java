/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.emulator;

import lombok.extern.slf4j.Slf4j;
import nl.piter.vterm.api.CharacterTerminal;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static nl.piter.vterm.emulator.VTxCharDefs.CTRL_ESC;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class VTxEmulatorIT {

    private static CharacterTerminal createCharacterTermMock() {
        return new CharacterTerminalMock();
    }

    @Test
    public void testSend() throws IOException {

        ByteArrayOutputStream outps = new ByteArrayOutputStream(1024);
        VTxEmulator emulator = new VTxEmulator(createCharacterTermMock(), null, outps);

        byte[] bytes = {CTRL_ESC, '[', '?', '1', ';', '2', 'c'};
        emulator.send(bytes);

        byte[] sendBytes = outps.toByteArray();
        assertThat(sendBytes).isEqualTo(bytes);
    }

    @Test
    public void setCursor() throws IOException {
        // \[[<rows>;<columns>H
        byte[] bytes = {CTRL_ESC, '[', '1', '4', ';', '4', '3', 'H'};
        ByteArrayInputStream inps = new ByteArrayInputStream(bytes);
        CharacterTerminal charTerm = createCharacterTermMock();
        charTerm.setCursor(-1, -1);
        VTxEmulator emulator = new VTxEmulator(charTerm, inps, null);
        emulator.nextToken();
        // rows-1,cols-1:
        assertThat(charTerm.getCursorY()).isEqualTo(13);
        assertThat(charTerm.getCursorX()).isEqualTo(42);
    }

    @Test
    public void sendAndrequestCursor() throws IOException {
        // \[[<rows>;<columns>H
        byte[] bytes = {CTRL_ESC, '[', '1', '4', ';', '4', '3', 'H', CTRL_ESC, '[', '6', 'n'};
        ByteArrayInputStream inps = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream outps = new ByteArrayOutputStream(1024);

        CharacterTerminal charTerm = createCharacterTermMock();
        charTerm.setCursor(-1, -1);
        VTxEmulator emulator = new VTxEmulator(charTerm, inps, outps);
        emulator.nextToken();
        // rows-1,cols-1:
        assertThat(charTerm.getCursorY()).isEqualTo(13);
        assertThat(charTerm.getCursorX()).isEqualTo(42);
        emulator.nextToken();
        byte[] sendBytes = outps.toByteArray();
        log.error("Received: {}", Util.prettyByteString(sendBytes));
    }

    @Test
    public void requestColor() throws IOException {

        int col = 123;
        String req = String.format("%c]4;%d;?%c", CTRL_ESC, col, 007);
        byte[] bytes = req.getBytes(StandardCharsets.UTF_8);

        log.debug("Send bytes:{}", Util.prettyByteString(bytes));

        ByteArrayInputStream inps = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream outps = new ByteArrayOutputStream(1024);

        CharacterTerminal charTerm = createCharacterTermMock();
        VTxEmulator emulator = new VTxEmulator(charTerm, inps, outps);
        emulator.nextToken();

        byte[] sendBytes = outps.toByteArray();
        log.error("Received: {}", Util.prettyByteString(sendBytes));
    }
}

