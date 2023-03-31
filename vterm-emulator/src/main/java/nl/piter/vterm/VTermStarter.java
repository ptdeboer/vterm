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

import static nl.piter.vterm.ui.VTermSessionManager.SESSION_SHELLCHANNEL;

/**
 * API binding (for Platinum/VBrowser).
 */
public class VTermStarter {

    public static void main(String[] args) {
        new VTermStarter().withChannelProvider(new VTermChannelProvider()).start(args);
    }

    // === instance === //

    private VTermChannelProvider vTermChannelProvider;

    public VTermStarter() {
    }

    public VTermStarter withChannelProvider(VTermChannelProvider vTermChannelProvider) {
        VTermStarter starter = new VTermStarter();
        starter.vTermChannelProvider=vTermChannelProvider;
        return starter;
    }

    public VTerm start(String[] args) {
        return new VTerm().withVTermChannelProvider(vTermChannelProvider).start(args);
    }

    public VTerm start(ShellChannel optShellChannel, URI optLocation) {
        return new VTerm().withVTermChannelProvider(vTermChannelProvider).start(SESSION_SHELLCHANNEL, optShellChannel, optLocation,null);
    }

    public VTerm start(String sessionType, ShellChannel optShellChannel, URI optionalLoc) {
        return new VTerm().withVTermChannelProvider(vTermChannelProvider).start(sessionType, optShellChannel, optionalLoc,null);
    }


}
