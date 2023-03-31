/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.channels.impl;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import lombok.extern.slf4j.Slf4j;
import nl.piter.vterm.api.ShellChannel;
import nl.piter.vterm.api.TermChannelOptions;
import nl.piter.vterm.api.TermUI;
import nl.piter.vterm.sys.SysEnv;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static nl.piter.vterm.emulator.Util.isEmpty;

/**
 * Open BASH Shell Channel to local filesystem
 */
@Slf4j
public class PtyChannel implements ShellChannel {

    private final TermChannelOptions options;
    private final String startingPath;
    private final Map<String, String> env;
    private final TermUI ui;
    private String[] commands;

    private InputStream inps;
    private OutputStream outps;
    private InputStream errs;
    private PtyProcess ptyProcess;

    public PtyChannel(String path, TermChannelOptions options, TermUI ui) {
        this.startingPath = path;
        this.options = options;
        this.commands = options.getCommand();
        this.env = options.getEnv();
        this.ui = ui;

        if (this.commands == null) {
            this.commands = new String[]{"/bin/bash", "-i"};
        }
    }

    @Override
    public OutputStream getStdin() {
        outps = ptyProcess.getOutputStream();
        return outps;
    }

    @Override
    public InputStream getStdout() {
        inps = ptyProcess.getInputStream();
        return inps;
    }

    @Override
    public InputStream getStderr() {
        errs = ptyProcess.getErrorStream();
        return errs;
    }

    public String getType() {
        return "PTY";
    }

    @Override
    public void connect() throws IOException {

        String cwd = !isEmpty(startingPath) ? startingPath : SysEnv.sysEnv().getUserHome();

        Map<String, String> allEnv=new HashMap<>();
        allEnv.putAll(System.getenv());
        if (env!=null) {
            allEnv.putAll(env);
        }
        allEnv.put("TERM", options.getTermType());

        log.warn("commands: {}; path={}; env={},", commands, cwd, allEnv);
        PtyProcessBuilder builder = new PtyProcessBuilder(commands)
                .setEnvironment(allEnv)
                .setDirectory(cwd)
                .setConsole(false)
                .setCygwin(false)
                .setLogFile(null);

        this.ptyProcess = builder.start();
        new StderrWatcher(getStderr()).start();
    }

    @Override
    public void disconnect(boolean waitForTermination) {
        if (ptyProcess.isAlive()) {
            ptyProcess.destroy();
            try {
                ptyProcess.waitFor(20, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                log.warn("Interrupted:{}", e.getMessage());
            }
        }

        if (waitForTermination) {
            if (ptyProcess.isAlive()) {
                try {
                    log.warn("Process still (pid={}) running, waiting for exit...", ptyProcess.pid());
                    if (ui != null) {
                        ui.showMessage("Process still running after SIGTERM. Waiting for exit...");
                    }
                    ptyProcess.waitFor(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    log.warn("Interrupted:{}", e.getMessage());
                }
            }
            if (ptyProcess.isAlive()) {
                log.warn("Process still running. Forcing destroy.");
                ptyProcess.destroyForcibly();
            } else {
                log.debug("Processed stopped with exit value: {}", ptyProcess.exitValue());
            }
        }

    }

    @Override
    public boolean setPtyTermType(String type) {
        log.warn("Can't set TERM type to:{}", type);
        return false;
    }

    @Override
    public boolean sendPtyTermSize(int col, int row, int wp, int hp) {
        return sendPtyTermSize(col, row);
    }

    public boolean sendPtyTermSize(int col, int row) {
        ptyProcess.setWinSize(new WinSize(col, row));
        return true;
    }

    @Override
    public int[] getPtyTermSize() throws IOException {
        WinSize size = ptyProcess.getWinSize();
        return new int[]{size.getRows(), size.getColumns()};
    }

    public void waitFor() throws InterruptedException {
        this.ptyProcess.waitFor();
    }

    public int exitValue() {
        return ptyProcess.exitValue();
    }

}
