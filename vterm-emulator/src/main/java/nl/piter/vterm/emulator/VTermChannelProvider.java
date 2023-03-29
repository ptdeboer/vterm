/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.emulator;

import nl.piter.vterm.api.ShellChannel;
import nl.piter.vterm.api.ShellChannelFactory;
import nl.piter.vterm.api.TermChannelOptions;
import nl.piter.vterm.api.TermUI;
import nl.piter.vterm.channels.impl.PtyChannelFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Hashtable;
import java.util.Map;

public class VTermChannelProvider {

    protected Map<String, ShellChannelFactory> factories = new Hashtable<>();
    protected Map<String, TermChannelOptions> defaultOptions = new Hashtable<>();

    public VTermChannelProvider() {
        this.registerChannelFactory("PTY", new PtyChannelFactory());
    }

    public void registerChannelFactory(String type, ShellChannelFactory factory) {
        factories.put(type, factory);
    }

    public ShellChannel createChannel(String type, URI uri, String username, char[] password,
                                      TermChannelOptions options, TermUI ui) throws IOException {
        //
        ShellChannelFactory factory = factories.get(type);
        if (factory != null) {
            return factory.createChannel(uri, username, password, options, ui);
        }
        throw new IOException("Channel type not supported:" + type + " (when connecting to:" + uri + ")");
    }

    public TermChannelOptions getChannelOptions(String type) {
        return defaultOptions.get(type);
    }

    public void setChannelOptions(String type, TermChannelOptions newOptions) {
        defaultOptions.put(type, newOptions);
    }

}
