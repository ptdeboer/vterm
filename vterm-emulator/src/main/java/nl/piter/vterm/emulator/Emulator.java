/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.emulator;

import nl.piter.vterm.api.EmulatorListener;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * VT10X/XTerm (VTx) emulator. Reads from Tokenizer and outputs to CharacterTerminal.
 */
public interface Emulator {

    void start();

    String getType();

    byte[] getKeyCode(String keystr);

    Charset getEncoding();

    void signalTerminate();

    void signalHalt(boolean b);

    void step();

    void setTermType(String type);

    boolean sendSize(int nr_columns, int nr_rows) throws IOException;

    void send(byte[] code) throws IOException;

    void send(byte keychar) throws IOException;

    int[] getRegion();

    void addListener(EmulatorListener listener);

    void removeListener(EmulatorListener listener);

    void setSlowScrolling(boolean val);

    boolean getSlowScrolling();

}
