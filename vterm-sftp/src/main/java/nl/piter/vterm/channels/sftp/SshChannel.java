/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.channels.sftp;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;
import lombok.extern.slf4j.Slf4j;
import nl.piter.vterm.api.ShellChannel;
import nl.piter.vterm.api.TermChannelOptions;
import nl.piter.vterm.api.TermConst;
import nl.piter.vterm.channels.sftp.jsch.SshSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Slf4j
public class SshChannel implements ShellChannel {

    public static class SshChannelOptions {
        public String termType = "xterm"; //"xterm-256color";
        public int num_cols = 80;
        public int num_rows = 24;

        public SshChannelOptions(String termType, int numCols, int numRows) {
            this.termType = termType;
            this.num_cols = numCols;
            this.num_rows = numRows;
        }
    }

    private final SshSession session;
    private ChannelShell channel;
    private final Object waitForObject = new Object();
    private final SshChannelOptions options;
    private OutputStream stdin;
    private InputStream stdout;

    public SshChannel(SshSession sftpSession, ChannelShell shellChannel, TermChannelOptions options) {
        this.session = sftpSession;
        this.channel = shellChannel;
        // TODO: proper options forwarding!
        this.options = new SshChannelOptions(options.getTermType(), options.getDefaultColumns(), options.getDefaultRows());
    }

    public void connect() throws IOException {
        //
        try {
            if (options != null) {
                if (options.termType != null) {
                    channel.setPtyType(options.termType);
                }
                channel.setPtySize(options.num_cols, options.num_rows, options.num_cols, options.num_rows);
            } else {
                channel.setPtyType(TermConst.TERM_XTERM);
                channel.setPtySize(80, 24, 80, 24);
            }

            this.stdin = channel.getOutputStream();
            this.stdout = channel.getInputStream();

            channel.connect();
        } catch (IOException | JSchException e) {
            throw new IOException("Could connect to:" + session, e);
        }
    }

    @Override
    public OutputStream getStdin() {
        return this.stdin;
    }

    @Override
    public InputStream getStdout() {
        return this.stdout;
    }

    @Override
    public InputStream getStderr() {
        return null;
    }

    public boolean isConnected() {
        return ((this.channel != null) && (channel.isConnected()));
    }

    @Override
    public void disconnect(boolean waitForTermination) {
        autoClose(this.stdin);
        autoClose(this.stdout);
        //
        if (this.channel != null) {
            this.channel.disconnect();
        }
        if (this.session !=null) {
            session.close();
            session.dispose();
        }

        this.channel = null;

        // signal disconnect:
        synchronized (this.waitForObject) {
            this.waitForObject.notifyAll();
        }
    }

    public String getType() {
        return "SSH";
    }

    public void setPtySize(int col, int row, int wp, int hp) {
        if (!this.isConnected()) {
            log.error("setPtySize(): NOT connected!");
            return;
        }
        this.channel.setPtySize(col, row, wp, hp);
    }

    public void setPtyType(String type) {
        if (!this.isConnected()) {
            log.error("setPtyType(): NOT connected!");
            return;
        }
        this.options.termType = type;
        this.channel.setPtyType(type);
    }

    @Override
    public boolean setPtyTermType(String type) {
        this.setPtyType(type);
        return true;
    }

    @Override
    public boolean sendPtyTermSize(int col, int row, int wp, int hp) {
        this.setPtySize(col, row, wp, hp);
        return true;
    }

    @Override
    public int[] getPtyTermSize() {
        // must use ctrl sequence
        return null;
    }

    @Override
    public void waitFor() throws InterruptedException {
        boolean wait = true;
        while (wait) {
            try {
                this.waitForObject.wait(30 * 1000);
                if (this.channel.isClosed())
                    wait = false;
            } catch (InterruptedException e) {
                throw e;
            }
        }
    }

    @Override
    public int exitValue() {
        return channel.getExitStatus();
    }

    public String toString() {
        return "SSHChannel:[session='" + session + "', connnected='" + isConnected() + "']";
    }

    private void autoClose(InputStream inps) {
        try {
            inps.close();
        } catch (IOException e) {
            log.warn("autoClose(): IOException:" + e.getMessage(), e);
        }
    }

    private void autoClose(OutputStream outps) {
        try {
            outps.close();
        } catch (IOException e) {
            log.warn("autoClose(): IOException:" + e.getMessage(), e);
        }
    }
}
