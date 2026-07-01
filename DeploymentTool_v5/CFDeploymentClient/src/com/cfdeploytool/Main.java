package com.cfdeploytool;

import com.cfdeploytool.ui.MainFrame;
import com.cfdeploytool.ui.ThemeManager;
import com.cfdeploytool.ui.dialog.LoginDialog;

import javax.swing.*;

/**
 * CF Deployment Tool — Application Entry Point.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Cross-platform L&F works best with custom-painted controls
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Look and feel: " + e.getMessage());
            }
            ThemeManager.apply();

            LoginDialog login = new LoginDialog(null);
            login.setVisible(true);
            if (login.isAuthenticated()) {
                MainFrame frame = new MainFrame();
                ThemeManager.applyToFrame(frame);
                frame.setVisible(true);
            } else {
                System.exit(0);
            }
        });
    }
}
