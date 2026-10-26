package com.campusclub.gui;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;

/**
 * Reusable Swing metric card component for top-level dashboard statistics.
 * Displays title, numeric value, description, and an accent indicator bar.
 */
public class DashboardCard extends JPanel {
    private final JLabel titleLabel;
    private final JLabel valueLabel;
    private final JLabel subtitleLabel;

    public DashboardCard(String title, String initialValue, String subtitle, Color accentColor) {
        setLayout(new BorderLayout(5, 5));
        setBackground(UITheme.CARD_BG);

        // Top accent line + subtle border + inner padding
        setBorder(new CompoundBorder(
                new MatteBorder(3, 0, 0, 0, accentColor),
                new CompoundBorder(
                        BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                        new EmptyBorder(12, 16, 12, 16)
                )
        ));

        titleLabel = new JLabel(title);
        titleLabel.setFont(UITheme.FONT_BOLD);
        titleLabel.setForeground(UITheme.TEXT_MUTED);

        valueLabel = new JLabel(initialValue);
        valueLabel.setFont(UITheme.FONT_METRIC_VALUE);
        valueLabel.setForeground(accentColor);

        subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(UITheme.FONT_SMALL);
        subtitleLabel.setForeground(UITheme.TEXT_MUTED);

        JPanel contentPanel = new JPanel(new GridLayout(2, 1, 2, 2));
        contentPanel.setOpaque(false);
        contentPanel.add(titleLabel);
        contentPanel.add(valueLabel);

        add(contentPanel, BorderLayout.CENTER);
        add(subtitleLabel, BorderLayout.SOUTH);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setValue(int value) {
        valueLabel.setText(String.valueOf(value));
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle);
    }

    public String getValue() {
        return valueLabel.getText();
    }

    public String getTitle() {
        return titleLabel.getText();
    }
}
