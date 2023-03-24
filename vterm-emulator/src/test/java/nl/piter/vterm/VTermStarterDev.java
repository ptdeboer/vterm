/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm;

import lombok.extern.slf4j.Slf4j;
import nl.piter.vterm.emulator.VTermChannelProvider;
import nl.piter.vterm.ui.VTerm;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * Custom starter for dev environment.
 * This will include the log4j.xml from test/resources.
 */
@Slf4j
public class VTermStarterDev {

    public static void main(String[] args) {

        String cwd = System.getProperty("user.dir");
        log.info("Starting [dev] from:{}", cwd);

        try {
            // permission fix for classpath resource (copied by build)
            setExePermissions("ptty.lxe");
        } catch (Exception e) {
            e.printStackTrace();
        }

        VTermChannelProvider provider = new VTermChannelProvider();
        new VTerm().withVTermChannelProvider(provider)
                .start(args);

    }

    private static void setExePermissions(String resource) throws URISyntaxException, IOException {
        URL fileUrl = Thread.currentThread().getContextClassLoader().getResource(resource);
        if (fileUrl == null) throw new FileNotFoundException("can not find:" + resource);
        Path path = Paths.get(fileUrl.toURI());
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxr-xr-x"));
    }

}
