package com.cfdeploytool.ui.dialog;

import com.cfdeploytool.model.EnvironmentConfig;
import com.cfdeploytool.service.EnvironmentService;
import com.cfdeploytool.service.ServerManager;
import com.cfdeploytool.ui.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Dialog for selecting the environment profile at launch, with a settings gear
 * to edit/configure profiles in a nested dialog.
 */
public class ChooseEnvDialog extends JDialog {

    private final EnvironmentService environmentService;
    private final ServerManager serverManager;
    private final boolean isSettingsMode;
    private boolean confirmed = false;

    private JComboBox<EnvironmentConfig> envCombo;
    private boolean isRefreshingEnvCombo = false;

    public ChooseEnvDialog(Window parent, EnvironmentService environmentService, ServerManager serverManager, boolean isSettingsMode) {
        super(parent, isSettingsMode ? "Environment Settings" : "Select Environment", ModalityType.DOCUMENT_MODAL);
        this.environmentService = environmentService;
        this.serverManager = serverManager;
        this.isSettingsMode = isSettingsMode;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        refreshEnvironmentList();

        setSize(480, 250);
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        JPanel rootPanel = new JPanel(new BorderLayout(0, 12));
        rootPanel.setBackground(ThemeManager.BG_SURFACE);
        rootPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // 1. Header Section
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setOpaque(false);
        JLabel title = ThemeManager.createHeaderLabel(isSettingsMode ? "Manage Environments" : "Select Active Environment");
        JLabel subtitle = ThemeManager.createSecondaryLabel("Select environment to connect to target agents");
        headerText.add(title);
        headerText.add(Box.createVerticalStrut(2));
        headerText.add(subtitle);
        headerPanel.add(headerText, BorderLayout.WEST);

        // Settings Button in Top-Right
        JButton settingsBtn = ThemeManager.createIconButton(ThemeManager.getSettingsIcon(ThemeManager.TEXT_PRIMARY));
        settingsBtn.setToolTipText("Manage Environment Profiles");
        settingsBtn.addActionListener(e -> openConfigDialog());
        headerPanel.add(settingsBtn, BorderLayout.EAST);

        rootPanel.add(headerPanel, BorderLayout.NORTH);

        // 2. Center Content - Selector Card (Only env list)
        JPanel selectorCard = ThemeManager.createCardPanel();
        selectorCard.setLayout(new BorderLayout(0, 8));

        JLabel comboLabel = new JLabel("Environment profile");
        comboLabel.setFont(ThemeManager.FONT_BOLD);
        comboLabel.setForeground(ThemeManager.TEXT_PRIMARY);

        envCombo = new JComboBox<>();
        envCombo.setFont(ThemeManager.FONT_REGULAR);
        envCombo.setBackground(ThemeManager.BG_INPUT);
        envCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        selectorCard.add(comboLabel, BorderLayout.NORTH);
        selectorCard.add(envCombo, BorderLayout.CENTER);

        rootPanel.add(selectorCard, BorderLayout.CENTER);

        // 3. Bottom Footer Panel
        JButton cancelBtn = ThemeManager.createSecondaryButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        String actionBtnText = isSettingsMode ? "Apply & Close" : (getParent() instanceof JFrame ? "Switch Environment" : "Launch Client");
        JButton confirmBtn = ThemeManager.createAccentButton(actionBtnText);
        confirmBtn.addActionListener(e -> handleConfirm());

        JPanel footerActions = ThemeManager.createActionBar(cancelBtn, confirmBtn);
        rootPanel.add(footerActions, BorderLayout.SOUTH);

        add(rootPanel);
    }

    public void refreshEnvironmentList() {
        List<EnvironmentConfig> envs = environmentService.getEnvironments();
        EnvironmentConfig active = environmentService.getActiveEnvironment();

        isRefreshingEnvCombo = true;
        try {
            EnvironmentConfig selectedBefore = (EnvironmentConfig) envCombo.getSelectedItem();
            envCombo.removeAllItems();
            for (EnvironmentConfig env : envs) {
                envCombo.addItem(env);
            }

            EnvironmentConfig toSelect = active;
            if (selectedBefore != null) {
                toSelect = environmentService.findById(selectedBefore.getId()).orElse(active);
            }
            selectComboItem(toSelect);
        } finally {
            isRefreshingEnvCombo = false;
        }
    }

    private void selectComboItem(EnvironmentConfig env) {
        for (int i = 0; i < envCombo.getItemCount(); i++) {
            if (envCombo.getItemAt(i).getId().equals(env.getId())) {
                envCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void handleConfirm() {
        EnvironmentConfig selected = (EnvironmentConfig) envCombo.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select an environment profile.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        environmentService.setActiveEnvironmentId(selected.getId());
        confirmed = true;
        dispose();
    }

    private void openConfigDialog() {
        EnvironmentConfig current = (EnvironmentConfig) envCombo.getSelectedItem();
        if (current == null) {
            current = environmentService.getActiveEnvironment();
        }
        EnvConfigDialog dialog = new EnvConfigDialog(this, current, environmentService);
        dialog.setVisible(true);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Helper to create minimalist icon buttons with hovers & tooltips.
     */
    public static JButton createIconButton(String symbol, String tooltip) {
        JButton btn = new JButton(symbol) {
            private boolean hovering = false;
            {
                setFont(ThemeManager.FONT_HEADER.deriveFont(16f));
                setForeground(ThemeManager.TEXT_SECONDARY);
                setFocusPainted(false);
                setBorderPainted(false);
                setContentAreaFilled(false);
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setToolTipText(tooltip);
                setPreferredSize(new Dimension(32, 32));
                setMinimumSize(new Dimension(32, 32));
                setMaximumSize(new Dimension(32, 32));
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        hovering = true;
                        setForeground(ThemeManager.ACCENT_PRIMARY);
                        repaint();
                    }
                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        hovering = false;
                        setForeground(ThemeManager.TEXT_SECONDARY);
                        repaint();
                    }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                if (hovering) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(ThemeManager.BG_HOVER);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        return btn;
    }

    // ==================== NESTED DIALOG FOR agent.properties EDITING ====================
    public static class EnvConfigDialog extends JFrame {
        private final EnvironmentService environmentService;
        private EnvironmentConfig config;

        private JComboBox<EnvironmentConfig> configEnvCombo;
        private boolean isRefreshingCombo = false;

        private JTextField nameField;
        private JTextField portField;
        private JTextField cfT2Field;
        private JTextField cfT4Field;
        private JTextField pluginT2Field;
        private JTextField pluginT4Field;
        private JTextField pluginDllT2Field;
        private JTextField pluginDllT4Field;
        private JTextField backupDirField;
        private JTextField deployBackupDirField;
        private JTextArea tcServicesAndProgramsArea;
        private JTextArea runningTcProgramsArea;

        public EnvConfigDialog(Window owner, EnvironmentConfig config, EnvironmentService environmentService) {
            super("Environment Settings");
            this.config = config;
            this.environmentService = environmentService;

            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            initComponents();
            refreshConfigList();
            loadConfigData(config);

            setSize(650, 720);
            setMinimumSize(new Dimension(550, 600));
            setLocationRelativeTo(owner);

            // Modal behavior simulator for JFrame
            if (owner != null) {
                owner.setEnabled(false);
                addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosed(java.awt.event.WindowEvent e) {
                        if (owner instanceof ChooseEnvDialog) {
                            ((ChooseEnvDialog) owner).refreshEnvironmentList();
                        }
                        owner.setEnabled(true);
                        owner.setVisible(true);
                        owner.toFront();
                    }
                });
            }
        }

        private void initComponents() {
            JPanel mainPanel = new JPanel(new BorderLayout(0, 12));
            mainPanel.setBackground(ThemeManager.BG_SURFACE);
            mainPanel.setBorder(new EmptyBorder(18, 20, 18, 20));

            // Header Panel
            JPanel header = ThemeManager.createPageHeader("agent.properties Configuration",
                    "Configure profiles and edit deployment parameters");
            mainPanel.add(header, BorderLayout.NORTH);

            // Center Panel containing Selector Card & Fields Card
            JPanel body = new JPanel();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setBackground(ThemeManager.BG_SURFACE);

            // Selector Card (Compact with Icons)
            JPanel selectorCard = ThemeManager.createCardPanel();
            selectorCard.setLayout(new BorderLayout(0, 8));

            JLabel label = new JLabel("Select profile to edit");
            label.setFont(ThemeManager.FONT_BOLD);
            label.setForeground(ThemeManager.TEXT_PRIMARY);

            configEnvCombo = new JComboBox<>();
            configEnvCombo.setFont(ThemeManager.FONT_REGULAR);
            configEnvCombo.setBackground(ThemeManager.BG_INPUT);
            configEnvCombo.setPreferredSize(new Dimension(280, 32));
            configEnvCombo.addActionListener(e -> {
                if (isRefreshingCombo) return;
                EnvironmentConfig selected = (EnvironmentConfig) configEnvCombo.getSelectedItem();
                if (selected != null) {
                    saveCurrentFormToConfig(); // Save current before switching
                    config = selected;
                    loadConfigData(config);
                }
            });

            // Management Icon Buttons
            JButton newBtn = ThemeManager.createIconButton(ThemeManager.getAddIcon(ThemeManager.TEXT_PRIMARY));
            newBtn.setToolTipText("Create New Profile");
            newBtn.addActionListener(e -> createNewProfile());

            JButton duplicateBtn = ThemeManager.createIconButton(ThemeManager.getCopyIcon(ThemeManager.TEXT_PRIMARY));
            duplicateBtn.setToolTipText("Duplicate Current Profile");
            duplicateBtn.addActionListener(e -> duplicateProfile());

            JButton deleteBtn = ThemeManager.createIconButton(ThemeManager.getDeleteIcon(ThemeManager.TEXT_PRIMARY));
            deleteBtn.setToolTipText("Delete Current Profile");
            deleteBtn.addActionListener(e -> deleteProfile());

            JPanel iconActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            iconActions.setOpaque(false);
            iconActions.add(newBtn);
            iconActions.add(duplicateBtn);
            iconActions.add(deleteBtn);

            JPanel selectorComboPanel = new JPanel(new BorderLayout(8, 0));
            selectorComboPanel.setOpaque(false);
            selectorComboPanel.add(configEnvCombo, BorderLayout.CENTER);
            selectorComboPanel.add(iconActions, BorderLayout.EAST);

            selectorCard.add(label, BorderLayout.NORTH);
            selectorCard.add(selectorComboPanel, BorderLayout.CENTER);
            body.add(selectorCard);
            body.add(Box.createVerticalStrut(12));

            // Configuration Form Card
            JPanel formCard = ThemeManager.createCardPanel();
            formCard.setLayout(new BorderLayout(0, 12));

            JPanel formGrid = new JPanel(new GridBagLayout());
            formGrid.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(6, 4, 6, 4);
            gbc.weightx = 1;

            int row = 0;
            nameField = addRow(formGrid, gbc, row++, "name (display)", "Environment display name", null);
            portField = addRow(formGrid, gbc, row++, "port", "Agent listen port", "8585");
            cfT2Field = addRow(formGrid, gbc, row++, "path.customfunction.t2", "CustomFunction 2T deploy path", null);
            cfT4Field = addRow(formGrid, gbc, row++, "path.customfunction.t4", "CustomFunction 4T deploy path", null);
            pluginT2Field = addRow(formGrid, gbc, row++, "path.plugin.t2", "Plugin 2T deploy path", null);
            pluginT4Field = addRow(formGrid, gbc, row++, "path.plugin.t4", "Plugin 4T deploy path", null);
            pluginDllT2Field = addRow(formGrid, gbc, row++, "path.plugindll.t2", "Plugin DLL 2T deploy path", null);
            pluginDllT4Field = addRow(formGrid, gbc, row++, "path.plugindll.t4", "Plugin DLL 4T deploy path", null);

            backupDirField = addRow(formGrid, gbc, row++, "corporate.backup.path",
                    "On-agent folder for manual backups (Backup tab)", null);
            deployBackupDirField = addRow(formGrid, gbc, row++, "corporate.Dbackup.path",
                    "On-agent folder for pre-deploy backups when deploying files (Corporate)", null);

            tcServicesAndProgramsArea = addAreaRow(formGrid, gbc, row++, "task.services.and.programs",
                    "TC Windows services and executable paths to control.\nFormat per line: TYPE|name_or_path|Display Name\nTypes: SERVICE, PROGRAM, BATCH\nExamples:\n  SERVICE|MyWindowsService|My Service\n  PROGRAM|C:\\apps\\app.exe|My App\n  BATCH|C:\\scripts\\deploy.bat|Deploy Script", 3);
            runningTcProgramsArea = addAreaRow(formGrid, gbc, row, "task.running.tc.programs",
                    "Processes to monitor in Task Manager (comma or newline separated)", 2);

            formCard.add(formGrid, BorderLayout.CENTER);
            body.add(formCard);

            JPanel scrollWrapper = new JPanel(new BorderLayout());
            scrollWrapper.setBackground(ThemeManager.BG_SURFACE);
            scrollWrapper.add(body, BorderLayout.NORTH);

            JScrollPane scroll = new JScrollPane(scrollWrapper);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(ThemeManager.BG_SURFACE);
            scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.getVerticalScrollBar().setUnitIncrement(14);
            mainPanel.add(scroll, BorderLayout.CENTER);

            // Footer actions
            JButton cancelBtn = ThemeManager.createSecondaryButton("Cancel");
            cancelBtn.addActionListener(e -> dispose());

            JButton saveBtn = ThemeManager.createAccentButton("Save");
            saveBtn.addActionListener(e -> handleSave());

            JPanel footerActions = ThemeManager.createActionBar(cancelBtn, saveBtn);
            mainPanel.add(footerActions, BorderLayout.SOUTH);

            add(mainPanel);
        }

        private JTextArea addAreaRow(JPanel form, GridBagConstraints gbc, int row,
                                     String propertyKey, String hint, int rowsCount) {
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            form.add(createKeyLabel(propertyKey), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            JTextArea area = ThemeManager.styleTextArea(new JTextArea(rowsCount, 30));
            area.setToolTipText(hint);
            JScrollPane scroll = ThemeManager.styleScrollPane(area);
            scroll.setPreferredSize(new Dimension(scroll.getPreferredSize().width, rowsCount * 18 + 12));
            form.add(scroll, gbc);
            return area;
        }

        private JTextField addRow(JPanel form, GridBagConstraints gbc, int row,
                                  String propertyKey, String hint, String defaultVal) {
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            form.add(createKeyLabel(propertyKey), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            JTextField field = ThemeManager.styleTextField(new JTextField(30));
            if (defaultVal != null) {
                field.setText(defaultVal);
            }
            field.setToolTipText(hint);
            form.add(field, gbc);
            return field;
        }

        private JLabel createKeyLabel(String propertyKey) {
            JLabel lbl = new JLabel(propertyKey);
            lbl.setFont(ThemeManager.FONT_MONO);
            lbl.setForeground(ThemeManager.TEXT_SECONDARY);
            return lbl;
        }

        private void refreshConfigList() {
            List<EnvironmentConfig> envs = environmentService.getEnvironments();
            isRefreshingCombo = true;
            try {
                configEnvCombo.removeAllItems();
                for (EnvironmentConfig env : envs) {
                    configEnvCombo.addItem(env);
                }
                selectComboItem(config);
            } finally {
                isRefreshingCombo = false;
            }
        }

        private void selectComboItem(EnvironmentConfig env) {
            for (int i = 0; i < configEnvCombo.getItemCount(); i++) {
                if (configEnvCombo.getItemAt(i).getId().equals(env.getId())) {
                    configEnvCombo.setSelectedIndex(i);
                    return;
                }
            }
        }

        private void loadConfigData(EnvironmentConfig env) {
            nameField.setText(env.getName());
            portField.setText(String.valueOf(env.getPort()));
            cfT2Field.setText(env.getPathCustomFunctionT2());
            cfT4Field.setText(env.getPathCustomFunctionT4());
            pluginT2Field.setText(env.getPathPluginT2());
            pluginT4Field.setText(env.getPathPluginT4());
            pluginDllT2Field.setText(env.getPathPluginDllT2());
            pluginDllT4Field.setText(env.getPathPluginDllT4());
            backupDirField.setText(env.getBackupDir());
            deployBackupDirField.setText(env.getDeployBackupDir());
            tcServicesAndProgramsArea.setText(env.getTcServicesAndPrograms());
            runningTcProgramsArea.setText(env.getRunningTcPrograms());
        }

        private void saveCurrentFormToConfig() {
            String name = nameField.getText().trim();
            if (name.isEmpty()) return;

            int port = 8585;
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException ignored) {}

            config.setName(name);
            config.setPort(port);
            config.setPathCustomFunctionT2(cfT2Field.getText().trim());
            config.setPathCustomFunctionT4(cfT4Field.getText().trim());
            config.setPathPluginT2(pluginT2Field.getText().trim());
            config.setPathPluginT4(pluginT4Field.getText().trim());
            config.setPathPluginDllT2(pluginDllT2Field.getText().trim());
            config.setPathPluginDllT4(pluginDllT4Field.getText().trim());
            config.setBackupDir(backupDirField.getText().trim());
            config.setDeployBackupDir(deployBackupDirField.getText().trim());
            config.setTcServicesAndPrograms(tcServicesAndProgramsArea.getText());
            config.setRunningTcPrograms(runningTcProgramsArea.getText().trim());
        }

        private void createNewProfile() {
            saveCurrentFormToConfig();
            EnvironmentConfig newEnv = new EnvironmentConfig();
            environmentService.addEnvironment(newEnv);
            config = newEnv;
            refreshConfigList();
            loadConfigData(config);
            nameField.requestFocus();
        }

        private void duplicateProfile() {
            saveCurrentFormToConfig();
            EnvironmentConfig copy = config.copy();
            environmentService.addEnvironment(copy);
            config = copy;
            refreshConfigList();
            loadConfigData(config);
        }

        private void deleteProfile() {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete the environment profile \"" + config.getName() + "\"?\nNote: All target servers registered inside this environment will also be deleted.",
                    "Confirm delete", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
            if (!environmentService.deleteEnvironment(config.getId())) {
                JOptionPane.showMessageDialog(this,
                        "Cannot delete the last environment.", "Delete",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            config = environmentService.getActiveEnvironment();
            refreshConfigList();
            loadConfigData(config);
        }

        private void handleSave() {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Profile name is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                nameField.requestFocus();
                return;
            }

            int port = 8585;
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Port must be a number.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                portField.requestFocus();
                return;
            }

            config.setName(name);
            config.setPort(port);
            config.setPathCustomFunctionT2(cfT2Field.getText().trim());
            config.setPathCustomFunctionT4(cfT4Field.getText().trim());
            config.setPathPluginT2(pluginT2Field.getText().trim());
            config.setPathPluginT4(pluginT4Field.getText().trim());
            config.setPathPluginDllT2(pluginDllT2Field.getText().trim());
            config.setPathPluginDllT4(pluginDllT4Field.getText().trim());
            config.setBackupDir(backupDirField.getText().trim());
            config.setDeployBackupDir(deployBackupDirField.getText().trim());
            config.setTcServicesAndPrograms(tcServicesAndProgramsArea.getText());
            config.setRunningTcPrograms(runningTcProgramsArea.getText().trim());

            environmentService.updateEnvironment(config);
            dispose();
        }
    }
}
