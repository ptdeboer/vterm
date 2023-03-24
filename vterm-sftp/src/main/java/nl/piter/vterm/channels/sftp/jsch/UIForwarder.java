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
import lombok.extern.slf4j.Slf4j;
import nl.piter.vterm.api.TermUI;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class UIForwarder implements UserInfo, UIKeyboardInteractive {

    private final TermUI ui;

    public UIForwarder(TermUI ui) {
        this.ui = ui;
    }

    @Override
    public String getPassphrase() {
        return ui.askInput("Please provide passphrase", "passphrase", true);
    }

    @Override
    public String getPassword() {
        return ui.askInput("Please provide password", "password", true);
    }

    @Override
    public boolean promptPassword(String s) {
        return true;
    }

    @Override
    public boolean promptPassphrase(String s) {
        return true;
    }

    @Override
    public boolean promptYesNo(String message) {
        return ui.askConfirmation(message, TermUI.DialogOption.DIALOG_YES_NO);
    }

    @Override
    public void showMessage(String message) {
        ui.showMessage(message);
    }

    @Override
    public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompts, boolean[] echo) {

        log.debug("destination : {}", destination);
        log.debug("name        : {}", name);
        log.debug("instruction : {}", instruction);

        String msg = destination + ":" + name + ":" + instruction;

        List<String> answers = new ArrayList();

        for (int i = 0; i < prompts.length; i++) {
            String answer = this.ui.askInput(msg + ":" + prompts[i], prompts[i], !echo[i]);
            answers.add(answer);
        }

        return answers.toArray(new String[0]);
    }

}
