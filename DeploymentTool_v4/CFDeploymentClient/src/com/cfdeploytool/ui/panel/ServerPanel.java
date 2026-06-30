package com.cfdeploytool.ui.panel;

import com.cfdeploytool.model.Server;
import com.cfdeploytool.model.Server.ServerStatus;
import com.cfdeploytool.service.ServerManager;
import com.cfdeploytool.ui.ThemeManager;
import com.cfdeploytool.ui.dialog.AddServerDialog;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Panel for managing registered deployment servers.
 * Provides a table view with CRUD operations and health checking.
 */
public class ServerPanel extends JPanel {

    private final ServerManager serverManager;
    private final JFrame parentFrame;
    private JTable serverTable;
    private ServerTableModel tableModel;

    public ServerPanel(ServerManager serverManager, JFrame parentFrame) {
        this.serverManager = serverManager;
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout(0, 0));
        setBackground(ThemeManager.BG_SURFACE);
        setBorder(new EmptyBorder(16, 20, 16, 20));
        initComponents();
    }

    private void initComponents() {
        // Header with title and actions
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ThemeManager.BG_SURFACE);
        headerPanel.setBorder(new EmptyBorder(0, 0, 12, 0));

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBackground(ThemeManager.BG_SURFACE);

        JLabel titleLabel = ThemeManager.createHeaderLabel("Server Management");
        JLabel subtitleLabel = ThemeManager.createSecondaryLabel(
                "Register and manage target deployment servers");
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(subtitleLabel);

        JButton refreshBtn = ThemeManager.createSecondaryButton("Check Status");
        refreshBtn.addActionListener(e -> refreshServerStatus());
        JButton editBtn = ThemeManager.createSecondaryButton("Edit");
        editBtn.addActionListener(e -> editSelectedServer());
        JButton removeBtn = ThemeManager.createDangerButton("Remove");
        removeBtn.addActionListener(e -> removeSelectedServer());
        JButton addBtn = ThemeManager.createAccentButton("Add Server");
        addBtn.addActionListener(e -> addServer());

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(ThemeManager.createActionBar(refreshBtn, editBtn, removeBtn, addBtn), BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Server table
        tableModel = new ServerTableModel();
        serverTable = new JTable(tableModel);
        ThemeManager.styleTable(serverTable);
        serverTable.setRowHeight(40);
        serverTable.setIntercellSpacing(new Dimension(0, 1));
        serverTable.getTableHeader().setBorder(new LineBorder(ThemeManager.BORDER, 1));
        serverTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Custom renderers
        serverTable.setDefaultRenderer(Object.class, ThemeManager.createTableRenderer());
        serverTable.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());

        // Column widths
        serverTable.getColumnModel().getColumn(0).setPreferredWidth(160); // Name
        serverTable.getColumnModel().getColumn(1).setPreferredWidth(160); // Host
        serverTable.getColumnModel().getColumn(2).setPreferredWidth(60);  // Port
        serverTable.getColumnModel().getColumn(3).setPreferredWidth(110); // Type
        serverTable.getColumnModel().getColumn(4).setPreferredWidth(90);  // Status
        serverTable.getColumnModel().getColumn(5).setPreferredWidth(200); // Description

        // Double-click to edit
        serverTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedServer();
                }
            }
        });

        // Right-click context menu
        JPopupMenu contextMenu = createContextMenu();
        serverTable.setComponentPopupMenu(contextMenu);

        JScrollPane scrollPane = ThemeManager.styleScrollPane(serverTable);

        add(scrollPane, BorderLayout.CENTER);

        // Empty state
        if (serverManager.getServerCount() == 0) {
            showEmptyState();
        }
    }

    private void showEmptyState() {
        // Will be replaced when servers are added
    }

    private JPopupMenu createContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(ThemeManager.BG_CARD);
        menu.setBorder(new LineBorder(ThemeManager.BORDER, 1));

        JMenuItem editItem = new JMenuItem("Edit Server");
        editItem.setBackground(ThemeManager.BG_CARD);
        editItem.setForeground(ThemeManager.TEXT_PRIMARY);
        editItem.addActionListener(e -> editSelectedServer());

        JMenuItem removeItem = new JMenuItem("Remove Server");
        removeItem.setBackground(ThemeManager.BG_CARD);
        removeItem.setForeground(ThemeManager.ERROR);
        removeItem.addActionListener(e -> removeSelectedServer());

        JMenuItem checkItem = new JMenuItem("Check Status");
        checkItem.setBackground(ThemeManager.BG_CARD);
        checkItem.setForeground(ThemeManager.TEXT_PRIMARY);
        checkItem.addActionListener(e -> checkSelectedServerStatus());

        menu.add(editItem);
        menu.add(checkItem);
        menu.addSeparator();
        menu.add(removeItem);

        return menu;
    }

    // ==================== SERVER OPERATIONS ====================

    private void addServer() {
        AddServerDialog dialog = new AddServerDialog(parentFrame, serverManager);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            serverManager.addServer(dialog.getServer());
            tableModel.fireTableDataChanged();
        }
    }

    private void editSelectedServer() {
        int row = serverTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a server to edit.",
                    "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<Server> servers = serverManager.getServers();
        Server server = servers.get(row);
        AddServerDialog dialog = new AddServerDialog(parentFrame, serverManager, server);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            serverManager.updateServer(dialog.getServer());
            tableModel.fireTableDataChanged();
        }
    }

    private void removeSelectedServer() {
        int row = serverTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a server to remove.",
                    "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<Server> servers = serverManager.getServers();
        Server server = servers.get(row);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove \"" + server.getName() + "\"?",
                "Confirm Removal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            serverManager.removeServer(server.getId());
            tableModel.fireTableDataChanged();
        }
    }

    private void refreshServerStatus() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                serverManager.checkAllServersHealth();
                return null;
            }

            @Override
            protected void done() {
                tableModel.fireTableDataChanged();
                setCursor(Cursor.getDefaultCursor());
            }
        };
        worker.execute();
    }

    private void checkSelectedServerStatus() {
        int row = serverTable.getSelectedRow();
        if (row < 0) return;
        List<Server> servers = serverManager.getServers();
        Server server = servers.get(row);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                serverManager.checkServerHealth(server);
                return null;
            }

            @Override
            protected void done() {
                tableModel.fireTableDataChanged();
                setCursor(Cursor.getDefaultCursor());
            }
        };
        worker.execute();
    }

    /**
     * Refreshes the table data from the server manager.
     */
    public void refreshData() {
        tableModel.fireTableDataChanged();
    }

    // ==================== TABLE MODEL ====================

    private class ServerTableModel extends AbstractTableModel {
        private final String[] COLUMNS = {"Name", "Host", "Port", "Type", "Status", "Description"};

        @Override
        public int getRowCount() {
            return serverManager.getServers().size();
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
            List<Server> servers = serverManager.getServers();
            if (rowIndex >= servers.size()) return "";
            Server server = servers.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> server.getName();
                case 1 -> server.getHost();
                case 2 -> server.getPort();
                case 3 -> server.getServerTypesDisplayString();
                case 4 -> server.getStatus();
                case 5 -> server.getDescription();
                default -> "";
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }

    // ==================== CUSTOM RENDERERS ====================

    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            if (value instanceof ServerStatus status) {
                switch (status) {
                    case ONLINE -> {
                        label.setText("Online");
                        if (!isSelected) label.setForeground(ThemeManager.SUCCESS);
                    }
                    case OFFLINE -> {
                        label.setText("Offline");
                        if (!isSelected) label.setForeground(ThemeManager.ERROR);
                    }
                    case UNKNOWN -> {
                        label.setText("Unknown");
                        if (!isSelected) label.setForeground(ThemeManager.TEXT_MUTED);
                    }
                }
            }

            if (!isSelected) {
                label.setBackground(row % 2 == 0 ? ThemeManager.BG_SURFACE : ThemeManager.TABLE_ROW_ALT);
            }
            label.setBorder(new EmptyBorder(4, 8, 4, 8));
            return label;
        }
    }
}
