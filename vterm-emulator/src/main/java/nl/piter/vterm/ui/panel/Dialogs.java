/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.ui.panel;

import javax.swing.*;
import java.awt.*;

/**
 * Collection of Dialogs.
 */
public class Dialogs {

    private final JFrame frame;

    public Dialogs(JFrame frame) {
        this.frame = frame;
    }

    public void showMessage(String message) {
        JOptionPane.showMessageDialog(frame, message);
    }

    public String askInput(String title, String message) {
        // Thanks to Swing's serialization, we can send Swing Components !
        JTextField textField = new JTextField(20);

        Object[] inputFields = {message, textField};
        int result = JOptionPane.showConfirmDialog(frame, inputFields, title, JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            return textField.getText();
        } else {
            return null;
        }
    }

    public String askForSecret(String title, String message) {
        // Thanks to Swing's serialization, we can send Swing Components !
        JTextField passwordField = new JPasswordField(20);
        Object[] inputFields = {message, passwordField};
        int result = JOptionPane.showConfirmDialog(null, inputFields, title, JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            return passwordField.getText();
        } else {
            return null;
        }
    }

    public void showException(Throwable e) {
        // Thanks to Swing's serialization, we can send Swing Components !
        JTextField passwordField = new JPasswordField(20);
        JLabel label = new JLabel(e.getClass().getCanonicalName());
        label.setFont(new Font("Dialog", Font.BOLD, 16));
        Object[] message = {label, e.getMessage(), e.getStackTrace()};
        JOptionPane.showMessageDialog(null, message, "Exception", JOptionPane.WARNING_MESSAGE);
    }

    public boolean askYesNo(String message) {
        int result = JOptionPane.showOptionDialog(null, message, "Please choose.", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE, null, null, JOptionPane.NO_OPTION);
        return (result == JOptionPane.YES_OPTION);
    }

    public boolean askOkCancel(String message) {
        int result = JOptionPane.showOptionDialog(null, message, "Please choose.", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE, null, null, JOptionPane.CANCEL_OPTION);
        return (result == JOptionPane.OK_OPTION);
    }

}
