/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.sys;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

@Slf4j
public class SysEnv {

    private static SysEnv instance = null;

    private SysEnv() {
        log.debug("SysEnv:");
        log.debug("- user.name={}", getUserName());
        log.debug("- user.home={}", getUserHome());
        log.debug("- user.os={}", getOsName());
        this.sysFS = new SysFS();
    }

    public static SysEnv sysEnv() {
        if (instance == null) {
            instance = new SysEnv();
        }
        return instance;
    }

    // --- //
    private final SysFS sysFS;

    public boolean isLinux() {
        return true;
    }

    public boolean isWindows() {
        return false; // hah!
    }

    public String getOsName() {
        return getSysProperty("os.name");
    }

    public String getUserName() {
        return getSysProperty("user.name");
    }

    public String getUserHome() {
        return getSysProperty("user.home");
    }

    public String getSysProperty(String name) {
        return System.getProperty(name);
    }

    public Path getUserHomeDir() {
        return sysFS.resolvePath(getUserHome());
    }

    public Properties loadProperties(Path propPath, boolean audoInitIfNotExisting) throws IOException {
        log.debug("Trying to load properties: {}", propPath);
        Properties properties = new Properties();
        if (!Files.exists(propPath)) {
            if (audoInitIfNotExisting) {
                return new Properties();
            } else {
                return null;
            }
        }
        try (InputStream inps = sysFS.newInputStream(propPath)) {
            properties.load(inps);
        }
        return properties;
    }

    public void saveProperties(Path propFile, Properties properties, String propertiesHeader) throws IOException {
        log.debug("Saving properties to: {}", propFile);

        try (OutputStream outps = sysFS.newOutputStream(propFile)) {
            properties.store(outps, propertiesHeader);
        }
    }

    public Path resolveUserHomePath(String subPath) throws IOException {
        return getUserHomeDir().resolve(subPath).normalize();
    }

    public SysFS sysFS() {
        return sysFS;
    }

}
