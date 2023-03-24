/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm;

import nl.piter.vterm.api.ShellChannel;
import nl.piter.vterm.emulator.VTermChannelProvider;
import nl.piter.vterm.ui.VTerm;

import java.net.URI;

/**
 * API binding (from Platinum/VBrowser).
 */
public class VTermStarter {

    public static void main(String[] args) {
        new VTermStarter().withChannelProvider(new VTermChannelProvider()).start(args);
    }

    // === instance === //

    private VTermChannelProvider vTermChannelProvider;

    public VTermStarter withChannelProvider(VTermChannelProvider vTermChannelProvider) {
        this.vTermChannelProvider = vTermChannelProvider;
        return this;
    }

    public VTerm start(String[] args) {
        return new VTerm().withVTermChannelProvider(vTermChannelProvider).start(args);
    }

    /**
     * <p>
     * Start session with either an authentication ShellChannel or an URI or both.
     * </p>
     * If no ShellChannel has been provided the VTerm will try to the provided URI using the VTermChannelProvider.
     * If a ShellChannel is provided together with an location, the path will be updated.
     *
     * @param optShellChannel authenticated shell channel
     * @param optionalLoc     optional location. Can be combined with authenticated shell channel.
     */
    public VTerm start(ShellChannel optShellChannel, URI optionalLoc) {
        return new VTerm().withVTermChannelProvider(vTermChannelProvider).start(optShellChannel, optionalLoc);
    }

}
