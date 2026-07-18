package com.cfdeploytool;

import com.cfdeploytool.persistence.FileStore;
import com.cfdeploytool.service.EnvironmentService;
import com.cfdeploytool.service.ServerManager;
import com.cfdeploytool.ui.MainFrame;
import com.cfdeploytool.ui.ThemeManager;
import com.cfdeploytool.ui.dialog.ChooseEnvDialog;
import com.cfdeploytool.ui.dialog.LoginDialog;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;

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
                Path baseDir = Paths.get(System.getProperty("user.dir"));
                FileStore fileStore = new FileStore(baseDir);
                EnvironmentService environmentService = new EnvironmentService(fileStore);
                ServerManager serverManager = new ServerManager(fileStore, environmentService);

                ChooseEnvDialog envDialog = new ChooseEnvDialog(null, environmentService, serverManager, false);
                envDialog.setVisible(true);

                if (envDialog.isConfirmed()) {
                    MainFrame frame = new MainFrame(fileStore, environmentService, serverManager);
                    ThemeManager.applyToFrame(frame);
                    frame.setVisible(true);
                } else {
                    System.exit(0);
                }
            } else {
                System.exit(0);
            }
        });
    }
}

