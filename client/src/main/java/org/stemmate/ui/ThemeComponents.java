package org.stemmate.ui;

import javax.swing.AbstractButton;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class ThemeComponents {
    private ThemeComponents() {
    }

    static MouseAdapter hover(AbstractButton button, Color normal, Color hover, Color foreground) {
        return new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                button.setBackground(hover);
                button.setForeground(foreground);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.setBackground(normal);
                button.setForeground(foreground);
            }
        };
    }
}
