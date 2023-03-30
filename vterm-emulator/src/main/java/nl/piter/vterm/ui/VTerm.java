/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.ui;

import nl.piter.vterm.api.ShellChannel;
import nl.piter.vterm.emulator.VTermChannelProvider;

import javax.swing.*;
import java.io.IOException;
import java.net.URI;

import static nl.piter.vterm.ui.VTermSessionManager.SESSION_SHELLCHANNEL;

/**
 * The VT10X/XTerm (VTx) emulator: Reads from Tokenizer, output to CharacterTerminal.
 */
public class VTerm {

    private VTermChannelProvider channelProvider;

    private VTermFrame vtermJFrame;

    public VTerm() {
    }

    public VTerm withVTermChannelProvider(VTermChannelProvider channelProvider) {
        this.channelProvider = channelProvider;
        return this;
    }

    /**
     * Start from script with arguments.
     */
    public VTerm start(String args[]) {
        return start(null,null,null,args);
    }

    /**
     * Start with Custom ShellChannel provided by already authenticated sub system like an SFTP Browser.
     */
    public VTerm start(ShellChannel shellChan, URI optionalLoc) {
        return start(SESSION_SHELLCHANNEL,shellChan, optionalLoc, new String[0]);
    }

    public VTerm start(String sessionType, final ShellChannel shellChan, final URI optionalLocation, String args[]) {
        // check args[]
        if (this.channelProvider == null) {
            this.channelProvider = new VTermChannelProvider();
        }

        this.vtermJFrame = new VTermFrame(channelProvider);

        // Always create windows during Swing Event thread
        Runnable creator = () -> {
            // Center on screen
            vtermJFrame.setLocationRelativeTo(null);
            vtermJFrame.setVisible(true);
            vtermJFrame.showSplash();
            vtermJFrame.requestFocus();

            vtermJFrame.updateFrameSize();
            if (shellChan != null) {
                vtermJFrame.controller().startShellChannel(shellChan, true);
            }
            else if (optionalLocation != null) {
                try {
                    vtermJFrame.controller().openLocation(sessionType, optionalLocation);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        };

        SwingUtilities.invokeLater(creator);
        return this;
    }

}
