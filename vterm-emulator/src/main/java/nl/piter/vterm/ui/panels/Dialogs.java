/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.ui.panels;

import nl.piter.vterm.ui.VTermFrame;

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

    public static String[] askInputDialog(Frame frame, String title, String[] fields, String[] values) {

        int numFields = fields.length;

        Object[] inputFields = new Object[numFields * 2];
        for (int i = 0; i < numFields; i++) {
            inputFields[i * 2] = fields[i];
            JTextField tf=new JTextField();
            tf.setText(values[i]);
            inputFields[i * 2 + 1] = tf;
        }

        int result = JOptionPane.showConfirmDialog(frame, inputFields, title, JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        String[] results = new String[numFields];
        for (int i = 0; i < numFields; i++)
            results[i] = ((JTextField) inputFields[i * 2+1]).getText();
        return results;
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
