package org.stemmate.ui;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;

public final class AppTheme {
    public static final Color CANVAS = new Color(0xF7F7F5);
    public static final Color SURFACE = new Color(0xFFFFFF);
    public static final Color INPUT = new Color(0xFAFAF8);
    public static final Color BORDER = new Color(0xE5E5DF);
    public static final Color TEXT_PRIMARY = new Color(0x171716);
    public static final Color TEXT_SECONDARY = new Color(0x73736C);
    public static final Color BRAND_50 = new Color(0xF2F7F4);
    public static final Color BRAND_100 = new Color(0xE2EDE7);
    public static final Color BRAND_500 = new Color(0x2D6A4F);
    public static final Color BRAND_600 = new Color(0x1F4E3D);
    public static final Color BRAND_700 = new Color(0x16382C);
    public static final Color BRAND_800 = new Color(0x112B22);
    public static final Color BRAND_900 = new Color(0x0D1F19);
    public static final Color WARM_50 = new Color(0xFAFAFA);
    public static final Color WARM_100 = new Color(0xF4F4F0);
    public static final Color WARM_200 = new Color(0xE5E5DF);
    public static final Color WARM_300 = new Color(0xD4D4CB);
    public static final Color WARM_600 = new Color(0x73736C);
    public static final Color WARM_800 = new Color(0x262624);
    public static final Color WARM_900 = new Color(0x171716);
    public static final Color AMBER_50 = new Color(0xFFF8E8);
    public static final Color AMBER_500 = new Color(0xB7791F);
    public static final Color AMBER_700 = new Color(0x855B12);
    public static final Color EMERALD_50 = new Color(0xEDF8F1);
    public static final Color EMERALD_500 = new Color(0x2F855A);
    public static final Color EMERALD_700 = new Color(0x236B48);
    public static final Color RED_50 = new Color(0xFFF2F1);
    public static final Color RED_500 = new Color(0xC94B45);
    public static final Color RED_700 = new Color(0x963B37);
    public static final Color INDIGO_50 = new Color(0xF3F3FF);
    public static final Color INDIGO_500 = new Color(0x5B5FC7);
    public static final Color INDIGO_700 = new Color(0x45489C);

    public static final Font UI_REGULAR = loadFont("PlusJakartaSans-Regular.ttf", "Segoe UI", Font.PLAIN, 13);
    public static final Font UI_SMALL = UI_REGULAR.deriveFont(12f);
    public static final Font UI_BOLD = loadFont("PlusJakartaSans-Bold.ttf", "Segoe UI", Font.BOLD, 13);
    public static final Font HEADING = UI_BOLD.deriveFont(20f);
    public static final Font META = loadFont("JetBrainsMono-Regular.ttf", "Consolas", Font.PLAIN, 11);
    public static final Font META_BOLD = loadFont("JetBrainsMono-Bold.ttf", "Consolas", Font.BOLD, 10);

    private AppTheme() {
    }

    public static void install() {
        FlatLightLaf.setup();
        UIManager.put("Panel.background", SURFACE);
        UIManager.put("Viewport.background", CANVAS);
        UIManager.put("ScrollPane.background", CANVAS);
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 12);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("Component.focusWidth", 3);
        UIManager.put("Component.focusColor", BRAND_500);
        UIManager.put("Component.focusedBorderColor", BRAND_500);
        UIManager.put("Button.background", WARM_100);
        UIManager.put("Button.foreground", WARM_800);
        UIManager.put("TextField.background", INPUT);
        UIManager.put("TextField.foreground", TEXT_PRIMARY);
        UIManager.put("TextArea.background", INPUT);
        UIManager.put("TextArea.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.background", INPUT);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.alternateRowColor", WARM_50);
        UIManager.put("Table.selectionBackground", BRAND_100);
        UIManager.put("Table.selectionForeground", TEXT_PRIMARY);
        UIManager.put("TableHeader.background", WARM_50);
        UIManager.put("TableHeader.foreground", TEXT_SECONDARY);
    }

    public static void styleSurface(JComponent component) {
        component.setBackground(SURFACE);
        component.setForeground(TEXT_PRIMARY);
        component.setFont(UI_REGULAR);
        component.setBorder(new RoundedBorder(BORDER, 16, 1));
    }

    public static void stylePrimaryButton(AbstractButton button) {
        button.setBackground(BRAND_600);
        button.setForeground(Color.WHITE);
        button.setFont(UI_BOLD);
        button.setBorder(new RoundedBorder(BRAND_600, 12, 1));
        button.setFocusPainted(true);
        button.addMouseListener(ThemeComponents.hover(button, BRAND_600, BRAND_700, Color.WHITE));
    }

    public static void styleSecondaryButton(AbstractButton button) {
        button.setBackground(WARM_100);
        button.setForeground(WARM_800);
        button.setFont(UI_BOLD);
        button.setBorder(new RoundedBorder(BORDER, 12, 1));
        button.setFocusPainted(true);
        button.addMouseListener(ThemeComponents.hover(button, WARM_100, WARM_200, WARM_800));
    }

    private static Font loadFont(String resourceName, String fallback, int style, float size) {
        try (InputStream stream = AppTheme.class.getResourceAsStream("/fonts/" + resourceName)) {
            if (stream != null) {
                return Font.createFont(Font.TRUETYPE_FONT, stream)
                        .deriveFont(style, size);
            }
        } catch (FontFormatException | IOException ignored) {
            // Fall back to a system font when a packaged font cannot be loaded.
        }
        return new Font(isAvailable(fallback) ? fallback : Font.SANS_SERIF, style, Math.round(size));
    }

    private static boolean isAvailable(String family) {
        for (String installed : GraphicsEnvironment
                .getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (installed.equalsIgnoreCase(family)) {
                return true;
            }
        }
        return false;
    }
}
