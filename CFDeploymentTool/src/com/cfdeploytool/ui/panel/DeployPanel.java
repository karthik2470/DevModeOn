package com.cfdeploytool.ui.panel;

import com.cfdeploytool.model.*;
import com.cfdeploytool.model.DeploymentFile.FileType;
import com.cfdeploytool.service.DeploymentService;
import com.cfdeploytool.service.EnvironmentService;
import com.cfdeploytool.service.ServerManager;
import com.cfdeploytool.ui.MainFrame;
import com.cfdeploytool.ui.ThemeManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for selecting files and target servers, and initiating deployment.
 */
public class DeployPanel extends JPanel {

    private final ServerManager serverManager;
    private final EnvironmentService environmentService;
    private final DeploymentService deploymentService;
    private final ProgressPanel progressPanel;
    private final HistoryPanel historyPanel;
    private final MainFrame mainFrame;

    // File selection
    private final List<DeploymentFile> selectedFiles = new ArrayList<>();
    private JTable fileTable;
    private FileTableModel fileTableModel;

    // Server selection
    private JPanel serverCheckboxPanel;
    private final List<JCheckBox> serverCheckboxes = new ArrayList<>();

    // Category selection
    private JRadioButton customFunctionRadio;
    private JRadioButton pluginRadio;
    private JLabel categoryPathLabel;

    // Version (4T, manual update)
    private JTable versionTable;
    private DefaultTableModel versionTableModel;
    private JTextField newVersionField;
    private JLabel versionPathHintLabel;
    private JButton refreshVersionBtn;
    private int versionReadGeneration;
    private JButton suggestVersionBtn;
    private JButton updateVersionBtn;

    // Deploy button
    private JButton deployButton;
    private JLabel corporateRequirementLabel;

    public DeployPanel(ServerManager serverManager, DeploymentService deploymentService,
                       EnvironmentService environmentService,
                       ProgressPanel progressPanel, HistoryPanel historyPanel, MainFrame mainFrame) {
        this.serverManager = serverManager;
        this.environmentService = environmentService;
        this.deploymentService = deploymentService;
        this.progressPanel = progressPanel;
        this.historyPanel = historyPanel;
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout(0, 0));
        setBackground(ThemeManager.BG_SURFACE);
        setBorder(new EmptyBorder(16, 20, 16, 20));
        initComponents();
        environmentService.addChangeListener(this::onEnvironmentChanged);
    }

    public void onEnvironmentChanged() {
        SwingUtilities.invokeLater(this::updateCategoryInfo);
    }

    private void initComponents() {
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBackground(ThemeManager.BG_SURFACE);

        JLabel titleLabel = ThemeManager.createHeaderLabel("Deploy Files");
        JLabel subtitleLabel = ThemeManager.createSecondaryLabel(
                "Select files and target servers for deployment");
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(subtitleLabel);

        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.setBackground(ThemeManager.BG_SURFACE);
        leftColumn.add(titlePanel);
        leftColumn.add(Box.createVerticalStrut(10));
        leftColumn.add(createCategoryPanel());

        JPanel headerRow = new JPanel(new GridBagLayout());
        headerRow.setBackground(ThemeManager.BG_SURFACE);
        GridBagConstraints rowGbc = new GridBagConstraints();
        rowGbc.gridx = 0;
        rowGbc.gridy = 0;
        rowGbc.weightx = 1.0;
        rowGbc.weighty = 1.0;
        rowGbc.fill = GridBagConstraints.BOTH;
        rowGbc.anchor = GridBagConstraints.NORTHWEST;
        rowGbc.insets = new Insets(0, 0, 0, 16);
        headerRow.add(leftColumn, rowGbc);

        rowGbc.gridx = 1;
        rowGbc.weightx = 0;
        rowGbc.fill = GridBagConstraints.VERTICAL;
        rowGbc.anchor = GridBagConstraints.NORTHEAST;
        rowGbc.insets = new Insets(0, 0, 0, 0);
        JPanel versionPanel = createVersionPanel();
        headerRow.add(versionPanel, rowGbc);

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBackground(ThemeManager.BG_SURFACE);
        northPanel.setBorder(new EmptyBorder(0, 0, 12, 0));
        northPanel.add(headerRow, BorderLayout.CENTER);
        northPanel.setPreferredSize(new Dimension(0, 220));
        add(northPanel, BorderLayout.NORTH);

        // Split pane: Files (left) | Servers (right)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setBackground(ThemeManager.BG_SURFACE);
        splitPane.setDividerLocation(600);
        splitPane.setDividerSize(6);
        splitPane.setBorder(null);

        splitPane.setLeftComponent(createFilePanel());
        splitPane.setRightComponent(createServerSelectionPanel());

        add(splitPane, BorderLayout.CENTER);

        // Bottom: Deploy button
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottomPanel.setBackground(ThemeManager.BG_SURFACE);
        bottomPanel.setBorder(new EmptyBorder(16, 0, 4, 0));

        deployButton = ThemeManager.createPrimaryActionButton("Deploy Selected Files");
        deployButton.setEnabled(false);
        deployButton.addActionListener(e -> startDeployment());
        bottomPanel.add(deployButton);

        add(bottomPanel, BorderLayout.SOUTH);
        updateCategoryInfo();
    }

    private JPanel createCategoryPanel() {
        JPanel panel = ThemeManager.createCardPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        typePanel.setBackground(ThemeManager.BG_CARD);
        typePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        customFunctionRadio = new JRadioButton("CustomFunction");
        pluginRadio = new JRadioButton("Plugin");
        ButtonGroup typeGroup = new ButtonGroup();
        typeGroup.add(customFunctionRadio);
        typeGroup.add(pluginRadio);
        customFunctionRadio.setSelected(true);

        for (JRadioButton rb : new JRadioButton[]{customFunctionRadio, pluginRadio}) {
            ThemeManager.styleRadio(rb);
            rb.addActionListener(e -> updateCategoryInfo());
            typePanel.add(rb);
        }

        categoryPathLabel = ThemeManager.createSecondaryLabel("");
        categoryPathLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tierHint = ThemeManager.createSecondaryLabel(
                "Other servers -> 2T path  |  Corporate servers -> 2T + 4T paths");
        tierHint.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(typePanel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(tierHint);
        panel.add(Box.createVerticalStrut(6));
        panel.add(categoryPathLabel);
        return panel;
    }

    private JPanel createVersionPanel() {
        JPanel panel = ThemeManager.createCardPanel();
        panel.setLayout(new BorderLayout(0, 8));
        panel.setPreferredSize(new Dimension(460, 200));
        panel.setMinimumSize(new Dimension(420, 190));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(ThemeManager.BG_CARD);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Version (Corporate / 4T)");
        title.setFont(ThemeManager.FONT_BOLD);
        title.setForeground(ThemeManager.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        DeploymentCategory category = DeploymentCategory.CUSTOM_FUNCTION;
        versionPathHintLabel = new JLabel();
        versionPathHintLabel.setFont(ThemeManager.FONT_SMALL);
        versionPathHintLabel.setForeground(ThemeManager.TEXT_SECONDARY);
        versionPathHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        updateVersionPathHint(category);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(versionPathHintLabel);
        panel.add(header, BorderLayout.NORTH);

        versionTableModel = new DefaultTableModel(new String[]{"Server", "Version"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        versionTableModel.addRow(new Object[]{"-", "Select Corporate server(s), then Refresh"});
        versionTable = new JTable(versionTableModel);
        ThemeManager.styleTable(versionTable);
        versionTable.setRowHeight(26);
        versionTable.getColumnModel().getColumn(0).setPreferredWidth(140);
        versionTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        versionTable.setFillsViewportHeight(true);

        JScrollPane tableScroll = ThemeManager.styleScrollPane(versionTable);
        tableScroll.setPreferredSize(new Dimension(420, 88));
        tableScroll.setMinimumSize(new Dimension(400, 72));
        panel.add(tableScroll, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBackground(ThemeManager.BG_CARD);

        JPanel refreshRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        refreshRow.setBackground(ThemeManager.BG_CARD);
        refreshRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        refreshVersionBtn = ThemeManager.createSecondaryButton("Refresh");
        refreshVersionBtn.addActionListener(e -> refreshServerVersions());
        suggestVersionBtn = ThemeManager.createSecondaryButton("Suggest +1");
        suggestVersionBtn.addActionListener(e -> suggestNextVersion());
        refreshRow.add(refreshVersionBtn);
        refreshRow.add(suggestVersionBtn);

        JPanel updateRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        updateRow.setBackground(ThemeManager.BG_CARD);
        updateRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel newVerLabel = ThemeManager.createSecondaryLabel("New version:");
        newVersionField = ThemeManager.styleTextField(new JTextField(8));
        newVersionField.setPreferredSize(new Dimension(72, 32));
        updateVersionBtn = ThemeManager.createAccentButton("Update version");
        updateVersionBtn.addActionListener(e -> updateVersionOnServers());
        updateRow.add(newVerLabel);
        updateRow.add(newVersionField);
        updateRow.add(updateVersionBtn);

        footer.add(refreshRow);
        footer.add(Box.createVerticalStrut(6));
        footer.add(updateRow);
        panel.add(footer, BorderLayout.SOUTH);

        return panel;
    }

    private DeploymentCategory getSelectedCategory() {
        return pluginRadio.isSelected() ? DeploymentCategory.PLUGIN : DeploymentCategory.CUSTOM_FUNCTION;
    }

    private void updateCategoryInfo() {
        DeploymentCategory category = getSelectedCategory();

        if (categoryPathLabel != null) {
            categoryPathLabel.setText("<html><b>Other (2T):</b> " + escapeHtml(category.getPath(DeploymentTier.T2))
                    + "<br><b>Corporate (4T):</b> " + escapeHtml(category.getPath(DeploymentTier.T4)) + "</html>");
        }
        updateVersionPathHint(category);
        if (serverCheckboxPanel != null) {
            refreshServerList();
        } else {
            refreshServerVersions();
        }
        updateDeployButtonState();
    }

    private void updateVersionPathHint(DeploymentCategory category) {
        if (versionPathHintLabel != null) {
            versionPathHintLabel.setText("<html><b>File:</b> " + escapeHtml(category.getVersionFileName())
                    + " | <b>4T path:</b> " + escapeHtml(category.getVersionPath()) + "</html>");
        }
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void refreshServerVersions() {
        if (versionTableModel == null) {
            return;
        }

        List<Server> selected = getSelectedServers();
        List<Server> servers = selected.stream().filter(Server::isCorporate).toList();
        if (selected.isEmpty()) {
            setVersionTableMessage("-", "Select server(s) on the right, then Refresh");
            return;
        }
        if (servers.isEmpty()) {
            setVersionTableMessage("-", "Corporate server required for 4T version");
            return;
        }

        final int generation = ++versionReadGeneration;
        DeploymentCategory category = getSelectedCategory();
        refreshVersionBtn.setEnabled(false);
        setVersionTableMessage("-", "Loading...");

        SwingWorker<List<com.cfdeploytool.service.HttpDeploymentClient.VersionReadResult>, Void> worker =
                new SwingWorker<>() {
            @Override
            protected List<com.cfdeploytool.service.HttpDeploymentClient.VersionReadResult> doInBackground() {
                return deploymentService.readVersions(servers, category);
            }

            @Override
            protected void done() {
                refreshVersionBtn.setEnabled(true);
                if (generation != versionReadGeneration) {
                    return;
                }
                try {
                    displayVersionResults(get());
                } catch (Exception e) {
                    setVersionTableMessage("-", "Failed: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void setVersionTableMessage(String server, String message) {
        versionTableModel.setRowCount(0);
        versionTableModel.addRow(new Object[]{server, message});
    }

    private void displayVersionResults(
            List<com.cfdeploytool.service.HttpDeploymentClient.VersionReadResult> results) {
        versionTableModel.setRowCount(0);
        if (results.isEmpty()) {
            versionTableModel.addRow(new Object[]{"-", "No version data"});
            return;
        }
        for (var r : results) {
            String version;
            if (r.isSuccess()) {
                version = r.getVersion() != null ? r.getVersion() : "0";
            } else {
                version = "Error: " + r.getMessage();
            }
            versionTableModel.addRow(new Object[]{r.getServer().getName(), version});
        }
    }

    private void suggestNextVersion() {
        for (int row = 0; row < versionTableModel.getRowCount(); row++) {
            String ver = String.valueOf(versionTableModel.getValueAt(row, 1));
            if (ver.isBlank() || ver.startsWith("-") || ver.startsWith("Select")
                    || ver.startsWith("Loading") || ver.startsWith("Corporate")
                    || ver.startsWith("Error") || ver.startsWith("Failed")) {
                continue;
            }
            newVersionField.setText(VersionUtil.increment(ver.trim()));
            return;
        }
        newVersionField.setText("1");
    }

    private void updateVersionOnServers() {
        List<Server> servers = getSelectedServers().stream().filter(Server::isCorporate).toList();
        String newVersion = newVersionField.getText().trim();
        if (servers.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Select at least one Corporate server for version update (4T path only).",
                    "Version Update", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (newVersion.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a new version value.", "Version Update",
                    JOptionPane.WARNING_MESSAGE);
            newVersionField.requestFocus();
            return;
        }

        DeploymentCategory category = getSelectedCategory();
        int confirm = JOptionPane.showConfirmDialog(this,
                "Update " + category.getVersionFileName() + " to \"" + newVersion + "\" on "
                        + servers.size() + " server(s)?\n\n4T path: " + category.getVersionPath(),
                "Confirm Version Update", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        updateVersionBtn.setEnabled(false);
        mainFrame.setStatus("Updating version...");

        SwingWorker<List<com.cfdeploytool.service.HttpDeploymentClient.VersionUpdateResult>, Void> worker =
                new SwingWorker<>() {
            @Override
            protected List<com.cfdeploytool.service.HttpDeploymentClient.VersionUpdateResult> doInBackground() {
                return deploymentService.updateVersions(servers, category, newVersion);
            }

            @Override
            protected void done() {
                updateVersionBtn.setEnabled(true);
                try {
                    List<com.cfdeploytool.service.HttpDeploymentClient.VersionUpdateResult> results = get();
                    int ok = 0;
                    for (var r : results) {
                        if (r.isSuccess()) {
                            ok++;
                        }
                    }
                    refreshServerVersions();
                    mainFrame.setStatus("Version updated on " + ok + "/" + results.size() + " server(s)");
                } catch (Exception e) {
                    mainFrame.setStatus("Version update failed");
                    JOptionPane.showMessageDialog(DeployPanel.this, e.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private JPanel createFilePanel() {
        JPanel panel = ThemeManager.createCardPanel();
        panel.setLayout(new BorderLayout(0, 8));

        // File section header
        JPanel fileHeader = new JPanel(new BorderLayout());
        fileHeader.setBackground(ThemeManager.BG_CARD);

        JLabel fileLabel = new JLabel("Selected Files");
        fileLabel.setFont(ThemeManager.FONT_BOLD);
        fileLabel.setForeground(ThemeManager.TEXT_PRIMARY);

        JButton addFileBtn = ThemeManager.createAccentButton("Add Files");
        addFileBtn.addActionListener(e -> addFiles());
        JButton removeFileBtn = ThemeManager.createSecondaryButton("Remove");
        removeFileBtn.addActionListener(e -> removeSelectedFiles());
        JButton clearFileBtn = ThemeManager.createSecondaryButton("Clear All");
        clearFileBtn.addActionListener(e -> clearFiles());

        fileHeader.add(fileLabel, BorderLayout.WEST);
        fileHeader.add(ThemeManager.createActionBar(clearFileBtn, removeFileBtn, addFileBtn), BorderLayout.EAST);

        panel.add(fileHeader, BorderLayout.NORTH);

        // File table
        fileTableModel = new FileTableModel();
        fileTable = new JTable(fileTableModel);
        ThemeManager.styleTable(fileTable);
        fileTable.getColumnModel().getColumn(0).setPreferredWidth(250); // Name
        fileTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Type
        fileTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Size

        panel.add(ThemeManager.styleScrollPane(fileTable), BorderLayout.CENTER);

        // File count label
        JLabel fileCountLabel = ThemeManager.createSecondaryLabel("No files selected");
        fileCountLabel.setBorder(new EmptyBorder(4, 0, 0, 0));
        panel.add(fileCountLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createServerSelectionPanel() {
        JPanel panel = ThemeManager.createCardPanel();
        panel.setLayout(new BorderLayout(0, 8));

        // Server section header
        JPanel serverHeader = new JPanel(new BorderLayout());
        serverHeader.setBackground(ThemeManager.BG_CARD);

        JLabel serverLabel = new JLabel("");
        serverLabel.setFont(ThemeManager.FONT_BOLD);
        serverLabel.setForeground(ThemeManager.TEXT_PRIMARY);

        JButton refreshBtn = ThemeManager.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> refreshServerList());
        JButton deselectAllBtn = ThemeManager.createSecondaryButton("Deselect All");
        deselectAllBtn.addActionListener(e -> toggleAllServers(false));
        JButton selectAllBtn = ThemeManager.createSecondaryButton("Select All");
        selectAllBtn.addActionListener(e -> toggleAllServers(true));

        serverHeader.add(serverLabel, BorderLayout.WEST);
        serverHeader.add(ThemeManager.createActionBar(refreshBtn, deselectAllBtn, selectAllBtn), BorderLayout.EAST);

        JPanel serverNorth = new JPanel(new BorderLayout(0, 6));
        serverNorth.setBackground(ThemeManager.BG_CARD);
        serverNorth.add(serverHeader, BorderLayout.NORTH);
        corporateRequirementLabel = ThemeManager.createSecondaryLabel("");
        corporateRequirementLabel.setBorder(new EmptyBorder(0, 4, 4, 4));
        serverNorth.add(corporateRequirementLabel, BorderLayout.SOUTH);
        panel.add(serverNorth, BorderLayout.NORTH);

        // Server checkboxes in scroll pane
        serverCheckboxPanel = new JPanel();
        serverCheckboxPanel.setLayout(new BoxLayout(serverCheckboxPanel, BoxLayout.Y_AXIS));
        serverCheckboxPanel.setBackground(ThemeManager.BG_CARD);
        serverCheckboxPanel.setBorder(new EmptyBorder(8, 4, 8, 4));

        refreshServerList();

        JScrollPane scrollPane = ThemeManager.styleScrollPane(serverCheckboxPanel);
        scrollPane.getViewport().setBackground(ThemeManager.BG_CARD);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    // ==================== FILE OPERATIONS ====================

    private void addFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("Select Deployment Files");

        // File filters
        chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("All Deployable Files", "exe", "jar", "dll", "bat", "xml", "properties", "yml", "yaml", "json", "cfg", "conf", "ini", "config"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Executables (*.exe)", "exe"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Batch Scripts (*.bat)", "bat"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("JAR Files (*.jar)", "jar"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("DLL Files (*.dll)", "dll"));
        chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("Config Files", "xml", "properties", "yml", "yaml", "json", "cfg", "conf", "ini", "config"));
        chooser.setAcceptAllFileFilterUsed(true);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = chooser.getSelectedFiles();
            for (File file : files) {
                FileType type = DeploymentFile.inferFileType(file);
                DeploymentFile df = new DeploymentFile(file, type);
                if (!selectedFiles.contains(df)) {
                    selectedFiles.add(df);
                }
            }
            fileTableModel.fireTableDataChanged();
            updateDeployButtonState();
        }
    }

    private void removeSelectedFiles() {
        int[] rows = fileTable.getSelectedRows();
        if (rows.length == 0) return;

        // Remove in reverse order to maintain indices
        List<DeploymentFile> toRemove = new ArrayList<>();
        for (int row : rows) {
            if (row < selectedFiles.size()) {
                toRemove.add(selectedFiles.get(row));
            }
        }
        selectedFiles.removeAll(toRemove);
        fileTableModel.fireTableDataChanged();
        updateDeployButtonState();
    }

    private void clearFiles() {
        selectedFiles.clear();
        fileTableModel.fireTableDataChanged();
        updateDeployButtonState();
    }

    // ==================== SERVER SELECTION ====================

    /** Called when opening Deploy tab or after server registry changes. */
    public void refreshServers() {
        refreshServerList();
    }

    private void refreshServerList() {
        serverCheckboxPanel.removeAll();
        serverCheckboxes.clear();

        updateCorporateRequirementLabel();

        List<Server> servers = serverManager.getServers();
        if (!serverManager.hasCorporateServer()) {
            JLabel emptyLabel = ThemeManager.createSecondaryLabel(
                    "  Add one Corporate server on the Servers tab before you can deploy.");
            emptyLabel.setBorder(new EmptyBorder(20, 8, 20, 8));
            serverCheckboxPanel.add(emptyLabel);
        } else if (servers.isEmpty()) {
            JLabel emptyLabel = ThemeManager.createSecondaryLabel(
                    "  No servers registered. Go to the Servers tab to add servers.");
            emptyLabel.setBorder(new EmptyBorder(20, 8, 20, 8));
            serverCheckboxPanel.add(emptyLabel);
        } else {
            for (Server server : servers) {
                JCheckBox cb = new JCheckBox();
                ThemeManager.styleCheckBox(cb);

                String statusIcon = switch (server.getStatus()) {
                    case ONLINE -> "[ON]";
                    case OFFLINE -> "[OFF]";
                    case UNKNOWN -> "[?]";
                };
                String typeTag = server.isCorporate() ? "Corporate, 2T+4T" : "Other, 2T";
                cb.setText("  " + statusIcon + "  " + server.getName()
                        + "  [" + typeTag + "]  (" + server.getHost() + ":" + server.getPort() + ")");
                cb.putClientProperty("serverId", server.getId());
                cb.addActionListener(e -> {
                    updateDeployButtonState();
                    refreshServerVersions();
                });

                JPanel row = new JPanel(new BorderLayout());
                row.setBackground(ThemeManager.BG_CARD);
                row.setBorder(new CompoundBorder(
                        new LineBorder(ThemeManager.BORDER, 0),
                        new EmptyBorder(6, 4, 6, 4)));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                row.add(cb, BorderLayout.CENTER);

                serverCheckboxes.add(cb);
                serverCheckboxPanel.add(row);
            }

            if (serverManager.getCorporateServers().size() == 1) {
                Server corporate = serverManager.getCorporateServers().get(0);
                for (JCheckBox cb : serverCheckboxes) {
                    if (corporate.getId().equals(cb.getClientProperty("serverId"))) {
                        cb.setSelected(true);
                        break;
                    }
                }
            }
        }

        serverCheckboxPanel.revalidate();
        serverCheckboxPanel.repaint();
        updateDeployButtonState();
        refreshServerVersions();
    }

    private void toggleAllServers(boolean selected) {
        for (JCheckBox cb : serverCheckboxes) {
            cb.setSelected(selected);
        }
        updateDeployButtonState();
        refreshServerVersions();
    }

    private List<Server> getSelectedServers() {
        List<Server> selected = new ArrayList<>();
        List<Server> allServers = serverManager.getServers();

        for (JCheckBox cb : serverCheckboxes) {
            if (cb.isSelected()) {
                String serverId = (String) cb.getClientProperty("serverId");
                for (Server s : allServers) {
                    if (s.getId().equals(serverId)) {
                        selected.add(s);
                        break;
                    }
                }
            }
        }
        return selected;
    }

    // ==================== DEPLOY ====================

    private void updateCorporateRequirementLabel() {
        if (corporateRequirementLabel == null) {
            return;
        }
        if (!serverManager.hasCorporateServer()) {
            corporateRequirementLabel.setForeground(ThemeManager.ERROR);
            corporateRequirementLabel.setText(
                    "Required: register exactly one Corporate server (Servers tab) before deploying.");
        } else {
            Server corporate = serverManager.getCorporateServer().orElse(null);
            corporateRequirementLabel.setForeground(ThemeManager.TEXT_SECONDARY);
            corporateRequirementLabel.setText("Corporate server: " + corporate.getName()
                    + " - must be selected to deploy.");
        }
    }

    private boolean isCorporateSelected() {
        return getSelectedServers().stream().anyMatch(Server::isCorporate);
    }

    private boolean canDeploy() {
        return !selectedFiles.isEmpty()
                && serverManager.hasCorporateServer()
                && isCorporateSelected()
                && serverCheckboxes.stream().anyMatch(JCheckBox::isSelected);
    }

    private void updateDeployButtonState() {
        if (deployButton == null) return;
        updateCorporateRequirementLabel();
        boolean canDeploy = canDeploy();
        deployButton.setEnabled(canDeploy);

        if (!serverManager.hasCorporateServer()) {
            deployButton.setText("Add Corporate Server to Deploy");
        } else if (!isCorporateSelected()) {
            deployButton.setText("Select Corporate Server to Deploy");
        } else if (canDeploy) {
            int serverCount = (int) serverCheckboxes.stream().filter(JCheckBox::isSelected).count();
            deployButton.setText("Deploy " + selectedFiles.size() + " file(s) ["
                    + getSelectedCategory().getDisplayName() + ", Other->2T / Corporate->2T+4T] to "
                    + serverCount + " server(s)");
        } else {
            deployButton.setText("Deploy Selected Files");
        }
    }

    /**
     * Prompts for a required ticket number. Returns null if the user cancels.
     */
    private String promptTicketNumber() {
        JTextField ticketField = ThemeManager.styleTextField(new JTextField(28));
        ticketField.setToolTipText("e.g. INC1234567, CHG-98765");

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(ThemeManager.BG_SURFACE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 8, 0);
        JLabel prompt = new JLabel("Ticket number (required):");
        prompt.setFont(ThemeManager.FONT_BOLD);
        prompt.setForeground(ThemeManager.TEXT_PRIMARY);
        form.add(prompt, gbc);

        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        form.add(ticketField, gbc);

        while (true) {
            int choice = JOptionPane.showConfirmDialog(this, form, "Ticket Number",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return null;
            }
            String ticket = ticketField.getText().trim();
            if (!ticket.isEmpty()) {
                return ticket;
            }
            JOptionPane.showMessageDialog(this,
                    "A ticket number is required before deployment can proceed.",
                    "Ticket Required", JOptionPane.WARNING_MESSAGE);
            ticketField.requestFocusInWindow();
        }
    }

    private void startDeployment() {
        if (!serverManager.hasCorporateServer()) {
            JOptionPane.showMessageDialog(this,
                    "Register a Corporate server on the Servers tab before deploying.",
                    "Corporate Server Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!isCorporateSelected()) {
            JOptionPane.showMessageDialog(this,
                    "Select the Corporate server in the target list to deploy.",
                    "Corporate Server Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<Server> targetServers = getSelectedServers();
        if (selectedFiles.isEmpty() || targetServers.isEmpty()) return;

        String ticketNumber = promptTicketNumber();
        if (ticketNumber == null) {
            return;
        }

        DeploymentCategory category = getSelectedCategory();
        DeploymentRequest request = new DeploymentRequest(
                new ArrayList<>(selectedFiles), targetServers, category, DeploymentScope.BOTH);
        request.setTicketNumber(ticketNumber);

        StringBuilder confirmMessage = new StringBuilder();
        confirmMessage.append("Ticket: ").append(ticketNumber).append("\n");
        confirmMessage.append("Category: ").append(category.getDisplayName()).append("\n");
        confirmMessage.append("Tiers: Other servers -> 2T, Corporate servers -> 2T + 4T\n");
        confirmMessage.append("Files: ").append(selectedFiles.size()).append("\n");
        confirmMessage.append("Servers: ").append(targetServers.size()).append("\n\n");
        confirmMessage.append("Target paths per server:\n");
        for (Server server : targetServers) {
            confirmMessage.append("  • ").append(server.getName());
            if (server.isCorporate()) {
                confirmMessage.append(" (Corporate - 2T + 4T, paths must exist)\n");
            } else {
                confirmMessage.append(" (Other - 2T)\n");
            }
            for (DeploymentTier tier : request.getTiersForServer(server)) {
                confirmMessage.append("      ").append(tier.getDisplayName())
                        .append(" -> ").append(category.getPath(tier)).append("\n");
            }
        }
        confirmMessage.append("\n\nCorporate servers: deploy folders are not auto-created if missing.");
        confirmMessage.append("\nCorporate deploy backups (2T deploy only, once per file):");
        confirmMessage.append("\n  - corporate.backup.path: existing file on server (before overwrite)");
        confirmMessage.append("\n  - corporate.Dbackup.path: new deployed file (after deploy)");
        confirmMessage.append("\n4T deploy copies the file with no extra backup.");
        confirmMessage.append("\nVersion is NOT auto-updated - use Version Update section on Deploy tab if needed.");
        confirmMessage.append("\nProceed with deployment?");

        int confirm = JOptionPane.showConfirmDialog(this, confirmMessage.toString(),
                "Confirm Deployment", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        // Reset and switch to progress tab
        progressPanel.reset(request);
        mainFrame.switchToProgressTab();
        mainFrame.setStatus("Deploying...");
        deployButton.setEnabled(false);

        // Run deployment in background
        SwingWorker<DeploymentHistory, Void> worker = new SwingWorker<>() {
            @Override
            protected DeploymentHistory doInBackground() {
                return deploymentService.deploy(request, progressPanel.createCallback());
            }

            @Override
            protected void done() {
                try {
                    DeploymentHistory history = get();
                    mainFrame.setStatus("Deployment complete - " + history.getOverallStatus());
                    historyPanel.refreshData();
                } catch (Exception e) {
                    mainFrame.setStatus("Deployment failed: " + e.getMessage());
                }
                deployButton.setEnabled(true);
                updateDeployButtonState();
            }
        };
        worker.execute();
    }

    // ==================== FILE TABLE MODEL ====================

    private class FileTableModel extends AbstractTableModel {
        private final String[] COLUMNS = {"File Name", "Type", "Size"};

        @Override
        public int getRowCount() {
            return selectedFiles.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= selectedFiles.size()) return "";
            DeploymentFile df = selectedFiles.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> df.getFileName();
                case 1 -> df.getFileType().getDisplayName();
                case 2 -> df.getFormattedSize();
                default -> "";
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
}
