package com.cfdeploytool.ui.panel;

import com.cfdeploytool.model.EnvironmentConfig;
import com.cfdeploytool.model.Server;
import com.cfdeploytool.service.EnvironmentService;
import com.cfdeploytool.service.ServerManager;
import com.cfdeploytool.service.TaskManagementService;
import com.cfdeploytool.service.TaskManagementService.ProcessStatus;
import com.cfdeploytool.service.TaskManagementService.TaskControlResult;
import com.cfdeploytool.service.TaskManagementService.TaskStatusResult;
import com.cfdeploytool.ui.MainFrame;
import com.cfdeploytool.ui.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Task Management Panel containing dynamic server tabs,
 * services/programs control areas, and running process monitoring.
 */
public class TaskManagementPanel extends JPanel {

    private final ServerManager serverManager;
    private final EnvironmentService environmentService;
    private final TaskManagementService taskService;
    private final MainFrame mainFrame;

    private JTabbedPane tabbedPane;
    private JCheckBox autoRefreshCheck;
    private Timer refreshTimer;

    private final Map<String, ServerTaskPanel> serverPanels = new HashMap<>();

    public TaskManagementPanel(ServerManager serverManager, EnvironmentService environmentService,
                               TaskManagementService taskService, MainFrame mainFrame) {
        this.serverManager = serverManager;
        this.environmentService = environmentService;
        this.taskService = taskService;
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout());
        setBackground(ThemeManager.BG_SURFACE);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        buildHeader();
        buildTabbedPane();
        setupTimer();
    }

    private void buildHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(0, 0, 12, 0));

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel titleLabel = ThemeManager.createHeaderLabel("Task Management");
        JLabel subtitleLabel = ThemeManager.createSecondaryLabel(
                "Monitor and control services, programs, and processes on remote servers");
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(subtitleLabel);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        controls.setOpaque(false);

        autoRefreshCheck = new JCheckBox("Auto Refresh (5s)", true);
        autoRefreshCheck.setFont(ThemeManager.FONT_SMALL);
        autoRefreshCheck.setForeground(ThemeManager.TEXT_SECONDARY);
        autoRefreshCheck.setOpaque(false);
        autoRefreshCheck.addActionListener(e -> {
            if (autoRefreshCheck.isSelected()) {
                refreshTimer.start();
            } else {
                refreshTimer.stop();
            }
        });

        JButton refreshBtn = ThemeManager.createSecondaryButton("Refresh Status");
        refreshBtn.addActionListener(e -> refreshActiveTab());

        controls.add(autoRefreshCheck);
        controls.add(refreshBtn);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(controls, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
    }

    private void buildTabbedPane() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(ThemeManager.FONT_BOLD);
        tabbedPane.setBackground(ThemeManager.BG_CARD);
        tabbedPane.setForeground(ThemeManager.TEXT_PRIMARY);

        tabbedPane.addChangeListener(e -> refreshActiveTab());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void setupTimer() {
        refreshTimer = new Timer(5000, e -> {
            if (isShowing() && autoRefreshCheck.isSelected()) {
                refreshActiveTab();
            }
        });
        refreshTimer.start();
    }

    public void refreshServers() {
        List<Server> servers = serverManager.getServers();

        // Remove tabs for servers that no longer exist
        List<String> toRemove = new ArrayList<>();
        for (String serverId : serverPanels.keySet()) {
            boolean exists = servers.stream().anyMatch(s -> s.getId().equals(serverId));
            if (!exists) {
                toRemove.add(serverId);
            }
        }
        for (String id : toRemove) {
            ServerTaskPanel panel = serverPanels.remove(id);
            tabbedPane.remove(panel);
        }

        // Add or sync tabs
        int index = 0;
        for (Server server : servers) {
            ServerTaskPanel panel = serverPanels.get(server.getId());
            if (panel == null) {
                panel = new ServerTaskPanel(server);
                serverPanels.put(server.getId(), panel);
                tabbedPane.insertTab(server.getName(), null, panel, server.getHost(), index);
            } else {
                // Update title just in case server name changed
                tabbedPane.setTitleAt(index, server.getName());
            }
            index++;
        }

        if (tabbedPane.getTabCount() > 0 && tabbedPane.getSelectedIndex() < 0) {
            tabbedPane.setSelectedIndex(0);
        }
    }

    public void refreshActiveTab() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex >= 0) {
            ServerTaskPanel activePanel = (ServerTaskPanel) tabbedPane.getComponentAt(selectedIndex);
            if (activePanel != null) {
                activePanel.triggerStatusQuery();
            }
        }
    }

    public void onEnvironmentChanged() {
        // Clear and rebuild panel contents when configuration changes
        for (ServerTaskPanel panel : serverPanels.values()) {
            panel.rebuildTablesModel();
        }
        refreshActiveTab();
    }

    // ==================== INNER SERVER TAB PANEL ====================

    private class ServerTaskPanel extends JPanel {
        private final Server server;

        private JTable leftTable;
        private DefaultTableModel leftModel;
        private JTable rightTable;
        private DefaultTableModel rightModel;

        private JButton startBtn;
        private JButton stopBtn;
        private JButton restartBtn;
        private JButton killBtn;

        private JLabel statusText;
        private boolean isQuerying = false;

        public ServerTaskPanel(Server server) {
            this.server = server;
            setLayout(new BorderLayout(0, 12));
            setOpaque(false);
            setBorder(new EmptyBorder(8, 0, 0, 0));

            buildComponents();
            rebuildTablesModel();
        }

        private void buildComponents() {
            // Left Side: Services & Programs Panel
            JPanel leftPanel = new JPanel(new BorderLayout(0, 8));
            leftPanel.setOpaque(false);
            JLabel leftTitle = new JLabel("Services and Programs");
            leftTitle.setFont(ThemeManager.FONT_BOLD);
            leftTitle.setForeground(ThemeManager.TEXT_PRIMARY);
            leftPanel.add(leftTitle, BorderLayout.NORTH);

            leftModel = new DefaultTableModel(new Object[]{"Name", "Type", "Status", "Identifier/Path"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            leftTable = new JTable(leftModel);
            ThemeManager.styleTable(leftTable);
            leftTable.getColumnModel().getColumn(0).setPreferredWidth(150);
            leftTable.getColumnModel().getColumn(1).setPreferredWidth(70);
            leftTable.getColumnModel().getColumn(2).setPreferredWidth(90);
            leftTable.getColumnModel().getColumn(3).setPreferredWidth(180);

            leftTable.getSelectionModel().addListSelectionListener(e -> updateLeftButtonsState());
            leftPanel.add(ThemeManager.styleScrollPane(leftTable), BorderLayout.CENTER);

            // Left actions
            JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            leftActions.setOpaque(false);
            startBtn = ThemeManager.createSuccessButton("Start");
            stopBtn = ThemeManager.createDangerButton("Stop");
            restartBtn = ThemeManager.createSecondaryButton("Restart");

            startBtn.addActionListener(e -> executeControlAction("start", "serviceOrProg"));
            stopBtn.addActionListener(e -> executeControlAction("stop", "serviceOrProg"));
            restartBtn.addActionListener(e -> executeControlAction("restart", "serviceOrProg"));

            leftActions.add(startBtn);
            leftActions.add(stopBtn);
            leftActions.add(restartBtn);
            leftPanel.add(leftActions, BorderLayout.SOUTH);

            // Right Side: Running Programs
            JPanel rightPanel = new JPanel(new BorderLayout(0, 8));
            rightPanel.setOpaque(false);
            JLabel rightTitle = new JLabel("Running Processes");
            rightTitle.setFont(ThemeManager.FONT_BOLD);
            rightTitle.setForeground(ThemeManager.TEXT_PRIMARY);
            rightPanel.add(rightTitle, BorderLayout.NORTH);

            rightModel = new DefaultTableModel(new Object[]{"Process Name", "PID", "Status"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            rightTable = new JTable(rightModel);
            ThemeManager.styleTable(rightTable);
            rightTable.getSelectionModel().addListSelectionListener(e -> updateRightButtonsState());
            rightPanel.add(ThemeManager.styleScrollPane(rightTable), BorderLayout.CENTER);

            // Right actions
            JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            rightActions.setOpaque(false);
            killBtn = ThemeManager.createDangerButton("Kill Process");
            killBtn.addActionListener(e -> executeControlAction("kill", "process"));
            rightActions.add(killBtn);
            rightPanel.add(rightActions, BorderLayout.SOUTH);

            // Split Pane
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
            splitPane.setOpaque(false);
            splitPane.setDividerLocation(520);
            splitPane.setBorder(null);

            add(splitPane, BorderLayout.CENTER);

            // Footer Status Panel
            JPanel footer = new JPanel(new BorderLayout());
            footer.setOpaque(false);
            statusText = ThemeManager.createSecondaryLabel("Status: Not queried yet");
            footer.add(statusText, BorderLayout.WEST);

            add(footer, BorderLayout.SOUTH);

            updateLeftButtonsState();
            updateRightButtonsState();
        }

        public void rebuildTablesModel() {
            leftModel.setRowCount(0);
            rightModel.setRowCount(0);

            EnvironmentConfig config = environmentService.getActiveEnvironment();
            if (config == null) return;

            // Load Services & Programs configured in Environment Settings
            String leftConfig = config.getTcServicesAndPrograms();
            if (leftConfig != null && !leftConfig.isBlank()) {
                String[] lines = leftConfig.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    String[] parts = line.split(":", 3);
                    if (parts.length >= 3) {
                        String type = parts[0].trim().toUpperCase();
                        String pathOrName = parts[1].trim();
                        String displayName = parts[2].trim();
                        leftModel.addRow(new Object[]{displayName, type, "UNKNOWN", pathOrName});
                    }
                }
            }

            // Load Running processes configured in Settings
            String rightConfig = config.getRunningTcPrograms();
            if (rightConfig != null && !rightConfig.isBlank()) {
                String[] tokens = rightConfig.split("[,\\n]");
                for (String token : tokens) {
                    token = token.trim();
                    if (token.isEmpty()) continue;
                    rightModel.addRow(new Object[]{token, "-", "UNKNOWN"});
                }
            }
        }

        private void updateLeftButtonsState() {
            int row = leftTable.getSelectedRow();
            boolean selectionExist = (row >= 0);
            startBtn.setEnabled(selectionExist && !isQuerying);
            stopBtn.setEnabled(selectionExist && !isQuerying);
            restartBtn.setEnabled(selectionExist && !isQuerying);
        }

        private void updateRightButtonsState() {
            int row = rightTable.getSelectedRow();
            boolean selectionExist = (row >= 0);
            if (selectionExist) {
                String pid = (String) rightModel.getValueAt(row, 1);
                String status = (String) rightModel.getValueAt(row, 2);
                boolean isRunning = "RUNNING".equalsIgnoreCase(status) && !"-".equals(pid);
                killBtn.setEnabled(isRunning && !isQuerying);
            } else {
                killBtn.setEnabled(false);
            }
        }

        public void triggerStatusQuery() {
            if (isQuerying) return;
            isQuerying = true;
            statusText.setText("Querying status from agent...");
            leftTable.setEnabled(false);
            rightTable.setEnabled(false);

            EnvironmentConfig envConfig = environmentService.getActiveEnvironment();
            if (envConfig == null) {
                isQuerying = false;
                statusText.setText("Status: No active environment configuration found.");
                leftTable.setEnabled(true);
                rightTable.setEnabled(true);
                return;
            }

            // Build lists for payload
            List<String> services = new ArrayList<>();
            List<String> programs = new ArrayList<>();
            for (int i = 0; i < leftModel.getRowCount(); i++) {
                String type = (String) leftModel.getValueAt(i, 1);
                String pathOrName = (String) leftModel.getValueAt(i, 3);
                if ("SERVICE".equalsIgnoreCase(type)) {
                    services.add(pathOrName);
                } else {
                    java.io.File f = new java.io.File(pathOrName);
                    programs.add(f.getName() + "|" + pathOrName);
                }
            }

            List<String> processes = new ArrayList<>();
            for (int i = 0; i < rightModel.getRowCount(); i++) {
                String procName = (String) rightModel.getValueAt(i, 0);
                // Keep only unique process names in request
                if (!processes.contains(procName)) {
                    processes.add(procName);
                }
            }

            final String svcReq = String.join(",", services);
            final String progReq = String.join(",", programs);
            final String procReq = String.join(",", processes);

            new Thread(() -> {
                TaskStatusResult res = taskService.queryStatus(server, svcReq, progReq, procReq);
                SwingUtilities.invokeLater(() -> updateUIWithQueryResult(res));
            }).start();
        }

        private void updateUIWithQueryResult(TaskStatusResult result) {
            isQuerying = false;
            leftTable.setEnabled(true);
            rightTable.setEnabled(true);

            if (!result.isSuccess()) {
                statusText.setText("Query failed: " + result.getMessage());
                statusText.setForeground(ThemeManager.ERROR);
                updateLeftButtonsState();
                updateRightButtonsState();
                return;
            }

            // Update Left Table statuses
            Map<String, String> sMap = result.getServicesStatus();
            Map<String, String> pMap = result.getProgramsStatus();

            for (int i = 0; i < leftModel.getRowCount(); i++) {
                String type = (String) leftModel.getValueAt(i, 1);
                String pathOrName = (String) leftModel.getValueAt(i, 3);
                String status = "UNKNOWN";

                if ("SERVICE".equalsIgnoreCase(type)) {
                    status = sMap.getOrDefault(pathOrName, "STOPPED");
                } else {
                    java.io.File f = new java.io.File(pathOrName);
                    status = pMap.getOrDefault(f.getName(), "STOPPED");
                }
                leftModel.setValueAt(status, i, 2);
            }

            // Update Right Table: expanding PIDs
            // We read the original process names we want to display from settings
            EnvironmentConfig envConfig = environmentService.getActiveEnvironment();
            if (envConfig != null) {
                String rightConfig = envConfig.getRunningTcPrograms();
                List<String> configured = new ArrayList<>();
                if (rightConfig != null && !rightConfig.isBlank()) {
                    for (String token : rightConfig.split("[,\\n]")) {
                        if (!token.trim().isEmpty()) {
                            configured.add(token.trim());
                        }
                    }
                }

                // Preserve selections if possible
                int prevSelRow = rightTable.getSelectedRow();
                String prevSelProc = null;
                String prevSelPid = null;
                if (prevSelRow >= 0) {
                    prevSelProc = (String) rightModel.getValueAt(prevSelRow, 0);
                    prevSelPid = (String) rightModel.getValueAt(prevSelRow, 1);
                }

                rightModel.setRowCount(0);
                Map<String, ProcessStatus> procMap = result.getProcessesStatus();

                for (String procName : configured) {
                    ProcessStatus pStatus = procMap.get(procName);
                    if (pStatus != null && pStatus.isRunning()) {
                        for (Integer pid : pStatus.getPids()) {
                            rightModel.addRow(new Object[]{procName, String.valueOf(pid), "RUNNING"});
                        }
                    } else {
                        rightModel.addRow(new Object[]{procName, "-", "NOT RUNNING"});
                    }
                }

                // Restore selection
                if (prevSelProc != null) {
                    for (int i = 0; i < rightModel.getRowCount(); i++) {
                        if (prevSelProc.equals(rightModel.getValueAt(i, 0)) &&
                                prevSelPid.equals(rightModel.getValueAt(i, 1))) {
                            rightTable.setRowSelectionInterval(i, i);
                            break;
                        }
                    }
                }
            }

            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            statusText.setText("Last updated: " + time);
            statusText.setForeground(ThemeManager.TEXT_SECONDARY);

            updateLeftButtonsState();
            updateRightButtonsState();
        }

        private void executeControlAction(String action, String areaType) {
            String type = "";
            String target = "";
            String displayName = "";

            if ("serviceOrProg".equalsIgnoreCase(areaType)) {
                int row = leftTable.getSelectedRow();
                if (row < 0) return;
                String rawType = (String) leftModel.getValueAt(row, 1);
                type = rawType.toLowerCase();
                target = (String) leftModel.getValueAt(row, 3);
                displayName = (String) leftModel.getValueAt(row, 0);
            } else if ("process".equalsIgnoreCase(areaType)) {
                int row = rightTable.getSelectedRow();
                if (row < 0) return;
                type = "process";
                String pid = (String) rightModel.getValueAt(row, 1);
                target = pid; // Target by PID is more specific and safe
                displayName = (String) rightModel.getValueAt(row, 0) + " (PID: " + pid + ")";
            }

            if (type.isEmpty() || target.isEmpty()) return;

            isQuerying = true;
            statusText.setText("Executing " + action + " on " + displayName + "...");
            leftTable.setEnabled(false);
            rightTable.setEnabled(false);
            startBtn.setEnabled(false);
            stopBtn.setEnabled(false);
            restartBtn.setEnabled(false);
            killBtn.setEnabled(false);

            final String fType = type;
            final String fTarget = target;
            final String fDisplay = displayName;

            new Thread(() -> {
                TaskControlResult res = taskService.controlTask(server, action, fType, fTarget);
                SwingUtilities.invokeLater(() -> {
                    isQuerying = false;
                    leftTable.setEnabled(true);
                    rightTable.setEnabled(true);
                    if (res.isSuccess()) {
                        mainFrame.setStatus(action + " on " + fDisplay + " completed successfully.");
                        triggerStatusQuery(); // Automatically refresh UI statuses
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Failed to " + action + " " + fDisplay + ":\n" + res.getMessage(),
                                "Operation Error", JOptionPane.ERROR_MESSAGE);
                        triggerStatusQuery();
                    }
                });
            }).start();
        }
    }
}
