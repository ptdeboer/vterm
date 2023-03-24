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

/**
 * VT10X/XTerm (VTx) emulator. Reads from Tokenizer and outputs to CharacterTerminal.
 */
public interface Emulator {

    void start();

    byte[] getKeyCode(String keystr);

    String getEncoding();

    void signalTerminate();

    void signalHalt(boolean b);

    boolean updateRegion(int nr_columns, int nr_rows, int region_y1, int region_y2);

    void step();

    void setTermType(String type);

    boolean sendSize(int nr_columns, int nr_rows) throws IOException;

    void send(byte[] code) throws IOException;

    void send(byte keychar) throws IOException;

    int[] getRegion();

    void addListener(EmulatorListener listener);

    void removeListener(EmulatorListener listener);

}
