/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.api;

public class TermConst {

    public final static String SSH_XFORWARDING_ENABLED = "ssh.xforwarding.enabled";
    public final static String SSH_XFORWARDING_HOST = "ssh.xforwarding.host";
    public final static String SSH_XFORWARDING_PORT = "ssh.xforwarding.port";
    public final static String SSH_CHANNEL_COMPRESSION = "ssh.channel.compression";

    public final static String TERM_XTERM = "xterm";
    public final static String XTERM_256COLOR = "xterm-256color";
    final public static String TERM_VT100 = "vt100";

    public enum CharSet {
        CHARSET_GRAPHICS,
        CHARSET_UK,
        CHARSET_US,
        CHARSET_DUTCH,
        CHARSET_FINNISH,
        CHARSET_FRENCH,
        CHARSET_GERMAN,
        CHARSET_SWEDISH,
        CHARSET_NORDANISH,
        CHARSET_OTHER
    }

    private TermConst() {
    }

}
