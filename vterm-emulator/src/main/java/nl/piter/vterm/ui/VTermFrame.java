/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.ui;

import lombok.extern.slf4j.Slf4j;
import nl.piter.vterm.api.ShellChannel;
import nl.piter.vterm.api.TermChannelOptions;
import nl.piter.vterm.api.TermConst;
import nl.piter.vterm.emulator.Emulator;
import nl.piter.vterm.emulator.VTermChannelProvider;
import nl.piter.vterm.sys.SysEnv;
import nl.piter.vterm.ui.panels.Dialogs;
import nl.piter.vterm.ui.panels.StatusBar;
import nl.piter.vterm.ui.panels.VTermPanel;
import nl.piter.vterm.ui.panels.charpane.ColorMap;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static java.awt.event.KeyEvent.VK_F10;
import static nl.piter.vterm.emulator.Util.isEmpty;
import static nl.piter.vterm.emulator.Util.splitQuotedArgs;
import static nl.piter.vterm.ui.VTermConst.*;
import static nl.piter.vterm.ui.VTermSessionManager.*;

/**
 * Master VTerm Frame.
 */
@Slf4j
public class VTermFrame extends JFrame {

    public static final String ACTION_XFORWARDING_ENABLE = "ACTION-XForwarding-enable";
    public static final String ACTION_XFORWARDING_CONFIG = "ACTION-XForwarding-config";

    // ========================================================================

    private static final String aboutText = ""
            + "<html><center>VTerm VT100+/xterm Emulator<br>"
            + "(beta version)<br>"
            + "(C) Piter.NL<br>"
            + "Author Piter T. de Boer<br>"
            + "Render Engine (C) Piter.NL</center></html>";
    private static final int verbose = 0; // 0=silent, 1 = error and fixmes,2=warn,3=info,4=debug,5=very debug,6=trace


    // ========================================================================

    private final boolean _saveConfigEnabled = true;

    // =======================
    // Session fields
    // =======================
    private String termType = TermConst.TERM_XTERM;
    private VTermSessionManager vtermManager;

    // === GUI === //
    private VTermPanel terminalPanel;
    // components:
    private JMenuBar menu;
    private JCheckBoxMenuItem sshXForwardingCB;
    private JMenuItem sshXForwardingConfig;
    private JCheckBoxMenuItem channelCompressionCB;
    private JCheckBoxMenuItem fontAAcheckBox;
    private JMenuItem startBashMenuItem;
    private JMenuItem closeSessionMenuItem;
    private JMenuItem startSSHMenuItem;
    private JCheckBoxMenuItem menuTypeVt100CB;
    private JCheckBoxMenuItem menuTypeXtermCB;
    private JCheckBoxMenuItem menuTypeXterm256;
    private VTermController termController;
    private JCheckBoxMenuItem optionsSyncScrolling;
    private StatusBar statusPanel;
    private JPanel snackBar;

    public VTermFrame(VTermChannelProvider termProvider) {
        // gui uses loaded settings !
        initGui();
        initTermManager(termProvider);
        this.applyGraphicConfigSettings();
        this.pack();
    }

    public void initGui() {

        // JFrame
        this.setTitle("VTerm");
        this.setLayout(new BorderLayout());
        this.termController = new VTermController(this);
        // Term Panel
        {
            terminalPanel = new VTermPanel();
            this.add(terminalPanel, BorderLayout.CENTER);
        }
        // Status bar
        {
            this.statusPanel = new StatusBar();
            this.add(statusPanel, BorderLayout.NORTH);
        }
        // MenuBar
        {
            menu = this.createMenuBar();
            this.setJMenuBar(menu);
            // Disable DEFAULT key binding:
            menu.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(VK_F10, 0), "none");
        }
        // SnackBar: 
//        {
//            this.snackBar=new JPanel();
//            this.add(snackBar,BorderLayout.SOUTH);
//        }
        // Listeners:
        {
            this.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    exit(0);
                }
            });
            this.terminalPanel.addComponentListener(termController);
            this.addComponentListener(termController);
            this.addWindowListener(termController);
        }
    }

    private void initTermManager(VTermChannelProvider termProvider) {
        // Controllers:
        this.vtermManager = new VTermSessionManager(termController, termController, termProvider, terminalPanel);
    }

    /**
     * Apply current configured properties to CharPane. Does reset graphics !
     */
    void applyGraphicConfigSettings() {
        // update from configuration:
        String propStr = vtermManager.getProperty(VTERM_SYNC_SLOW_SCROLLING);
        if (propStr != null) {
            boolean val = Boolean.parseBoolean(propStr);
            this.optionsSyncScrolling.setSelected(val);
        }
        // Directly set charPane options: do not reset graphics each time !
        propStr = vtermManager.getProperty(VTERM_COLOR_SCHEME);
        if (propStr != null)
            this.terminalPanel.updateColorMap(ColorMap.getColorMap(propStr), false);

        propStr = vtermManager.getProperty(VTERM_FONT_TYPE);
        if (propStr != null)
            this.terminalPanel.updateFontType(propStr, false);

        propStr = vtermManager.getProperty(VTERM_FONT_SIZE);
        if (propStr != null)
            this.terminalPanel.updateFontSize(Integer.parseInt(propStr), false);

        propStr = vtermManager.getProperty(VTERM_FONT_ANTI_ALIASING);
        if (propStr != null) {
            boolean val = Boolean.parseBoolean(propStr);
            this.terminalPanel.getFontInfo().setAntiAliasing(val);
            this.fontAAcheckBox.setSelected(val);
        }

        propStr = vtermManager.getProperty(VTERM_TERM_TYPE);
        menuTypeVt100CB.setState(TermConst.TERM_VT100.equals(propStr));
        menuTypeXtermCB.setState(TermConst.TERM_XTERM.equals(propStr));
        menuTypeXterm256.setState(TermConst.XTERM_256COLOR.equals(propStr));
        this.terminalPanel.resetGraphics();

        optionsSyncScrolling.setState(vtermManager.getSlowScrolling());

        TermChannelOptions sshOptions = vtermManager.getChannelOptions(SESSION_SSH, true);
        channelCompressionCB.setState(sshOptions.getBooleanOption(TermConst.SSH_CHANNEL_COMPRESSION, false));
        channelCompressionCB.setState(sshOptions.getBooleanOption(TermConst.SSH_CHANNEL_COMPRESSION, false));
        channelCompressionCB.setState(sshOptions.getBooleanOption(TermConst.SSH_CHANNEL_COMPRESSION, false));
        sshXForwardingCB.setState(sshOptions.getBooleanOption(TermConst.SSH_XFORWARDING_ENABLED, false));
    }

    public JMenuBar createMenuBar() {
        //ChannelOptions options = getChannelOptions();

        JMenuBar menubar = new JMenuBar();
        JMenu menu;
        JMenuItem mitem;

        menu = new JMenu("Session");
        menubar.add(menu);

        startSSHMenuItem = new JMenuItem("Start SSH Session...");
        menu.add(startSSHMenuItem);
        startSSHMenuItem.addActionListener(termController);
        startSSHMenuItem.setActionCommand(SESSION_SSH);
        // mitem=new JMenuItem("Start Telnet Session..."); menu.add(mitem);
        // mitem.addActionListener(this);
        // mitem.setActionCommand(SESSION_TELNET);
        startBashMenuItem = new JMenuItem("Start Shell Session...");
        menu.add(startBashMenuItem);
        startBashMenuItem.addActionListener(termController);
        startBashMenuItem.setActionCommand(SESSION_PTY);
        menu.add(startBashMenuItem);
        {
            mitem = new JMenuItem("Repaint");
            menu.add(mitem);
            mitem.addActionListener(termController);
            mitem.setActionCommand("Repaint");
        }
        {
            mitem = new JMenuItem("Clear Screen");
            menu.add(mitem);
            mitem.addActionListener(termController);
            mitem.setActionCommand("ClearScreen");
        }
        {
            closeSessionMenuItem = new JMenuItem("Terminate Session");
            menu.add(closeSessionMenuItem);
            closeSessionMenuItem.addActionListener(termController);
            closeSessionMenuItem.setActionCommand("Close");
            closeSessionMenuItem.setEnabled(false);
        }

        mitem = new JMenuItem("Quit VTerm");
        menu.add(mitem);
        mitem.addActionListener(termController);
        mitem.setActionCommand("Quit");

        // SETTINGS MENU
        {
            menu = new JMenu("Settings");
            menubar.add(menu);

            {

                JMenu sshmenu = new JMenu("SSH");
                menu.add(sshmenu);
                mitem = new JMenuItem("Local Port Forwarding...");
                mitem.addActionListener(termController);
                mitem.setActionCommand("LocalPort");
                sshmenu.add(mitem);
                mitem = new JMenuItem("Remote Port Forwarding...");
                mitem.addActionListener(termController);
                mitem.setActionCommand("RemotePort");
                sshmenu.add(mitem);

                channelCompressionCB = new JCheckBoxMenuItem("Compression...");
                channelCompressionCB.addActionListener(termController);
                channelCompressionCB.setActionCommand("Compression");

                sshmenu.add(channelCompressionCB);
                {
                    JMenu xForwardingMenu = new JMenu("X Forwarding");
                    sshmenu.add(xForwardingMenu);

                    sshXForwardingCB = new JCheckBoxMenuItem("enable X Forwarding");
                    sshXForwardingCB.addActionListener(termController);
                    sshXForwardingCB.setActionCommand(ACTION_XFORWARDING_ENABLE);
                    sshXForwardingCB.setEnabled(true);
                    xForwardingMenu.add(sshXForwardingCB);

                    sshXForwardingConfig = new JMenuItem("X Forwarding Settings");
                    sshXForwardingConfig.addActionListener(termController);
                    sshXForwardingConfig.setActionCommand(ACTION_XFORWARDING_CONFIG);
                    sshXForwardingConfig.setEnabled(false);

                    xForwardingMenu.add(sshXForwardingConfig);
                }
            }

            {
                JMenu emulatorMenu = new JMenu("Emulator");
                menu.add(emulatorMenu);

                menuTypeVt100CB = new JCheckBoxMenuItem(TermConst.TERM_VT100);
                menuTypeVt100CB.addActionListener(termController);
                menuTypeVt100CB.setActionCommand(TermConst.TERM_VT100);
                emulatorMenu.add(menuTypeVt100CB);
                menuTypeVt100CB.setEnabled(false);

                menuTypeXtermCB = new JCheckBoxMenuItem(TermConst.TERM_XTERM);
                menuTypeXtermCB.addActionListener(termController);
                menuTypeXtermCB.setActionCommand(TermConst.TERM_XTERM);
                emulatorMenu.add(menuTypeXtermCB);

                menuTypeXterm256 = new JCheckBoxMenuItem(TermConst.XTERM_256COLOR);
                menuTypeXterm256.addActionListener(termController);
                menuTypeXterm256.setActionCommand(TermConst.XTERM_256COLOR);
                emulatorMenu.add(menuTypeXterm256);
            }

            JSeparator sep = new JSeparator();
            menu.add(sep);

            // Font-> 
            {
                JMenu fontmenu = new JMenu("Font");
                menu.add(fontmenu);
                {
                    // Font->size
                    JMenu sizemenu = new JMenu("size");
                    fontmenu.add(sizemenu);

                    String[] sizes = {"7", "8", "9", "10", "11", "12", "13", "14", "16", "18", "20", "24"};

                    for (String s : sizes) {
                        mitem = new JMenuItem(s);
                        mitem.addActionListener(termController);
                        mitem.setActionCommand("fontsize-" + s);
                        sizemenu.add(mitem);
                    }
                }
                // Font->type
                {
                    JMenu typemenu = new JMenu("type");
                    fontmenu.add(typemenu);

                    // selection of MONO spaced fonts; 
                    String[] types = {"Monospaced", "Courier", "Courier New", "Courier 10 Pitch",
                            "Luxi Mono", "Liberation Mono", "DejaVu Sans Mono", "Bera Sans Mono"};

                    for (String s : types) {
                        mitem = new JMenuItem(s);
                        mitem.addActionListener(termController);
                        mitem.setActionCommand("fonttype-" + s);
                        typemenu.add(mitem);
                    }
                }
                // Font-Anti aliasing
                {
                    fontAAcheckBox = new JCheckBoxMenuItem("Anti aliasing");
                    fontAAcheckBox.addActionListener(termController);
                    fontAAcheckBox.setActionCommand("font-aa");
                    fontAAcheckBox.setState(terminalPanel.getFontInfo().hasAntiAliasing());
                    fontmenu.add(fontAAcheckBox);
                }

            }
            // Color ->
            {
                JMenu colormenu = new JMenu("Colors");
                menu.add(colormenu);
                {
                    JMenu schememenu = new JMenu("Scheme");
                    colormenu.add(schememenu);

                    String[] names = ColorMap.getColorMapNames();

                    for (int i = 0; i < names.length; i++) {
                        mitem = new JMenuItem(names[i]);
                        mitem.addActionListener(termController);
                        mitem.setActionCommand("colorscheme-" + names[i]);
                        schememenu.add(mitem);
                    }
                }
            }
            // Scroll options.
            {
                optionsSyncScrolling = new JCheckBoxMenuItem("Synchronize Scrolling");
                optionsSyncScrolling.addActionListener(termController);
                optionsSyncScrolling.setActionCommand("syncScrolling");
                menu.add(optionsSyncScrolling);
            }

            sep = new JSeparator();
            menu.add(sep);
            {
                mitem = new JMenuItem("Save");
                mitem.addActionListener(termController);
                mitem.setActionCommand("SaveSettings");
                mitem.setVisible(_saveConfigEnabled);
                menu.add(mitem);
            }
        }

        menu = new JMenu("Help");
        menubar.add(menu);
        mitem = new JMenuItem("About...");
        mitem.addActionListener(termController);
        mitem.setActionCommand("About");
        menu.add(mitem);
        mitem = new JMenuItem("Debug ON");
        mitem.addActionListener(termController);
        mitem.setActionCommand("DebugON");
        menu.add(mitem);
        mitem = new JMenuItem("Debug OFF");
        mitem.addActionListener(termController);
        mitem.setActionCommand("DebugOFF");
        menu.add(mitem);
        mitem = new JMenuItem("Test Screen");
        mitem.addActionListener(termController);
        mitem.setActionCommand("testscreen");
        menu.add(mitem);
        //updateMenuSettings();
        return menubar;
    }

    public void setTitle(String str) {
        super.setTitle("VTerm:" + str);
    }

    private void exit(int i) {

        if (this.vtermManager.isRunning()) {
            try {
                this.vtermManager.terminate();
            } catch (IOException e) {
                log.error("Exception when termination:" + e.getMessage(), e);
            }
        }

        dispose();
    }

    @Override
    public void dispose() {
        terminateSession();
        if (terminalPanel != null) {
            this.terminalPanel.dispose();
        }
        this.terminalPanel = null;
        super.setVisible(false);
        super.dispose();
    }

    // after change of graphics: repack main Frame.
    void updateFrameSize() {
        Runnable update = new Runnable() {
            public void run() {
                validate();
                pack();
            }
        };

        SwingUtilities.invokeLater(update);
    }

    public void actionPerformed(ActionEvent e) {
        String action = e.getActionCommand();
        Component source = (Component) e.getSource();

        if (action.compareTo(SESSION_PTY) == 0) {

            TermChannelOptions options = vtermManager.getChannelOptions(SESSION_PTY, true);

            String cmd;
            String args;

            cmd = vtermManager.getConfigProperty(VTermConst.VTERM_SESSION_LAST_SHELL_CMD);
            args = vtermManager.getConfigProperty(VTermConst.VTERM_SESSION_LAST_SHELL_ARGS);

            if (isEmpty(cmd)) {
                cmd = "/bin/bash";
            }
            if (isEmpty(args)) {
                cmd = "-1";
            }

            String[] fields = new String[]{"Shell path (no arguments)", "Arguments"};
            String[] values = new String[]{cmd, args};
            String[] results = Dialogs.askInputDialog(this, "Argument (list)", fields, values);

            if ((results == null) || (results.length == 0)) {
                return;
            }

            cmd = results[0];
            List<String> argList = splitQuotedArgs(results[1]);
            argList.add(0, cmd);
            options.setCommand(argList.toArray(new String[0]));

            vtermManager.storeChannelOptions(SESSION_PTY, options);
            vtermManager.saveConfigProperty(VTermConst.VTERM_SESSION_LAST_SHELL_CMD, cmd);
            vtermManager.saveConfigProperty(VTermConst.VTERM_SESSION_LAST_SHELL_ARGS, args);
            vtermManager.setSessionType(SESSION_PTY);
            vtermManager.startSession();

        } else if (action.compareTo(SESSION_TELNET) == 0) {
            if (!vtermManager.isRunning()) {
                vtermManager.setSessionType(SESSION_TELNET);
                String str = new Dialogs(this).askInput("Provide details", "Enter username@hostname");
                parseLocation("telnet", str);
                vtermManager.startSession();
            }
        } else if (action.compareTo(SESSION_SSH) == 0) {

            if (vtermManager.isRunning()) {
                termController.showMessage("Session is still running!");
            } else {
                vtermManager.setSessionType(SESSION_SSH);

                String sessionstr = vtermManager.getConfigProperty(VTermConst.VTERM_SESSION_LAST_URI_SSH);

                if (sessionstr == null) {
                    if (vtermManager.getStartURI() != null)
                        sessionstr = vtermManager.getStartURI().toString();
                    else
                        sessionstr = "user@localhost";
                }

                String locstr = JOptionPane.showInputDialog(this, "Enter:<username>@<hostname>",
                        sessionstr);

                if (locstr == null)
                    return;

                vtermManager.saveConfigProperty(VTermConst.VTERM_SESSION_LAST_URI_SSH, locstr);
                parseLocation("ssh", locstr);
                vtermManager.startSession();
            }
        } else if (action.equals(ACTION_XFORWARDING_ENABLE)) {
            this.sshXForwardingConfig.setEnabled(sshXForwardingCB.getState());
        } else if (action.equals(ACTION_XFORWARDING_CONFIG)) {
            TermChannelOptions sshOptions = vtermManager.getChannelOptions(SESSION_SSH, true);


            String xForwardingHost = sshOptions.getOption(TermConst.SSH_XFORWARDING_HOST);
            int xForwardingPort = sshOptions.getIntOption(TermConst.SSH_XFORWARDING_PORT, -1);

            String display = JOptionPane.showInputDialog(this, "XDisplay name (hostname:0)",
                    (xForwardingHost == null) ? "" : (xForwardingHost + ":" + xForwardingPort));

            if (display == null) {
                sshOptions.setOption(TermConst.SSH_XFORWARDING_ENABLED, false);
            }

            xForwardingHost = display.substring(0, display.indexOf(':'));
            xForwardingPort = Integer.parseInt(display.substring(display.indexOf(':') + 1));

            // if port= ":0" - ":99" use:6000-6099 else use given port (>=1024)

            if (xForwardingPort < 1024) {
                xForwardingPort += 6000;
            }

            sshOptions.setOption(TermConst.SSH_XFORWARDING_HOST, xForwardingHost);
            sshOptions.setOption(TermConst.SSH_XFORWARDING_PORT, xForwardingPort);
            sshOptions.setOption(TermConst.SSH_XFORWARDING_ENABLED, xForwardingHost != null);
            vtermManager.setChannelOptions(SESSION_SSH, sshOptions);

        } else if (action.startsWith("fontsize")) {
            String[] strs = action.split("-");

            if (strs.length > 1 && strs[1] != null) {
                Integer val = Integer.valueOf(strs[1]);
                this.terminalPanel.updateFontSize(val, true);
                this.vtermManager.setProperty("vterm.font.size", "" + val);
            }

            terminalPanel.repaintGraphics(false);
            updateFrameSize();
        } else if (action.startsWith("fonttype")) {
            String[] strs = action.split("-");

            if (strs.length > 1 && strs[1] != null) {
                this.terminalPanel.updateFontType(strs[1], true);
                this.vtermManager.setProperty("vterm.font.type", "" + strs[1]);
            }

            terminalPanel.repaintGraphics(false);
            updateFrameSize();
        } else if (action.startsWith("colorscheme")) {
            String[] strs = action.split("-");

            if (strs.length > 1 && strs[1] != null) {
                String name = strs[1];
                terminalPanel.updateColorMap(ColorMap.getColorMap(name), true);
                this.vtermManager.setProperty("vterm.color.scheme", name);
            }

            updateFrameSize();
        } else if (action.startsWith("syncScrolling")) {
            boolean state = this.optionsSyncScrolling.getState();

            this.vtermManager.setSlowScrolling(state);
        } else if (action.compareTo("font-aa") == 0) {
            boolean val = fontAAcheckBox.getState();
            terminalPanel.getFontInfo().setAntiAliasing(val);
            this.vtermManager.setProperty("vterm.font.antiAliasing", "" + val);

            terminalPanel.repaintGraphics(false);
            updateFrameSize();
        } else if (action.equals("Close")) {
            terminateSession();
        } else if (action.equals("Repaint")) {
            terminalPanel.repaintGraphics(false);
        } else if (action.equals("ClearScreen")) {
            terminalPanel.clearAll();
        } else if (action.equals("Quit")) {
            exit(0);
        } else if (action.equals("DebugON")) {
            setLogLevelToDebug();
        } else if (action.equals("DebugOFF")) {
            setLogLevelToError();
        } else if (action.equals("About")) {
            this.showMessage(aboutText);
        } else if (action.equals("testscreen")) {
            terminalPanel.drawTestScreen();
        } else if (source == this.menuTypeVt100CB) {
            // radiobuttons...
            this.menuTypeVt100CB.setSelected(true);
            this.menuTypeXtermCB.setSelected(false);
            this.menuTypeXterm256.setSelected(false);
            this.termType = TermConst.TERM_VT100;
            vtermManager.setTermType(this.termType);
        } else if (source == this.menuTypeXtermCB) {
            this.menuTypeVt100CB.setSelected(false);
            this.menuTypeXtermCB.setSelected(true);
            this.menuTypeXterm256.setSelected(false);
            this.termType = TermConst.TERM_XTERM;
            vtermManager.setTermType(this.termType);
        } else if (source == this.menuTypeXterm256) {
            this.menuTypeVt100CB.setSelected(false);
            this.menuTypeXtermCB.setSelected(false);
            this.menuTypeXterm256.setSelected(true);
            this.termType = TermConst.XTERM_256COLOR;
            vtermManager.setTermType(this.termType);

        } else if (action.equals("SaveSettings")) {
            vtermManager.saveSettings();
        }
    }

    private int getIntOption(Map<String, String> options, String key, int defaultVal) {
        String value = options.get(key);
        if (value == null) {
            return defaultVal;
        }
        return Integer.parseInt(value);
    }

    protected void openLocation(URI loc) throws IOException {
        // parse URI: 

        String scheme = loc.getScheme();
        String host = loc.getHost();

        if (isEmpty(host)) {
            host = "localhost";
        }

        String user = loc.getUserInfo();

        if (isEmpty(user)) {
            user = SysEnv.sysEnv().getUserName();
        }

        int port = loc.getPort();
        if (port <= 0)
            port = 22;

        String path = loc.getPath();

        try {
            vtermManager.setStartURI(new URI(scheme, user, host, port, path, null, null));
        } catch (URISyntaxException e) {
            throw new IOException(e.getMessage(), e);
        }

        vtermManager.startSession();
    }

    private void parseLocation(String scheme, String locstr) {
        try {

            String user;
            int port = 0;
            String host;

            if (locstr == null)
                return;

            // user ...@
            int s = locstr.indexOf('@');

            if (s < 0) {
                user = "";
            } else {
                user = locstr.substring(0, s);
                locstr = locstr.substring(s + 1);
            }

            // host ... :
            s = locstr.indexOf(':');
            if (s < 0) {
                host = locstr;
            } else {
                host = locstr.substring(0, s);
                locstr = locstr.substring(s + 1);
                port = Integer.valueOf(locstr);
            }

            vtermManager.setStartURI(new URI(scheme, user, host, port, null, null, null));
        } catch (Exception e) {
            this.showMessage("Invalid Location. Must be user@host[:port]. Value=" + locstr);
        }
    }

    private void menuUpdateSessionAlive(boolean val) {
        if (val) {
            startSSHMenuItem.setEnabled(false);
            startBashMenuItem.setEnabled(false);
            closeSessionMenuItem.setEnabled(true);
        } else {
            startSSHMenuItem.setEnabled(true);
            startBashMenuItem.setEnabled(true);
            closeSessionMenuItem.setEnabled(false);
        }
    }

    void showSplash() {
        this.terminalPanel.drawSplash();
    }

    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    protected Emulator getEmulator() {
        if (this.terminalPanel == null) {
            return null;
        }
        return terminalPanel.getEmulator();
    }

    public void terminateSession() {
        this.menuUpdateSessionAlive(false);

        // Emulator:
        if (getEmulator() != null) {
            this.getEmulator().signalTerminate();
        }

        // Terminal;
        if (this.terminalPanel != null) {
            this.terminalPanel.terminate();
        }

        // Shell Channel:
        try {
            this.vtermManager.terminate();
        } catch (IOException e) {
            log.warn("IOException during disconnect():" + e.getMessage(), e);
        }

    }

    private void setLogLevelToDebug() {
        log.warn("***Fixme:setLogLevelToDebug()");
    }

    private void setLogLevelToError() {
        log.warn("***Fixme:setLogLevelToError()");
    }

    public VTermPanel getTerminalPanel() {
        return this.terminalPanel;
    }

    public void startShellChannel(ShellChannel shellChan, boolean start) {
        this.vtermManager.setShellChannel(shellChan);
        if (start) {
            vtermManager.startSession();
        }
    }

    public VTermSessionManager getVTermManager() {
        return this.vtermManager;
    }

    public void emulatorStarted() {
        this.menuUpdateSessionAlive(true);
    }

    public void emulatorStopped() {
        terminateSession(); // terminate if still running
        menuUpdateSessionAlive(false);
        repaint();
        showMessage("Session Ended");
    }

    public StatusBar statusPanel() {
        return statusPanel;
    }

}
