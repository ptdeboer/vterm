/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.channels.sftp.jsch;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import nl.piter.vterm.api.TermUI;
import nl.piter.vterm.channels.sftp.SftpConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * SftpSession manages a JSch Session. This Session can be used to create more channels.
 */
@Slf4j
public class SshSession implements AutoCloseable {

    private final TermUI ui;
    private final Session session;
    private final JSch jsch;
    private final SftpConfig config;

    public SshSession(JSch jsch, SftpConfig config, TermUI ui) throws IOException {
        this.jsch = jsch;
        this.config = config;
        this.ui = ui;
        this.session = initSession();
    }

    public void connect() throws IOException {
        try {
            session.connect();
        } catch (JSchException e) {
            throw new IOException("Failed to connect:" + e.getMessage(), e);
        }
    }

    public boolean isConnected() {
        return this.session.isConnected();
    }

    protected Session initSession() throws IOException {
        log.debug("initSession(): {}", this);
        try {
            Session session = jsch.getSession(config.user, config.host, config.port);
            if (this.ui != null) {
                session.setUserInfo(new UIForwarder(ui));
            } else {
                session.setUserInfo(new DummyRobot());
            }
            addKnownHosts();
            addUserKeyFiles();
            session.setConfig(config.getProperties());
            return session;
        } catch (JSchException e) {
            throw new IOException("Couldn't connect to server:" + this, e);
        }
    }

    protected void addUserKeyFiles() {
        //
        if (config.userConfigDir == null) {
            log.debug("addUserIDFiles(): No userConfigDir configured");
            return;
        }
        Path confPath = getConfigPath();
        String[] keys = config.privateKeys;
        if ((keys == null) || (keys.length <= 0)) {
            log.debug("addUserIDFiles(): No private keys");
            return;
        }

        for (String key : keys) {
            try {
                Path keyFile = confPath.resolve(key);
                log.info("Using key file: '{}'", keyFile);

                if (Files.exists(keyFile)) {
                    log.debug("addUserIDFiles(): adding existing identity:{}\n", keyFile);
                    jsch.addIdentity(keyFile.toString());
                } else {
                    log.debug("addUserIDFiles(): ignoring missing identity file:{}\n", keyFile);
                }
            } catch (JSchException e) {
                log.error("Got JSchException adding file:{}/{} => JSchException:{}", confPath, key, e);
            }
        }
    }

    /**
     * Config directory is for example" ~/.ssh
     */
    private Path getConfigPath() {
        String configDir = config.getUserConfigDir();
        return Paths.get(configDir).toAbsolutePath().normalize();
    }

    protected void addKnownHosts() {
        //
        if ((config.sshKnowHostFile == null) || (config.sshKnowHostFile == null)) {
            log.debug("addUserIDFiles(): No userConfigDir or knownHostFile configured");
            return;
        }
        // Config director is for example" ~/.ssh/
        String configDir = config.userConfigDir;
        String knownHostsFile = config.sshKnowHostFile;

        try {
            Path confPath = getConfigPath();
            Path hostsFile = confPath.resolve(knownHostsFile);
            if (Files.exists(hostsFile)) {
                log.info("Using known hosts file: '{}'", hostsFile);
                jsch.setKnownHosts(hostsFile.toString());
            } else {
                log.warn("Known hosts file does not exist: '{}'", hostsFile);
            }
        } catch (JSchException e) {
            log.error("addKnownHosts():Failed to add known hosts file:{}/{} => JSchException:{}", configDir, knownHostsFile, e);
        }
    }

    public ChannelShell createShellChannel() throws JSchException {
        log.debug("createShellChannel() to:{}", this);
        Channel channel = session.openChannel("shell");
        return (ChannelShell) channel;
    }

    protected void disconnect() {
        if (this.session.isConnected()) {
            this.session.disconnect();
        }
    }

    public void dispose() {
        disconnect();
    }

    @Override
    public void close() {
        this.disconnect();
    }

    public String getServerString() {
        return String.format("%s@%s:%s", config.user, config.host, config.port);
    }

    public String toString() {
        return "SshSession:[server:'" + getServerString() + "']";
    }

}
