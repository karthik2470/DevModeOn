package com.cfdeploytool.ui.panel;

import com.cfdeploytool.model.EnvironmentConfig;
import com.cfdeploytool.model.Server;
import com.cfdeploytool.service.EnvironmentService;
import com.cfdeploytool.service.ServerManager;
import com.cfdeploytool.service.TaskManagementService;
import com.cfdeploytool.service.TaskManagementService.BatchOutputResult;
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
 * services/programs/batch control areas, running process monitoring,
 * and a real-time batch console output tab.
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
                "Monitor and control services, programs, batch scripts, and processes on remote servers");
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
        for (ServerTaskPanel panel : serverPanels.values()) {
            panel.rebuildTablesModel();
        }
        refreshActiveTab();
    }

    // ==================== INNER SERVER TAB PANEL ====================

    private class ServerTaskPanel extends JPanel {
        private final Server server;

        // Services & Programs table (left side)
        private JTable leftTable;
        private DefaultTableModel leftModel;

        // Running Processes table (right side)
        private JTable rightTable;
        private DefaultTableModel rightModel;

        // Action buttons (left pane)
        private JButton startBtn;
        private JButton stopBtn;
        private JButton restartBtn;
        private JButton openConsoleBtn;

        // Action buttons (right pane)
        private JButton killBtn;

        private JLabel statusText;
        private boolean isQuerying = false;

        // Sub-tab pane inside each server tab
        private JTabbedPane serverSubTabs;
        private ConsolePanel consolePanel;

        public ServerTaskPanel(Server server) {
            this.server = server;
            setLayout(new BorderLayout(0, 12));
            setOpaque(false);
            setBorder(new EmptyBorder(8, 0, 0, 0));

            buildComponents();
            rebuildTablesModel();
        }

        private void buildComponents() {
            serverSubTabs = new JTabbedPane();
            serverSubTabs.setFont(ThemeManager.FONT_REGULAR);
            serverSubTabs.setBackground(ThemeManager.BG_CARD);
            serverSubTabs.setForeground(ThemeManager.TEXT_PRIMARY);

            // ── Tab 1: Services & Programs + Running Processes ──
            JPanel servicesTab = buildServicesTab();
            serverSubTabs.addTab("Services & Programs", servicesTab);

            // ── Tab 2: Console ──
            consolePanel = new ConsolePanel();
            serverSubTabs.addTab("Console", consolePanel);

            add(serverSubTabs, BorderLayout.CENTER);

            // Footer status bar
            JPanel footer = new JPanel(new BorderLayout());
            footer.setOpaque(false);
            statusText = ThemeManager.createSecondaryLabel("Status: Not queried yet");
            footer.add(statusText, BorderLayout.WEST);
            add(footer, BorderLayout.SOUTH);
        }

        private JPanel buildServicesTab() {
            JPanel tabPanel = new JPanel(new BorderLayout(0, 0));
            tabPanel.setOpaque(false);

            // ── Left Side: Services & Programs ──
            JPanel leftPanel = new JPanel(new BorderLayout(0, 8));
            leftPanel.setOpaque(false);
            leftPanel.setBorder(new EmptyBorder(8, 0, 0, 0));

            JLabel leftTitle = new JLabel("Services and Programs");
            leftTitle.setFont(ThemeManager.FONT_BOLD);
            leftTitle.setForeground(ThemeManager.TEXT_PRIMARY);
            leftPanel.add(leftTitle, BorderLayout.NORTH);

            leftModel = new DefaultTableModel(
                    new Object[]{"Name", "Type", "Status", "Identifier/Path"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) { return false; }
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
            JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
            leftActions.setOpaque(false);
            startBtn = ThemeManager.createSuccessButton("Start");
            stopBtn = ThemeManager.createDangerButton("Stop");
            restartBtn = ThemeManager.createSecondaryButton("Restart");
            openConsoleBtn = ThemeManager.createSecondaryButton(">_ Console");
            openConsoleBtn.setToolTipText("Open batch script console output (BATCH entries only)");

            startBtn.addActionListener(e -> executeControlAction("start", "serviceOrProg"));
            stopBtn.addActionListener(e -> executeControlAction("stop", "serviceOrProg"));
            restartBtn.addActionListener(e -> executeControlAction("restart", "serviceOrProg"));
            openConsoleBtn.addActionListener(e -> openConsoleForSelected());

            leftActions.add(startBtn);
            leftActions.add(stopBtn);
            leftActions.add(restartBtn);
            leftActions.add(openConsoleBtn);
            leftPanel.add(leftActions, BorderLayout.SOUTH);

            // ── Right Side: Running Processes ──
            JPanel rightPanel = new JPanel(new BorderLayout(0, 8));
            rightPanel.setOpaque(false);
            rightPanel.setBorder(new EmptyBorder(8, 0, 0, 0));

            JLabel rightTitle = new JLabel("Running Processes");
            rightTitle.setFont(ThemeManager.FONT_BOLD);
            rightTitle.setForeground(ThemeManager.TEXT_PRIMARY);
            rightPanel.add(rightTitle, BorderLayout.NORTH);

            rightModel = new DefaultTableModel(new Object[]{"Process Name", "PID", "Status"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) { return false; }
            };
            rightTable = new JTable(rightModel);
            ThemeManager.styleTable(rightTable);
            rightTable.getSelectionModel().addListSelectionListener(e -> updateRightButtonsState());
            rightPanel.add(ThemeManager.styleScrollPane(rightTable), BorderLayout.CENTER);

            JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
            rightActions.setOpaque(false);
            killBtn = ThemeManager.createDangerButton("Kill Process");
            killBtn.addActionListener(e -> executeControlAction("kill", "process"));
            rightActions.add(killBtn);
            rightPanel.add(rightActions, BorderLayout.SOUTH);

            // Split pane
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
            splitPane.setOpaque(false);
            splitPane.setDividerLocation(520);
            splitPane.setBorder(null);

            tabPanel.add(splitPane, BorderLayout.CENTER);

            updateLeftButtonsState();
            updateRightButtonsState();
            return tabPanel;
        }

        public void rebuildTablesModel() {
            leftModel.setRowCount(0);
            rightModel.setRowCount(0);

            EnvironmentConfig config = environmentService.getActiveEnvironment();
            if (config == null) return;

            // Load Services, Programs, and Batch entries configured in environment settings.
            // Format: TYPE:path_or_name:display_name
            // TYPE can be: SERVICE, PROGRAM, BATCH
            String leftConfig = config.getTcServicesAndPrograms();
            if (leftConfig != null && !leftConfig.isBlank()) {
                String[] lines = leftConfig.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    // Delimiter is '|' to avoid conflict with Windows drive-letter colons (e.g. C:\path)
                    // Format per line:  TYPE|path_or_name|Display Name
                    // TYPE: SERVICE, PROGRAM, BATCH
                    String[] parts = line.split("\\|", 3);
                    if (parts.length >= 3) {
                        String type = parts[0].trim().toUpperCase();
                        String pathOrName = parts[1].trim();
                        String displayName = parts[2].trim();
                        leftModel.addRow(new Object[]{displayName, type, "UNKNOWN", pathOrName});
                    }
                }
            }

            // Load Running process names
            String rightConfig = config.getRunningTcPrograms();
            if (rightConfig != null && !rightConfig.isBlank()) {
                String[] tokens = rightConfig.split("[,\n]");
                for (String token : tokens) {
                    token = token.trim();
                    if (token.isEmpty()) continue;
                    rightModel.addRow(new Object[]{token, "-", "UNKNOWN"});
                }
            }
        }

        private void updateLeftButtonsState() {
            int row = leftTable.getSelectedRow();
            boolean sel = row >= 0;
            startBtn.setEnabled(sel && !isQuerying);
            stopBtn.setEnabled(sel && !isQuerying);
            restartBtn.setEnabled(sel && !isQuerying);

            // Console button only enabled for BATCH rows
            if (sel) {
                String type = (String) leftModel.getValueAt(row, 1);
                openConsoleBtn.setEnabled("BATCH".equalsIgnoreCase(type) && !isQuerying);
            } else {
                openConsoleBtn.setEnabled(false);
            }
        }

        private void updateRightButtonsState() {
            int row = rightTable.getSelectedRow();
            boolean sel = row >= 0;
            if (sel) {
                String pid = (String) rightModel.getValueAt(row, 1);
                String status = (String) rightModel.getValueAt(row, 2);
                boolean isRunning = "RUNNING".equalsIgnoreCase(status) && !"-".equals(pid);
                killBtn.setEnabled(isRunning && !isQuerying);
            } else {
                killBtn.setEnabled(false);
            }
        }

        private void openConsoleForSelected() {
            int row = leftTable.getSelectedRow();
            if (row < 0) return;
            String type = (String) leftModel.getValueAt(row, 1);
            if (!"BATCH".equalsIgnoreCase(type)) return;
            String batPath = (String) leftModel.getValueAt(row, 3);
            String displayName = (String) leftModel.getValueAt(row, 0);
            consolePanel.loadScript(server, batPath, displayName);
            serverSubTabs.setSelectedIndex(1); // Switch to Console tab
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

            // Build status query payload
            List<String> services = new ArrayList<>();
            List<String> programs = new ArrayList<>();
            List<String> batches = new ArrayList<>();

            for (int i = 0; i < leftModel.getRowCount(); i++) {
                String type = (String) leftModel.getValueAt(i, 1);
                String pathOrName = (String) leftModel.getValueAt(i, 3);
                if ("SERVICE".equalsIgnoreCase(type)) {
                    services.add(pathOrName);
                } else if ("BATCH".equalsIgnoreCase(type)) {
                    batches.add(pathOrName);
                } else {
                    java.io.File f = new java.io.File(pathOrName);
                    programs.add(f.getName() + "|" + pathOrName);
                }
            }

            List<String> processes = new ArrayList<>();
            for (int i = 0; i < rightModel.getRowCount(); i++) {
                String procName = (String) rightModel.getValueAt(i, 0);
                if (!processes.contains(procName)) {
                    processes.add(procName);
                }
            }

            final String svcReq = String.join(",", services);
            final String progReq = String.join(",", programs);
            final String procReq = String.join(",", processes);
            final String batReq = String.join("||", batches); // use || as delimitier (paths may have commas)

            new Thread(() -> {
                TaskStatusResult res = taskService.queryStatus(server, svcReq, progReq, procReq, batReq);
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
            Map<String, String> bMap = result.getBatchesStatus();

            for (int i = 0; i < leftModel.getRowCount(); i++) {
                String type = (String) leftModel.getValueAt(i, 1);
                String pathOrName = (String) leftModel.getValueAt(i, 3);
                String status = "UNKNOWN";

                if ("SERVICE".equalsIgnoreCase(type)) {
                    status = sMap.getOrDefault(pathOrName, "STOPPED");
                } else if ("BATCH".equalsIgnoreCase(type)) {
                    // Look up by normalized lower-case key (agent normalizes)
                    String normKey = pathOrName.trim().toLowerCase().replace('/', '\\');
                    status = bMap.getOrDefault(normKey, bMap.getOrDefault(pathOrName, "STOPPED"));
                } else {
                    java.io.File f = new java.io.File(pathOrName);
                    status = pMap.getOrDefault(f.getName(), "STOPPED");
                }
                leftModel.setValueAt(status, i, 2);
            }

            // Update Right Table: expand running processes + PIDs
            EnvironmentConfig envConfig = environmentService.getActiveEnvironment();
            if (envConfig != null) {
                String rightConfig = envConfig.getRunningTcPrograms();
                List<String> configured = new ArrayList<>();
                if (rightConfig != null && !rightConfig.isBlank()) {
                    for (String token : rightConfig.split("[,\n]")) {
                        if (!token.trim().isEmpty()) {
                            configured.add(token.trim());
                        }
                    }
                }

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
                // Map BATCH -> "batch", SERVICE -> "service", PROGRAM -> "program"
                type = rawType.toLowerCase();
                target = (String) leftModel.getValueAt(row, 3);
                displayName = (String) leftModel.getValueAt(row, 0);
            } else if ("process".equalsIgnoreCase(areaType)) {
                int row = rightTable.getSelectedRow();
                if (row < 0) return;
                type = "process";
                String pid = (String) rightModel.getValueAt(row, 1);
                target = pid;
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
            openConsoleBtn.setEnabled(false);
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
                        triggerStatusQuery();
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Failed to " + action + " " + fDisplay + ":\n" + res.getMessage(),
                                "Operation Error", JOptionPane.ERROR_MESSAGE);
                        triggerStatusQuery();
                    }
                });
            }).start();
        }

        // ==================== CONSOLE PANEL (inner) ====================

        private class ConsolePanel extends JPanel {
            private JLabel scriptLabel;
            private JLabel runningLabel;
            private JTextArea outputArea;
            private JButton loadBtn;
            private JButton clearBtn;
            private JCheckBox autoPollCheck;
            private Timer pollTimer;
            private Server consoleServer;
            private String consoleBatPath;

            public ConsolePanel() {
                setLayout(new BorderLayout(0, 8));
                setOpaque(false);
                setBorder(new EmptyBorder(8, 0, 0, 0));
                buildUI();
            }

            private void buildUI() {
                // ── Header card ──
                JPanel headerCard = ThemeManager.createCardPanel();
                headerCard.setLayout(new BorderLayout(12, 0));

                JPanel infoPanel = new JPanel();
                infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
                infoPanel.setOpaque(false);
                scriptLabel = new JLabel("No script loaded — select a BATCH entry from Services & Programs, then click ⬛ Console");
                scriptLabel.setFont(ThemeManager.FONT_BOLD);
                scriptLabel.setForeground(ThemeManager.TEXT_PRIMARY);

                runningLabel = new JLabel("");
                runningLabel.setFont(ThemeManager.FONT_SMALL);
                runningLabel.setForeground(ThemeManager.TEXT_SECONDARY);

                infoPanel.add(scriptLabel);
                infoPanel.add(Box.createVerticalStrut(2));
                infoPanel.add(runningLabel);
                headerCard.add(infoPanel, BorderLayout.CENTER);

                JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
                btnPanel.setOpaque(false);

                autoPollCheck = new JCheckBox("Auto-Poll (2s)", false);
                autoPollCheck.setFont(ThemeManager.FONT_SMALL);
                autoPollCheck.setForeground(ThemeManager.TEXT_SECONDARY);
                autoPollCheck.setOpaque(false);
                autoPollCheck.addActionListener(e -> toggleAutoPoll());

                loadBtn = ThemeManager.createSecondaryButton("Load Output");
                loadBtn.setEnabled(false);
                loadBtn.addActionListener(e -> loadOutput());

                clearBtn = ThemeManager.createSecondaryButton("Clear");
                clearBtn.addActionListener(e -> outputArea.setText(""));

                btnPanel.add(autoPollCheck);
                btnPanel.add(loadBtn);
                btnPanel.add(clearBtn);
                headerCard.add(btnPanel, BorderLayout.EAST);
                add(headerCard, BorderLayout.NORTH);

                // ── Console output area ──
                outputArea = new JTextArea();
                outputArea.setEditable(false);
                outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                outputArea.setBackground(new Color(0x0d, 0x11, 0x17));
                outputArea.setForeground(new Color(0xc9, 0xd1, 0xd9));
                outputArea.setCaretColor(new Color(0xc9, 0xd1, 0xd9));
                outputArea.setBorder(new EmptyBorder(12, 14, 12, 14));
                outputArea.setLineWrap(true);
                outputArea.setWrapStyleWord(false);

                JScrollPane scroll = new JScrollPane(outputArea);
                scroll.setBorder(BorderFactory.createLineBorder(ThemeManager.BORDER));
                scroll.getViewport().setBackground(new Color(0x0d, 0x11, 0x17));
                scroll.getVerticalScrollBar().setUnitIncrement(14);

                add(scroll, BorderLayout.CENTER);

                // Poll timer
                pollTimer = new Timer(2000, ev -> loadOutput());
                pollTimer.setRepeats(true);
            }

            /** Called when user clicks "⬛ Console" on a BATCH row */
            public void loadScript(Server server, String batPath, String displayName) {
                // Stop any existing poll timer
                pollTimer.stop();
                autoPollCheck.setSelected(false);

                this.consoleServer = server;
                this.consoleBatPath = batPath;
                scriptLabel.setText("Script: " + displayName + "  [" + batPath + "]");
                runningLabel.setText("Status: unknown — click Load Output or enable Auto-Poll");
                loadBtn.setEnabled(true);
                outputArea.setText("");
            }

            private void loadOutput() {
                if (consoleBatPath == null || consoleServer == null) return;

                new Thread(() -> {
                    BatchOutputResult res = taskService.queryBatchOutput(consoleServer, consoleBatPath);
                    SwingUtilities.invokeLater(() -> {
                        if (!res.isSuccess()) {
                            runningLabel.setText("⚠ Failed to fetch output: " + res.getOutput());
                            runningLabel.setForeground(ThemeManager.ERROR);
                            // Stop polling on connection error
                            if (autoPollCheck.isSelected()) {
                                pollTimer.stop();
                                autoPollCheck.setSelected(false);
                            }
                            return;
                        }

                        String statusText = res.isRunning() ? "🟢 RUNNING" : "⚫ STOPPED";
                        runningLabel.setText("Status: " + statusText +
                                "   Last fetched: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                        runningLabel.setForeground(res.isRunning() ? ThemeManager.SUCCESS : ThemeManager.TEXT_SECONDARY);

                        // Replace full output (ring buffer already caps at 2000 lines on agent)
                        int caretPos = outputArea.getText().isEmpty() ? 0 : outputArea.getDocument().getLength();
                        boolean wasAtBottom = isScrolledToBottom();
                        outputArea.setText(res.getOutput());

                        // Auto-scroll to bottom
                        if (wasAtBottom || caretPos == 0) {
                            outputArea.setCaretPosition(outputArea.getDocument().getLength());
                        }

                        // If batch has stopped, stop polling
                        if (!res.isRunning() && autoPollCheck.isSelected()) {
                            pollTimer.stop();
                            autoPollCheck.setSelected(false);
                        }
                    });
                }).start();
            }

            private boolean isScrolledToBottom() {
                JScrollPane scroll = (JScrollPane) outputArea.getParent().getParent();
                JScrollBar bar = scroll.getVerticalScrollBar();
                return bar.getValue() + bar.getVisibleAmount() >= bar.getMaximum() - 20;
            }

            private void toggleAutoPoll() {
                if (autoPollCheck.isSelected()) {
                    if (consoleBatPath == null) {
                        autoPollCheck.setSelected(false);
                        return;
                    }
                    loadOutput(); // immediate first load
                    pollTimer.start();
                } else {
                    pollTimer.stop();
                }
            }
        }
    }
}
