package com.cfdeploytool.ui;

import com.cfdeploytool.model.DeployPathResolver;
import com.cfdeploytool.persistence.FileStore;
import com.cfdeploytool.service.*;
import com.cfdeploytool.service.TaskManagementService;
import com.cfdeploytool.ui.panel.*;
import com.cfdeploytool.ui.panel.TaskManagementPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Main window with sidebar navigation and content area.
 */
public class MainFrame extends JFrame {

    private final ServerManager serverManager;
    private final EnvironmentService environmentService;

    private JPanel contentCards;
    private CardLayout cardLayout;
    private final List<ThemeManager.NavButton> navButtons = new ArrayList<>();
    private ThemeManager.NavButton settingsNavButton;
    private JLabel activeEnvSidebarLabel;
    private JLabel serverCountLabel;

    private ServerPanel serverPanel;
    private DeployPanel deployPanel;
    private ProgressPanel progressPanel;
    private HistoryPanel historyPanel;
    private BackupPanel backupPanel;
    private TaskManagementPanel taskPanel;
    private JLabel statusLabel;

    public MainFrame(FileStore fileStore, EnvironmentService environmentService, ServerManager serverManager) {
        this.serverManager = serverManager;
        this.environmentService = environmentService;
        DeployPathResolver.init(environmentService);

        HistoryService historyService = new HistoryService(fileStore);
        DeploymentService deploymentService = new DeploymentService(historyService, environmentService);
        BackupService backupService = new BackupService();

        initializeFrame();
        buildUi(historyService, deploymentService, backupService);

        environmentService.addChangeListener(this::refreshSidebarFooter);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                deploymentService.shutdown();
                System.exit(0);
            }
        });
    }

    private void initializeFrame() {
        setTitle("CF Deployment Tool");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 780);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);
        setIconImage(createAppIcon());
    }

    private void buildUi(HistoryService historyService,
                         DeploymentService deploymentService, BackupService backupService) {
        progressPanel = new ProgressPanel();
        historyPanel = new HistoryPanel(historyService, environmentService);
        deployPanel = new DeployPanel(serverManager, deploymentService, environmentService,
                progressPanel, historyPanel, this);
        serverPanel = new ServerPanel(serverManager, this);
        backupPanel = new BackupPanel(backupService, serverManager, environmentService, this);
        TaskManagementService taskService = new TaskManagementService();
        taskPanel = new TaskManagementPanel(serverManager, environmentService, taskService, this);

        cardLayout = new CardLayout(8, 8);
        contentCards = new JPanel(cardLayout);
        contentCards.setBackground(ThemeManager.BG_SURFACE);
        contentCards.add(serverPanel, "servers");
        contentCards.add(deployPanel, "deploy");
        contentCards.add(progressPanel, "progress");
        contentCards.add(historyPanel, "history");
        contentCards.add(backupPanel, "backup");
        contentCards.add(taskPanel, "tasks");

        JPanel sidebar = buildSidebar();
        JPanel mainArea = new JPanel(new BorderLayout());
        mainArea.setBackground(ThemeManager.BG_SURFACE);
        mainArea.add(contentCards, BorderLayout.CENTER);

        statusLabel = new JLabel(" Ready");
        statusLabel.setFont(ThemeManager.FONT_SMALL);
        statusLabel.setForeground(ThemeManager.TEXT_SECONDARY);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(ThemeManager.BG_CARD);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.BORDER),
                new EmptyBorder(8, 16, 8, 16)));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ThemeManager.BG_SURFACE);
        root.add(sidebar, BorderLayout.WEST);
        root.add(mainArea, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);

        setContentPane(root);
        refreshSidebarFooter();
        showPage("deploy", 1);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(ThemeManager.NAV_BG);
        sidebar.setPreferredSize(new Dimension(ThemeManager.SIDEBAR_WIDTH, 0));
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, ThemeManager.BORDER));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(16, 0, 8, 0));

        JLabel brand = new JLabel("CF Deploy");
        brand.setFont(ThemeManager.FONT_TITLE.deriveFont(16f));
        brand.setForeground(ThemeManager.ACCENT_PRIMARY);
        brand.setBorder(new EmptyBorder(4, ThemeManager.SIDEBAR_PAD, 4, ThemeManager.SIDEBAR_PAD));
        ThemeManager.bindFullWidth(brand, 28);

        JLabel brandSub = ThemeManager.createSecondaryLabel("Deployment Suite");
        brandSub.setBorder(new EmptyBorder(0, ThemeManager.SIDEBAR_PAD, 0, ThemeManager.SIDEBAR_PAD));
        ThemeManager.bindFullWidth(brandSub, 16);

        top.add(brand);
        top.add(brandSub);
        top.add(Box.createVerticalStrut(12));
        addNav(top, "Servers", "servers", 0);
        addNav(top, "Deploy", "deploy", 1);
        addNav(top, "Progress", "progress", 2);
        addNav(top, "History", "history", 3);
        addNav(top, "Backup", "backup", 4);
        addNav(top, "Task Manager", "tasks", 5);

        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(12, 0, 12, 0));

        JLabel envTitle = ThemeManager.createSecondaryLabel("Active environment");
        envTitle.setBorder(new EmptyBorder(0, ThemeManager.SIDEBAR_PAD, 0, ThemeManager.SIDEBAR_PAD));
        ThemeManager.bindFullWidth(envTitle, 18);

        activeEnvSidebarLabel = new JLabel("Default");
        activeEnvSidebarLabel.setFont(ThemeManager.FONT_BOLD);
        activeEnvSidebarLabel.setForeground(ThemeManager.NAV_TEXT_ACTIVE);
        activeEnvSidebarLabel.setBorder(new EmptyBorder(4, ThemeManager.SIDEBAR_PAD, 4, ThemeManager.SIDEBAR_PAD));
        ThemeManager.bindFullWidth(activeEnvSidebarLabel, 22);

        JPanel switchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, ThemeManager.SIDEBAR_PAD, 0));
        switchPanel.setOpaque(false);
        JButton switchBtn = ThemeManager.createSecondaryButton("Switch");
        switchBtn.setPreferredSize(new Dimension(88, 28));
        switchBtn.addActionListener(e -> switchEnvironment());
        switchPanel.add(switchBtn);
        ThemeManager.bindFullWidth(switchPanel, 32);

        settingsNavButton = ThemeManager.createNavButton("Settings");
        settingsNavButton.addActionListener(e -> showSettingsPage());
        footer.add(envTitle);
        footer.add(activeEnvSidebarLabel);
        footer.add(switchPanel);
        footer.add(Box.createVerticalStrut(6));
        footer.add(ThemeManager.wrapSidebarNav(settingsNavButton));

        serverCountLabel = ThemeManager.createSecondaryLabel("0 servers");
        serverCountLabel.setBorder(new EmptyBorder(6, ThemeManager.SIDEBAR_PAD, 0, ThemeManager.SIDEBAR_PAD));
        ThemeManager.bindFullWidth(serverCountLabel, 18);
        footer.add(serverCountLabel);

        sidebar.add(top, BorderLayout.NORTH);
        sidebar.add(footer, BorderLayout.SOUTH);
        return sidebar;
    }

    private void addNav(JPanel parent, String label, String card, int index) {
        ThemeManager.NavButton btn = ThemeManager.createNavButton(label);
        btn.addActionListener(e -> showPage(card, index));
        parent.add(ThemeManager.wrapSidebarNav(btn));
        navButtons.add(btn);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(ThemeManager.BG_CARD);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.BORDER),
                new EmptyBorder(12, 20, 12, 20)));

        JLabel title = new JLabel("CF Deployment Tool");
        title.setFont(ThemeManager.FONT_TITLE);
        title.setForeground(ThemeManager.TEXT_PRIMARY);
        bar.add(title, BorderLayout.WEST);

        JLabel sub = ThemeManager.createSecondaryLabel("Enterprise file deployment automation");
        bar.add(sub, BorderLayout.SOUTH);
        return bar;
    }

    private void showPage(String card, int navIndex) {
        cardLayout.show(contentCards, card);
        settingsNavButton.setSelected(false);
        for (int i = 0; i < navButtons.size(); i++) {
            navButtons.get(i).setSelected(i == navIndex);
        }
        if ("deploy".equals(card)) {
            deployPanel.refreshServers();
        } else if ("tasks".equals(card)) {
            taskPanel.refreshServers();
            taskPanel.refreshActiveTab();
        }
    }

    private void showSettingsPage() {
        settingsNavButton.setSelected(true);
        com.cfdeploytool.ui.dialog.ChooseEnvDialog.EnvConfigDialog envDialog = new com.cfdeploytool.ui.dialog.ChooseEnvDialog.EnvConfigDialog(
                this, environmentService.getActiveEnvironment(), environmentService);
        envDialog.setVisible(true);
        settingsNavButton.setSelected(false);
        notifyEnvironmentChanged();
    }

    private void switchEnvironment() {
        com.cfdeploytool.ui.dialog.ChooseEnvDialog envDialog = new com.cfdeploytool.ui.dialog.ChooseEnvDialog(this, environmentService, serverManager, false);
        envDialog.setVisible(true);
        if (envDialog.isConfirmed()) {
            notifyEnvironmentChanged();
            setStatus("Switched active environment to \"" + environmentService.getActiveEnvironmentName() + "\"");
        }
    }

    public void refreshSidebarFooter() {
        SwingUtilities.invokeLater(() -> {
            activeEnvSidebarLabel.setText(environmentService.getActiveEnvironmentName());
            serverCountLabel.setText(serverManager.getServerCount() + " servers");
        });
    }

    public void notifyEnvironmentChanged() {
        refreshSidebarFooter();
        deployPanel.onEnvironmentChanged();
        taskPanel.onEnvironmentChanged();
    }

    public void switchToProgressTab() {
        showPage("progress", 2);
    }

    public void switchToHistoryTab() {
        showPage("history", 3);
    }

    public void switchToBackupTab() {
        showPage("backup", 4);
    }

    public void setStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(" " + text));
    }

    public ServerManager getServerManager() {
        return serverManager;
    }

    private Image createAppIcon() {
        int size = 32;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(ThemeManager.ACCENT_PRIMARY);
        g.fillRoundRect(0, 0, size, size, 8, 8);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(10, 16, 22, 16);
        g.drawLine(17, 10, 22, 16);
        g.drawLine(17, 22, 22, 16);
        g.dispose();
        return img;
    }
}
