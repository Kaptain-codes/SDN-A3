package org.stemmate.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.BorderLayout;

import static org.junit.jupiter.api.Assertions.assertSame;

class DesktopShellFrameLayoutTest {
    @Test
    void catalogueKeepsTitleSearchHeaderAndFiltersVisibleTogether() {
        JPanel header = new JPanel();
        JPanel filters = new JPanel();

        JPanel top = DesktopShellFrame.catalogueTop(header, filters);
        BorderLayout layout = (BorderLayout) top.getLayout();

        assertSame(header, layout.getLayoutComponent(BorderLayout.NORTH));
        assertSame(filters, layout.getLayoutComponent(BorderLayout.CENTER));
    }
}
