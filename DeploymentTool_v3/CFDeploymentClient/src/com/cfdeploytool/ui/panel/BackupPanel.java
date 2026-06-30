package com.cfdeploytool.ui.panel;

import com.cfdeploytool.model.DeploymentCategory;
import com.cfdeploytool.model.DeploymentTier;
import com.cfdeploytool.model.EnvironmentConfig;
import com.cfdeploytool.model.Server;
import com.cfdeploytool.service.BackupService;
import com.cfdeploytool.service.EnvironmentService;
import com.cfdeploytool.service.HttpDeploymentClient.BackupFilesResult;
import com.cfdeploytool.service.HttpDeploymentClient.ListDirectoryResult;
import com.cfdeploytool.service.HttpDeploymentClient.RemoteFileEntry;
import com.cfdeploytool.service.ServerManager;
import com.cfdeploytool.ui.MainFrame;
import com.cfdeploytool.ui.ThemeManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Backup selected files from the agent's 2T deploy path (CustomFunction or Plugin).
 */
public class BackupPanel extends JPanel {

    private final BackupService backupService;
    private final ServerManager serverManager;
    private final EnvironmentService environmentService;
    private final MainFrame mainFrame;

    private JRadioButton customFunctionRadio;
    private JRadioButton pluginRadio;
    private JRadioButton pluginDllRadio;
    private JLabel sourcePathLabel;
    private JTextField backupDirField;
    private JComboBox<String> serverCombo;
    private JPanel fileListPanel;
    private final List<JCheckBox> fileCheckboxes = new ArrayList<>();
    private JButton loadFilesButton;
    private JButton backupButton;
    private JButton selectAllButton;
    private JButton selectNoneButton;
    private JLabel statusLabel;
    private JProgressBar progressBar;

    private DeploymentCategory selectedCategory = DeploymentCategory.CUSTOM_FUNCTION;

    public BackupPanel(BackupService backupService, ServerManager serverManager,
                       EnvironmentService environmentService, MainFrame mainFrame) {
        this.backupService = backupService;
        this.serverManager = serverManager;
        this.environmentService = environmentService;
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout(0, 0));
        setBackground(ThemeManager.BG_SURFACE);
        setBorder(new EmptyBorder(16, 20, 16, 20));
        initComponents();
        refreshFromEnvironment();
        environmentService.addChangeListener(this::refreshFromEnvironment);
    }

    private void initComponents() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ThemeManager.BG_SURFACE);
        headerPanel.setBorder(new EmptyBorder(0, 0, 12, 0));

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBackground(ThemeManager.BG_SURFACE);
        titlePanel.add(ThemeManager.createHeaderLabel("Backup"));
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(ThemeManager.createSecondaryLabel(
                "List files from the 2T deploy path on the agent and copy selected files to the backup directory"));

        headerPanel.add(titlePanel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        JPanel card = ThemeManager.createCardPanel();
        card.setLayout(new BorderLayout(0, 12));

        card.add(buildTypeSection(), BorderLayout.NORTH);
        card.add(buildFileSection(), BorderLayout.CENTER);
        card.add(buildActionSection(), BorderLayout.SOUTH);

        add(card, BorderLayout.CENTER);
    }

    private JPanel buildTypeSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(ThemeManager.BG_CARD);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(fieldLabel("Backup type:"), gbc);

        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        typePanel.setBackground(ThemeManager.BG_CARD);
        customFunctionRadio = new JRadioButton("CustomFunction", true);
        pluginRadio = new JRadioButton("Plugin JAR");
        pluginDllRadio = new JRadioButton("Plugin DLL");
        styleRadio(customFunctionRadio);
        styleRadio(pluginRadio);
        styleRadio(pluginDllRadio);
        ButtonGroup group = new ButtonGroup();
        group.add(customFunctionRadio);
        group.add(pluginRadio);
        group.add(pluginDllRadio);
        typePanel.add(customFunctionRadio);
        typePanel.add(pluginRadio);
        typePanel.add(pluginDllRadio);
        customFunctionRadio.addActionListener(e -> onCategoryChanged(DeploymentCategory.CUSTOM_FUNCTION));
        pluginRadio.addActionListener(e -> onCategoryChanged(DeploymentCategory.PLUGIN));
        pluginDllRadio.addActionListener(e -> onCategoryChanged(DeploymentCategory.PLUGIN_DLL));

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(typePanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panel.add(fieldLabel("2T source path:"), gbc);

        sourcePathLabel = new JLabel(" ");
        sourcePathLabel.setFont(ThemeManager.FONT_REGULAR);
        sourcePathLabel.setForeground(ThemeManager.TEXT_PRIMARY);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(sourcePathLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        panel.add(fieldLabel("Agent server:"), gbc);

        serverCombo = new JComboBox<>();
        serverCombo.setFont(ThemeManager.FONT_REGULAR);
        serverCombo.setBackground(ThemeManager.BG_INPUT);
        serverCombo.setForeground(ThemeManager.TEXT_PRIMARY);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(serverCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(fieldLabel("Backup directory:"), gbc);

        backupDirField = styledTextField();
        backupDirField.setToolTipText("Backup root on the agent (Settings: corporate.backup.path)");
        gbc.gridx = 1;
        panel.add(backupDirField, gbc);

        return panel;
    }

    private JPanel buildFileSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(ThemeManager.BG_CARD);
        panel.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(ThemeManager.BG_CARD);

        JLabel filesLabel = new JLabel("Files on agent (2T)");
        filesLabel.setFont(ThemeManager.FONT_BOLD);
        filesLabel.setForeground(ThemeManager.TEXT_PRIMARY);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnRow.setBackground(ThemeManager.BG_CARD);
        loadFilesButton = ThemeManager.createSecondaryButton("Load files");
        loadFilesButton.addActionListener(e -> loadFiles());
        selectAllButton = ThemeManager.createSecondaryButton("Select all");
        selectAllButton.addActionListener(e -> setAllSelected(true));
        selectNoneButton = ThemeManager.createSecondaryButton("Select none");
        selectNoneButton.addActionListener(e -> setAllSelected(false));
        btnRow.add(loadFilesButton);
        btnRow.add(selectAllButton);
        btnRow.add(selectNoneButton);

        toolbar.add(filesLabel, BorderLayout.WEST);
        toolbar.add(btnRow, BorderLayout.EAST);
        panel.add(toolbar, BorderLayout.NORTH);

        fileListPanel = new JPanel();
        fileListPanel.setLayout(new BoxLayout(fileListPanel, BoxLayout.Y_AXIS));
        fileListPanel.setBackground(ThemeManager.BG_INPUT);

        JScrollPane scroll = ThemeManager.styleScrollPane(fileListPanel);
        scroll.setPreferredSize(new Dimension(400, 280));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildActionSection() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(ThemeManager.BG_CARD);
        panel.setBorder(new EmptyBorder(8, 0, 0, 0));

        statusLabel = ThemeManager.createSecondaryLabel("Choose type, then Load files from the agent.");
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        ThemeManager.styleProgressBar(progressBar);
        progressBar.setVisible(false);

        JPanel progressPanel = new JPanel(new BorderLayout(4, 4));
        progressPanel.setBackground(ThemeManager.BG_CARD);
        progressPanel.add(statusLabel, BorderLayout.NORTH);
        progressPanel.add(progressBar, BorderLayout.CENTER);

        backupButton = ThemeManager.createAccentButton("Backup selected");
        backupButton.setPreferredSize(new Dimension(180, 40));
        backupButton.addActionListener(e -> startBackup());

        panel.add(progressPanel, BorderLayout.CENTER);
        panel.add(backupButton, BorderLayout.EAST);
        return panel;
    }

    private void onCategoryChanged(DeploymentCategory category) {
        selectedCategory = category;
        clearFileList();
        refreshSourcePathLabel();
    }

    private void refreshFromEnvironment() {
        refreshServerCombo();
        refreshSourcePathLabel();
        EnvironmentConfig env = environmentService.getActiveEnvironment();
        backupDirField.setText(env.getBackupDir() != null ? env.getBackupDir() : "");
    }

    private void refreshSourcePathLabel() {
        String path = environmentService.resolvePath(selectedCategory, DeploymentTier.T2);
        sourcePathLabel.setText(path != null && !path.isBlank() ? path : "(not configured - set in Settings)");
    }

    private void refreshServerCombo() {
        serverCombo.removeAllItems();
        List<Server> servers = serverManager.getServers();
        if (servers.isEmpty()) {
            serverCombo.addItem("(No servers - add in Servers tab)");
            return;
        }
        Optional<Server> corporate = serverManager.getCorporateServer();
        int selectIndex = 0;
        for (int i = 0; i < servers.size(); i++) {
            Server s = servers.get(i);
            serverCombo.addItem(s.getName() + "  (" + s.getHost() + ":" + s.getPort() + ")");
            if (corporate.isPresent() && s.getId().equals(corporate.get().getId())) {
                selectIndex = i;
            }
        }
        serverCombo.setSelectedIndex(selectIndex);
    }

    private Server resolveSelectedServer() {
        List<Server> servers = serverManager.getServers();
        int idx = serverCombo.getSelectedIndex();
        if (servers.isEmpty() || idx < 0 || idx >= servers.size()) {
            return null;
        }
        return servers.get(idx);
    }

    private String resolveSourcePath() {
        return environmentService.resolvePath(selectedCategory, DeploymentTier.T2);
    }

    private void loadFiles() {
        Server server = resolveSelectedServer();
        if (server == null) {
            JOptionPane.showMessageDialog(this,
                    "Register a server in the Servers tab first.",
                    "No server", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String path = resolveSourcePath();
        if (path == null || path.isBlank()) {
            JOptionPane.showMessageDialog(this,
                    "Configure the 2T path for this type in Settings.",
                    "Path not set", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setBusy(true, "Loading files from agent...");
        SwingWorker<ListDirectoryResult, Void> worker = new SwingWorker<>() {
            @Override
            protected ListDirectoryResult doInBackground() {
                return backupService.listFiles(server, path);
            }

            @Override
            protected void done() {
                setBusy(false, null);
                try {
                    ListDirectoryResult result = get();
                    if (!result.isSuccess()) {
                        statusLabel.setText(result.getMessage());
                        mainFrame.setStatus("List failed: " + result.getMessage());
                        JOptionPane.showMessageDialog(BackupPanel.this, result.getMessage(),
                                "List failed", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    populateFileList(result.getFiles());
                    statusLabel.setText(result.getFiles().size() + " file(s) - select items to back up");
                    mainFrame.setStatus("Listed " + result.getFiles().size() + " file(s) from agent");
                } catch (Exception e) {
                    statusLabel.setText("Error: " + e.getMessage());
                    mainFrame.setStatus("List failed");
                }
            }
        };
        worker.execute();
    }

    private void populateFileList(List<RemoteFileEntry> files) {
        clearFileList();
        if (files.isEmpty()) {
            JLabel empty = new JLabel("  No files in this directory on the agent.");
            empty.setFont(ThemeManager.FONT_REGULAR);
            empty.setForeground(ThemeManager.TEXT_SECONDARY);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            fileListPanel.add(empty);
        } else {
            for (RemoteFileEntry entry : files) {
                JCheckBox cb = new JCheckBox(entry.name() + "  (" + entry.formattedSize() + ")");
                cb.putClientProperty("fileName", entry.name());
                cb.setFont(ThemeManager.FONT_REGULAR);
                cb.setBackground(ThemeManager.BG_INPUT);
                cb.setForeground(ThemeManager.TEXT_PRIMARY);
                cb.setAlignmentX(Component.LEFT_ALIGNMENT);
                fileCheckboxes.add(cb);
                fileListPanel.add(cb);
            }
        }
        fileListPanel.revalidate();
        fileListPanel.repaint();
    }

    private void clearFileList() {
        fileCheckboxes.clear();
        fileListPanel.removeAll();
        fileListPanel.revalidate();
        fileListPanel.repaint();
    }

    private void setAllSelected(boolean selected) {
        for (JCheckBox cb : fileCheckboxes) {
            cb.setSelected(selected);
        }
    }

    private List<String> getSelectedFileNames() {
        List<String> names = new ArrayList<>();
        for (JCheckBox cb : fileCheckboxes) {
            if (cb.isSelected()) {
                Object name = cb.getClientProperty("fileName");
                if (name != null) {
                    names.add(name.toString());
                }
            }
        }
        return names;
    }

    private void startBackup() {
        Server server = resolveSelectedServer();
        if (server == null) {
            JOptionPane.showMessageDialog(this, "Select a server first.", "No server",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String sourcePath = resolveSourcePath();
        String backupDir = backupDirField.getText().trim();
        if (backupDir.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Enter a backup directory (agent machine path).",
                    "Backup directory", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<String> selected = getSelectedFileNames();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Select at least one file, or use Load files first.",
                    "No files selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Back up " + selected.size() + " file(s) from:\n  " + sourcePath
                        + "\n\nto backup root on agent:\n  " + backupDir
                        + "\n\nServer: " + server.getName(),
                "Confirm backup", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        setBusy(true, "Backing up " + selected.size() + " file(s)...");
        SwingWorker<BackupFilesResult, Void> worker = new SwingWorker<>() {
            @Override
            protected BackupFilesResult doInBackground() {
                return backupService.backupSelected(server, sourcePath, backupDir, selected);
            }

            @Override
            protected void done() {
                setBusy(false, null);
                try {
                    BackupFilesResult result = get();
                    if (result.isSuccess()) {
                        String msg = result.getMessage()
                                + (result.getBackupPath() != null ? "\n" + result.getBackupPath() : "");
                        statusLabel.setText(result.getMessage());
                        mainFrame.setStatus("Backup OK - " + result.getFileCount() + " file(s)");
                        JOptionPane.showMessageDialog(BackupPanel.this, msg, "Backup complete",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        statusLabel.setText(result.getMessage());
                        mainFrame.setStatus("Backup failed");
                        JOptionPane.showMessageDialog(BackupPanel.this, result.getMessage(),
                                "Backup failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    statusLabel.setText("Error: " + e.getMessage());
                    mainFrame.setStatus("Backup failed");
                }
            }
        };
        worker.execute();
    }

    private void setBusy(boolean busy, String message) {
        loadFilesButton.setEnabled(!busy);
        backupButton.setEnabled(!busy);
        selectAllButton.setEnabled(!busy);
        selectNoneButton.setEnabled(!busy);
        customFunctionRadio.setEnabled(!busy);
        pluginRadio.setEnabled(!busy);
        pluginDllRadio.setEnabled(!busy);
        progressBar.setVisible(busy);
        if (message != null) {
            statusLabel.setText(message);
        }
    }

    public void refreshData() {
        refreshFromEnvironment();
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(ThemeManager.FONT_BOLD);
        label.setForeground(ThemeManager.TEXT_SECONDARY);
        label.setPreferredSize(new Dimension(120, 28));
        return label;
    }

    private JTextField styledTextField() {
        JTextField field = new JTextField(40);
        field.setFont(ThemeManager.FONT_REGULAR);
        field.setBackground(ThemeManager.BG_INPUT);
        field.setForeground(ThemeManager.TEXT_PRIMARY);
        field.setCaretColor(ThemeManager.TEXT_PRIMARY);
        field.setBorder(new CompoundBorder(
                new LineBorder(ThemeManager.BORDER, 1, true),
                new EmptyBorder(6, 10, 6, 10)));
        return field;
    }

    private void styleRadio(JRadioButton radio) {
        radio.setFont(ThemeManager.FONT_REGULAR);
        radio.setBackground(ThemeManager.BG_CARD);
        radio.setForeground(ThemeManager.TEXT_PRIMARY);
    }
}
