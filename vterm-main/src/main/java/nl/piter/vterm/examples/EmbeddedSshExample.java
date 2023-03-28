package nl.piter.vterm.examples;

import nl.piter.vterm.channels.sftp.SshChannelFactory;
import nl.piter.vterm.emulator.VTermChannelProvider;
import nl.piter.vterm.ui.VTermSessionManager;
import nl.piter.vterm.ui.panels.VTermPanel;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Embedded example with minimal UI for custom UIs.
 */
public class EmbeddedSshExample {

    private final VTermSessionManager vtermManager;

    public static void main(String[] args) throws URISyntaxException {
        new EmbeddedSshExample().start();
    }

    public EmbeddedSshExample() {
        JFrame frame=new JFrame();
        frame.setLayout(new BorderLayout());
        VTermPanel vtermPanel=new VTermPanel();
        frame.add(vtermPanel,BorderLayout.CENTER);

        // wire controllers:
        ControllerAdaptor controller=new ControllerAdaptor();
        VTermChannelProvider provider = new VTermChannelProvider();
        provider.registerChannelFactory("SSH", new SshChannelFactory());

        vtermManager = new VTermSessionManager(controller, controller, provider, vtermPanel);
        vtermPanel.addComponentListener(new ResizeAdaptor(vtermManager));

        frame.pack();
        frame.setVisible(true);
    }

    private void start() throws URISyntaxException {
        vtermManager.startSession("SSH",new URI("ssh://localhost:22/"));
    }

}
