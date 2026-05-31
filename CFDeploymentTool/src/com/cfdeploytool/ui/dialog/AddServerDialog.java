package com.cfdeploytool.ui.dialog;

import com.cfdeploytool.model.Server;
import com.cfdeploytool.model.Server.ServerType;
import com.cfdeploytool.service.ServerManager;
import com.cfdeploytool.ui.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ItemEvent;

/**
 * Modal dialog for adding or editing a server registration.
 */
public class AddServerDialog extends JDialog {

    private JTextField nameField;
    private JTextField hostField;
    private JSpinner portSpinner;
    private JTextArea descriptionArea;
    private JCheckBox otherServerCheck;
    private JCheckBox corporateServerCheck;
    private boolean confirmed = false;
    private Server server;
    private final ServerManager serverManager;

    public AddServerDialog(JFrame parent, ServerManager serverManager) {
        this(parent, serverManager, null);
    }

    public AddServerDialog(JFrame parent, ServerManager serverManager, Server existingServer) {
        super(parent, existingServer != null ? "Edit Server" : "Add Server", true);
        this.serverManager = serverManager;
        this.server = existingServer;
        initComponents();
        layoutComponents();
        if (existingServer != null) {
            populateFields(existingServer);
        } else {
            selectServerType(ServerType.OTHER);
        }
        setSize(500, 520);
        setLocationRelativeTo(parent);
        setResizable(false);
        ThemeManager.applyToDialog(this);
    }

    private void initComponents() {
        nameField = ThemeManager.styleTextField(new JTextField());
        hostField = ThemeManager.styleTextField(new JTextField());

        SpinnerNumberModel portModel = new SpinnerNumberModel(8585, 1, 65535, 1);
        portSpinner = new JSpinner(portModel);
        portSpinner.setFont(ThemeManager.FONT_REGULAR);
        portSpinner.setBackground(ThemeManager.BG_INPUT);
        portSpinner.setForeground(ThemeManager.TEXT_PRIMARY);
        JComponent editor = portSpinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JFormattedTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setBackground(ThemeManager.BG_INPUT);
            tf.setForeground(ThemeManager.TEXT_PRIMARY);
            tf.setCaretColor(ThemeManager.TEXT_PRIMARY);
        }

        descriptionArea = ThemeManager.styleTextArea(new JTextArea(3, 20));
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        otherServerCheck = new JCheckBox("Other server");
        corporateServerCheck = new JCheckBox("Corporate server");
        ThemeManager.styleCheckBox(otherServerCheck);
        ThemeManager.styleCheckBox(corporateServerCheck);
        otherServerCheck.setBackground(ThemeManager.BG_SURFACE);
        corporateServerCheck.setBackground(ThemeManager.BG_SURFACE);

        otherServerCheck.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selectServerType(ServerType.OTHER);
            }
        });
        corporateServerCheck.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selectServerType(ServerType.CORPORATE);
            }
        });
    }

    private void selectServerType(ServerType type) {
        otherServerCheck.setSelected(type == ServerType.OTHER);
        corporateServerCheck.setSelected(type == ServerType.CORPORATE);
    }

    private ServerType getSelectedServerType() {
        return corporateServerCheck.isSelected() ? ServerType.CORPORATE : ServerType.OTHER;
    }

    private void layoutComponents() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(ThemeManager.BG_SURFACE);
        content.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel titleLabel = ThemeManager.createHeaderLabel(
                server != null ? "Edit Server" : "Register New Server");
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(titleLabel);
        content.add(Box.createVerticalStrut(4));

        JLabel subtitleLabel = ThemeManager.createSecondaryLabel(
                "Other = 2T path only  |  Corporate = 2T + 4T paths, folders not auto-created");
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(subtitleLabel);
        content.add(Box.createVerticalStrut(20));

        content.add(createFormRow("Server Name *", nameField));
        content.add(Box.createVerticalStrut(12));
        content.add(createFormRow("Host / IP Address *", hostField));
        content.add(Box.createVerticalStrut(12));
        content.add(createFormRow("Agent Port", portSpinner));
        content.add(Box.createVerticalStrut(12));

        JPanel typePanel = new JPanel();
        typePanel.setLayout(new BoxLayout(typePanel, BoxLayout.Y_AXIS));
        typePanel.setBackground(ThemeManager.BG_SURFACE);
        typePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        typePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel typeLabel = new JLabel("Server type *");
        typeLabel.setFont(ThemeManager.FONT_SMALL);
        typeLabel.setForeground(ThemeManager.TEXT_SECONDARY);
        typeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        typePanel.add(typeLabel);
        typePanel.add(Box.createVerticalStrut(6));

        JPanel checks = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        checks.setBackground(ThemeManager.BG_SURFACE);
        checks.setAlignmentX(Component.LEFT_ALIGNMENT);
        checks.add(otherServerCheck);
        checks.add(corporateServerCheck);
        typePanel.add(checks);
        content.add(typePanel);
        content.add(Box.createVerticalStrut(12));

        JPanel descPanel = new JPanel(new BorderLayout(0, 4));
        descPanel.setBackground(ThemeManager.BG_SURFACE);
        descPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        descPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JLabel descLabel = new JLabel("Description");
        descLabel.setFont(ThemeManager.FONT_SMALL);
        descLabel.setForeground(ThemeManager.TEXT_SECONDARY);
        descPanel.add(descLabel, BorderLayout.NORTH);
        descPanel.add(ThemeManager.styleScrollPane(descriptionArea), BorderLayout.CENTER);
        content.add(descPanel);

        content.add(Box.createVerticalStrut(24));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setBackground(ThemeManager.BG_SURFACE);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JButton cancelBtn = ThemeManager.createSecondaryButton("Cancel");
        cancelBtn.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        JButton saveBtn = ThemeManager.createAccentButton(server != null ? "Save Changes" : "Add Server");
        saveBtn.addActionListener(e -> onSave());

        buttonPanel.add(cancelBtn);
        buttonPanel.add(saveBtn);
        content.add(buttonPanel);

        setContentPane(content);
    }

    private JPanel createFormRow(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(0, 4));
        row.setBackground(ThemeManager.BG_SURFACE);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        JLabel lbl = new JLabel(label);
        lbl.setFont(ThemeManager.FONT_SMALL);
        lbl.setForeground(ThemeManager.TEXT_SECONDARY);
        row.add(lbl, BorderLayout.NORTH);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private void populateFields(Server server) {
        nameField.setText(server.getName());
        hostField.setText(server.getHost());
        portSpinner.setValue(server.getPort());
        descriptionArea.setText(server.getDescription());
        selectServerType(server.getServerType());
    }

    private void onSave() {
        String name = nameField.getText().trim();
        String host = hostField.getText().trim();
        int port = (int) portSpinner.getValue();
        String description = descriptionArea.getText().trim();
        ServerType serverType = getSelectedServerType();

        if (name.isEmpty()) {
            showValidationError("Server name is required.");
            nameField.requestFocus();
            return;
        }
        if (host.isEmpty()) {
            showValidationError("Host or IP address is required.");
            hostField.requestFocus();
            return;
        }
        if (!otherServerCheck.isSelected() && !corporateServerCheck.isSelected()) {
            showValidationError("Select Other server or Corporate server.");
            return;
        }

        if (server == null) {
            server = new Server(name, host, port, description);
        } else {
            server.setName(name);
            server.setHost(host);
            server.setPort(port);
            server.setDescription(description);
        }
        server.setServerType(serverType);

        if (serverManager != null && !serverManager.canRegisterCorporateServer(server)) {
            showValidationError("Only one Corporate server is allowed. Edit the existing Corporate server instead.");
            return;
        }

        confirmed = true;
        dispose();
    }

    private void showValidationError(String message) {
        JOptionPane.showMessageDialog(this, message, "Validation Error", JOptionPane.WARNING_MESSAGE);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Server getServer() {
        return server;
    }
}
