package nl.piter.vterm.examples;

import nl.piter.vterm.emulator.VTermChannelProvider;
import nl.piter.vterm.ui.VTermSessionManager;
import nl.piter.vterm.ui.panels.VTermPanel;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Embedded example with minimal UI for custom Application.
 */
public class EmbeddedBashExample {

    private final VTermSessionManager vtermManager;

    public static void main(String[] args) throws URISyntaxException {
        new EmbeddedBashExample().start();
    }

    public EmbeddedBashExample() {
        JFrame frame=new JFrame();
        frame.setLayout(new BorderLayout());
        VTermPanel vtermPanel=new VTermPanel();
        frame.add(vtermPanel,BorderLayout.CENTER);

        ControllerAdaptor controller=new ControllerAdaptor();
        vtermManager = new VTermSessionManager(controller, controller, new VTermChannelProvider(), vtermPanel);

        frame.pack();
        frame.setVisible(true);
    }

    private void start() throws URISyntaxException {
        vtermManager.startSession("BASH",new URI("file:///"));
    }

}
