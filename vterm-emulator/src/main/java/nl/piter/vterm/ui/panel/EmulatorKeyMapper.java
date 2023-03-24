/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.ui.panel;

import lombok.extern.slf4j.Slf4j;
import nl.piter.vterm.api.TerminalKeyListener;
import nl.piter.vterm.emulator.Emulator;
import nl.piter.vterm.emulator.VTxTokenDefs;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Emulator KeyMapper.
 * <p>
 * Transform AWT KeyEvents to Emulator Codes
 */
@Slf4j
public class EmulatorKeyMapper implements TerminalKeyListener, KeyListener {

    private Emulator emulator = null;

    private char lastPressed;

    public EmulatorKeyMapper(Emulator emulator) {
        this.emulator = emulator;
    }

    public void keyPressed(KeyEvent e) {
        int keycode = e.getKeyCode();
        char keychar = e.getKeyChar();

        lastPressed = keychar;
        // use customizable keystrings:
        String keystr = KeyEvent.getKeyText(keycode);
        int mods = e.getModifiersEx();

        // keystr matches Token String Reprentation: 
        // (Optionally prefix with "VT52_"/"VT100"  for VT52/VT100 codes:

        if (emulator == null) {
            log.warn("*** NO EMULATOR PRESENT ***\n");
            return;
        }

        boolean ctrl = (mods & KeyEvent.CTRL_DOWN_MASK) > 0;
        boolean alt = (mods & KeyEvent.ALT_DOWN_MASK) > 0;
        try {

            if (alt) {
                if (keycode == KeyEvent.VK_F9)
                    emulator.signalHalt(true);

                if (keycode == KeyEvent.VK_F10)
                    emulator.step();

                if (keycode == KeyEvent.VK_F11)
                    emulator.signalHalt(false);

                if (keycode == KeyEvent.VK_F12)
                    emulator.signalTerminate();
            }

            if (ctrl) {
                // CTRL-A to CTRL-Z

                log.debug("CTRL-{}", keycode);
                // special CTRL-SPACE = send NUL 

                if ((keycode >= KeyEvent.VK_A) && (keycode <= KeyEvent.VK_Z)) {
                    emulator.send((byte) (keycode - KeyEvent.VK_A + 1));
                    return;
                }

                // Special Characters! 
                switch (keycode) {
                    case '@':
                    case ' ':
                        emulator.send((byte) 0);
                        break;
                    case '[':
                        emulator.send((byte) VTxTokenDefs.CTRL_ESC);
                        break;
                    default:
                }

                if (keycode == KeyEvent.VK_PAGE_UP) {

                } else if (keycode == KeyEvent.VK_PAGE_DOWN) {

                }
            } else if (keycode == KeyEvent.VK_DELETE)
                emulator.send(emulator.getKeyCode("DELETE"));
            else if (keycode == KeyEvent.VK_PAGE_UP)
                emulator.send(emulator.getKeyCode("PAGE_UP"));
            else if (keycode == KeyEvent.VK_PAGE_DOWN)
                emulator.send(emulator.getKeyCode("PAGE_DOWN"));
            else if (keycode == KeyEvent.VK_END)
                emulator.send(emulator.getKeyCode("END"));
            else {
                byte[] code = this.emulator.getKeyCode(keystr);

                if (code != null) {
                    emulator.send(code);
                    return;
                } else if ((keychar & 0xff00) == 0) {
                    emulator.send((byte) keychar);
                }
            }

        } catch (Exception ee) {
            this.handle("keyPressed()", ee);
        }
        e.consume();
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
        log.debug("Event:{}", e);

        int keycode = e.getKeyCode();
        char keychar = e.getKeyChar();
        // use customizable keystrings:
        String keystr = KeyEvent.getKeyText(keycode);
        int mods = e.getModifiersEx();

        log.debug("keyTyped(): mods='{}', keycode='{}', char='{}', keystr='{}'",
                mods,
                keycode,
                keychar,
                keystr);

        // International Keymappings: 
        // if a key is "typed" but not pressed, this was a combo character, 
        // like "e =>ë, or 'u=>ú 

        if (lastPressed == keychar)
            return;

        keystr = "" + keychar;

        // try to send keychar as UTF string ! 
        try {
            try {
                emulator.send(keystr.getBytes(emulator.getEncoding()));
            } catch (UnsupportedEncodingException e1) {
                emulator.send(keystr.getBytes());
            }
        } catch (IOException ex) {
            handle("IOException", ex);
        }

    }

    protected void handle(String action, Exception e) {
        log.error("{}::Exception:{}", action, e.getMessage());
        log.debug("Exception", e);
    }
}
