package com.cfdeploytool.ui.panel;

import com.cfdeploytool.model.DeploymentHistory;
import com.cfdeploytool.service.HistoryService;
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
 * Panel displaying deployment history in a table view.
 * Double-click to see detailed results.
 */
public class HistoryPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss");

    private final HistoryService historyService;
    private JTable historyTable;
    private HistoryTableModel tableModel;
    private List<DeploymentHistory> historyList;

    public HistoryPanel(HistoryService historyService) {
        this.historyService = historyService;
        setLayout(new BorderLayout(0, 0));
        setBackground(ThemeManager.BG_SURFACE);
        setBorder(new EmptyBorder(16, 20, 16, 20));
        initComponents();
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

        JLabel titleLabel = ThemeManager.createHeaderLabel("Deployment History");
        JLabel subtitleLabel = ThemeManager.createSecondaryLabel(
                "View past deployments and their results. Double-click for details.");
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(subtitleLabel);

        JButton refreshBtn = ThemeManager.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> refreshData());
        JButton clearBtn = ThemeManager.createDangerButton("Clear History");
        clearBtn.addActionListener(e -> clearHistory());

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(ThemeManager.createActionBar(refreshBtn, clearBtn), BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // History table
        tableModel = new HistoryTableModel();
        historyTable = new JTable(tableModel);
        ThemeManager.styleTable(historyTable);
        historyTable.setRowHeight(40);
        historyTable.setIntercellSpacing(new Dimension(0, 1));
        historyTable.getTableHeader().setBorder(new LineBorder(ThemeManager.BORDER, 1));
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyTable.setDefaultRenderer(Object.class, ThemeManager.createTableRenderer());

        // Status column renderer
        historyTable.getColumnModel().getColumn(6).setCellRenderer(new StatusColumnRenderer());

        // Column widths
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(150); // Date
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Ticket
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(60);  // Files
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(70);  // Servers
        historyTable.getColumnModel().getColumn(4).setPreferredWidth(70);  // Success
        historyTable.getColumnModel().getColumn(5).setPreferredWidth(60);  // Failed
        historyTable.getColumnModel().getColumn(6).setPreferredWidth(90);  // Status

        // Double-click for details
        historyTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showSelectedDetails();
                }
            }
        });

        JScrollPane scrollPane = ThemeManager.styleScrollPane(historyTable);

        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Refreshes the history table from the service.
     */
    public void refreshData() {
        historyList = historyService.getHistory();
        if (tableModel != null) {
            tableModel.fireTableDataChanged();
        }
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

    // ==================== TABLE MODEL ====================

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
