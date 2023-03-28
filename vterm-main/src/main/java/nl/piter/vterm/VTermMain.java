/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm;

import nl.piter.vterm.channels.sftp.SshChannelFactory;
import nl.piter.vterm.emulator.VTermChannelProvider;

/**
 * Main class to start from the start script 'vterm.sh'.
 */
public class VTermMain {

    public static void main(String[] args) {
        VTermChannelProvider provider = new VTermChannelProvider();
        provider.registerChannelFactory("SSH", new SshChannelFactory());
        new VTermStarter().withChannelProvider(provider).start(args);
    }

}
