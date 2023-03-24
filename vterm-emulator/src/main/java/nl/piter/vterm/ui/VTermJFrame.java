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
import nl.piter.vterm.emulator.VTxEmulator;
import nl.piter.vterm.sys.SysEnv;
import nl.piter.vterm.ui.charpane.ColorMap;
import nl.piter.vterm.ui.panel.Dialogs;
import nl.piter.vterm.ui.panel.TermPanel;
import nl.piter.vterm.util.StringUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static nl.piter.vterm.ui.VTermConst.*;

/**
 * Master VTerm Frame.
 */
@Slf4j
public class VTermJFrame extends JFrame implements Runnable {

    public static final String ACTION_XFORWARDING_ENABLE = "ACTION-XForwarding-enable";
    public static final String ACTION_XFORWARDING_CONFIG = "ACTION-XForwarding-config";

    // ========================================================================

    private static final String SESSION_SSH = "SSH";
    protected static final String SESSION_BASH = "BASH";
    protected static final String SESSION_TELNET = "TELNET";
    private static final String SESSION_SHELLCHANNEL = "SHELLCHANNEL";
    private static final String aboutText = ""
            + "<html><center>VTerm VT100+/xterm Emulator<br>"
            + "(beta version)<br>"
            + "(C) Piter.NL<br>"
            + "Author Piter T. de Boer<br>"
            + "Render Engine (C) Piter.NL</center></html>";
    private static final int verbose = 0; // 0=silent, 1 = error and fixmes,2=warn,3=info,4=debug,5=very debug,6=trace

    // ========================================================================

    private java.net.URI startURI = null;
    private final boolean _saveConfigEnabled = true;
    private String sessionType = SESSION_SSH;

    // =======================
    // Session fields
    // =======================
    private String termType = TermConst.TERM_XTERM;
    private final VTermChannelProvider termProvider;
    private ShellChannel shellChannel;
    private Properties properties = null;

    // Thread, runtime.
    private boolean sessionAlive;
    private Thread thread = null;

    // === GUI === //
    private TermPanel terminalPanel;
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
    private JPanel statusPanel;
    private JLabel statusInfo;

    public VTermJFrame(VTermChannelProvider termProvider) {
        this.termProvider = termProvider;

        loadConfigSettings();

        // gui uses loaded settings !
        initGui();

        try {
            startURI = new URI("ssh", SysEnv.sysEnv().getUserName(), "localhost", 22, null,
                    null, null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    void loadConfigSettings() {
        this.properties = loadConfigProperties();
    }

    public void initGui() {

        // JFrame
        this.setTitle("");
        this.setLayout(new BorderLayout());
        this.termController = new VTermController(this);

        // status bar
        {
            this.statusPanel = new JPanel();
            this.add(statusPanel, BorderLayout.NORTH);
            statusPanel.setLayout(new FlowLayout());
            this.statusInfo = new JLabel("status:");
            statusPanel.add(statusInfo);
        }
        // TermPanel
        {
            terminalPanel = new TermPanel();
            terminalPanel.initGUI();
            this.add(terminalPanel, BorderLayout.CENTER);
        }
        // MenuBar
        {
            menu = this.createMenuBar();
            this.setJMenuBar(menu);
        }
        // Apply loaded config settings to CharPane !
        this.applyGraphicConfigSettings();

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

        this.pack();
    }

    /**
     * Apply current configured properties to CharPane. Does reset graphics !
     */
    void applyGraphicConfigSettings() {
        // update configuration: 
        String propStr = getStringProperty(VTERM_SYNC_SCROLLING);
        if (propStr != null) {
            boolean val = Boolean.parseBoolean(propStr);
            this.optionsSyncScrolling.setSelected(val);
            this.terminalPanel.setSynchronizedScrolling(val);
        }

        // directly set charPane options: do not reset graphics each time !
        propStr = getStringProperty(VTERM_COLOR_SCHEME);
        if (propStr != null)
            this.terminalPanel.updateColorMap(ColorMap.getColorMap(propStr), false);

        propStr = getStringProperty(VTERM_FONT_TYPE);
        if (propStr != null)
            this.terminalPanel.updateFontType(propStr, false);

        propStr = getStringProperty(VTERM_FONT_SIZE);
        if (propStr != null)
            this.terminalPanel.updateFontSize(Integer.parseInt(propStr), false);

        propStr = getStringProperty(VTERM_FONT_ANTI_ALIASING);

        if (propStr != null) {
            boolean val = Boolean.parseBoolean(propStr);
            this.terminalPanel.getFontInfo().setAntiAliasing(val);
            this.fontAAcheckBox.setSelected(val);
        }

        this.terminalPanel.resetGraphics();
    }

    public TermChannelOptions getChannelOptions(String type, boolean autoCreate) {

        TermChannelOptions options = termProvider.getChannelOptions(type);
        if ((autoCreate) && (options == null)) {
            options = new TermChannelOptions();
            options.setTermType((String) this.properties().get(VTermConst.VTERM_TERM_TYPE));
        }

        options.setDefaultSize(this.terminalPanel.getRowCount(), this.terminalPanel.getColumnCount());
        return options;
    }

    public void setChannelOptions(String type, TermChannelOptions options) {
        termProvider.setChannelOptions(type, options);
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
        startBashMenuItem = new JMenuItem("Start BASH Session...");
        menu.add(startBashMenuItem);
        startBashMenuItem.addActionListener(termController);
        startBashMenuItem.setActionCommand(SESSION_BASH);
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

            /*
             * mitem=new JMenuItem("HTTP...");
             *
             * mitem.addActionListener(this); mitem.setActionCommand("HTTP");
             * menu.add(mitem); mitem=new JMenuItem("SOCKS5...");
             * mitem.addActionListener(this); mitem.setActionCommand("SOCKS5");
             * menu.add(mitem); menubar.add(menu);
             */
            {
                TermChannelOptions sshOptions = getChannelOptions(SESSION_SSH, true);

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
                channelCompressionCB.setState(sshOptions.getBooleanOption(TermConst.SSH_CHANNEL_COMPRESSION, false));

                sshmenu.add(channelCompressionCB);

                {
                    JMenu xForwardingMenu = new JMenu("X Forwarding");
                    sshmenu.add(xForwardingMenu);

                    sshXForwardingCB = new JCheckBoxMenuItem("enable X Forwarding");
                    sshXForwardingCB.addActionListener(termController);
                    sshXForwardingCB.setActionCommand(ACTION_XFORWARDING_ENABLE);
                    sshXForwardingCB.setState(sshOptions.getBooleanOption(TermConst.SSH_XFORWARDING_ENABLED, false));
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
                            "Luxi Mono", "Liberation Mono", "DejaVu Sans Mono",
                            "Lucida Sans Typewriter", "Andale Mono", "Impact"};

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
            // Font-Anti aliasing
            {
                optionsSyncScrolling = new JCheckBoxMenuItem("Synchronize Scrolling");
                optionsSyncScrolling.addActionListener(termController);
                optionsSyncScrolling.setActionCommand("syncScrolling");
                optionsSyncScrolling.setState(terminalPanel.getSynchronizedScrolling());
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

    //    public void updateMenuSettings()
    //    {
    //        getChannelOptions().useChannelCompression = compressionCheckBoxMenuItem.getState();
    //        getChannelOptions().userChannelXForwarding  = x11CheckBoxMenuItem.getState();
    //    }

    public void setTitle(String str) {
        super.setTitle("VTerm:" + str);
    }

    private void exit(int i) {
        dispose();
    }

    @Override
    public void dispose() {
        terminateSession();
        super.dispose();

        if (terminalPanel != null)
            this.terminalPanel.dispose();

        this.terminalPanel = null;
    }

    // after change of graphics: repack main Frame.

    void updateFrameSize() {
        Runnable update = new Runnable() {
            public void run() {
                terminalPanel.setSize(terminalPanel.getPreferredSize());
                setSize(getPreferredSize());
                pack();
            }
        };

        SwingUtilities.invokeLater(update);
    }

    public void actionPerformed(ActionEvent e) {
        String action = e.getActionCommand();
        Component source = (Component) e.getSource();

        //ChannelOptions options = this.getChannelOptions(); 

        if (action.compareTo(SESSION_BASH) == 0) {
            this.sessionType = SESSION_BASH;
            startSession();
        } else if (action.compareTo(SESSION_TELNET) == 0) {
            if (thread == null) {
                this.sessionType = SESSION_TELNET;
                String str = new Dialogs(this).askInput("Provide details", "Enter username@hostname");
                parseLocation("telnet", str);
                startSession();
            }
        } else if (action.compareTo(SESSION_SSH) == 0) {
            if (thread == null) {
                this.sessionType = SESSION_SSH;

                String sessionstr = this.getConfigProperty("vterm.last.session.ssh");

                if (sessionstr == null) {
                    sessionstr = startURI.toString();
                }

                String locstr = JOptionPane.showInputDialog(this, "Enter:<username>@<hostname>",
                        sessionstr);

                if (locstr == null)
                    return;

                this.savePersistantProperty("vterm.last.session.ssh", locstr);
                parseLocation("ssh", locstr);
                startSession();

            }
        } else if (action.equals(ACTION_XFORWARDING_ENABLE)) {
            this.sshXForwardingConfig.setEnabled(sshXForwardingCB.getState());
        } else if (action.equals(ACTION_XFORWARDING_CONFIG)) {
            TermChannelOptions sshOptions = this.getChannelOptions(SESSION_SSH, true);


            String xForwardingHost = sshOptions.getOptions().get(TermConst.SSH_XFORWARDING_HOST);
            int xForwardingPort = getIntOption(sshOptions.getOptions(), TermConst.SSH_XFORWARDING_PORT, -1);

            String display = JOptionPane
                    .showInputDialog(
                            this,
                            "XDisplay name (hostname:0)",
                            (xForwardingHost == null) ? ""
                                    : (xForwardingHost + ":" + xForwardingPort));

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
            setChannelOptions(SESSION_SSH, sshOptions);

        } else if (action.startsWith("fontsize")) {
            String[] strs = action.split("-");

            if (strs.length > 1 && strs[1] != null) {
                Integer val = Integer.valueOf(strs[1]);
                this.terminalPanel.updateFontSize(val, true);

                this.properties.setProperty("vterm.font.size", "" + val);
            }

            terminalPanel.repaintGraphics(false);
            updateFrameSize();
        } else if (action.startsWith("fonttype")) {
            String[] strs = action.split("-");

            if (strs.length > 1 && strs[1] != null) {
                this.terminalPanel.updateFontType(strs[1], true);
                this.properties.setProperty("vterm.font.type", "" + strs[1]);
            }

            terminalPanel.repaintGraphics(false);
            updateFrameSize();
        } else if (action.startsWith("colorscheme")) {
            String[] strs = action.split("-");

            if (strs.length > 1 && strs[1] != null) {
                String name = strs[1];
                terminalPanel.updateColorMap(ColorMap.getColorMap(name), true);
                this.properties.setProperty("vterm.color.scheme", name);
            }

            updateFrameSize();
        } else if (action.startsWith("syncScrolling")) {
            boolean state = this.optionsSyncScrolling.getState();
            terminalPanel.setSynchronizedScrolling(state);
            this.properties.setProperty("vterm.syncScrolling", "" + state);
        } else if (action.compareTo("font-aa") == 0) {
            boolean val = fontAAcheckBox.getState();
            terminalPanel.getFontInfo().setAntiAliasing(val);
            this.properties.setProperty("vterm.font.antiAliasing", "" + val);

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
            this.menuTypeVt100CB.setSelected(true);
            this.menuTypeXtermCB.setSelected(false);
            this.menuTypeXterm256.setSelected(false);
            this.termType = TermConst.TERM_VT100;
            this.setTermType(this.termType);
        } else if (source == this.menuTypeXtermCB) {
            this.menuTypeVt100CB.setSelected(false);
            this.menuTypeXtermCB.setSelected(true);
            this.menuTypeXterm256.setSelected(false);
            this.termType = TermConst.TERM_XTERM;
            this.setTermType(this.termType);
        } else if (source == this.menuTypeXterm256) {
            this.menuTypeVt100CB.setSelected(false);
            this.menuTypeXtermCB.setSelected(false);
            this.menuTypeXterm256.setSelected(true);
            this.termType = TermConst.XTERM_256COLOR;
            this.setTermType(this.termType);

        } else if (action.equals("SaveSettings")) {
            saveSettings();
        }
    }

    private int getIntOption(Map<String, String> options, String key, int defaultVal) {
        String value = options.get(key);
        if (value==null) {
            return defaultVal;
        }
        return Integer.parseInt(value);
    }


    protected void openLocation(URI loc) throws IOException {
        // parse URI: 

        String scheme = loc.getScheme();
        String host = loc.getHost();

        if (StringUtil.isEmpty(host)) {
            host = "localhost";
        }

        String user = loc.getUserInfo();

        if (StringUtil.isEmpty(user)) {
            user = SysEnv.sysEnv().getUserName();
        }

        int port = loc.getPort();
        if (port <= 0)
            port = 22;

        String path = loc.getPath();

        try {
            startURI = new URI(scheme, user, host, port, path, null, null);
        } catch (URISyntaxException e) {
            throw new IOException(e.getMessage(), e);
        }

        this.startSession();
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

            this.startURI = new URI(scheme, user, host, port, null, null, null);
        } catch (Exception e) {
            this.showMessage("Invalid Location. Must be user@host[:port]. Value=" + locstr);
        }
    }

    /**
     * Creates new Thread which invokes run() method.
     */
    public void startSession() {
        this.thread = new Thread(this);
        this.thread.start();

        this.menuUpdateSessionAlive(true);
    }

    private void menuUpdateSessionAlive(boolean val) {
        sessionAlive = val;

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

    /**
     * Start session, will only return when session has ended
     */
    public void run() {
        executeSession();
    }

    protected void executeSession() {
        // ================
        // PRE SESSION !!!
        // ================
        if (startURI == null) {
            throw new NullPointerException("Start URI is NULL!");
        }

        InputStream inps = null;
        OutputStream outps = null;
        InputStream errs = null;

        // Complete Reset ! 
        terminalPanel.reset();

        log.info(">>> Starting Session Type={}", sessionType);
        this.sessionAlive = true;

        if (this.sessionType.compareTo(SESSION_TELNET) == 0) {
            try {
                Socket sock = new Socket(startURI.getHost(), startURI.getPort());
                inps = sock.getInputStream();
                outps = sock.getOutputStream();

                // telnet vt100 identification
                // DOES NOT WORK !
                byte IAC = (byte) 255;
                byte WILL = (byte) 251;
                byte SB = (byte) 250;
                byte SE = (byte) 240;

                //
                // DOES NOT WORK:
                //

                byte[] bytes = {
                        // terminal type
                        IAC, WILL, (byte) 24, IAC, SB, (byte) 24, (byte) 0, 'V', 'T', '1', '0',
                        '0', IAC, SE,
                        // terminal speed
                        IAC, WILL, (byte) 32, IAC, SB, (byte) 32, (byte) 0, '9', '6', '0', '0',
                        ',', '9', '6', '0', '0', IAC, SE};

                outps.write(bytes);

                VTxEmulator emulator = new VTxEmulator(this.terminalPanel.getCharacterTerminal(),
                        inps, outps);
                // emulator.setErrorInput(errs);
                emulator.addListener(this.termController);

                terminalPanel.setEmulator(emulator);
                terminalPanel.requestFocus();
                // start input/ouput loop (method will not return)
                emulator.start();
                // exit

                // done:
                sock.close();
            } catch (Exception | Error e) {
                log.error("Exception:" + e.getMessage(), e);
            }

        } else if (this.sessionType.compareTo(SESSION_BASH) == 0) {

            try {

                TermChannelOptions options = this.getChannelOptions(SESSION_BASH, true);
                this.shellChannel = this.termProvider.createChannel(SESSION_BASH, null, null, null,
                        options, this.termController);
                shellChannel.connect();

                VTxEmulator emulator = new VTxEmulator(this.terminalPanel.getCharacterTerminal(),
                        shellChannel.getStdout(), shellChannel.getStdin());

                emulator.setErrorInput(errs);
                emulator.addListener(this.termController);
                // emulator.setErrorInput(errs);

                this.terminalPanel.setEmulator(emulator);
                terminalPanel.requestFocus();

                startShellProcessWatcher(shellChannel, emulator);

                // start input/output loop (method will not return)
                emulator.start();
            } catch (Exception ex) {
                log.error("Could not start bash. Got exception:" + ex.getMessage(), ex);
                showError(ex);
            }
            // if (bashChannel != null)
            //     bashChannel.disconnect();

        } else if (this.sessionType.compareTo(SESSION_SSH) == 0) {
            TermChannelOptions options = this.getChannelOptions(SESSION_SSH, true);

            try {
                // ================================
                // Only here is SSHChannel visible!
                // ================================

                this.shellChannel = termProvider.createChannel(SESSION_SSH, startURI,
                        startURI.getUserInfo(), null, options, termController);
                shellChannel.connect();

                VTxEmulator emulator = new VTxEmulator(this.terminalPanel.getCharacterTerminal(),
                        shellChannel.getStdout(), shellChannel.getStdin());

                this.terminalPanel.setEmulator(emulator);
                emulator.addListener(this.termController);

                // set focus to terminal panel:
                this.terminalPanel.requestFocus();

                emulator.reset();
                // start input/output loop (method will not return)
                emulator.start();

            } catch (Exception | Error e) {
                log.error("Exception:" + e.getMessage(), e);
                showError(e);
            }
        } else if (this.sessionType.compareTo(SESSION_SHELLCHANNEL) == 0) {
            try {
                // ================================
                // Use external shell channel
                // ================================
                if (this.shellChannel == null)
                    throw new IOException("No Shell Channel specified!");
                // shellChannel.connect();

                VTxEmulator emulator = new VTxEmulator(this.terminalPanel.getCharacterTerminal(),
                        shellChannel.getStdout(), shellChannel.getStdin());

                this.terminalPanel.setEmulator(emulator);
                emulator.addListener(this.termController);

                // set focus to terminal panel:
                this.terminalPanel.requestFocus();

                emulator.reset();
                // start input/ouput loop (method will not return)
                emulator.start();

            } catch (Exception | Error e) {
                log.error("Exception:" + e.getMessage(), e);
                showError(e);
            }
        }
        // ================
        // POST SESSION !!!
        // ================

        log.info("*** Session Ended: emulator stopped.  ***");

        terminateSession(); // terminate if still running 
        menuUpdateSessionAlive(false);
        repaint();
        showMessage("Session Ended");
    }

    /**
     * Watches a shell process and signals emulator when shell process died.
     */
    private void startShellProcessWatcher(final ShellChannel shellProc, final Emulator shellEmu) {
        Runnable run = new Runnable() {
            public void run() {
                int val = 0;

                try {
                    shellProc.waitFor();
                    val = shellProc.exitValue();
                } catch (InterruptedException e) {
                    log.error("InterruptedException:" + e.getMessage(), e);
                }

                if (val == 0)
                    log.info("BASH Stopped normally. Exit value is 0.");
                else
                    log.error("*** Bash died abnormally. Exit value={}", val);

                shellEmu.signalTerminate();
            }
        };

        Thread procWatcher = new Thread(run);
        procWatcher.start();
    }

    private void showError(Throwable e) {
        new Dialogs(this).showException(e);
    }

    protected void setShellChannel(ShellChannel chan) {
        this.sessionType = SESSION_SHELLCHANNEL;
        this.shellChannel = chan;
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

        if (getEmulator() != null) {
            this.getEmulator().signalTerminate();
        }

        // Terminal;
        if (this.terminalPanel != null) {
            this.terminalPanel.terminate();
        }

        // Shell Channel:
        try {
            if (this.shellChannel != null) {
                this.shellChannel.disconnect(false);
            }
        } catch (IOException e) {
            log.warn("IOException during disconnect():" + e.getMessage(), e);
        }

        this.shellChannel = null;
        thread = null;
    }

    /**
     * Sends emulator the new preferred size. Depends on implementation whether this will be
     * respected
     *
     * @param nr_columns
     * @param nr_rows
     */
    public void sendTermSize(int nr_columns, int nr_rows) {
        log.debug("sendTermSize(:[{},{}]", nr_columns, nr_rows);
        boolean channelSupported = false;
        boolean emulatorSupported = false;

        try {
            if (shellChannel != null)
                channelSupported = this.shellChannel.setPtyTermSize(nr_columns, nr_rows, nr_columns, nr_rows);
        } catch (IOException e) {
            log.error("IOException: Couldn't send (pty)terminal size:" + e.getMessage(), e);
        }

        if (channelSupported) {
            // update size and reset region:
            this.getEmulator().updateRegion(nr_columns, nr_rows, 0, nr_rows);
        } else {
            // Try control sequences, this is terminal implementation specific!
            if (this.getEmulator() != null) {
                try {
                    emulatorSupported = this.getEmulator().sendSize(nr_columns, nr_rows);
                } catch (IOException e) {
                    log.error("IOException: Couldn't send emulator size:" + e.getMessage(), e);
                }
            }
        }

        if (!channelSupported && !emulatorSupported) {
            log.warn("Nor channel nor emulator were able to set new size/region to:[{},{}]!", nr_columns, nr_rows);
        }
    }

    /**
     * Sends getEmulator() the new preferred size. Depends on implemenation whether this will be
     * respected
     */
    public void setTermType(String type) {
        this.properties().put(VTERM_TERM_TYPE, type);
        try {
            if (shellChannel != null)
                this.shellChannel.setPtyTermType(type);

        } catch (IOException e) {
            log.error("IOException: Couldn't send terminal type" + type + ":" + e.getMessage(), e);
        }

        // update getEmulator()
        if (this.getEmulator() != null) {
            this.getEmulator().setTermType(type);
        }

    }

    String getConfigProperty(String name) {
        if (properties == null) {
            properties = loadConfigProperties();

            if (properties == null)
                return null;
        }

        Object val = properties.getProperty(name);
        if (val != null)
            return val.toString();

        return null;
    }

    protected Properties loadConfigProperties() {
        try {
            Path propFileUri = getPropertiesFile();
            return SysEnv.sysEnv().loadProperties(propFileUri,true);
        } catch (Exception e) {
            log.warn("Exception: Couldn't load config vterm.prop:" + e.getMessage(), e);
            return new Properties();
        }
    }

    /**
     * save persistant property
     */
    protected void savePersistantProperty(String name, String value) {
        if (properties == null) {
            properties = loadConfigProperties();
        }
        properties.put(name, value);
        saveConfig();
    }

    public Path getPropertiesDir() throws IOException {
        return SysEnv.sysEnv().resolveUserHomePath(".config/vterm");
    }

    public Path getPropertiesFile() throws IOException {
        return SysEnv.sysEnv().resolveUserHomePath(".config/vterm/vterm.prop");
    }

    void saveConfig() {
        URI propFileUri = null;

        try {
            SysEnv.sysEnv().sysFS().mkdirs(getPropertiesDir());
            Path propFile = getPropertiesFile();
            SysEnv.sysEnv().saveProperties(propFile, properties,
                    "VTerm Properties");
        } catch (Exception e) {
            log.warn("Exception: Couldn't write config file:'" + propFileUri + "':" + e.getMessage(), e);
        }

    }

    /**
     * Save current (view) settings
     */
    void saveSettings() {
        if (properties == null) {
            properties = loadConfigProperties();
        }
        saveConfig();
    }

    private void setLogLevelToDebug() {
        log.warn("***Fixme:setLogLevelToDebug()");
    }

    private void setLogLevelToError() {
        log.warn("***Fixme:setLogLevelToError()");
    }

    public TermPanel getTerminalPanel() {
        return this.terminalPanel;
    }

    protected Properties properties() {
        return this.properties;
    }

    String getStringProperty(String key) {
        Object val = properties.get(key);
        if (val != null) {
            return val.toString();
        }
        return null;
    }

}
