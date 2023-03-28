/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.channels.impl;

import lombok.extern.slf4j.Slf4j;
import nl.piter.vterm.api.ShellChannel;
import nl.piter.vterm.api.TermChannelOptions;
import nl.piter.vterm.sys.SysEnv;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Open BASH Shell Channel to local filesystem
 */
@Slf4j
public class BASHChannel implements ShellChannel {

    private final TermChannelOptions options;
    private Process shellProcess = null;
    private InputStream inps = null;
    private OutputStream outps = null;
    private InputStream errs = null;
    private int exitValue;

    public BASHChannel(String host, TermChannelOptions options) {
        this.options = options;
    }

    @Override
    public OutputStream getStdin() {
        return outps;
    }

    @Override
    public InputStream getStdout() {
        return inps;
    }

    @Override
    public InputStream getStderr() {
        return errs;
    }

    public String getType() {
        return "BASH";
    }

    @Override
    public void connect() throws IOException {
        log.info("Connecting to OS:{}", SysEnv.sysEnv().getOsName());
        this.shellProcess = null;

        try {
            // pseudo tty which invokes bash.

            boolean plainbash = false;
            List<String> cmds = new ArrayList<>();

            if (SysEnv.sysEnv().isLinux()) {
                // linux executable .lxe :-)
                cmds.add(getExePath("ptty.lxe"));

            } else if (SysEnv.sysEnv().isWindows()) {
                cmds.add(getExePath("ptty.exe"));
            } else {
                log.warn("exec bash: Can't determine OS:{}", SysEnv.sysEnv().getOsName());
                return;
            }

            // Default startup size. Currently only way to set size of pty. Resize not working (yet).
            if (options.getDefaultRows() > 0) {
                cmds.add("-h");
                cmds.add("" + options.getDefaultRows());
            }
            if (options.getDefaultColumns() > 0) {
                cmds.add("-w");
                cmds.add("" + options.getDefaultColumns());
            }

            cmds.add("-sh");
            cmds.add("/bin/bash");

            shellProcess = Runtime.getRuntime().exec(cmds.toArray(new String[0]));
//            inps = new BufferedInputStream(shellProcess.getInputStream());
//            outps = new BufferedOutputStream(shellProcess.getOutputStream());
//            errs = new BufferedInputStream(shellProcess.getErrorStream());

            inps = shellProcess.getInputStream();
            outps = shellProcess.getOutputStream();
            errs = shellProcess.getErrorStream();

            // final PseudoTtty ptty;

            if (plainbash) {
                errs = shellProcess.getErrorStream();
                // ptty=new PseudoTtty(inps,outps,errs);
                // inps=ptty.getInputStream();
                // outps=ptty.getOutputStream();
            } else {
                // ptty=null;
            }
        } catch (Exception e) {
            log.error("Couldn't initialize bash session:{}", e.getMessage());
            log.debug("Couldn't initialize bash.", e);
            throw new IOException("Failed to start bash session.\n" + e.getMessage(), e);
        }
        new StderrWatcher(errs);
    }

    private String getExePath(String file) {

        Path path = Paths.get(file).toAbsolutePath();
        if (Files.isRegularFile(path)) {
            log.info("Using executable:{} => {}", file, path);
            return path.toString();
        }

        // Check class path:
        java.net.URL url = Thread.currentThread().getContextClassLoader().getResource(file);
        String resolvePath = (url != null) ? url.getPath() : null;
        if (resolvePath == null) {
            // check relative or absolute file:
            path = Paths.get(file);
            if (Files.exists(path) && Files.isRegularFile(path)) {
                resolvePath = path.toAbsolutePath().toString();
            }
        }
        if (resolvePath != null) {
            log.info("Using executable:{} => {}", file, resolvePath);
            return resolvePath;
        } else {
            log.error("Failed to resolved binary:{}", file);
            return "bin/" + file;
        }
    }

    @Override
    public void disconnect(boolean wait) {
        if (this.shellProcess != null) {
            this.shellProcess.destroy();

            if (wait) {
                try {
                    shellProcess.waitFor();
                } catch (InterruptedException e) {
                    log.error("Interupted during waitFor(): continuing ... ");
                }
                exitValue = shellProcess.exitValue();
                this.shellProcess = null;
            }
        }

    }

    @Override
    public String getPtyTermType() {
        return null;
    }

    @Override
    public boolean setPtyTermType(String type) {
        log.warn("Can't set TERM type to:{}", type);
        return false;
    }

    @Override
    public boolean sendPtyTermSize(int col, int row, int wp, int hp) {
        log.warn("Can't set TERM size to:[{},{},{},{}]", col, row, wp, hp);
//        try {
//            // \033[8;h;wt
//            // printf '\033[8;40;100t'
//            String str="[8;40;100t";
//
//            byte bytes[]=new byte[11];
//            bytes[0]=27;
//            bytes[1]=(byte)'[';
//            bytes[2]=(byte)'8';
//            bytes[3]=(byte)';';
//            bytes[4]=(byte)'2';
//            bytes[5]=(byte)'0';
//            bytes[6]=(byte)';';
//            bytes[7]=(byte)'4';
//            bytes[8]=(byte)'0';
//            bytes[9]=(byte)'t';
//            bytes[10]=0;
//
//            this.getStdin().write(bytes);
//            this.getStdin().flush();
//
//        }catch (Exception e) {
//            return false;
//        }
        return false;
    }

    @Override
    public int[] getPtyTermSize() {
        return null;
    }

    public void waitFor() throws InterruptedException {
        this.shellProcess.waitFor();
    }

    public int exitValue() {
        if (shellProcess != null) {
            exitValue = shellProcess.exitValue();
        }
        return exitValue;
    }

}
