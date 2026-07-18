package com.cfdeploytool.ui.panel;

import com.cfdeploytool.model.*;
import com.cfdeploytool.model.DeploymentResult.ResultStatus;
import com.cfdeploytool.service.DeploymentService;
import com.cfdeploytool.service.HttpDeploymentClient;
import com.cfdeploytool.ui.ThemeManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Panel showing real-time deployment progress with per-server logs and version updates.
 */
public class ProgressPanel extends JPanel {

    private JProgressBar overallProgress;
    private JLabel overallLabel;
    private JLabel currentActionLabel;
    private JPanel serverProgressPanel;
    private JPanel versionPanel;

    private final Map<String, JProgressBar> serverProgressBars = new HashMap<>();
    private final Map<String, JLabel> serverStatusLabels = new HashMap<>();
    private final Map<String, JPanel> serverLogPanels = new HashMap<>();

    private int totalOperations = 0;
    private int completedOperations = 0;
    private int successCount = 0;
    private int failCount = 0;

    private JLabel summarySuccessLabel;
    private JLabel summaryFailLabel;
    private JLabel summaryTotalLabel;

    public ProgressPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(ThemeManager.BG_SURFACE);
        setBorder(new EmptyBorder(16, 20, 16, 20));
        initComponents();
    }

    private void initComponents() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ThemeManager.BG_SURFACE);
        headerPanel.setBorder(new EmptyBorder(0, 0, 12, 0));

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBackground(ThemeManager.BG_SURFACE);

        JLabel titleLabel = ThemeManager.createHeaderLabel("Deployment Progress");
        JLabel subtitleLabel = ThemeManager.createSecondaryLabel("Validation, deployment, and version tracking");
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(subtitleLabel);

        headerPanel.add(titlePanel, BorderLayout.WEST);

        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        summaryPanel.setBackground(ThemeManager.BG_SURFACE);
        summaryTotalLabel = createSummaryBadge("0 / 0", ThemeManager.ACCENT_SECONDARY);
        summarySuccessLabel = createSummaryBadge("0 OK", ThemeManager.SUCCESS);
        summaryFailLabel = createSummaryBadge("0 Fail", ThemeManager.ERROR);
        summaryPanel.add(summaryTotalLabel);
        summaryPanel.add(summarySuccessLabel);
        summaryPanel.add(summaryFailLabel);
        headerPanel.add(summaryPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(ThemeManager.BG_SURFACE);

        JPanel overallCard = ThemeManager.createCardPanel();
        overallCard.setLayout(new BorderLayout(0, 8));
        overallCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        overallCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel overallHeader = new JPanel(new BorderLayout());
        overallHeader.setBackground(ThemeManager.BG_CARD);
        overallLabel = new JLabel("Overall Progress");
        overallLabel.setFont(ThemeManager.FONT_BOLD);
        overallLabel.setForeground(ThemeManager.TEXT_PRIMARY);
        currentActionLabel = ThemeManager.createSecondaryLabel("Waiting for deployment...");
        overallHeader.add(overallLabel, BorderLayout.WEST);
        overallHeader.add(currentActionLabel, BorderLayout.EAST);

        overallProgress = new JProgressBar(0, 100);
        overallProgress.setStringPainted(true);
        overallProgress.setString("0%");
        ThemeManager.styleProgressBar(overallProgress);
        overallProgress.setPreferredSize(new Dimension(0, 24));

        overallCard.add(overallHeader, BorderLayout.NORTH);
        overallCard.add(overallProgress, BorderLayout.CENTER);
        contentPanel.add(overallCard);
        contentPanel.add(Box.createVerticalStrut(12));

        versionPanel = new JPanel();
        versionPanel.setLayout(new BoxLayout(versionPanel, BoxLayout.Y_AXIS));
        versionPanel.setBackground(ThemeManager.BG_SURFACE);
        versionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(versionPanel);
        contentPanel.add(Box.createVerticalStrut(12));

        JLabel serverSectionLabel = new JLabel("Per-Server Progress & Logs");
        serverSectionLabel.setFont(ThemeManager.FONT_BOLD);
        serverSectionLabel.setForeground(ThemeManager.TEXT_PRIMARY);
        serverSectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        serverSectionLabel.setBorder(new EmptyBorder(0, 4, 8, 0));
        contentPanel.add(serverSectionLabel);

        serverProgressPanel = new JPanel();
        serverProgressPanel.setLayout(new BoxLayout(serverProgressPanel, BoxLayout.Y_AXIS));
        serverProgressPanel.setBackground(ThemeManager.BG_SURFACE);
        contentPanel.add(serverProgressPanel);

        JScrollPane mainScroll = new JScrollPane(contentPanel);
        mainScroll.setBorder(null);
        mainScroll.setBackground(ThemeManager.BG_SURFACE);
        mainScroll.getViewport().setBackground(ThemeManager.BG_SURFACE);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(mainScroll, BorderLayout.CENTER);
    }

    private JLabel createSummaryBadge(String text, Color color) {
        JLabel badge = new JLabel(text);
        badge.setFont(ThemeManager.FONT_BOLD);
        badge.setForeground(color);
        badge.setOpaque(true);
        badge.setBackground(ThemeManager.BG_CARD);
        badge.setBorder(new CompoundBorder(
                new LineBorder(color.darker(), 1, true),
                new EmptyBorder(4, 12, 4, 12)));
        return badge;
    }

    public void reset(DeploymentRequest request) {
        SwingUtilities.invokeLater(() -> {
            this.totalOperations = request.getTotalOperations();
            this.completedOperations = 0;
            this.successCount = 0;
            this.failCount = 0;

            overallProgress.setValue(0);
            overallProgress.setString("0%");
            overallLabel.setText("Overall Progress - 0 / " + totalOperations);
            currentActionLabel.setText("Validating target paths...");

            summaryTotalLabel.setText("0 / " + totalOperations);
            summarySuccessLabel.setText("0 OK");
            summaryFailLabel.setText("0 Fail");

            serverProgressPanel.removeAll();
            versionPanel.removeAll();
            serverProgressBars.clear();
            serverStatusLabels.clear();
            serverLogPanels.clear();

            DeploymentCategory category = request.getCategory();
            DeploymentScope scope = request.getScope();

            String ticket = request.getTicketNumber();
            JLabel categoryLabel = ThemeManager.createSecondaryLabel(
                    "Ticket: " + (ticket != null && !ticket.isBlank() ? ticket : "-")
                            + "  |  Category: " + category.getDisplayName()
                            + "  |  Other->2T, Corporate->2T+4T");
            categoryLabel.setBorder(new EmptyBorder(0, 4, 8, 0));
            versionPanel.add(categoryLabel);

            for (Server server : request.getTargetServers()) {
                int filesPerServer = request.getFiles().size() * request.getTiersForServer(server).size();
                String cardTitle = server.getName()
                        + (server.isCorporate() ? " (Corporate, 2T+4T)" : " (Other, 2T)");

                JPanel serverCard = ThemeManager.createCardPanel();
                serverCard.setLayout(new BorderLayout(0, 6));
                serverCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
                serverCard.setAlignmentX(Component.LEFT_ALIGNMENT);
                serverCard.setBorder(new CompoundBorder(
                        new TitledBorder(new LineBorder(ThemeManager.BORDER, 1, true), cardTitle),
                        new EmptyBorder(8, 8, 8, 8)));

                JPanel serverHeader = new JPanel(new BorderLayout());
                serverHeader.setBackground(ThemeManager.BG_CARD);

                StringBuilder pathText = new StringBuilder();
                for (DeploymentTier tier : request.getTiersForServer(server)) {
                    pathText.append(tier.getDisplayName()).append(" -> ")
                            .append(category.getPath(tier)).append("  ");
                }
                JLabel pathLabel = ThemeManager.createSecondaryLabel(pathText.toString().trim());
                JLabel statusLabel = ThemeManager.createSecondaryLabel("Validating...");
                serverStatusLabels.put(server.getName(), statusLabel);

                JProgressBar bar = new JProgressBar(0, filesPerServer);
                bar.setStringPainted(true);
                bar.setString("0 / " + filesPerServer);
                ThemeManager.styleProgressBar(bar);
                bar.setForeground(ThemeManager.ACCENT_SECONDARY);
                bar.setPreferredSize(new Dimension(0, 18));
                serverProgressBars.put(server.getName(), bar);

                serverHeader.add(pathLabel, BorderLayout.WEST);
                serverHeader.add(statusLabel, BorderLayout.EAST);

                JPanel logPanel = new JPanel();
                logPanel.setLayout(new BoxLayout(logPanel, BoxLayout.Y_AXIS));
                logPanel.setBackground(ThemeManager.BG_CARD);
                logPanel.setBorder(new EmptyBorder(4, 0, 0, 0));
                serverLogPanels.put(server.getName(), logPanel);

                JScrollPane logScroll = new JScrollPane(logPanel);
                logScroll.setBorder(new LineBorder(ThemeManager.BORDER, 1, true));
                logScroll.setPreferredSize(new Dimension(0, 80));
                logScroll.getViewport().setBackground(ThemeManager.BG_CARD);

                JPanel center = new JPanel();
                center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
                center.setBackground(ThemeManager.BG_CARD);
                center.add(bar);
                center.add(Box.createVerticalStrut(6));
                center.add(logScroll);

                serverCard.add(serverHeader, BorderLayout.NORTH);
                serverCard.add(center, BorderLayout.CENTER);

                serverProgressPanel.add(serverCard);
                serverProgressPanel.add(Box.createVerticalStrut(8));
            }

            serverProgressPanel.revalidate();
            serverProgressPanel.repaint();
            versionPanel.revalidate();
            versionPanel.repaint();
        });
    }

    public DeploymentService.ProgressCallback createCallback() {
        return new DeploymentService.ProgressCallback() {
            @Override
            public void onValidationResult(HttpDeploymentClient.PathValidationResult result) {
                SwingUtilities.invokeLater(() -> {
                    String serverName = result.getServer().getName();
                    JLabel statusLabel = serverStatusLabels.get(serverName);
                    if (statusLabel != null) {
                        statusLabel.setText(result.isValid() ? "Path OK" : "Path invalid");
                        statusLabel.setForeground(result.isValid() ? ThemeManager.SUCCESS : ThemeManager.ERROR);
                    }
                    appendServerLog(serverName, result.getTier().getDisplayName()
                            + " - Validate: " + result.getMessage(), result.isValid());
                    if (!result.isValid()) {
                        currentActionLabel.setText("Validation failed - deployment aborted");
                        currentActionLabel.setForeground(ThemeManager.ERROR);
                    }
                });
            }

            @Override
            public void onProgress(DeploymentResult result, int completed, int total) {
                SwingUtilities.invokeLater(() -> {
                    completedOperations = completed;
                    if (result.isSuccess()) {
                        successCount++;
                    } else if (result.getStatus() == ResultStatus.FAILED) {
                        failCount++;
                    }

                    int percent = total > 0 ? (int) ((completed * 100.0) / total) : 0;
                    overallProgress.setValue(percent);
                    overallProgress.setString(percent + "%");
                    overallLabel.setText("Overall Progress - " + completed + " / " + total);
                    summaryTotalLabel.setText(completed + " / " + total);
                    summarySuccessLabel.setText(successCount + " OK");
                    summaryFailLabel.setText(failCount + " Fail");

                    JProgressBar serverBar = serverProgressBars.get(result.getServerName());
                    if (serverBar != null) {
                        serverBar.setValue(serverBar.getValue() + 1);
                        serverBar.setString(serverBar.getValue() + " / " + serverBar.getMaximum());
                        if (!result.isSuccess()) {
                            serverBar.setForeground(ThemeManager.WARNING);
                        }
                    }

                    JLabel statusLabel = serverStatusLabels.get(result.getServerName());
                    if (statusLabel != null) {
                        statusLabel.setText(result.getStatus().getDisplayName());
                        statusLabel.setForeground(result.isSuccess() ? ThemeManager.SUCCESS : ThemeManager.ERROR);
                    }
                });
            }

            @Override
            public void onComplete(List<DeploymentResult> results) {
                SwingUtilities.invokeLater(() -> {
                    currentActionLabel.setText(failCount == 0 && completedOperations > 0
                            ? "Deployment complete!" : "Deployment finished with issues");
                    currentActionLabel.setForeground(failCount == 0 ? ThemeManager.SUCCESS : ThemeManager.WARNING);
                    overallProgress.setForeground(failCount == 0 ? ThemeManager.SUCCESS : ThemeManager.WARNING);
                });
            }

            @Override
            public void onServerStart(Server server) {
                SwingUtilities.invokeLater(() -> {
                    JLabel statusLabel = serverStatusLabels.get(server.getName());
                    if (statusLabel != null) {
                        statusLabel.setText("Deploying...");
                        statusLabel.setForeground(ThemeManager.ACCENT_SECONDARY);
                    }
                    currentActionLabel.setText("Deploying to " + server.getName() + "...");
                });
            }

            @Override
            public void onFileStart(Server server, DeploymentFile file, DeploymentTier tier) {
                SwingUtilities.invokeLater(() ->
                        currentActionLabel.setText("Sending " + file.getFileName()
                                + " -> " + server.getName() + " [" + tier.getDisplayName() + "]"));
            }

            @Override
            public void onLog(Server server, String message, boolean success) {
                SwingUtilities.invokeLater(() -> appendServerLog(server.getName(), message, success));
            }
        };
    }

    private void appendServerLog(String serverName, String message, boolean success) {
        JPanel logPanel = serverLogPanels.get(serverName);
        if (logPanel == null) {
            return;
        }

        JPanel entry = new JPanel(new BorderLayout(8, 0));
        entry.setBackground(ThemeManager.BG_CARD);
        entry.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        entry.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel iconLabel = new JLabel(success ? "OK" : "X");
        iconLabel.setFont(ThemeManager.FONT_SMALL);
        iconLabel.setForeground(success ? ThemeManager.SUCCESS : ThemeManager.ERROR);
        iconLabel.setPreferredSize(new Dimension(16, 18));

        JLabel textLabel = new JLabel(message);
        textLabel.setFont(ThemeManager.FONT_SMALL);
        textLabel.setForeground(ThemeManager.TEXT_SECONDARY);

        entry.add(iconLabel, BorderLayout.WEST);
        entry.add(textLabel, BorderLayout.CENTER);
        logPanel.add(entry);
        logPanel.revalidate();
        logPanel.repaint();
    }
}
