package com.cfdeploytool.ui.panel;

import com.cfdeploytool.model.DeploymentHistory;
import com.cfdeploytool.service.HistoryService;
import com.cfdeploytool.service.EnvironmentService;
import com.cfdeploytool.ui.ThemeManager;
import com.cfdeploytool.ui.dialog.DeploymentResultDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel displaying deployment history and package tracking under two tabs:
 * "Deployment History" and "Tracker". Allows exporting tracker to CSV.
 */
public class HistoryPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss");

    private final HistoryService historyService;
    private final EnvironmentService environmentService;

    // Tab 1: Deployment History
    private JTable historyTable;
    private HistoryTableModel historyModel;

    // Tab 2: Tracker
    private JTable trackerTable;
    private TrackerTableModel trackerModel;

    private List<DeploymentHistory> historyList;

    public HistoryPanel(HistoryService historyService, EnvironmentService environmentService) {
        this.historyService = historyService;
        this.environmentService = environmentService;
        setLayout(new BorderLayout(0, 0));
        setBackground(ThemeManager.BG_SURFACE);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        initComponents();
        setupEnvironmentListener();
        refreshData();
    }

    private void initComponents() {
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ThemeManager.BG_SURFACE);
        headerPanel.setBorder(new EmptyBorder(0, 0, 12, 0));

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBackground(ThemeManager.BG_SURFACE);

        JLabel titleLabel = ThemeManager.createHeaderLabel("History & Tracker");
        JLabel subtitleLabel = ThemeManager.createSecondaryLabel(
                "View past deployment runs and audit package tracking variables");
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(subtitleLabel);

        // Actions
        JButton exportCsvBtn = ThemeManager.createSecondaryButton("Export Tracker");
        exportCsvBtn.addActionListener(e -> exportTrackerToCsv());

        JButton refreshBtn = ThemeManager.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> refreshData());

        JButton clearBtn = ThemeManager.createDangerButton("Clear History");
        clearBtn.addActionListener(e -> clearHistory());

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(ThemeManager.createActionBar(exportCsvBtn, refreshBtn, clearBtn), BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(ThemeManager.FONT_BOLD);
        tabbedPane.setBackground(ThemeManager.BG_CARD);
        tabbedPane.setForeground(ThemeManager.TEXT_PRIMARY);

        // --- Tab 1: Deployment History ---
        historyModel = new HistoryTableModel();
        historyTable = new JTable(historyModel);
        ThemeManager.styleTable(historyTable);
        historyTable.setRowHeight(40);
        historyTable.setIntercellSpacing(new Dimension(0, 1));
        historyTable.getTableHeader().setBorder(new LineBorder(ThemeManager.BORDER, 1));
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyTable.setDefaultRenderer(Object.class, ThemeManager.createTableRenderer());
        historyTable.getColumnModel().getColumn(6).setCellRenderer(new StatusColumnRenderer());

        // Widths
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(150); // Date
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Ticket
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(60);  // Files
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(70);  // Servers
        historyTable.getColumnModel().getColumn(4).setPreferredWidth(70);  // Success
        historyTable.getColumnModel().getColumn(5).setPreferredWidth(60);  // Failed
        historyTable.getColumnModel().getColumn(6).setPreferredWidth(90);  // Status

        historyTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showSelectedDetails();
                }
            }
        });
        tabbedPane.addTab("Deployment History", ThemeManager.styleScrollPane(historyTable));

        // --- Tab 2: Tracker ---
        trackerModel = new TrackerTableModel();
        trackerTable = new JTable(trackerModel);
        ThemeManager.styleTable(trackerTable);
        trackerTable.setRowHeight(40);
        trackerTable.setIntercellSpacing(new Dimension(0, 1));
        trackerTable.getTableHeader().setBorder(new LineBorder(ThemeManager.BORDER, 1));
        trackerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        trackerTable.setDefaultRenderer(Object.class, ThemeManager.createTableRenderer());

        // Widths
        trackerTable.getColumnModel().getColumn(0).setPreferredWidth(180); // Kit Name
        trackerTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Ticket No
        trackerTable.getColumnModel().getColumn(2).setPreferredWidth(100); // PIC
        trackerTable.getColumnModel().getColumn(3).setPreferredWidth(140); // Fix Date
        trackerTable.getColumnModel().getColumn(4).setPreferredWidth(140); // Deploy Date
        trackerTable.getColumnModel().getColumn(5).setPreferredWidth(260); // Deployable List

        tabbedPane.addTab("Tracker", ThemeManager.styleScrollPane(trackerTable));

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void setupEnvironmentListener() {
        if (environmentService != null) {
            environmentService.addChangeListener(() -> SwingUtilities.invokeLater(this::updateTrackerHeader));
        }
    }

    private void updateTrackerHeader() {
        if (trackerTable == null || environmentService == null) return;
        String envName = environmentService.getActiveEnvironment() != null
                ? environmentService.getActiveEnvironment().getName() : "Current Env";
        trackerTable.getColumnModel().getColumn(4).setHeaderValue("(" + envName + ") Deploy Date");
        trackerTable.getTableHeader().repaint();
    }

    /**
     * Refreshes the history and tracker tables from the service.
     */
    public void refreshData() {
        historyList = historyService.getHistory();
        if (historyModel != null) {
            historyModel.fireTableDataChanged();
        }
        if (trackerModel != null) {
            trackerModel.fireTableDataChanged();
        }
        updateTrackerHeader();
    }

    private void showSelectedDetails() {
        int row = historyTable.getSelectedRow();
        if (row < 0 || historyList == null || row >= historyList.size()) return;

        DeploymentHistory history = historyList.get(row);
        Window parent = SwingUtilities.getWindowAncestor(this);
        JFrame frame = (parent instanceof JFrame) ? (JFrame) parent : null;
        DeploymentResultDialog dialog = new DeploymentResultDialog(frame, history);
        dialog.setVisible(true);
    }

    private void clearHistory() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to clear all deployment history?\nThis cannot be undone.",
                "Clear History", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            historyService.clearHistory();
            refreshData();
        }
    }

    private void exportTrackerToCsv() {
        if (historyList == null || historyList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No tracker data available to export.",
                    "Export Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Tracker Data to CSV");
        String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss"));
        chooser.setSelectedFile(new java.io.File("tracker_" + timestamp + ".csv"));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new java.io.File(file.getAbsolutePath() + ".csv");
            }

            try (java.io.FileWriter writer = new java.io.FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                String envName = environmentService.getActiveEnvironment() != null
                        ? environmentService.getActiveEnvironment().getName() : "Env";
                writer.write("Kit Name,Ticket No,PIC,Fix Date,(" + envName + ") Deploy Date,Deployable List\n");

                for (DeploymentHistory h : historyList) {
                    String kit = escapeCsv(h.getKitName() != null ? h.getKitName() : "");
                    String ticket = escapeCsv(h.getTicketNumber() != null ? h.getTicketNumber() : "");
                    String pic = escapeCsv(h.getPic() != null ? h.getPic() : "");
                    String dateStr = h.getDeployedAt().format(DATE_FORMAT).trim();
                    String files = String.join(", ", h.getFileNames());
                    String filesEscaped = escapeCsv(files);

                    writer.write(String.format("%s,%s,%s,%s,%s,%s\n",
                            kit, ticket, pic, dateStr, dateStr, filesEscaped));
                }

                JOptionPane.showMessageDialog(this, "Tracker data successfully exported to:\n" + file.getAbsolutePath(),
                        "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to export tracker data:\n" + e.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ==================== HISTORY TABLE MODEL ====================

    private class HistoryTableModel extends AbstractTableModel {
        private final String[] COLUMNS = {"Date", "Ticket", "Files", "Servers", "Success", "Failed", "Status"};

        @Override
        public int getRowCount() {
            return historyList != null ? historyList.size() : 0;
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
            if (historyList == null || rowIndex >= historyList.size()) return "";
            DeploymentHistory h = historyList.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> h.getDeployedAt().format(DATE_FORMAT);
                case 1 -> h.getTicketNumber() != null && !h.getTicketNumber().isBlank()
                        ? h.getTicketNumber() : "-";
                case 2 -> h.getTotalFiles();
                case 3 -> h.getTotalServers();
                case 4 -> h.getSuccessCount();
                case 5 -> h.getFailedCount();
                case 6 -> h.getOverallStatus();
                default -> "";
            };
        }
    }

    // ==================== TRACKER TABLE MODEL ====================

    private class TrackerTableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return historyList != null ? historyList.size() : 0;
        }

        @Override
        public int getColumnCount() {
            return 6;
        }

        @Override
        public String getColumnName(int column) {
            String envName = (environmentService != null && environmentService.getActiveEnvironment() != null)
                    ? environmentService.getActiveEnvironment().getName() : "Current Env";
            return switch (column) {
                case 0 -> "Kit Name";
                case 1 -> "Ticket No";
                case 2 -> "PIC";
                case 3 -> "Fix Date";
                case 4 -> "(" + envName + ") Deploy Date";
                case 5 -> "Deployable List";
                default -> "";
            };
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (historyList == null || rowIndex >= historyList.size()) return "";
            DeploymentHistory h = historyList.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> h.getKitName() != null && !h.getKitName().isBlank() ? h.getKitName() : "-";
                case 1 -> h.getTicketNumber() != null && !h.getTicketNumber().isBlank() ? h.getTicketNumber() : "-";
                case 2 -> h.getPic() != null && !h.getPic().isBlank() ? h.getPic() : "-";
                case 3 -> h.getDeployedAt().format(DATE_FORMAT);
                case 4 -> h.getDeployedAt().format(DATE_FORMAT);
                case 5 -> h.getFileNames() != null ? String.join(", ", h.getFileNames()) : "";
                default -> "";
            };
        }
    }

    // ==================== STATUS RENDERER ====================

    private static class StatusColumnRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            String status = String.valueOf(value);
            switch (status) {
                case "SUCCESS" -> {
                    label.setText("Success");
                    if (!isSelected) label.setForeground(ThemeManager.SUCCESS);
                }
                case "FAILED" -> {
                    label.setText("Failed");
                    if (!isSelected) label.setForeground(ThemeManager.ERROR);
                }
                case "PARTIAL" -> {
                    label.setText("Partial");
                    if (!isSelected) label.setForeground(ThemeManager.WARNING);
                }
                default -> {
                    if (!isSelected) label.setForeground(ThemeManager.TEXT_MUTED);
                }
            }

            if (!isSelected) {
                label.setBackground(row % 2 == 0 ? ThemeManager.BG_SURFACE : ThemeManager.TABLE_ROW_ALT);
            }
            label.setBorder(new EmptyBorder(4, 8, 4, 8));
            label.setFont(ThemeManager.FONT_BOLD);
            return label;
        }
    }
}
