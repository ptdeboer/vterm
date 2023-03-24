/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.api;

public interface TermUI {

    enum DialogOption {
        DIALOG_YES_NO,
        DIALOG_OK_CANCEL
    }

    void showMessage(String message);

    boolean askConfirmation(String message, DialogOption option);

    String askInput(String message, String type, boolean isSecret);

}
