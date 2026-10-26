package com.campusclub.gui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Central UI Theme and Design System for Campus Club Management System Swing GUI.
 * Provides consistent colors, typography, buttons, tables, and card styling.
 */
public final class UITheme {

    // Private constructor to prevent instantiation
    private UITheme() {}

    // ==========================================
    // Color Palette (Professional Navy & Slate)
    // ==========================================
    public static final Color PRIMARY_NAVY = new Color(25, 42, 86);      // #192A56 - Header / Brand
    public static final Color PRIMARY_BLUE = new Color(37, 99, 235);     // #2563EB - Primary Actions
    public static final Color PRIMARY_BLUE_HOVER = new Color(29, 78, 216);
    public static final Color ACCENT_EMERALD = new Color(16, 185, 129);  // #10B981 - Success / Join
    public static final Color ACCENT_EMERALD_HOVER = new Color(5, 150, 105);
    public static final Color DANGER_RED = new Color(239, 68, 68);       // #EF4444 - Danger / Logout / Reject
    public static final Color DANGER_RED_HOVER = new Color(220, 38, 38);
    public static final Color WARNING_AMBER = new Color(245, 158, 11);   // #F59E0B - Warning / Pending
    public static final Color PURPLE_ACCENT = new Color(124, 58, 237);   // #7C3AED - Recommendations

    public static final Color BG_LIGHT = new Color(248, 250, 252);       // #F8FAFC - Main App Background
    public static final Color CARD_BG = Color.WHITE;
    public static final Color BORDER_COLOR = new Color(226, 232, 240);   // #E2E8F0
    public static final Color TEXT_PRIMARY = new Color(15, 23, 42);      // #0F172A
    public static final Color TEXT_MUTED = new Color(100, 116, 139);     // #64748B
    public static final Color TABLE_HEADER_BG = new Color(241, 245, 249);
    public static final Color TABLE_SELECTION_BG = new Color(219, 234, 254);

    // ==========================================
    // Typography (Segoe UI font hierarchy)
    // ==========================================
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_METRIC_VALUE = new Font("Segoe UI", Font.BOLD, 24);

    // ==========================================
    // Button Factories
    // ==========================================

    public static JButton createPrimaryButton(String text) {
        return createStyledButton(text, PRIMARY_BLUE, Color.WHITE);
    }

    public static JButton createSuccessButton(String text) {
        return createStyledButton(text, ACCENT_EMERALD, Color.WHITE);
    }

    public static JButton createDangerButton(String text) {
        return createStyledButton(text, DANGER_RED, Color.WHITE);
    }

    public static JButton createSecondaryButton(String text) {
        JButton button = createStyledButton(text, new Color(241, 245, 249), TEXT_PRIMARY);
        button.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(7, 14, 7, 14)
        ));
        return button;
    }

    public static JButton createStyledButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text);
        button.setFont(FONT_BOLD);
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(8, 16, 8, 16));
        button.setOpaque(true);
        return button;
    }

    // ==========================================
    // Panel & Card Factories
    // ==========================================

    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD_BG);
        panel.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));
        return panel;
    }

    public static JPanel createSectionHeader(String title, String subtitle) {
        JPanel panel = new JPanel(new GridLayout(2, 1, 2, 2));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(0, 0, 8, 0));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(FONT_SUBTITLE);
        titleLbl.setForeground(TEXT_PRIMARY);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(FONT_SMALL);
        subLbl.setForeground(TEXT_MUTED);

        panel.add(titleLbl);
        panel.add(subLbl);
        return panel;
    }

    // ==========================================
    // Table Styling Helper
    // ==========================================

    public static void styleTable(JTable table) {
        table.setFont(FONT_REGULAR);
        table.setRowHeight(28);
        table.setGridColor(BORDER_COLOR);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setSelectionBackground(TABLE_SELECTION_BG);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_HEADER);
        header.setBackground(TABLE_HEADER_BG);
        header.setForeground(TEXT_PRIMARY);
        header.setPreferredSize(new Dimension(header.getWidth(), 34));
        header.setReorderingAllowed(false);

        // Center-align column headers
        DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) header.getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.LEFT);
    }
}
