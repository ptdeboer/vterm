/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.channels.sftp.jsch;

import com.jcraft.jsch.JSch;
import lombok.extern.slf4j.Slf4j;
import nl.piter.vterm.api.TermUI;
import nl.piter.vterm.channels.sftp.SftpConfig;
import nl.piter.vterm.sys.SysEnv;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Slf4j
public class SshSessionManager {

    private final JSch jsch;

    public SshSessionManager() {
        this.jsch = new JSch();
    }

    public SshSession createFor(String user, String host, int port, TermUI ui) throws IOException {
        SftpConfig config = creatConfig(user, host, port);
        log.debug("Using SftpConfig: {}", config);
        return new SshSession(jsch, config, ui);
    }

    private SftpConfig creatConfig(String user, String host, int port) {
        if (port <= 0) {
            port = 22;
        }

        Path userHome = Paths.get(SysEnv.sysEnv().getUserHome()).toAbsolutePath().normalize();
        Properties properties = new Properties();
        return SftpConfig.builder()
                .user(user)
                .host(host)
                .port(port)
                .userConfigDir(userHome.resolve(SftpConfig.SSH_USER_CONFIG_SIBDUR).toString())
                .sshKnowHostFile(SftpConfig.SSH_USER_KNOWN_HOSTS)
                .privateKeys(new String[]{SftpConfig.SSH_USER_DEFAULT_ID_RSA})
                .properties(properties)
                .build();
    }

}
