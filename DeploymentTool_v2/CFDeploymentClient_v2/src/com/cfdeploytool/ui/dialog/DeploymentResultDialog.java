package com.cfdeploytool.ui.dialog;

import com.cfdeploytool.model.DeploymentHistory;
import com.cfdeploytool.model.DeploymentResult;
import com.cfdeploytool.ui.ThemeManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Modal dialog showing detailed deployment results for a specific deployment.
 * Displays a summary header and a results table.
 */
public class DeploymentResultDialog extends JDialog {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss");

    private final DeploymentHistory history;

    public DeploymentResultDialog(JFrame parent, DeploymentHistory history) {
        super(parent, "Deployment Details", true);
        this.history = history;
        initComponents();
        setSize(800, 550);
        setLocationRelativeTo(parent);
        ThemeManager.applyToDialog(this);
    }

    private void initComponents() {
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBackground(ThemeManager.BG_SURFACE);
        content.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header with summary
        content.add(createSummaryPanel(), BorderLayout.NORTH);

        // Results table
        content.add(createResultsTable(), BorderLayout.CENTER);

        // Close button
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(ThemeManager.BG_SURFACE);
        JButton closeBtn = ThemeManager.createSecondaryButton("Close");
        closeBtn.addActionListener(e -> dispose());
        bottomPanel.add(closeBtn);
        content.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private JPanel createSummaryPanel() {
        JPanel panel = ThemeManager.createCardPanel();
        panel.setLayout(new BorderLayout(0, 12));

        // Title row
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setBackground(ThemeManager.BG_CARD);

        JLabel titleLabel = ThemeManager.createHeaderLabel("Deployment Results");
        String ticket = history.getTicketNumber();
        String meta = history.getDeployedAt().format(DATE_FORMAT)
                + (ticket != null && !ticket.isBlank() ? "  |  Ticket: " + ticket : "");
        JLabel dateLabel = ThemeManager.createSecondaryLabel(meta);
        titleRow.add(titleLabel, BorderLayout.WEST);
        titleRow.add(dateLabel, BorderLayout.EAST);
        panel.add(titleRow, BorderLayout.NORTH);

        // Stats row
        JPanel statsRow = new JPanel(new GridLayout(1, 5, 12, 0));
        statsRow.setBackground(ThemeManager.BG_CARD);

        statsRow.add(createStatCard("Total Operations",
                String.valueOf(history.getTotalOperations()), ThemeManager.ACCENT_SECONDARY));
        statsRow.add(createStatCard("Files",
                String.valueOf(history.getTotalFiles()), ThemeManager.TEXT_PRIMARY));
        statsRow.add(createStatCard("Servers",
                String.valueOf(history.getTotalServers()), ThemeManager.TEXT_PRIMARY));
        statsRow.add(createStatCard("Succeeded",
                String.valueOf(history.getSuccessCount()), ThemeManager.SUCCESS));
        statsRow.add(createStatCard("Failed",
                String.valueOf(history.getFailedCount()), ThemeManager.ERROR));

        panel.add(statsRow, BorderLayout.CENTER);

        // Files and servers list
        JPanel detailsRow = new JPanel(new GridLayout(1, 2, 12, 0));
        detailsRow.setBackground(ThemeManager.BG_CARD);

        detailsRow.add(createListCard("Files", history.getFileNames()));
        detailsRow.add(createListCard("Servers", history.getServerNames()));

        panel.add(detailsRow, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createStatCard(String label, String value, Color valueColor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(ThemeManager.BG_SURFACE);
        card.setBorder(new CompoundBorder(
                new LineBorder(ThemeManager.BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(ThemeManager.FONT_TITLE);
        valueLabel.setForeground(valueColor);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel nameLabel = ThemeManager.createSecondaryLabel(label);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(valueLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(nameLabel);

        return card;
    }

    private JPanel createListCard(String title, List<String> items) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(ThemeManager.BG_SURFACE);
        card.setBorder(new CompoundBorder(
                new LineBorder(ThemeManager.BORDER, 1, true),
                new EmptyBorder(6, 10, 6, 10)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(ThemeManager.FONT_SMALL);
        titleLabel.setForeground(ThemeManager.TEXT_SECONDARY);
        card.add(titleLabel, BorderLayout.NORTH);

        JLabel listLabel = new JLabel(String.join(", ", items));
        listLabel.setFont(ThemeManager.FONT_REGULAR);
        listLabel.setForeground(ThemeManager.TEXT_PRIMARY);
        card.add(listLabel, BorderLayout.CENTER);

        return card;
    }

    private JScrollPane createResultsTable() {
        List<DeploymentResult> results = history.getResults();
        ResultTableModel model = new ResultTableModel(results);
        JTable table = new JTable(model);
        ThemeManager.styleTable(table);
        table.getTableHeader().setBorder(new LineBorder(ThemeManager.BORDER, 1));
        table.setDefaultRenderer(Object.class, ThemeManager.createTableRenderer());

        // Status column renderer
        table.getColumnModel().getColumn(2).setCellRenderer(new ResultStatusRenderer());

        table.getColumnModel().getColumn(0).setPreferredWidth(180); // File
        table.getColumnModel().getColumn(1).setPreferredWidth(180); // Server
        table.getColumnModel().getColumn(2).setPreferredWidth(100); // Status
        table.getColumnModel().getColumn(3).setPreferredWidth(300); // Message

        JScrollPane scrollPane = ThemeManager.styleScrollPane(table);

        return scrollPane;
    }

    // ==================== TABLE MODEL ====================

    private static class ResultTableModel extends AbstractTableModel {
        private final String[] COLUMNS = {"File", "Server", "Status", "Message"};
        private final List<DeploymentResult> results;

        ResultTableModel(List<DeploymentResult> results) {
            this.results = results;
        }

        @Override
        public int getRowCount() {
            return results.size();
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
            if (rowIndex >= results.size()) return "";
            DeploymentResult r = results.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> r.getFileName();
                case 1 -> r.getServerName();
                case 2 -> r.getStatus().getDisplayName();
                case 3 -> r.getMessage();
                default -> "";
            };
        }
    }

    // ==================== STATUS RENDERER ====================

    private static class ResultStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            String status = String.valueOf(value);
            switch (status) {
                case "Success" -> {
                    label.setText(status);
                    if (!isSelected) label.setForeground(ThemeManager.SUCCESS);
                }
                case "Failed" -> {
                    label.setText(status);
                    if (!isSelected) label.setForeground(ThemeManager.ERROR);
                }
                case "Skipped" -> {
                    label.setText(status);
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
