/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.channels.impl;

import nl.piter.vterm.api.ShellChannel;
import nl.piter.vterm.api.ShellChannelFactory;
import nl.piter.vterm.api.TermChannelOptions;
import nl.piter.vterm.api.TermUI;

import java.net.URI;

public class PtyChannelFactory implements ShellChannelFactory {

    @Override
    public ShellChannel createChannel(URI uri, String username, char[] password, TermChannelOptions options, TermUI ui) {
        return new PtyChannel(uri.getPath(), options);
    }
}
