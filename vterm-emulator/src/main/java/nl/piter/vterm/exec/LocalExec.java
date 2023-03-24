/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.exec;

import java.io.IOException;

/**
 * Helper class for local execution of script/commands etc. Is Factory class for LocalProcess.
 */
public class LocalExec {

    /**
     * Execute command and return {stdout,stderr,status} triple as String array.
     * <p>
     * The String array cmds[] holds the command and argument to execute. cmds[0] is the command to
     * execute and cmds[1],...,cmds[n] are the arguments. Method blocks until process has
     * terminated!
     * <p>
     * Method returns String array result[] which has at:
     * <li>result[0] complete output of stdout
     * <li>result[1] complete output of stderr;
     * <li>result[2] has the exit value in String value.
     * <p>
     * This method assumes no big output of text. Resulting String array (or array elements) might
     * be null upon error.
     */
    public static String[] execute(String[] cmds) throws IOException {

        String[] result = new String[3];
        // PRE: new empty process:
        LocalProcess proc = new LocalProcess();
        // capture stderr, stdout
        proc.setCaptureOutput(true, true);
        // Execute command and wait:
        proc.execute(cmds, true);
        // POST:
        String stdout = proc.getStdout();
        String stderr = proc.getStderr();
        int exit = proc.getExitValue();
        // result
        result[0] = stdout;
        result[1] = stderr;
        result[2] = "" + exit;
        // dipose
        dispose(proc);
        // exit
        return result;
    }

    private static void dispose(LocalProcess proc) {
        proc.dispose();
    }

    /**
     * Execute cmds[0] and returns LocalProcess object.
     * <p>
     * Returns Process object of terminated process or when wait=false the Process object of running
     * process.
     *
     * @param wait - wait until process completes. if false the Actual runnign LocalProcess is
     *             returned.
     */
    public static LocalProcess execute(String[] cmds, boolean wait) throws IOException {
        // new empty process:
        LocalProcess proc = new LocalProcess();
        // capture stderr, stdout
        proc.setCaptureOutput(true, true);
        // execute command and optionally wait:
        proc.execute(cmds, wait);
        // check
        return proc;
    }

}
