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
import nl.piter.vterm.emulator.VTermChannelProvider;
import nl.piter.vterm.emulator.VTxEmulator;
import nl.piter.vterm.sys.SysEnv;
import nl.piter.vterm.ui.panels.VTermPanel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Properties;

import static nl.piter.vterm.emulator.Util.isEmpty;
import static nl.piter.vterm.ui.VTermConst.VTERM_SYNC_SLOW_SCROLLING;
import static nl.piter.vterm.ui.VTermConst.VTERM_TERM_TYPE;

@Slf4j
public class VTermSessionManager implements Runnable {

    public static final String SESSION_SSH = "SSH";
    public static final String SESSION_PTY = "PTY";
    public static final String SESSION_TELNET = "TELNET";
    public static final String SESSION_SHELLCHANNEL = "SHELLCHANNEL";

    // ---
    private final VTermChannelProvider termProvider;

    private final VTermPanel terminalPanel;
    private java.net.URI startURI = null;
    private Properties properties = null;
    private final TermUI termUI;
    private final EmulatorListener emulatorListener;

    // Thread, runtime.
    private ShellChannel shellChannel;
    private Thread thread = null;
    private String sessionType;

    public VTermSessionManager(TermUI termUI, EmulatorListener emulatorListener, VTermChannelProvider termProvider, VTermPanel terminalPanel) {
        this.termUI = termUI;
        this.emulatorListener = emulatorListener;
        this.termProvider = termProvider;
        this.terminalPanel = terminalPanel;
        this.loadConfigSettings();
        // setup listener:
        terminalPanel.addComponentListener(new ResizeAdaptor(this));
    }

    /**
     * Creates new Thread which invokes run() method.
     */
    public void startSession() {
        this.thread = new Thread(this);
        this.thread.start();
    }

    public void startSession(String type, URI startUri) {
        this.setSessionType(type);
        this.setStartURI(startUri);
        startSession();
    }

    public void startShellChannel(ShellChannel shellChan) {
        setShellChannel(shellChan);
        setSessionType(SESSION_SHELLCHANNEL);
        startSession();
    }

    /**
     * Start session, will only return when session has ended
     */
    public void run() {
        executeSession();
    }

    protected void executeSession() {
        // ================
        // PRE SESSION
        // ================

        InputStream inps;
        OutputStream outps;
        InputStream errs = null;

        // Complete Reset !
        terminalPanel.reset();

        log.info(">>> Starting Session Type={}", sessionType);

        if (sessionType==null) {
            throw new NullPointerException("SessionTYPE is null");
        }else if (SESSION_TELNET.equals(this.sessionType)) {
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

                // DOES NOT WORK:
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
                emulator.addListener(this.emulatorListener);

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

        } else if (SESSION_PTY.equals(this.sessionType)) {

            try {

                TermChannelOptions options = this.getChannelOptions(SESSION_PTY, true);
                if(startURI==null) {
                    startURI = new URI("file", null, null, 0, SysEnv.sysEnv().getUserHome(), null, null);
                }
                this.shellChannel = this.termProvider.createChannel(SESSION_PTY, startURI, null, null,
                        options, this.termUI);
                shellChannel.connect();

                VTxEmulator emulator = new VTxEmulator(this.terminalPanel.getCharacterTerminal(),
                        shellChannel.getStdout(), shellChannel.getStdin());

                // Bash wiring:
                emulator.setErrorInput(errs);
                startShellProcessWatcher(shellChannel, emulator);

                // Wire TerminalPanel + Emulator
                this.terminalPanel.setEmulator(emulator);
                emulator.addListener(this.emulatorListener);
                terminalPanel.requestFocus();

                // start input/output loop (method will not return)
                updateAndStart(emulator);

            } catch (Exception ex) {
                log.error("Could not start bash. Got exception:" + ex.getMessage(), ex);
                this.termUI.showError(ex);
            }
        } else if (SESSION_SSH.equals(this.sessionType)) {
            TermChannelOptions options = this.getChannelOptions(SESSION_SSH, true);

            try {
                // ================================
                // Only here is SSHChannel visible!
                // ================================
                if (startURI==null) {
                    throw new NullPointerException("Start URI is null");
                }

                this.shellChannel = termProvider.createChannel(SESSION_SSH,
                        startURI, startURI.getUserInfo(), null, options, termUI);
                shellChannel.connect();

                VTxEmulator emulator = new VTxEmulator(this.terminalPanel.getCharacterTerminal(),
                        shellChannel.getStdout(), shellChannel.getStdin());

                // Wire TerminalPanel + Emulator
                this.terminalPanel.setEmulator(emulator);
                emulator.addListener(this.emulatorListener);
                this.terminalPanel.requestFocus();

                // start input/output loop (method will not return)
                updateAndStart(emulator);

            } catch (Exception | Error e) {
                log.error("Exception:" + e.getMessage(), e);
                termUI.showError(e);
            }
        } else if (SESSION_SHELLCHANNEL.equals(this.sessionType)) {
            try {
                // ================================
                // API call: use external shell channel
                // ================================
                if (this.shellChannel == null) {
                    throw new NullPointerException("No Shell Channel specified!");
                }

                VTxEmulator emulator = new VTxEmulator(this.terminalPanel.getCharacterTerminal(),
                        shellChannel.getStdout(), shellChannel.getStdin());

                // Term panel + emulator wirings:
                this.terminalPanel.setEmulator(emulator);
                emulator.addListener(this.emulatorListener);
                this.terminalPanel.requestFocus();

                // start input/output loop (method will not return)
                updateAndStart(emulator);

            } catch (Exception | Error e) {
                log.error("Exception:" + e.getMessage(), e);
                termUI.showError(e);
            }
        } else {
            throw new IllegalStateException("Session type is null or not valid:"+sessionType);
        }
        // ================
        // POST SESSION !!!
        // ================

        log.info("Session Ended: emulator stopped.");
    }

    private void updateAndStart(VTxEmulator emulator) {
        boolean value = getBoolProperty(VTERM_SYNC_SLOW_SCROLLING, false);
        emulator.setSlowScrolling(value);
        emulator.start();
    }

    public void setSessionType(String session) {
        this.sessionType = session;
    }

    public boolean isRunning() {
        return ((this.thread != null) && (thread.isAlive()));
    }

    public void setStartURI(URI uri) {
        this.startURI = uri;
    }

    public URI getStartURI() {
        return this.startURI;
    }

    public void setShellChannel(ShellChannel shellChan) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot set Shell Channel. Session active");
        }
        this.shellChannel = shellChan;
    }

    public void terminate() throws IOException {
        if (this.shellChannel != null) {
            this.shellChannel.disconnect(true);
        }

        this.shellChannel = null;
    }

    public TermChannelOptions getChannelOptions(String type, boolean autoCreate) {

        TermChannelOptions options = termProvider.getChannelOptions(type);
        if ((autoCreate) && (options == null)) {
            options = TermChannelOptions.create();
        }
        updateOptions(options, type);
        options.setDefaultSize(this.terminalPanel.getRowCount(), this.terminalPanel.getColumnCount());
        return options;
    }

    public void storeChannelOptions(String type, TermChannelOptions options) {
        termProvider.setChannelOptions(type, options);
    }

    private void updateOptions(TermChannelOptions options, String forYype) {
        // Copy options from UI:
        options.setTermType((String) this.properties.get(VTermConst.VTERM_TERM_TYPE));
    }

    public void setChannelOptions(String type, TermChannelOptions options) {
        termProvider.setChannelOptions(type, options);
    }

    protected void loadConfigSettings() {
        this.properties = loadConfigProperties();
    }

    public void setProperty(String name, String value) {
        this.properties.put(name, value);
    }

    /**
     * Sends emulator the new preferred size. Depends on implementation whether this will be
     * respected
     *
     * @param nr_columns
     * @param nr_rows
     */
    public void sendTermSize(int nr_columns, int nr_rows, int widthInPixels, int heightInPixels) {
        log.debug("sendPtyTermSize(:[{},{}]", nr_columns, nr_rows);
        boolean channelSupported;

        try {
            if ((shellChannel != null) && this.getEmulator() != null) {
                this.getEmulator().signalHalt(true);
                channelSupported = this.shellChannel.sendPtyTermSize(nr_columns, nr_rows, widthInPixels, heightInPixels);
                this.getEmulator().signalHalt(false);
                if (!channelSupported) {
                    log.error("Channel doesn't support updating resize:{}", this.shellChannel.getType());
                }
            } else {
                log.warn("Channel or Emulator not (yet) running.");
            }
        } catch (IOException e) {
            log.error("IOException: Couldn't send (pty)terminal size:" + e.getMessage(), e);
        }

    }


    // emulator is now connected to panel, must move.
    private Emulator getEmulator() {
        return terminalPanel.getEmulator();
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


    public String getConfigProperty(String name) {
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
            return SysEnv.sysEnv().loadProperties(propFileUri, true);
        } catch (Exception e) {
            log.warn("Exception: Couldn't load config vterm.prop:" + e.getMessage(), e);
            return new Properties();
        }
    }

    /**
     * Save persistent property. will create new vterm.prop file.
     */
    protected void saveConfigProperty(String name, String value) {
        if (properties == null) {
            properties = loadConfigProperties();
        }
        properties.put(name, value);
        saveSessionConfig();
    }

    private Path getPropertiesDir() throws IOException {
        return SysEnv.sysEnv().resolveUserHomePath(".config/vterm");
    }

    private Path getPropertiesFile() throws IOException {
        return SysEnv.sysEnv().resolveUserHomePath(".config/vterm/vterm.prop");
    }

    private void saveSessionConfig() {
        updateSessionProperties(properties);
        Path propFile = null;
        try {
            SysEnv.sysEnv().sysFS().mkdirs(getPropertiesDir());
            propFile = getPropertiesFile();
            SysEnv.sysEnv().saveProperties(propFile, properties, "VTerm Properties v2.");
        } catch (Exception e) {
            log.warn("Exception: Couldn't write config file:'" + propFile + "':" + e.getMessage(), e);
        }

    }

    private void updateSessionProperties(Properties properties) {
        int rows = this.terminalPanel.getRowCount();
        int cols = this.terminalPanel.getColumnCount();
        properties.put(VTermConst.VTERM_SESSION_DEFAULT_ROWS, Integer.toString(rows));
        properties.put(VTermConst.VTERM_SESSION_DEFAULT_COLUMNS, Integer.toString(cols));
    }

    /**
     * Save current (view) settings
     */
    public void saveSettings() {
        if (properties == null) {
            properties = loadConfigProperties();
        }
        saveSessionConfig();
    }

    protected Properties properties() {
        return this.properties;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public boolean getBoolProperty(String key, boolean defaultVal) {
        String val = properties.getProperty(key);
        if (isEmpty(val)) {
            return defaultVal;
        }
        return Boolean.parseBoolean(val);
    }

    /**
     * Watches a shell process and signals emulator when shell process died.
     */
    private void startShellProcessWatcher(final ShellChannel shellProc, final Emulator shellEmu) {
        Runnable run = () -> {
            int val = 0;

            try {
                shellProc.waitFor();
                val = shellProc.exitValue();
            } catch (InterruptedException e) {
                log.error("InterruptedException:" + e.getMessage(), e);
            }

            if (val == 0)
                log.info("Shell {} Stopped normally. Exit value is 0.", shellProc.getType());
            else
                log.error("Shell {} died abnormally. Exit value={}", shellProc.getType(), val);

            shellEmu.signalTerminate();
        };

        Thread procWatcher = new Thread(run);
        procWatcher.start();
    }

    public VTermPanel getTermPanel() {
        return terminalPanel;
    }

    public void setSlowScrolling(boolean val) {
        setProperty(VTERM_SYNC_SLOW_SCROLLING, Boolean.toString(val));
        if (getEmulator() != null) {
            getEmulator().setSlowScrolling(val);
        }
    }

    public boolean getSlowScrolling() {
        if (getEmulator() != null) {
            return getEmulator().getSlowScrolling();
        } else {
            return getBoolProperty(VTERM_SYNC_SLOW_SCROLLING, false);
        }
    }

    public void resizeTerminalToAWT() {
        // First update terminal panel
        terminalPanel.resizeTerminalToAWTSize();
        int rows = terminalPanel.getRowCount();
        int cols = terminalPanel.getColumnCount();
        // Then notify emulator + channel.
        sendTermSize(cols, rows, terminalPanel.getSize().width, terminalPanel.getSize().height);
    }

}
