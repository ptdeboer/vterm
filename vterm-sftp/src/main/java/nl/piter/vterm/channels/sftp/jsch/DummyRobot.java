/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.channels.sftp.jsch;

import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

public class DummyRobot implements UserInfo, UIKeyboardInteractive {

    @Override
    public String getPassphrase() {
        return null;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean promptPassphrase(String message) {
        return false;
    }

    @Override
    public boolean promptPassword(String message) {
        return false;
    }

    @Override
    public boolean promptYesNo(String message) {
        return false;
    }

    @Override
    public void showMessage(String message) {
    }

    @Override
    public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt, boolean[] echo) {
        return null;
    }

}
