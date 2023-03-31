/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.ui;

import lombok.extern.slf4j.Slf4j;
import nl.piter.vterm.api.*;
import nl.piter.vterm.emulator.Emulator;
import nl.piter.vterm.sys.SysEnv;
import nl.piter.vterm.ui.panels.Dialogs;
import nl.piter.vterm.ui.panels.VTermPanel;
import nl.piter.vterm.ui.panels.charpane.ColorMap;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static nl.piter.vterm.emulator.Util.isEmpty;
import static nl.piter.vterm.emulator.Util.splitQuotedArgs;
import static nl.piter.vterm.ui.VTermSessionManager.*;
import static nl.piter.vterm.ui.VTermSessionManager.SESSION_SSH;

/**
 * VTermController: UI Controller for: { VTermFrame, CharacterTerminal }
 */
@Slf4j
public class VTermController implements WindowListener, EmulatorListener,
        ActionListener, TermUI {

    private final VTermFrame vtermFrame;
    private String shortTitle;
    private String longTitle;

    public VTermController(VTermFrame vtermFrame) {
        this.vtermFrame = vtermFrame;
    }

    public void notifyTermTitle(int type, String arg) {
        if (type == 1)
            this.shortTitle = arg;
        else
            this.longTitle = arg;

        if (shortTitle == null)
            shortTitle = "";

        if (longTitle == null)
            longTitle = "";

        StringBuilder sb = new StringBuilder();
        if (!isEmpty(shortTitle)) {
            sb.append("[" + shortTitle + "] ");
        }
        if (!isEmpty(longTitle)) {
            sb.append(longTitle);
        }
        vtermFrame.setTitle(sb.toString());
        vtermFrame.statusPanel().setStatus(shortTitle + ":" + longTitle);
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
        terminalPanel().activate(); // charPane.startRenderers();
    }

    public void windowIconified(WindowEvent e) {
        terminalPanel().inactivate(); // charPane.stopRenderers();
    }

    public void windowOpened(WindowEvent e) {
        terminalPanel().activate(); // charPane.startRenderers();
    }

    public void notifyResized(int columns, int rows) {
        // This is a notification from the Emulator, Not AWT:
        vtermFrame.statusPanel().setStatusSize(columns, rows);
        // Will revalidate and update size from VTermPanel -> CharPane.
        vtermFrame.updateFrameSize();
    }

    @Override
    public void showMessage(String message) {
        vtermFrame.showMessage(message);
    }

    @Override
    public boolean askConfirmation(String message, DialogOption option) {
        if (option == DialogOption.DIALOG_YES_NO) {
            return new Dialogs(vtermFrame).askYesNo(message);
        } else {
            return new Dialogs(vtermFrame).askOkCancel(message);
        }
    }

    @Override
    public String askInput(String message, String type, boolean isSecret) {
        if (!isSecret) {
            return new Dialogs(vtermFrame).askInput("" + type, message);
        } else {
            return new Dialogs(vtermFrame).askForSecret("" + type, message);
        }
    }

    public void showError(Throwable e) {
        new Dialogs(vtermFrame).showException(e);
    }


    public void actionPerformed(ActionEvent e) {
        String action = e.getActionCommand();
        Component source = (Component) e.getSource();

        if (action.compareTo(SESSION_PTY) == 0) {

            TermChannelOptions options = sessionManager().getChannelOptions(SESSION_PTY, true);

            String cmd;
            String args;

            cmd = sessionManager().getConfigProperty(VTermConst.VTERM_SESSION_LAST_SHELL_CMD);
            args = sessionManager().getConfigProperty(VTermConst.VTERM_SESSION_LAST_SHELL_ARGS);

            if (isEmpty(cmd)) {
                cmd = "/bin/bash";
            }
            if (isEmpty(args)) {
                cmd = "-i";
            }

            String[] fields = new String[]{"Shell path (no arguments)", "Arguments"};
            String[] values = new String[]{cmd, args};
            String[] results = Dialogs.askInputDialog(vtermFrame, "Argument (list)", fields, values);

            if ((results == null) || (results.length == 0)) {
                return;
            }

            cmd = results[0];
            List<String> argList = splitQuotedArgs(results[1]);
            argList.add(0, cmd);
            options.setCommand(argList.toArray(new String[0]));

            sessionManager().storeChannelOptions(SESSION_PTY, options);
            sessionManager().saveConfigProperty(VTermConst.VTERM_SESSION_LAST_SHELL_CMD, cmd);
            sessionManager().saveConfigProperty(VTermConst.VTERM_SESSION_LAST_SHELL_ARGS, args);
            sessionManager().setSessionType(SESSION_PTY);
            sessionManager().startSession();

        } else if (action.compareTo(SESSION_TELNET) == 0) {
            if (!sessionManager().isRunning()) {
                sessionManager().setSessionType(SESSION_TELNET);
                String str = new Dialogs(vtermFrame).askInput("Provide details", "Enter username@hostname");
                parseLocation("telnet", str);
                sessionManager().startSession();
            }
        } else if (action.compareTo(SESSION_SSH) == 0) {

            if (sessionManager().isRunning()) {
                termController().showMessage("Session is still running!");
            } else {
                sessionManager().setSessionType(SESSION_SSH);

                String sessionstr = sessionManager().getConfigProperty(VTermConst.VTERM_SESSION_LAST_URI_SSH);

                if (sessionstr == null) {
                    if (sessionManager().getStartURI() != null)
                        sessionstr = sessionManager().getStartURI().toString();
                    else
                        sessionstr = "user@localhost";
                }

                String locstr = JOptionPane.showInputDialog(vtermFrame, "Enter:<username>@<hostname>",
                        sessionstr);

                if (locstr == null)
                    return;

                sessionManager().saveConfigProperty(VTermConst.VTERM_SESSION_LAST_URI_SSH, locstr);
                parseLocation("ssh", locstr);
                sessionManager().startSession();
            }
        } else if (action.equals(vtermFrame.ACTION_XFORWARDING_ENABLE)) {
            vtermFrame.sshXForwardingConfig.setEnabled(vtermFrame.sshXForwardingCB.getState());
        } else if (action.equals(vtermFrame.ACTION_XFORWARDING_CONFIG)) {
            TermChannelOptions sshOptions = sessionManager().getChannelOptions(SESSION_SSH, true);


            String xForwardingHost = sshOptions.getOption(TermConst.SSH_XFORWARDING_HOST);
            int xForwardingPort = sshOptions.getIntOption(TermConst.SSH_XFORWARDING_PORT, -1);

            String display = JOptionPane.showInputDialog(vtermFrame, "XDisplay name (hostname:0)",
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
            sessionManager().setChannelOptions(SESSION_SSH, sshOptions);

        } else if (action.startsWith("fontsize")) {
            String[] strs = action.split("-");

            if (strs.length > 1 && strs[1] != null) {
                Integer val = Integer.valueOf(strs[1]);
                this.terminalPanel().updateFontSize(val, true);
                this.sessionManager().setProperty(VTermConst.VTERM_FONT_SIZE, "" + val);
            }

            terminalPanel().repaintGraphics(false);
            vtermFrame.updateFrameSize();
        } else if (action.startsWith("fonttype")) {
            String[] strs = action.split("-");

            if (strs.length > 1 && strs[1] != null) {
                this.terminalPanel().updateFontType(strs[1], true);
                this.sessionManager().setProperty(VTermConst.VTERM_FONT_TYPE, "" + strs[1]);
            }

            terminalPanel().repaintGraphics(false);
            vtermFrame.updateFrameSize();
        } else if (action.startsWith("colorscheme")) {
            String[] strs = action.split("-");

            if (strs.length > 1 && strs[1] != null) {
                String name = strs[1];
                terminalPanel().updateColorMap(ColorMap.getColorMap(name), true);
                this.sessionManager().setProperty(VTermConst.VTERM_COLOR_SCHEME, name);
            }

            vtermFrame.updateFrameSize();
        } else if (action.startsWith("syncScrolling")) {
            boolean state = vtermFrame.optionsSyncScrolling.getState();

            this.sessionManager().setSlowScrolling(state);
        } else if (action.compareTo("font-aa") == 0) {
            boolean val = vtermFrame.fontAAcheckBox.getState();
            terminalPanel().getFontInfo().setAntiAliasing(val);
            this.sessionManager().setProperty(VTermConst.VTERM_FONT_ANTI_ALIASING, "" + val);

            terminalPanel().repaintGraphics(false);
            vtermFrame.updateFrameSize();
        } else if (action.equals("Close")) {
            terminateSession();
        } else if (action.equals("Repaint")) {
            terminalPanel().repaintGraphics(false);
        } else if (action.equals("ClearScreen")) {
            terminalPanel().clearAll();
        } else if (action.equals("Quit")) {
            exit(0);
        } else if (action.equals("DebugON")) {
        } else if (action.equals("DebugOFF")) {
        } else if (action.equals("About")) {
            this.showMessage(vtermFrame.aboutText);
        } else if (action.equals("testscreen")) {
            terminalPanel().drawTestScreen();
        } else if (source == vtermFrame.menuTypeVt100CB) {
            // radiobuttons...
            vtermFrame.menuTypeVt100CB.setSelected(true);
            vtermFrame.menuTypeXtermCB.setSelected(false);
            vtermFrame.menuTypeXterm256.setSelected(false);
            sessionManager().setTermType(TermConst.TERM_VT100);
        } else if (source == vtermFrame.menuTypeXtermCB) {
            vtermFrame.menuTypeVt100CB.setSelected(false);
            vtermFrame.menuTypeXtermCB.setSelected(true);
            vtermFrame.menuTypeXterm256.setSelected(false);
            sessionManager().setTermType(TermConst.TERM_XTERM);
        } else if (source == vtermFrame.menuTypeXterm256) {
            vtermFrame.menuTypeVt100CB.setSelected(false);
            vtermFrame.menuTypeXtermCB.setSelected(false);
            vtermFrame.menuTypeXterm256.setSelected(true);
            sessionManager().setTermType(TermConst.XTERM_256COLOR);

        } else if (action.equals("SaveSettings")) {
            sessionManager().saveSettings();
        }
    }

    private VTermController termController() {
        return this;
    }

    private VTermPanel terminalPanel() {
        return vtermFrame.vtermPanel();
    }

    private int getIntOption(Map<String, String> options, String key, int defaultVal) {
        String value = options.get(key);
        if (value == null) {
            return defaultVal;
        }
        return Integer.parseInt(value);
    }

//    protected void openLocation(String sessionType, URI loc) throws IOException {
//        // parse URI:
//
//        String scheme = loc.getScheme();
//        String host = loc.getHost();
//
//        if (isEmpty(host)) {
//            host = "localhost";
//        }
//
//        String user = loc.getUserInfo();
//
//        if (isEmpty(user)) {
//            user = SysEnv.sysEnv().getUserName();
//        }
//
//        int port = loc.getPort();
//        if (port <= 0)
//            port = 22;
//
//        String path = loc.getPath();
//
//        try {
//            sessionManager().setSessionType(sessionType);
//            sessionManager().setStartURI(new URI(scheme, user, host, port, path, null, null));
//        } catch (URISyntaxException e) {
//            throw new IOException(e.getMessage(), e);
//        }
//
//        sessionManager().startSession();
//    }

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

            sessionManager().setStartURI(new URI(scheme, user, host, port, null, null, null));
        } catch (Exception e) {
            this.showMessage("Invalid Location. Must be user@host[:port]. Value=" + locstr);
        }
    }


    public VTermSessionManager sessionManager() {
        return this.vtermFrame.sessionManager();
    }

    public void emulatorStarted() {
        vtermFrame.menuUpdateSessionAlive(true);
    }

    public void emulatorStopped() {
        terminateSession(); // terminate if still running
        vtermFrame.menuUpdateSessionAlive(false);
        vtermFrame.repaint();
        showMessage("Session Ended");
    }

    protected Emulator getEmulator() {
        if (terminalPanel() == null) {
            return null;
        }
        return terminalPanel().getEmulator();
    }

    public void exit(int value) {

        terminateSession();
        vtermFrame.setVisible(false);
        vtermFrame.dispose();
    }

    public void terminateSession() {

        // Terminal;
        if (this.terminalPanel() != null) {
            this.terminalPanel().terminate();
        }

        // Emulator:
        if (this.getEmulator() != null) {
            this.getEmulator().signalTerminate();
        }

        // Shell Channel:
        try {
            sessionManager().terminate();
        } catch (IOException e) {
            log.warn("IOException during disconnect():" + e.getMessage(), e);
        }

        vtermFrame.menuUpdateSessionAlive(false);
    }

}
