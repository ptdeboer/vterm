/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.exec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;

/**
 * Wrapper for a Local Process.
 */
public class LocalProcess {

    public static LocalProcess wrap(Process process) {
        return new LocalProcess(process);
    }

    private Process process = null;
    private OutputStream stdinStream = null; // output to process 'stdin'
    private InputStream stdoutStream = null;
    private InputStream stderrStream = null;
    private String stdoutString = null;
    private String stderrString = null;
    private boolean captureStdout = false;
    private boolean captureStderr = false;
    private String[] commands;
    private ActionTask streamReaderTask = null;
    private boolean isTerminated = false;

    protected LocalProcess(Process process) {
        this.process = process;
    }

    protected LocalProcess() {
    }

    public void captureOutput(boolean captureOut, boolean captureErr) {
        this.captureStdout = captureOut;
        this.captureStderr = captureErr;
    }

    public void waitFor() throws IOException {
        try {
            this.process.waitFor();
            // wait for completion of streamreader also !
            if (streamReaderTask != null) {
                streamReaderTask.waitFor();
            }
        } catch (InterruptedException e) {
            // Keep Flag!:
            Thread.currentThread().interrupt();
            throw new IOException("InterruptedException", e);
        } finally {
            isTerminated = true;
        }
    }

    public void setCaptureOutput(boolean captureStdout, boolean captureStderr) {
        this.captureStdout = captureStdout;
        this.captureStderr = captureStderr;
    }

    public void execute(String[] cmds) throws IOException {
        execute(cmds, true);
    }

    public void execute(String[] cmds, boolean syncWait) throws IOException {

        setCommands(cmds);

        if (commands == null)
            throw new IOException("Command string is empty !");

        try {
            this.process = Runtime.getRuntime().exec(commands);
        } catch (IOException e) {
            throw e;
        } catch (Throwable e) {
            throw new IOException(e.getMessage(), e);
        }

        isTerminated();

        // when doing a (synchonized) wait,
        // start streamReader in current thread !
        // this to avoid extra (thread) overhead when synchronized command
        // execution.

        if ((captureStdout) || (captureStderr)) {
            startStreamWatcher(syncWait);
        }

        if (syncWait) {
            waitFor();
        }
    }

    /**
     * Start StreamReaders to read from stderr,stdout.
     */
    protected void startStreamWatcher(boolean syncWait) throws IOException {
        //
        if ((!this.captureStderr) && (!this.captureStdout))
            return; // nothing to be done.

        if (this.captureStdout)
            stdoutStream = process.getInputStream();

        if (this.captureStderr)
            stderrStream = process.getErrorStream();

        streamReaderTask = new ActionTask(this, "StreamWatcher") {
            boolean stop = false;

            public void doTask() {

                // buf size is initial. Will Autoextend.
                StringWriter stdoutWriter = new StringWriter(1024);
                StringWriter stderrWriter = new StringWriter(1024);

                int val1 = -1, val2 = -1;

                try {
                    do {
                        // alternate read from stdout and stderr:
                        if (stdoutStream != null) {
                            val1 = stdoutStream.read();

                            if (val1 >= 0) {
                                stdoutWriter.write(val1);
                            }
                        }

                        if (stderrStream != null) {
                            val2 = stderrStream.read();

                            if (val2 >= 0) {
                                stderrWriter.write(val2);
                            }
                        }
                        // continue until EOF has occured on both streams.
                    } while ((!stop) && ((val1 >= 0) || (val2 >= 0)));
                } catch (IOException e) {
                    this.setException(e);
                }

                stdoutString = stdoutWriter.toString();
                stderrString = stderrWriter.toString();
            }

            @Override
            public void stopTask() {
                stop = true;
            }
        };

        if (!syncWait) {
            // background
            streamReaderTask.startTask();
        } else {
            // call run() directly
            streamReaderTask.run();
        }
    }

    /**
     * Set list of command + argument to start:
     * cmds[0] is the actual command, cmds[1],...,cmds[2] are the arguments.
     */
    void setCommands(String[] cmds) {
        this.commands = cmds;
    }

    /**
     * Returns stdout of terminated process as String. If this method is called during execution of
     * a process this method will return null.
     */
    public String getStdout() {
        return stdoutString;
    }

    /**
     * Returns stderr of terminated process as String. If this method is called during execution of
     * a process this method will return null.
     */
    public String getStderr() {
        return stderrString;
    }

    public int getExitValue() {
        return process.exitValue();
    }

    public void terminate() {
        dispose();
    }

    public boolean isTerminated() {
        // process has already terminated
        if (isTerminated) {
            return true;
        }
        // dirty way to check whether process hasn't exited
        try {
            this.process.exitValue();
            this.isTerminated = true;
        } catch (IllegalThreadStateException e) {
            // cannot get exitValue() from non terminated process:
            this.isTerminated = false;
        }
        //
        return isTerminated;
    }

    public OutputStream getStdinStream() {
        stdinStream = process.getOutputStream();
        return stdinStream;
    }

    public InputStream getStderrStream() {
        stderrStream = process.getErrorStream();
        return stderrStream;
    }

    public InputStream getStdoutStream() {
        stdoutStream = process.getInputStream();
        return stdoutStream;
    }

    public void dispose() {
        // stdin
        if (stdinStream != null) {
            try {
                stdinStream.close();
            } catch (Exception e) {
            }

            stdinStream = null;
        }
        //stdout
        if (stdoutStream != null) {
            try {
                stdoutStream.close();
            } catch (Exception e) {
            }

            stdoutStream = null;
        }
        //stderr
        if (stderrStream != null) {
            try {
                stderrStream.close();
            } catch (Exception e) {
            }

            stderrStream = null;
        }
        // dispose
        if (process != null) {
            process.destroy();
            process = null;
        }
    }

}
