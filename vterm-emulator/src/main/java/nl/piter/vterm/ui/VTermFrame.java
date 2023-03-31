/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.ui;

import lombok.extern.slf4j.Slf4j;
import nl.piter.vterm.api.TermChannelOptions;
import nl.piter.vterm.api.TermConst;
import nl.piter.vterm.emulator.VTermChannelProvider;
import nl.piter.vterm.ui.panels.StatusBar;
import nl.piter.vterm.ui.panels.VTermPanel;
import nl.piter.vterm.ui.panels.charpane.ColorMap;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static java.awt.event.KeyEvent.VK_F10;
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

    protected static final String aboutText = ""
            + "<html><center>VTerm VT100+/xterm Emulator<br>"
            + "(beta version)<br>"
            + "(C) Piter.NL<br>"
            + "Author Piter T. de Boer<br>"
            + "Render Engine (C) Piter.NL</center></html>";
    private static final int verbose = 0; // 0=silent, 1 = error and fixmes,2=warn,3=info,4=debug,5=very debug,6=trace

    // ========================================================================

    private final boolean saveConfigEnabled = true;

    // Controllers
    private VTermSessionManager sessionManager;
    private VTermController termController;

    // === GUI === //
    private VTermPanel terminalPanel;

    // Components:
    private JMenuBar menu;
    protected JCheckBoxMenuItem sshXForwardingCB;
    protected JMenuItem sshXForwardingConfig;
    private JCheckBoxMenuItem channelCompressionCB;
    protected JCheckBoxMenuItem fontAAcheckBox;
    private JMenuItem startBashMenuItem;
    private JMenuItem closeSessionMenuItem;
    private JMenuItem startSSHMenuItem;
    protected JCheckBoxMenuItem menuTypeVt100CB;
    protected JCheckBoxMenuItem menuTypeXtermCB;
    protected JCheckBoxMenuItem menuTypeXterm256;
    protected JCheckBoxMenuItem optionsSyncScrolling;
    protected StatusBar statusPanel;
    protected JPanel snackBar;
    //

    public VTermFrame(VTermChannelProvider termProvider) {
        // gui uses loaded settings !
        initTermController();
        initGui();
        initSessionManager(termProvider);
        this.applyGraphicConfigSettings();
        this.pack();
    }

    protected void initTermController() {
        this.termController = new VTermController(this);
    }

    public void initGui() {
        // JFrame
        this.setTitle("VTerm");
        this.setLayout(new BorderLayout());

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
                    controller().exit(0);
                }
            });
            this.addWindowListener(termController);
        }
    }

    private void initSessionManager(VTermChannelProvider termProvider) {
        // Controllers:
        this.sessionManager = new VTermSessionManager(termController, termController, termProvider, terminalPanel);
    }

    /**
     * Apply current configured properties to CharPane. Does reset graphics !
     */
    void applyGraphicConfigSettings() {
        // update from configuration:
        String propStr = sessionManager.getProperty(VTERM_SYNC_SLOW_SCROLLING);
        if (propStr != null) {
            boolean val = Boolean.parseBoolean(propStr);
            this.optionsSyncScrolling.setSelected(val);
        }
        // Directly set charPane options: do not reset graphics each time !
        propStr = sessionManager.getProperty(VTERM_COLOR_SCHEME);
        if (propStr != null)
            this.terminalPanel.updateColorMap(ColorMap.getColorMap(propStr), false);

        propStr = sessionManager.getProperty(VTERM_FONT_TYPE);
        if (propStr != null)
            this.terminalPanel.updateFontType(propStr, false);

        propStr = sessionManager.getProperty(VTERM_FONT_SIZE);
        if (propStr != null)
            this.terminalPanel.updateFontSize(Integer.parseInt(propStr), false);

        propStr = sessionManager.getProperty(VTERM_FONT_ANTI_ALIASING);
        if (propStr != null) {
            boolean val = Boolean.parseBoolean(propStr);
            this.terminalPanel.getFontInfo().setAntiAliasing(val);
            this.fontAAcheckBox.setSelected(val);
        }

        propStr = sessionManager.getProperty(VTERM_TERM_TYPE);
        menuTypeVt100CB.setState(TermConst.TERM_VT100.equals(propStr));
        menuTypeXtermCB.setState(TermConst.TERM_XTERM.equals(propStr));
        menuTypeXterm256.setState(TermConst.XTERM_256COLOR.equals(propStr));
        this.terminalPanel.resetGraphics();

        optionsSyncScrolling.setState(sessionManager.getSlowScrolling());

        TermChannelOptions sshOptions = sessionManager.getChannelOptions(SESSION_SSH, true);
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
                    xForwardingMenu.setEnabled(false);

                    sshXForwardingCB = new JCheckBoxMenuItem("enable X Forwarding");
                    sshXForwardingCB.addActionListener(termController);
                    sshXForwardingCB.setActionCommand(ACTION_XFORWARDING_ENABLE);
                    sshXForwardingCB.setEnabled(false);
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
                            "Luxi Mono", "Liberation Mono", "DejaVu Sans Mono", "Bera Sans Mono"};//"TXMIA"};

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
                mitem.setVisible(saveConfigEnabled);
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

    void showSplash() {
        this.terminalPanel.drawSplash();
    }

    // after change of graphics: repack main Frame.
    public void updateFrameSize() {
        Runnable update = new Runnable() {
            public void run() {
                validate();
                pack();
            }
        };

        SwingUtilities.invokeLater(update);
    }

    protected void menuUpdateSessionAlive(boolean val) {
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

    @Override
    public void dispose() {
        if (terminalPanel != null) {
            this.terminalPanel.dispose();
        }
        this.terminalPanel = null;
        super.dispose();
    }

    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public StatusBar statusPanel() {
        return statusPanel;
    }

    public VTermSessionManager sessionManager() {
        return this.sessionManager;
    }

    public VTermPanel vtermPanel() {
        return this.terminalPanel;
    }

    public VTermController controller() {
        return this.termController;
    }

}
