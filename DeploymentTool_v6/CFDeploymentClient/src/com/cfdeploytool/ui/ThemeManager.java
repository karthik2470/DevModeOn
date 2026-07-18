package com.cfdeploytool.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Path2D;

/**
 * Light "Studio" theme with custom-painted controls (works on all L&amp;Fs).
 */
public class ThemeManager {

    // === Palette — soft gray workspace, teal accent ===
    public static final Color NAV_BG = new Color(0xE8, 0xED, 0xF3);
    public static final Color NAV_SELECTED = new Color(0xFF, 0xFF, 0xFF);
    public static final Color NAV_HOVER = new Color(0xF8, 0xFA, 0xFC);
    public static final Color NAV_TEXT = new Color(0x33, 0x41, 0x55);
    public static final Color NAV_TEXT_ACTIVE = new Color(0x0F, 0x76, 0x6E);

    public static final Color BG_DARK = new Color(0xF1, 0xF5, 0xF9);
    public static final Color BG_SURFACE = new Color(0xF8, 0xFA, 0xFC);
    public static final Color BG_CARD = Color.WHITE;
    public static final Color BG_INPUT = Color.WHITE;
    public static final Color BG_HOVER = new Color(0xF1, 0xF5, 0xF9);

    public static final Color ACCENT_PRIMARY = new Color(0x0D, 0x94, 0x88);
    public static final Color ACCENT_SECONDARY = new Color(0x14, 0xB8, 0xA6);
    public static final Color ACCENT_HOVER = new Color(0x0F, 0x76, 0x6E);

    public static final Color TEXT_PRIMARY = new Color(0x1E, 0x29, 0x3B);
    public static final Color TEXT_SECONDARY = new Color(0x64, 0x74, 0x8B);
    public static final Color TEXT_MUTED = new Color(0x94, 0xA3, 0xB8);

    public static final Color SUCCESS = new Color(0x16, 0xA3, 0x4A);
    public static final Color WARNING = new Color(0xD9, 0x77, 0x06);
    public static final Color ERROR = new Color(0xDC, 0x26, 0x26);
    public static final Color INFO = new Color(0x02, 0x84, 0xC7);

    public static final Color BORDER = new Color(0xE2, 0xE8, 0xF0);
    public static final Color BORDER_FOCUS = new Color(0x5E, 0xEA, 0xD4);
    public static final Color SELECTION_BG = new Color(0xCC, 0xFB, 0xF1);
    public static final Color SELECTION_FG = new Color(0x0F, 0x76, 0x6E);
    public static final Color TABLE_ROW_ALT = new Color(0xF8, 0xFA, 0xFC);

    public static final int SIDEBAR_WIDTH = 200;
    public static final int SIDEBAR_PAD = 12;

    public static final int BTN_HEIGHT = 34;
    public static final int BTN_HEIGHT_LARGE = 42;
    public static final int BTN_RADIUS = 8;

    public static final Font FONT_REGULAR;
    public static final Font FONT_BOLD;
    public static final Font FONT_SMALL;
    public static final Font FONT_TITLE;
    public static final Font FONT_HEADER;
    public static final Font FONT_MONO;

    static {
        String fontFamily = "Segoe UI";
        for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if ("Segoe UI".equals(name)) {
                fontFamily = "Segoe UI";
                break;
            }
            fontFamily = "Dialog";
        }
        FONT_REGULAR = new Font(fontFamily, Font.PLAIN, 13);
        FONT_BOLD = new Font(fontFamily, Font.BOLD, 13);
        FONT_SMALL = new Font(fontFamily, Font.PLAIN, 12);
        FONT_TITLE = new Font(fontFamily, Font.BOLD, 20);
        FONT_HEADER = new Font(fontFamily, Font.BOLD, 15);
        FONT_MONO = new Font("Consolas", Font.PLAIN, 12);
    }

    public static void apply() {
        UIManager.put("control", BG_SURFACE);
        UIManager.put("Panel.background", BG_SURFACE);
        UIManager.put("Panel.foreground", TEXT_PRIMARY);
        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("Label.font", new FontUIResource(FONT_REGULAR));

        // Let ThemedButton paint — keep defaults neutral
        UIManager.put("Button.background", BG_CARD);
        UIManager.put("Button.foreground", TEXT_PRIMARY);
        UIManager.put("Button.font", new FontUIResource(FONT_BOLD));
        UIManager.put("Button.focus", new ColorUIResource(BORDER_FOCUS));
        UIManager.put("Button.select", BG_HOVER);

        UIManager.put("TextField.background", BG_INPUT);
        UIManager.put("TextField.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.caretForeground", TEXT_PRIMARY);
        UIManager.put("TextField.font", new FontUIResource(FONT_REGULAR));
        UIManager.put("TextField.border", inputBorder());
        UIManager.put("TextField.selectionBackground", SELECTION_BG);
        UIManager.put("TextField.selectionForeground", SELECTION_FG);

        UIManager.put("TextArea.background", BG_INPUT);
        UIManager.put("TextArea.foreground", TEXT_PRIMARY);
        UIManager.put("TextArea.font", new FontUIResource(FONT_REGULAR));
        UIManager.put("TextArea.selectionBackground", SELECTION_BG);

        UIManager.put("ComboBox.background", BG_INPUT);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.selectionBackground", SELECTION_BG);
        UIManager.put("ComboBox.font", new FontUIResource(FONT_REGULAR));

        UIManager.put("Table.background", BG_CARD);
        UIManager.put("Table.foreground", TEXT_PRIMARY);
        UIManager.put("Table.selectionBackground", SELECTION_BG);
        UIManager.put("Table.selectionForeground", SELECTION_FG);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("Table.font", new FontUIResource(FONT_REGULAR));
        UIManager.put("TableHeader.background", BG_DARK);
        UIManager.put("TableHeader.foreground", TEXT_SECONDARY);
        UIManager.put("TableHeader.font", new FontUIResource(FONT_BOLD));

        UIManager.put("ScrollPane.background", BG_SURFACE);
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.thumb", new Color(0xCB, 0xD5, 0xE1));
        UIManager.put("ScrollBar.track", BG_SURFACE);

        UIManager.put("ProgressBar.background", BG_DARK);
        UIManager.put("ProgressBar.foreground", ACCENT_PRIMARY);
        UIManager.put("ProgressBar.font", new FontUIResource(FONT_SMALL));

        UIManager.put("CheckBox.background", BG_CARD);
        UIManager.put("CheckBox.foreground", TEXT_PRIMARY);
        UIManager.put("CheckBox.font", new FontUIResource(FONT_REGULAR));
        UIManager.put("RadioButton.background", BG_CARD);
        UIManager.put("RadioButton.foreground", TEXT_PRIMARY);
        UIManager.put("RadioButton.font", new FontUIResource(FONT_REGULAR));

        UIManager.put("OptionPane.background", BG_SURFACE);
        UIManager.put("Dialog.background", BG_SURFACE);
        UIManager.put("SplitPane.background", BG_SURFACE);
        UIManager.put("SplitPane.dividerSize", 8);
        UIManager.put("Separator.foreground", BORDER);
        UIManager.put("TitledBorder.titleColor", TEXT_SECONDARY);
        UIManager.put("ToolTip.background", BG_CARD);
        UIManager.put("ToolTip.foreground", TEXT_PRIMARY);
        UIManager.put("ToolTip.border", new LineBorder(BORDER, 1));
        UIManager.put("PopupMenu.background", BG_CARD);
        UIManager.put("PopupMenu.foreground", TEXT_PRIMARY);
        UIManager.put("MenuItem.selectionBackground", SELECTION_BG);
    }

    private static Border inputBorder() {
        return new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(6, 10, 6, 10));
    }

    // ==================== Buttons (custom paint) ====================

    public static JButton createAccentButton(String text) {
        return new ThemedButton(text, ACCENT_PRIMARY, Color.WHITE, ACCENT_HOVER, null, false);
    }

    public static JButton createSecondaryButton(String text) {
        return new ThemedButton(text, BG_CARD, TEXT_PRIMARY, BG_HOVER, BORDER, false);
    }

    public static JButton createDangerButton(String text) {
        return new ThemedButton(text, ERROR, Color.WHITE, ERROR.brighter(), null, false);
    }

    public static JButton createSuccessButton(String text) {
        return new ThemedButton(text, SUCCESS, Color.WHITE, SUCCESS.brighter(), null, false);
    }

    public static JButton createPrimaryActionButton(String text) {
        ThemedButton b = new ThemedButton(text, ACCENT_PRIMARY, Color.WHITE, ACCENT_HOVER, null, true);
        b.setFont(FONT_BOLD.deriveFont(14f));
        return b;
    }

    /** Sidebar navigation item. */
    public static NavButton createNavButton(String text) {
        return new NavButton(text);
    }

    /** BoxLayout child: left-aligned and spans full sidebar/content width. */
    public static void bindFullWidth(JComponent component, int height) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        int h = height > 0 ? height : component.getPreferredSize().height;
        if (h <= 0) {
            h = 32;
        }
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
    }

    /** Wraps a sidebar nav control with consistent horizontal inset. */
    public static JPanel wrapSidebarNav(JComponent navControl) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(2, SIDEBAR_PAD, 2, SIDEBAR_PAD));
        bindFullWidth(wrap, 48);
        navControl.setPreferredSize(new Dimension(SIDEBAR_WIDTH - SIDEBAR_PAD * 2, 44));
        navControl.setMaximumSize(new Dimension(SIDEBAR_WIDTH - SIDEBAR_PAD * 2, 44));
        wrap.add(navControl, BorderLayout.CENTER);
        return wrap;
    }

    public static JPanel createActionBar(JComponent... items) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        bar.setOpaque(false);
        for (JComponent c : items) {
            bar.add(c);
        }
        return bar;
    }

    public static JPanel createPageHeader(String title, String subtitle) {
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 14, 0));

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        JLabel t = createHeaderLabel(title);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(t);
        if (subtitle != null && !subtitle.isEmpty()) {
            JLabel s = createSecondaryLabel(subtitle);
            s.setAlignmentX(Component.LEFT_ALIGNMENT);
            text.add(Box.createVerticalStrut(2));
            text.add(s);
        }
        header.add(text, BorderLayout.WEST);
        return header;
    }

    public static JPanel wrapPage(JComponent content) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BG_SURFACE);
        page.setBorder(new EmptyBorder(16, 20, 16, 20));
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    public static JPanel createSection(String title, JComponent body) {
        JPanel section = createCardPanel();
        section.setLayout(new BorderLayout(0, 10));

        JLabel lbl = new JLabel(title);
        lbl.setFont(FONT_BOLD);
        lbl.setForeground(TEXT_PRIMARY);
        lbl.setBorder(new EmptyBorder(0, 0, 4, 0));
        section.add(lbl, BorderLayout.NORTH);
        section.add(body, BorderLayout.CENTER);
        return section;
    }

    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_CARD);
        panel.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(14, 16, 14, 16)));
        return panel;
    }

    public static JLabel createHeaderLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_HEADER);
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    public static JLabel createSecondaryLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_SMALL);
        label.setForeground(TEXT_SECONDARY);
        return label;
    }

    public static void styleTable(JTable table) {
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_PRIMARY);
        table.setSelectionBackground(SELECTION_BG);
        table.setSelectionForeground(SELECTION_FG);
        table.setGridColor(BORDER);
        table.setRowHeight(36);
        table.setFont(FONT_REGULAR);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.getTableHeader().setBackground(BG_DARK);
        table.getTableHeader().setForeground(TEXT_SECONDARY);
        table.getTableHeader().setFont(FONT_BOLD);
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, createTableRenderer());
    }

    public static DefaultTableCellRenderer createTableRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? BG_CARD : TABLE_ROW_ALT);
                    c.setForeground(TEXT_PRIMARY);
                } else {
                    c.setBackground(SELECTION_BG);
                    c.setForeground(SELECTION_FG);
                }
                setBorder(new EmptyBorder(4, 10, 4, 10));
                return c;
            }
        };
    }

    public static JScrollPane styleScrollPane(JComponent view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(new LineBorder(BORDER, 1, true));
        scroll.getViewport().setBackground(
                view instanceof JTable ? BG_CARD : BG_INPUT);
        scroll.setBackground(BG_SURFACE);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        return scroll;
    }

    public static JTextField styleTextField(JTextField field) {
        field.setFont(FONT_REGULAR);
        field.setBackground(BG_INPUT);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(TEXT_PRIMARY);
        field.setBorder(inputBorder());
        return field;
    }

    public static JTextArea styleTextArea(JTextArea area) {
        area.setFont(FONT_REGULAR);
        area.setBackground(BG_INPUT);
        area.setForeground(TEXT_PRIMARY);
        area.setCaretColor(TEXT_PRIMARY);
        area.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(8, 10, 8, 10)));
        return area;
    }

    public static void styleRadio(JRadioButton radio) {
        radio.setBackground(BG_CARD);
        radio.setForeground(TEXT_PRIMARY);
        radio.setFont(FONT_REGULAR);
        radio.setFocusPainted(false);
    }

    public static void styleCheckBox(JCheckBox box) {
        box.setBackground(BG_CARD);
        box.setForeground(TEXT_PRIMARY);
        box.setFont(FONT_REGULAR);
        box.setFocusPainted(false);
    }

    public static void styleProgressBar(JProgressBar bar) {
        bar.setBackground(BG_DARK);
        bar.setForeground(ACCENT_PRIMARY);
        bar.setFont(FONT_SMALL);
        bar.setBorder(new LineBorder(BORDER, 1, true));
    }

    public static void applyToDialog(JDialog dialog) {
        dialog.getContentPane().setBackground(BG_SURFACE);
    }

    public static void applyToFrame(JFrame frame) {
        frame.getContentPane().setBackground(BG_SURFACE);
    }

    private static void sizeButton(JButton button, boolean large) {
        Font font = button.getFont();
        if (font == null) {
            font = large ? FONT_BOLD.deriveFont(14f) : FONT_BOLD;
        }
        FontMetrics fm = button.getFontMetrics(font);
        String label = button.getText() != null ? button.getText() : "";
        int pad = large ? 40 : 32;
        int w = Math.max(large ? 160 : 88, fm.stringWidth(label) + pad);
        int h = large ? BTN_HEIGHT_LARGE : BTN_HEIGHT;
        Dimension d = new Dimension(w, h);
        button.setMinimumSize(d);
        button.setPreferredSize(d);
    }

    // ==================== Custom button ====================

    static final class ThemedButton extends JButton {
        private final Color bg;
        private final Color fg;
        private final Color hover;
        private final Color outline;
        private final boolean large;
        private boolean hovering;

        ThemedButton(String text, Color bg, Color fg, Color hover, Color outline, boolean large) {
            super();
            this.bg = bg;
            this.fg = fg;
            this.hover = hover;
            this.outline = outline;
            this.large = large;
            setFont(large ? FONT_BOLD.deriveFont(14f) : FONT_BOLD);
            setForeground(fg);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setText(text);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovering = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovering = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            Color fill = !isEnabled() ? BG_HOVER
                    : (hovering ? hover : bg);

            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, BTN_RADIUS, BTN_RADIUS));

            if (outline != null && isEnabled()) {
                g2.setColor(outline);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 2, h - 2, BTN_RADIUS, BTN_RADIUS));
            }

            if (getIcon() != null) {
                Icon icon = getIcon();
                int ix = (w - icon.getIconWidth()) / 2;
                int iy = (h - icon.getIconHeight()) / 2;
                icon.paintIcon(this, g2, ix, iy);
            } else {
                g2.setColor(isEnabled() ? fg : TEXT_MUTED);
                FontMetrics fm = g2.getFontMetrics();
                String text = getText() != null ? getText() : "";
                int tx = (w - fm.stringWidth(text)) / 2;
                int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(text, tx, ty);
            }
            g2.dispose();
        }

        @Override
        public void setText(String text) {
            super.setText(text);
            if (getFont() != null) {
                sizeButton(this, large);
            }
        }
    }

    public static final class NavButton extends JButton {
        private boolean selected;
        private boolean hovering;

        NavButton(String text) {
            super();
            setFont(FONT_BOLD);
            setText(text);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new EmptyBorder(10, 16, 10, 16));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.LEFT_ALIGNMENT);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovering = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovering = false;
                    repaint();
                }
            });
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            if (selected) {
                g2.setColor(NAV_SELECTED);
                g2.fillRoundRect(4, 2, w - 8, h - 4, 8, 8);
                g2.setColor(ACCENT_PRIMARY);
                g2.fillRoundRect(4, 8, 4, h - 16, 2, 2);
            } else if (hovering) {
                g2.setColor(NAV_HOVER);
                g2.fillRoundRect(4, 2, w - 8, h - 4, 8, 8);
            }

            g2.setColor(selected ? NAV_TEXT_ACTIVE : NAV_TEXT);
            g2.setFont(getFont());
            g2.drawString(getText(), 8, (h + g2.getFontMetrics().getAscent()) / 2 - 2);
            g2.dispose();
        }
    }

    // ==================== CUSTOM VECTOR ICONS ====================

    public static JButton createIconButton(Icon icon) {
        ThemedButton btn = new ThemedButton("", BG_CARD, TEXT_PRIMARY, BG_HOVER, BORDER, false);
        btn.setIcon(icon);
        btn.setPreferredSize(new Dimension(32, 28));
        return btn;
    }

    public static Icon getRefreshIcon(Color color) { return new RefreshIcon(color); }
    public static Icon getSelectAllIcon(Color color) { return new SelectAllIcon(color); }
    public static Icon getSettingsIcon(Color color) { return new SettingsIcon(color); }
    public static Icon getAddIcon(Color color) { return new AddIcon(color); }
    public static Icon getCopyIcon(Color color) { return new CopyIcon(color); }
    public static Icon getDeleteIcon(Color color) { return new DeleteIcon(color); }

    private static class RefreshIcon implements Icon {
        private final Color color;
        RefreshIcon(Color color) { this.color = color; }
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.isEnabled() ? color : TEXT_MUTED);
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Draw 3/4 circle arc
            g2.drawArc(x + 2, y + 2, 12, 12, 40, 280);
            // Draw arrowhead
            g2.setStroke(new BasicStroke(1.0f));
            Path2D path = new Path2D.Double();
            path.moveTo(x + 12, y + 1);
            path.lineTo(x + 7, y + 4);
            path.lineTo(x + 11, y + 8);
            path.closePath();
            g2.fill(path);
            g2.dispose();
        }
    }

    private static class SelectAllIcon implements Icon {
        private final Color color;
        SelectAllIcon(Color color) { this.color = color; }
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.isEnabled() ? color : TEXT_MUTED);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(x + 1, y + 1, 14, 14, 3, 3);
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 4, y + 8, x + 7, y + 11);
            g2.drawLine(x + 7, y + 11, x + 12, y + 4);
            g2.dispose();
        }
    }

    private static class SettingsIcon implements Icon {
        private final Color color;
        SettingsIcon(Color color) { this.color = color; }
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.isEnabled() ? color : TEXT_MUTED);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawOval(x + 4, y + 4, 8, 8);
            g2.fillOval(x + 7, y + 7, 2, 2);
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND));
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                int x1 = (int) (x + 8 + 4 * Math.cos(angle));
                int y1 = (int) (y + 8 + 4 * Math.sin(angle));
                int x2 = (int) (x + 8 + 6 * Math.cos(angle));
                int y2 = (int) (y + 8 + 6 * Math.sin(angle));
                g2.drawLine(x1, y1, x2, y2);
            }
            g2.dispose();
        }
    }

    private static class AddIcon implements Icon {
        private final Color color;
        AddIcon(Color color) { this.color = color; }
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.isEnabled() ? color : TEXT_MUTED);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 3, y + 8, x + 13, y + 8);
            g2.drawLine(x + 8, y + 3, x + 8, y + 13);
            g2.dispose();
        }
    }

    private static class CopyIcon implements Icon {
        private final Color color;
        CopyIcon(Color color) { this.color = color; }
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.isEnabled() ? color : TEXT_MUTED);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(x + 1, y + 1, 9, 11, 1, 1);
            g2.setColor(c.getBackground() != null ? c.getBackground() : BG_CARD);
            g2.fillRoundRect(x + 5, y + 4, 9, 11, 1, 1);
            g2.setColor(c.isEnabled() ? color : TEXT_MUTED);
            g2.drawRoundRect(x + 5, y + 4, 9, 11, 1, 1);
            g2.dispose();
        }
    }

    private static class DeleteIcon implements Icon {
        private final Color color;
        DeleteIcon(Color color) { this.color = color; }
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.isEnabled() ? color : TEXT_MUTED);
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 4, y + 4, x + 12, y + 12);
            g2.drawLine(x + 12, y + 4, x + 4, y + 12);
            g2.dispose();
        }
    }
}
