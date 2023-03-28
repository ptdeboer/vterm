/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.sys;


import lombok.extern.slf4j.Slf4j;
import nl.piter.vterm.sys.SysFS;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
public class SysFSTest {

    @Test
    public void testFilesPaths() throws IOException {
        // under linux any path starting with c: is considered relative:
        testFilePath("c:/path/to/file");
        // ¿illegal in dos?:
        //testFilePath("/c:/path/to/file");
        testFilePath("/c/path/to/file");
        testFilePath("c/relative/path/to/file");
        testFilePath("///c/path/to/file");
        testFilePath("c:relative/path/to/file");
    }

    private void testFilePath(String filePath) {
        Path path = new SysFS().resolvePath(filePath);
        log.info("resolve:'{}'=>'{}'", filePath, path);
    }

}
