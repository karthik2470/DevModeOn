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
    private JComboBox<DeploymentCategory> deployModeCombo;

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
    private JCheckBox autoBackupCheck;
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
        setBorder(new EmptyBorder(8, 12, 8, 12));
        initComponents();
        environmentService.addChangeListener(this::onEnvironmentChanged);
    }

    public void onEnvironmentChanged() {
        SwingUtilities.invokeLater(this::updateCategoryInfo);
    }

    private void initComponents() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(ThemeManager.BG_SURFACE);
        topPanel.setBorder(new EmptyBorder(0, 0, 6, 0));

        JPanel headerPanel = ThemeManager.createPageHeader("Deploy Files",
                "Select files and target servers for deployment");

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        headerActions.setOpaque(false);

        autoBackupCheck = new JCheckBox("Auto Backup");
        ThemeManager.styleCheckBox(autoBackupCheck);
        autoBackupCheck.setSelected(true);

        deployButton = ThemeManager.createPrimaryActionButton("Deploy Selected Files");
        deployButton.setEnabled(false);
        deployButton.addActionListener(e -> startDeployment());

        headerActions.add(autoBackupCheck);
        headerActions.add(deployButton);
        headerPanel.add(headerActions, BorderLayout.EAST);

        topPanel.add(headerPanel);
        add(topPanel, BorderLayout.NORTH);

        // Right column container containing: Deploy Mode at top, and SplitPane for Servers/Version
        JPanel rightColumn = new JPanel(new GridBagLayout());
        rightColumn.setBackground(ThemeManager.BG_SURFACE);

        GridBagConstraints rGbc = new GridBagConstraints();
        rGbc.gridx = 0;
        rGbc.fill = GridBagConstraints.BOTH;
        rGbc.weightx = 1.0;

        // 1. Deploy Mode Panel (fixed height)
        rGbc.gridy = 0;
        rGbc.weighty = 0.0;
        rGbc.insets = new Insets(0, 0, 6, 0);
        JPanel deployModePanel = createDeployModePanel();
        rightColumn.add(deployModePanel, rGbc);

        // 2. Resizable Vertical Split (Target Servers and Version)
        rGbc.gridy = 1;
        rGbc.weighty = 1.0;
        rGbc.insets = new Insets(0, 0, 0, 0);

        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplit.setBackground(ThemeManager.BG_SURFACE);
        rightSplit.setDividerSize(6);
        rightSplit.setBorder(null);
        rightSplit.setResizeWeight(1.0);

        JPanel serverPanel = createServerSelectionPanel();
        JPanel versionPanel = createVersionPanel();

        rightSplit.setTopComponent(serverPanel);
        rightSplit.setBottomComponent(versionPanel);
        rightSplit.setDividerLocation(260);

        rightColumn.add(rightSplit, rGbc);

        // Split pane: Files (left) | Stacked Settings (right)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setBackground(ThemeManager.BG_SURFACE);
        splitPane.setDividerLocation(560);
        splitPane.setDividerSize(6);
        splitPane.setBorder(null);

        splitPane.setLeftComponent(createFilePanel());
        splitPane.setRightComponent(rightColumn);

        add(splitPane, BorderLayout.CENTER);
        updateCategoryInfo();
    }

    private JPanel createDeployModePanel() {
        JPanel panel = ThemeManager.createCardPanel();
        panel.setBorder(new CompoundBorder(
                new LineBorder(ThemeManager.BORDER, 1, true),
                new EmptyBorder(6, 10, 6, 10)));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(0, 44));
        panel.setMinimumSize(new Dimension(0, 44));

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setBackground(ThemeManager.BG_CARD);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel modeLabel = new JLabel("Deploy Mode:  ");
        modeLabel.setFont(ThemeManager.FONT_BOLD);
        modeLabel.setForeground(ThemeManager.TEXT_PRIMARY);
        row.add(modeLabel);

        deployModeCombo = new JComboBox<>(DeploymentCategory.values());
        deployModeCombo.setFont(ThemeManager.FONT_REGULAR);
        deployModeCombo.setBackground(ThemeManager.BG_INPUT);
        deployModeCombo.setMaximumSize(new Dimension(240, 32));
        deployModeCombo.setPreferredSize(new Dimension(200, 32));
        deployModeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof DeploymentCategory) {
                    setText(((DeploymentCategory) value).getDisplayName());
                }
                return this;
            }
        });
        deployModeCombo.addActionListener(e -> updateCategoryInfo());
        row.add(deployModeCombo);
        row.add(Box.createHorizontalStrut(8));

        JButton infoBtn = ThemeManager.createSecondaryButton("Info");
        infoBtn.setPreferredSize(new Dimension(60, 32));
        infoBtn.setMaximumSize(new Dimension(60, 32));
        infoBtn.setFont(ThemeManager.FONT_BOLD);
        infoBtn.setToolTipText("Show resolved deployment paths");
        infoBtn.addActionListener(e -> showPathInfoDialog());
        row.add(infoBtn);

        panel.add(row);
        return panel;
    }

    private void showPathInfoDialog() {
        DeploymentCategory category = getSelectedCategory();
        String message = "<html>"
                + "<h3>Deployment Paths for " + category.getDisplayName() + "</h3>"
                + "<p><b>Tiers:</b> Other servers &rarr; 2T path | Corporate servers &rarr; 2T + 4T paths</p>"
                + "<br>"
                + "<table border='0' cellpadding='4' cellspacing='0'>"
                + "<tr bgcolor='#F1F5F9'><td><b>Other (2T):</b></td><td><code>" + escapeHtml(category.getPath(DeploymentTier.T2)) + "</code></td></tr>"
                + "<tr><td><b>Corporate (4T):</b></td><td><code>" + escapeHtml(category.getPath(DeploymentTier.T4)) + "</code></td></tr>"
                + "</table>"
                + "</html>";

        JOptionPane.showMessageDialog(this, message, "Deployment Path Information", JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel createVersionPanel() {
        JPanel panel = ThemeManager.createCardPanel();
        panel.setBorder(new CompoundBorder(
                new LineBorder(ThemeManager.BORDER, 1, true),
                new EmptyBorder(6, 10, 6, 10)));
        panel.setLayout(new BorderLayout(0, 4));
        panel.setPreferredSize(new Dimension(460, 140));
        panel.setMinimumSize(new Dimension(420, 135));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.setBackground(ThemeManager.BG_CARD);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Version (Corporate / 4T)");
        title.setFont(ThemeManager.FONT_BOLD);
        title.setForeground(ThemeManager.TEXT_PRIMARY);
        header.add(title);
        header.add(Box.createHorizontalGlue());

        JButton versionInfoBtn = ThemeManager.createSecondaryButton("Info");
        versionInfoBtn.setPreferredSize(new Dimension(60, 24));
        versionInfoBtn.setMaximumSize(new Dimension(60, 24));
        versionInfoBtn.setFont(ThemeManager.FONT_BOLD.deriveFont(11f));
        versionInfoBtn.setToolTipText("Show version file details");
        versionInfoBtn.addActionListener(e -> showVersionPathHintDialog());
        header.add(versionInfoBtn);

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
        tableScroll.setPreferredSize(new Dimension(420, 42));
        tableScroll.setMinimumSize(new Dimension(400, 36));
        panel.add(tableScroll, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBackground(ThemeManager.BG_CARD);

        JPanel updateRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        updateRow.setBackground(ThemeManager.BG_CARD);
        updateRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 1. Refresh Version Icon Button (↻)
        refreshVersionBtn = ThemeManager.createIconButton(ThemeManager.getRefreshIcon(ThemeManager.TEXT_PRIMARY));
        refreshVersionBtn.setToolTipText("Refresh Server Versions");
        refreshVersionBtn.addActionListener(e -> refreshServerVersions());
        updateRow.add(refreshVersionBtn);

        // 2. Suggest +1 Icon Button
        suggestVersionBtn = ThemeManager.createIconButton(ThemeManager.getAddIcon(ThemeManager.TEXT_PRIMARY));
        suggestVersionBtn.setToolTipText("Suggest Next Version (+1)");
        suggestVersionBtn.addActionListener(e -> suggestNextVersion());
        updateRow.add(suggestVersionBtn);

        // Spacer
        updateRow.add(Box.createHorizontalStrut(6));

        // 3. New version label & field
        JLabel newVerLabel = ThemeManager.createSecondaryLabel("New version:");
        newVersionField = ThemeManager.styleTextField(new JTextField(8));
        newVersionField.setPreferredSize(new Dimension(72, 28));
        updateRow.add(newVerLabel);
        updateRow.add(newVersionField);

        // 4. Update Version Button
        updateVersionBtn = ThemeManager.createAccentButton("Update version");
        updateVersionBtn.addActionListener(e -> updateVersionOnServers());
        updateRow.add(updateVersionBtn);

        footer.add(updateRow);
        panel.add(footer, BorderLayout.SOUTH);

        return panel;
    }

    private void showVersionPathHintDialog() {
        DeploymentCategory category = getSelectedCategory();
        String message = "<html>"
                + "<h3>Version File Information</h3>"
                + "<table border='0' cellpadding='4' cellspacing='0'>"
                + "<tr bgcolor='#F1F5F9'><td><b>File Name:</b></td><td><code>" + escapeHtml(category.getVersionFileName()) + "</code></td></tr>"
                + "<tr><td><b>4T Path:</b></td><td><code>" + escapeHtml(category.getVersionPath()) + "</code></td></tr>"
                + "</table>"
                + "</html>";

        JOptionPane.showMessageDialog(this, message, "Version Path Information", JOptionPane.INFORMATION_MESSAGE);
    }

    private DeploymentCategory getSelectedCategory() {
        if (deployModeCombo == null) {
            return DeploymentCategory.CUSTOM_FUNCTION;
        }
        return (DeploymentCategory) deployModeCombo.getSelectedItem();
    }

    private void updateCategoryInfo() {
        DeploymentCategory category = getSelectedCategory();

        if (serverCheckboxPanel != null) {
            refreshServerList();
        } else {
            refreshServerVersions();
        }
        updateDeployButtonState();
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
        panel.setBorder(new CompoundBorder(
                new LineBorder(ThemeManager.BORDER, 1, true),
                new EmptyBorder(8, 10, 8, 10)));
        panel.setLayout(new BorderLayout(0, 4));

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
        fileTable.setRowHeight(24);
        fileTable.getColumnModel().getColumn(0).setPreferredWidth(250); // Name
        fileTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Type
        fileTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Size

        panel.add(ThemeManager.styleScrollPane(fileTable), BorderLayout.CENTER);

        // File count label
        JLabel fileCountLabel = ThemeManager.createSecondaryLabel("No files selected");
        fileCountLabel.setBorder(new EmptyBorder(2, 0, 0, 0));
        panel.add(fileCountLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createServerSelectionPanel() {
        JPanel panel = ThemeManager.createCardPanel();
        panel.setBorder(new CompoundBorder(
                new LineBorder(ThemeManager.BORDER, 1, true),
                new EmptyBorder(8, 10, 8, 10)));
        panel.setLayout(new BorderLayout(0, 4));

        // Server section header
        JPanel serverHeader = new JPanel(new BorderLayout());
        serverHeader.setBackground(ThemeManager.BG_CARD);

        JLabel serverLabel = new JLabel("Target Servers");
        serverLabel.setFont(ThemeManager.FONT_BOLD);
        serverLabel.setForeground(ThemeManager.TEXT_PRIMARY);

        JButton toggleSelectAllBtn = ThemeManager.createIconButton(ThemeManager.getSelectAllIcon(ThemeManager.TEXT_PRIMARY));
        toggleSelectAllBtn.setToolTipText("Toggle Select All / None");
        toggleSelectAllBtn.addActionListener(e -> {
            boolean hasSelected = getSelectedServers().size() > 0;
            toggleAllServers(!hasSelected);
        });

        JButton refreshBtn = ThemeManager.createIconButton(ThemeManager.getRefreshIcon(ThemeManager.TEXT_PRIMARY));
        refreshBtn.setToolTipText("Refresh Server List");
        refreshBtn.addActionListener(e -> refreshServerList());

        serverHeader.add(serverLabel, BorderLayout.WEST);
        serverHeader.add(ThemeManager.createActionBar(toggleSelectAllBtn, refreshBtn), BorderLayout.EAST);

        JPanel serverNorth = new JPanel(new BorderLayout(0, 2));
        serverNorth.setBackground(ThemeManager.BG_CARD);
        serverNorth.add(serverHeader, BorderLayout.NORTH);
        corporateRequirementLabel = ThemeManager.createSecondaryLabel("");
        corporateRequirementLabel.setBorder(new EmptyBorder(0, 4, 2, 4));
        serverNorth.add(corporateRequirementLabel, BorderLayout.SOUTH);
        panel.add(serverNorth, BorderLayout.NORTH);

        // Server checkboxes in scroll pane
        serverCheckboxPanel = new JPanel();
        serverCheckboxPanel.setLayout(new BoxLayout(serverCheckboxPanel, BoxLayout.Y_AXIS));
        serverCheckboxPanel.setBackground(ThemeManager.BG_CARD);
        serverCheckboxPanel.setBorder(new EmptyBorder(4, 2, 4, 2));

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
            EnvironmentConfig activeEnv = environmentService.getActiveEnvironment();
            List<String> allowedIds = activeEnv.getAllowedServerIds();
            for (Server server : servers) {
                JCheckBox cb = new JCheckBox();
                ThemeManager.styleCheckBox(cb);

                boolean isAllowed = allowedIds.isEmpty() || allowedIds.contains(server.getId());
                cb.setEnabled(isAllowed);
                if (!isAllowed) {
                    cb.setSelected(false);
                    cb.setToolTipText("Not allowed for active environment: " + activeEnv.getName());
                }

                String statusIcon = switch (server.getStatus()) {
                    case ONLINE -> "[ON]";
                    case OFFLINE -> "[OFF]";
                    case UNKNOWN -> "[?]";
                };
                String typeTag = server.isCorporate() ? "Corporate, 2T+4T" : "Other, 2T";
                String text = "  " + statusIcon + "  " + server.getName()
                        + "  [" + typeTag + "]  (" + server.getHost() + ":" + server.getPort() + ")";
                if (!isAllowed) {
                    text += " (Not Allowed)";
                }
                cb.setText(text);
                cb.putClientProperty("serverId", server.getId());
                cb.addActionListener(e -> {
                    updateDeployButtonState();
                    refreshServerVersions();
                });

                JPanel row = new JPanel(new BorderLayout());
                row.setBackground(ThemeManager.BG_CARD);
                row.setBorder(new CompoundBorder(
                        new LineBorder(ThemeManager.BORDER, 0),
                        new EmptyBorder(2, 4, 2, 4)));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
                row.add(cb, BorderLayout.CENTER);

                serverCheckboxes.add(cb);
                serverCheckboxPanel.add(row);
            }

            if (serverManager.getCorporateServers().size() == 1) {
                Server corporate = serverManager.getCorporateServers().get(0);
                for (JCheckBox cb : serverCheckboxes) {
                    if (cb.isEnabled() && corporate.getId().equals(cb.getClientProperty("serverId"))) {
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
            if (cb.isEnabled()) {
                cb.setSelected(selected);
            }
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

    private static class DeploymentPromptDetails {
        final String ticketNumber;
        final String pic;
        final String kitName;

        DeploymentPromptDetails(String ticket, String pic, String kit) {
            this.ticketNumber = ticket;
            this.pic = pic;
            this.kitName = kit;
        }
    }

    /**
     * Prompts for Ticket Number, PIC, and Kit ID Suffix. Returns null if canceled.
     */
    private DeploymentPromptDetails promptDeploymentDetails() {
        JTextField ticketField = ThemeManager.styleTextField(new JTextField(28));
        ticketField.setToolTipText("e.g. INC1234567, CHG-98765");

        JTextField picField = ThemeManager.styleTextField(new JTextField(28));
        picField.setToolTipText("e.g. John Doe");

        JTextField kitSuffixField = ThemeManager.styleTextField(new JTextField(28));
        kitSuffixField.setText("kit1");
        kitSuffixField.setToolTipText("e.g. kit1");

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(ThemeManager.BG_SURFACE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        
        int row = 0;
        
        // Ticket Number
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 2, 0);
        JLabel ticketLabel = new JLabel("Ticket number (required):");
        ticketLabel.setFont(ThemeManager.FONT_BOLD);
        ticketLabel.setForeground(ThemeManager.TEXT_PRIMARY);
        form.add(ticketLabel, gbc);
        
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 10, 0);
        form.add(ticketField, gbc);

        // PIC
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 2, 0);
        JLabel picLabel = new JLabel("Person In Charge (PIC) (required):");
        picLabel.setFont(ThemeManager.FONT_BOLD);
        picLabel.setForeground(ThemeManager.TEXT_PRIMARY);
        form.add(picLabel, gbc);
        
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 10, 0);
        form.add(picField, gbc);

        // Kit Suffix
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 2, 0);
        JLabel kitLabel = new JLabel("Kit Name Suffix (required):");
        kitLabel.setFont(ThemeManager.FONT_BOLD);
        kitLabel.setForeground(ThemeManager.TEXT_PRIMARY);
        form.add(kitLabel, gbc);
        
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 0, 0);
        form.add(kitSuffixField, gbc);

        while (true) {
            int choice = JOptionPane.showConfirmDialog(this, form, "Enter Deployment Details",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return null;
            }
            String ticket = ticketField.getText().trim();
            String pic = picField.getText().trim();
            String kitSuffix = kitSuffixField.getText().trim();

            if (ticket.isEmpty() || pic.isEmpty() || kitSuffix.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "All fields (Ticket, PIC, and Kit Suffix) are required before deployment can proceed.",
                        "Required Fields Missing", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            // Generate kit name in format: KBT_kit1_date_month(in short form)year
            // e.g. KBT_kit1_18Jul2026
            String dateSuffix = java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("ddMMMyyyy", java.util.Locale.ENGLISH));
            String kitName = "KBT_" + kitSuffix + "_" + dateSuffix;

            return new DeploymentPromptDetails(ticket, pic, kitName);
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

        EnvironmentConfig activeEnv = environmentService.getActiveEnvironment();
        List<String> allowedIds = activeEnv.getAllowedServerIds();
        if (!allowedIds.isEmpty()) {
            for (Server s : targetServers) {
                if (!allowedIds.contains(s.getId())) {
                    JOptionPane.showMessageDialog(this,
                            "Error: Server \"" + s.getName() + "\" does not belong to the active environment \"" + activeEnv.getName() + "\".\n" +
                                    "Please check your target servers list or environment configuration.",
                            "Wrong Server Selection", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }

        DeploymentPromptDetails details = promptDeploymentDetails();
        if (details == null) {
            return;
        }

        DeploymentCategory category = getSelectedCategory();
        DeploymentRequest request = new DeploymentRequest(
                new ArrayList<>(selectedFiles), targetServers, category, DeploymentScope.BOTH);
        request.setTicketNumber(details.ticketNumber);
        request.setPic(details.pic);
        request.setKitName(details.kitName);
        request.setEnvName(activeEnv.getName());
        request.setAutoBackup(autoBackupCheck.isSelected());

        StringBuilder confirmMessage = new StringBuilder();
        confirmMessage.append("Ticket: ").append(details.ticketNumber).append("\n");
        confirmMessage.append("PIC: ").append(details.pic).append("\n");
        confirmMessage.append("Kit Name: ").append(details.kitName).append("\n");
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
        autoBackupCheck.setEnabled(false);

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
                autoBackupCheck.setSelected(true);
                autoBackupCheck.setEnabled(true);
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
