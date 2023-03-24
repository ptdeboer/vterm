/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.channels.sftp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Properties;

/**
 * Value Object.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SftpConfig {

    public static final String SSH_USER_KNOWN_HOSTS = "known_hosts";
    public static final String SSH_USER_CONFIG_SIBDUR = ".ssh";
    public static final String SSH_USER_DEFAULT_ID_RSA = "id_rsa";

    // === instance ===

    public String host;
    public int port;
    public String user;
    public String userConfigDir;
    public String sshKnowHostFile;
    public String[] privateKeys;
    public String[] publicKeys;
    protected Properties properties;

    public Properties properties() {
        return initProperties();
    }

    private Properties initProperties() {
        if (properties == null) {
            properties = new Properties();
        }
        return properties;
    }

    public String getStringProperty(String name) {
        Object val = initProperties().get(name);
        if (val == null) {
            return null;
        }
        return val.toString();
    }

    public java.net.URI getServerURI() throws URISyntaxException {
        return new java.net.URI("ssh", user, host, port, "/", null, null);
    }

    @Override
    public String toString() {
        return "SftpConfig:["
                + "host:'" + host + "',port:'" + port + "',user:'" + user + "'"
                + ",userConfigDir:" + userConfigDir
                + ",sshKnowHostFile:'" + sshKnowHostFile + "'"
                + ",privateKeys:[" + Arrays.toString(privateKeys) + "]"
                + ",publicKeys:[" + Arrays.toString(publicKeys) + "]"
                + "]";
    }
}
