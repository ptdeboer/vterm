/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.api;

import java.io.IOException;
import java.net.URI;

/**
 * Interface for resources which can create a 'Shell'. Typically, a shell has a pseudo tty interface.
 *
 * @see ShellChannel
 */
public interface ShellChannelFactory {

    ShellChannel createChannel(URI uri, String user, char[] password, TermChannelOptions options, TermUI ui) throws IOException;

}
