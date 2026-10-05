package org.stemmate.ui;

import org.stemmate.model.SyncStatus;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Text status bar; status meaning is never conveyed by colour alone.
 */
public final class SyncStatusBar extends JPanel {
    private final JLabel message = new JLabel("Saved on this device. It will upload when you're back online.");

    public SyncStatusBar() {
        super(new BorderLayout());
        setBackground(AppTheme.BRAND_50);
        setBorder(new RoundedBorder(AppTheme.BRAND_100, 12, 1));
        getAccessibleContext().setAccessibleName("Synchronization status");
        message.getAccessibleContext().setAccessibleName("Synchronization status message");
        message.setFont(AppTheme.META);
        message.setForeground(AppTheme.BRAND_700);
        add(message, BorderLayout.CENTER);
    }

    public void showStatus(SyncStatus status) {
        if (status == null) {
            message.setText("Synchronization status is unavailable.");
            setBackground(AppTheme.WARM_100);
            setBorder(new RoundedBorder(AppTheme.BORDER, 12, 1));
            message.setForeground(AppTheme.TEXT_SECONDARY);
            return;
        }
        switch (status) {
            case WAITING_TO_SYNC -> {
                setBackground(AppTheme.AMBER_50);
                setBorder(new RoundedBorder(AppTheme.AMBER_500, 12, 1));
                message.setForeground(AppTheme.AMBER_700);
            }
            case SYNCING -> {
                setBackground(AppTheme.BRAND_50);
                setBorder(new RoundedBorder(AppTheme.BRAND_100, 12, 1));
                message.setForeground(AppTheme.BRAND_700);
            }
            case SYNCED -> {
                setBackground(AppTheme.EMERALD_50);
                setBorder(new RoundedBorder(AppTheme.EMERALD_500, 12, 1));
                message.setForeground(AppTheme.EMERALD_700);
            }
            case FAILED_RETRY -> {
                setBackground(AppTheme.RED_50);
                setBorder(new RoundedBorder(AppTheme.RED_500, 12, 1));
                message.setForeground(AppTheme.RED_700);
            }
        }
        message.setText(switch (status) {
            case WAITING_TO_SYNC -> "Saved on this device. It will upload when you're back online.";
            case SYNCING -> "Syncing...";
            case SYNCED -> "All changes synced.";
            case FAILED_RETRY -> "Sync failed. Retry when online.";
        });
    }
}
