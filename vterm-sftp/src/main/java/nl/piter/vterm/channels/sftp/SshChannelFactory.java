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
import nl.piter.vterm.api.ShellChannelFactory;
import nl.piter.vterm.api.TermChannelOptions;
import nl.piter.vterm.api.TermUI;
import nl.piter.vterm.channels.sftp.jsch.SshSession;
import nl.piter.vterm.channels.sftp.jsch.SshSessionManager;

import java.io.IOException;
import java.net.URI;

@Slf4j
public class SshChannelFactory implements ShellChannelFactory {

    private final SshSessionManager sessionMngr;

    public SshChannelFactory() {
        this.sessionMngr = new SshSessionManager();
    }

    public ShellChannel createChannel(URI uri, String user, char[] password, TermChannelOptions options, TermUI ui) throws IOException {
        try {
            SshSession sshSession = sessionMngr.createFor(user, uri.getHost(), uri.getPort(), ui);
            sshSession.connect();
            ChannelShell shellChannel = sshSession.createShellChannel();
            SshChannel sshShellChannel = new SshChannel(sshSession, shellChannel, options);
            return sshShellChannel;
        } catch (JSchException e) {
            throw new IOException("Failed to createShellChannel for user:" + user + " to: " + uri);
        }
    }

}
