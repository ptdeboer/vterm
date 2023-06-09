/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.examples;

import nl.piter.vterm.channels.sftp.SshChannelFactory;
import nl.piter.vterm.emulator.VTermChannelProvider;
import nl.piter.vterm.ui.VTermSessionManager;
import nl.piter.vterm.ui.panels.VTermPanel;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;

import static nl.piter.vterm.ui.VTermSessionManager.SESSION_SSH;

/**
 * Embedded example with minimal UI for custom UIs.
 */
public class EmbeddedSshExample {

    private final VTermSessionManager vtermManager;

    public static void main(String[] args) throws URISyntaxException {
        new EmbeddedSshExample().start();
    }

    public EmbeddedSshExample() {
        //Custom UI:
        JFrame frame = new JFrame();
        frame.setLayout(new BorderLayout());
        VTermPanel vtermPanel = new VTermPanel();
        frame.add(vtermPanel, BorderLayout.CENTER);

        // Configure SSH
        VTermChannelProvider provider = new VTermChannelProvider();
        provider.registerChannelFactory(SESSION_SSH, new SshChannelFactory());

        // Custom UI Controller:
        CustomControllerAdaptor controller = new CustomControllerAdaptor();
        vtermManager = new VTermSessionManager(controller, controller, provider, vtermPanel);

        frame.pack();
        frame.setVisible(true);
    }

    private void start() throws URISyntaxException {
        vtermManager.startSession("SSH", new URI("ssh://localhost:22/"));
    }

}
