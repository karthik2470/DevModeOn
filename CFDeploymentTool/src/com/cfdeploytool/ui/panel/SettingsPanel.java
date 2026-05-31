package com.cfdeploytool.ui.panel;

import com.cfdeploytool.model.EnvironmentConfig;
import com.cfdeploytool.service.EnvironmentService;
import com.cfdeploytool.ui.MainFrame;
import com.cfdeploytool.ui.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Settings: environment profiles with agent.properties-equivalent fields.
 */
public class SettingsPanel extends JPanel {

    private final EnvironmentService environmentService;
    private final MainFrame mainFrame;
    private final Runnable onSidebarRefresh;

    private JLabel activeEnvValueLabel;
    private JComboBox<EnvironmentConfig> envCombo;

    private JTextField nameField;
    private JTextField portField;
    private JTextField cfT2Field;
    private JTextField cfT4Field;
    private JTextField pluginT2Field;
    private JTextField pluginT4Field;
    private JTextField backupDirField;
    private JTextField deployBackupDirField;

    private String editingId;

    public SettingsPanel(EnvironmentService environmentService, MainFrame mainFrame,
                         Runnable onSidebarRefresh) {
        this.environmentService = environmentService;
        this.mainFrame = mainFrame;
        this.onSidebarRefresh = onSidebarRefresh;
        setLayout(new BorderLayout());
        setBackground(ThemeManager.BG_SURFACE);
        setBorder(new EmptyBorder(16, 20, 16, 20));
        initComponents();
        refreshEnvironmentList();
    }

    private void initComponents() {
        JPanel header = ThemeManager.createPageHeader("Settings",
                "Environment profiles - same fields as agent.properties");

        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setBackground(ThemeManager.BG_SURFACE);
        stack.setAlignmentX(Component.LEFT_ALIGNMENT);

        addStackCard(stack, createActiveEnvCard(), 88);
        stack.add(Box.createVerticalStrut(12));
        addStackCard(stack, createEnvSelectorCard(), 150);
        stack.add(Box.createVerticalStrut(12));
        addStackCard(stack, createConfigFormCard(), 530);

        JPanel scrollBody = new JPanel(new BorderLayout());
        scrollBody.setBackground(ThemeManager.BG_SURFACE);
        scrollBody.add(stack, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(scrollBody);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(ThemeManager.BG_SURFACE);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(14);

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    private void addStackCard(JPanel stack, JPanel card, int heightHint) {
        ThemeManager.bindFullWidth(card, heightHint);
        stack.add(card);
    }

    private JPanel createActiveEnvCard() {
        JPanel card = ThemeManager.createCardPanel();
        card.setLayout(new BorderLayout(0, 6));
        JLabel title = new JLabel("Current active environment");
        title.setFont(ThemeManager.FONT_BOLD);
        title.setForeground(ThemeManager.TEXT_PRIMARY);

        activeEnvValueLabel = new JLabel();
        activeEnvValueLabel.setFont(ThemeManager.FONT_TITLE.deriveFont(18f));
        activeEnvValueLabel.setForeground(ThemeManager.ACCENT_PRIMARY);

        card.add(title, BorderLayout.NORTH);
        card.add(activeEnvValueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createEnvSelectorCard() {
        JPanel card = ThemeManager.createCardPanel();
        card.setLayout(new BorderLayout(0, 10));
        JLabel title = new JLabel("Environment config");
        title.setFont(ThemeManager.FONT_BOLD);
        title.setForeground(ThemeManager.TEXT_PRIMARY);

        envCombo = new JComboBox<>();
        envCombo.setFont(ThemeManager.FONT_REGULAR);
        envCombo.setBackground(ThemeManager.BG_INPUT);
        envCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        envCombo.addActionListener(e -> {
            EnvironmentConfig selected = (EnvironmentConfig) envCombo.getSelectedItem();
            if (selected != null) {
                loadIntoForm(selected);
            }
        });

        JButton newBtn = ThemeManager.createSecondaryButton("New");
        newBtn.addActionListener(e -> createNewEnvironment());
        JButton duplicateBtn = ThemeManager.createSecondaryButton("Duplicate");
        duplicateBtn.addActionListener(e -> duplicateEnvironment());
        JButton deleteBtn = ThemeManager.createDangerButton("Delete");
        deleteBtn.addActionListener(e -> deleteEnvironment());
        JButton setActiveBtn = ThemeManager.createAccentButton("Set Active");
        setActiveBtn.addActionListener(e -> setActiveEnvironment());

        JPanel actions = ThemeManager.createActionBar(newBtn, duplicateBtn, deleteBtn, setActiveBtn);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(envCombo, BorderLayout.CENTER);

        card.add(top, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createConfigFormCard() {
        JPanel card = ThemeManager.createCardPanel();
        card.setLayout(new BorderLayout(0, 12));
        JLabel title = new JLabel("agent.properties fields");
        title.setFont(ThemeManager.FONT_BOLD);
        title.setForeground(ThemeManager.TEXT_PRIMARY);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.weightx = 1;

        int row = 0;
        nameField = addRow(form, gbc, row++, "name (display)", "Environment display name", null);
        portField = addRow(form, gbc, row++, "port", "Agent listen port", "8585");
        cfT2Field = addRow(form, gbc, row++, "path.customfunction.t2", "CustomFunction 2T deploy path", null);
        cfT4Field = addRow(form, gbc, row++, "path.customfunction.t4", "CustomFunction 4T deploy path", null);
        pluginT2Field = addRow(form, gbc, row++, "path.plugin.t2", "Plugin 2T deploy path", null);
        pluginT4Field = addRow(form, gbc, row++, "path.plugin.t4", "Plugin 4T deploy path", null);

        backupDirField = addRow(form, gbc, row++, "corporate.backup.path",
                "On-agent folder for manual backups (Backup tab)", null);
        deployBackupDirField = addRow(form, gbc, row, "corporate.Dbackup.path",
                "On-agent folder for pre-deploy backups when deploying files (Corporate)", null);

        JButton saveBtn = ThemeManager.createPrimaryActionButton("Save environment");
        saveBtn.addActionListener(e -> saveCurrentEnvironment());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        bottom.add(saveBtn);

        card.add(title, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private JTextField addRow(JPanel form, GridBagConstraints gbc, int row,
                              String propertyKey, String hint, String defaultVal) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        form.add(createKeyLabel(propertyKey), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        JTextField field = ThemeManager.styleTextField(new JTextField(40));
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

    public void refreshEnvironmentList() {
        List<EnvironmentConfig> envs = environmentService.getEnvironments();
        EnvironmentConfig active = environmentService.getActiveEnvironment();

        activeEnvValueLabel.setText(active.getName() + "  (" + active.getId() + ")");

        envCombo.removeAllItems();
        for (EnvironmentConfig env : envs) {
            envCombo.addItem(env);
        }

        EnvironmentConfig toEdit = active;
        if (editingId != null) {
            toEdit = environmentService.findById(editingId).orElse(active);
        }
        selectComboItem(toEdit);
        loadIntoForm(toEdit);
        onSidebarRefresh.run();
    }

    private void selectComboItem(EnvironmentConfig env) {
        for (int i = 0; i < envCombo.getItemCount(); i++) {
            if (envCombo.getItemAt(i).getId().equals(env.getId())) {
                envCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void loadIntoForm(EnvironmentConfig env) {
        editingId = env.getId();
        nameField.setText(env.getName());
        portField.setText(String.valueOf(env.getPort()));
        cfT2Field.setText(env.getPathCustomFunctionT2());
        cfT4Field.setText(env.getPathCustomFunctionT4());
        pluginT2Field.setText(env.getPathPluginT2());
        pluginT4Field.setText(env.getPathPluginT4());
        backupDirField.setText(env.getBackupDir());
        deployBackupDirField.setText(env.getDeployBackupDir());
    }

    private EnvironmentConfig readFromForm() {
        EnvironmentConfig env = environmentService.findById(editingId)
                .orElseGet(EnvironmentConfig::new);
        env.setName(nameField.getText().trim());
        try {
            env.setPort(Integer.parseInt(portField.getText().trim()));
        } catch (NumberFormatException e) {
            env.setPort(8585);
        }
        env.setPathCustomFunctionT2(cfT2Field.getText().trim());
        env.setPathCustomFunctionT4(cfT4Field.getText().trim());
        env.setPathPluginT2(pluginT2Field.getText().trim());
        env.setPathPluginT4(pluginT4Field.getText().trim());
        env.setBackupDir(backupDirField.getText().trim());
        env.setDeployBackupDir(deployBackupDirField.getText().trim());
        return env;
    }

    private void saveCurrentEnvironment() {
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Environment name is required.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        EnvironmentConfig env = readFromForm();
        environmentService.updateEnvironment(env);
        editingId = env.getId();
        refreshEnvironmentList();
        mainFrame.setStatus("Environment \"" + env.getName() + "\" saved");
        mainFrame.notifyEnvironmentChanged();
    }

    private void setActiveEnvironment() {
        EnvironmentConfig env = readFromForm();
        environmentService.updateEnvironment(env);
        environmentService.setActiveEnvironmentId(env.getId());
        editingId = env.getId();
        refreshEnvironmentList();
        mainFrame.setStatus("Active environment: " + env.getName());
        mainFrame.notifyEnvironmentChanged();
    }

    private void createNewEnvironment() {
        EnvironmentConfig env = new EnvironmentConfig();
        environmentService.addEnvironment(env);
        editingId = env.getId();
        refreshEnvironmentList();
        selectComboItem(env);
        loadIntoForm(env);
        nameField.requestFocus();
    }

    private void duplicateEnvironment() {
        EnvironmentConfig current = readFromForm();
        EnvironmentConfig copy = current.copy();
        environmentService.addEnvironment(copy);
        editingId = copy.getId();
        refreshEnvironmentList();
        selectComboItem(copy);
        loadIntoForm(copy);
    }

    private void deleteEnvironment() {
        if (editingId == null) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete this environment profile?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        if (!environmentService.deleteEnvironment(editingId)) {
            JOptionPane.showMessageDialog(this,
                    "Cannot delete the last environment.", "Delete",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        editingId = environmentService.getActiveEnvironmentId();
        refreshEnvironmentList();
        mainFrame.notifyEnvironmentChanged();
    }
}
