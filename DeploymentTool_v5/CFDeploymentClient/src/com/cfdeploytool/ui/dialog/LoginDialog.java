package com.cfdeploytool.ui.dialog;

import com.cfdeploytool.persistence.FileStore;
import com.cfdeploytool.service.AuthService;
import com.cfdeploytool.ui.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.file.Paths;

/**
 * Modal dialog for client authentication.
 */
public class LoginDialog extends JDialog {

    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JLabel errorLabel;
    private final AuthService authService;
    private boolean authenticated = false;

    public LoginDialog(Frame parent) {
        super(parent, "Login Authentication", true);
        
        // Initialize FileStore & AuthService
        FileStore fileStore = new FileStore(Paths.get(""));
        this.authService = new AuthService(fileStore);

        setSize(380, 400);
        setLocationRelativeTo(parent);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 16));
        mainPanel.setBackground(ThemeManager.BG_SURFACE);
        mainPanel.setBorder(new EmptyBorder(24, 28, 24, 28));

        // 1. Header Section
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("CF Deployment Tool");
        titleLabel.setFont(ThemeManager.FONT_TITLE.deriveFont(20f));
        titleLabel.setForeground(ThemeManager.ACCENT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Enter your credentials to continue");
        subtitleLabel.setFont(ThemeManager.FONT_SMALL);
        subtitleLabel.setForeground(ThemeManager.TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // 2. Form Section
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 0, 4, 0);
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        // Username
        gbc.gridy = 0;
        JLabel userLbl = new JLabel("Username");
        userLbl.setFont(ThemeManager.FONT_SMALL.deriveFont(Font.BOLD));
        userLbl.setForeground(ThemeManager.TEXT_SECONDARY);
        formPanel.add(userLbl, gbc);

        gbc.gridy = 1;
        usernameField = new JTextField();
        ThemeManager.styleTextField(usernameField);
        usernameField.setPreferredSize(new Dimension(300, 38));
        formPanel.add(usernameField, gbc);

        // Password
        gbc.gridy = 2;
        JLabel passLbl = new JLabel("Password");
        passLbl.setFont(ThemeManager.FONT_SMALL.deriveFont(Font.BOLD));
        passLbl.setForeground(ThemeManager.TEXT_SECONDARY);
        formPanel.add(passLbl, gbc);

        gbc.gridy = 3;
        passwordField = new JPasswordField();
        ThemeManager.styleTextField(passwordField);
        passwordField.setPreferredSize(new Dimension(300, 38));
        formPanel.add(passwordField, gbc);

        // Error message label (hidden by default)
        gbc.gridy = 4;
        errorLabel = new JLabel(" ");
        errorLabel.setFont(ThemeManager.FONT_SMALL);
        errorLabel.setForeground(ThemeManager.ERROR);
        formPanel.add(errorLabel, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // 3. Actions Section
        JPanel actionsPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        actionsPanel.setOpaque(false);

        JButton cancelBtn = ThemeManager.createSecondaryButton("Cancel");
        cancelBtn.setPreferredSize(new Dimension(140, 40));
        cancelBtn.addActionListener(e -> dispose());

        JButton loginBtn = ThemeManager.createAccentButton("Sign In");
        loginBtn.setPreferredSize(new Dimension(140, 40));
        loginBtn.addActionListener(e -> attemptLogin());

        actionsPanel.add(cancelBtn);
        actionsPanel.add(loginBtn);
        mainPanel.add(actionsPanel, BorderLayout.SOUTH);

        // Add key listeners for Enter key submission
        KeyAdapter enterSubmit = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    attemptLogin();
                }
            }
        };
        usernameField.addKeyListener(enterSubmit);
        passwordField.addKeyListener(enterSubmit);

        add(mainPanel);
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty()) {
            errorLabel.setText("Please enter username");
            usernameField.requestFocusInWindow();
            return;
        }
        if (password.isEmpty()) {
            errorLabel.setText("Please enter password");
            passwordField.requestFocusInWindow();
            return;
        }

        if (authService.authenticate(username, password)) {
            authenticated = true;
            dispose();
        } else {
            errorLabel.setText("Invalid username or password");
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
