package org.stemmate.ui;

import javax.swing.border.AbstractBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

public final class RoundedBorder extends AbstractBorder {
    private final Color color;
    private final int radius;
    private final int thickness;

    public RoundedBorder(Color color, int radius, int thickness) {
        this.color = color;
        this.radius = radius;
        this.thickness = thickness;
    }

    @Override
    public Insets getBorderInsets(Component component) {
        int padding = Math.max(6, radius / 2);
        return new Insets(padding, padding, padding, padding);
    }

    @Override
    public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
        Graphics2D copy = (Graphics2D) graphics.create();
        try {
            copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            copy.setColor(color);
            copy.setStroke(new BasicStroke(thickness));
            copy.drawRoundRect(
                    x + thickness / 2,
                    y + thickness / 2,
                    width - thickness,
                    height - thickness,
                    radius,
                    radius);
        } finally {
            copy.dispose();
        }
    }
}
