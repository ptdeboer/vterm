/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Generic interface for Shell Channels which have a pty (pseudo-terminal) associated with it.
 * <p>
 * Whether features are supported depends on the implementing shell channel.
 */
public interface ShellChannel {

    /**
     * Get stdin OutputStream (to write to remote shell) after channel has connected.
     */
    OutputStream getStdin();

    /**
     * Get stdout InputStream (to read from remote shell) after channel has connected.
     */
    InputStream getStdout();

    /**
     * Get Optional stderr InputStream if supported. Stderr might be mixed with stdout.
     */
    InputStream getStderr();

    void connect() throws IOException;

    void disconnect(boolean waitForTermination) throws IOException;

    /**
     * @return terminal type, for example "vt100" or "xterm".
     * @throws IOException
     */
    String getPtyTermType() throws IOException;

    /**
     * Tries to set terminal type to underlaying shell channel. For example ror example "vt100" or
     * "xterm".
     *
     * @return true of terminal type was succesfuly updated. False if terminal type is not
     * supported.
     * @throws IOException
     */
    boolean setPtyTermType(String type) throws IOException;

    /**
     * Tries to set terminal size to underlying shell channel.
     * Setting width and height in pixels do not seem to be supported anywhere.
     *
     * @return true of terminal type was successfully updated. False if terminal type is not
     * supported.
     * @throws IOException
     */
    boolean setPtyTermSize(int numColumns, int numRows, int widthInPixels, int heightInPixels) throws IOException;

    /**
     * Returns array of int[2]{col,row} or int[4]{col,row,wp,hp} of remote terminal (pty) size.
     * Return NULL if size couldn't be determined (terminal sizes not supported)
     */
    int[] getPtyTermSize() throws IOException;

    /**
     * Wait until shell has finished.
     */
    void waitFor() throws InterruptedException;

    /**
     * Exit value of shell process.
     */
    int exitValue();

}
