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
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.*;

/**
 * Minimal FS interactions.
 * Normalizes Paths.
 */
@Slf4j
public class SysFS {

    public Path resolvePath(String subPath) {
        return Paths.get(subPath).toAbsolutePath().normalize();
    }

    public InputStream newInputStream(String path) throws IOException {
        return newInputStream(this.resolvePath(path));
    }

    public InputStream newInputStream(Path path) throws IOException {
        return Files.newInputStream(path, READ);
    }

    public OutputStream newOutputStream(String path) throws IOException {
        return newOutputStream(this.resolvePath(path));
    }

    public OutputStream newOutputStream(Path path) throws IOException {
        return Files.newOutputStream(path, CREATE, WRITE);
    }

    public void mkdirs(Path paths) throws IOException {
        Files.createDirectories(paths);
    }

    public void mkdir(Path path) throws IOException {
        Files.createDirectory(path);
    }

}
