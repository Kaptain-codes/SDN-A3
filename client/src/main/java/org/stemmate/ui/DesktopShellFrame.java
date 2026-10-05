package org.stemmate.ui;

import org.stemmate.DesktopController;
import org.stemmate.model.Activity;
import org.stemmate.model.EquipmentKitRecord;
import org.stemmate.model.InclusionPrompt;
import org.stemmate.model.Material;
import org.stemmate.model.PlanStep;
import org.stemmate.model.SafetyNote;
import org.stemmate.model.SessionPlan;
import org.stemmate.model.SyncStatus;
import org.stemmate.repository.ActivityCriteria;
import org.stemmate.repository.exception.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public final class DesktopShellFrame extends JFrame {
    private static final String CARD_CATALOG = "catalog";
    private static final String CARD_SAVED = "saved";
    private static final String CARD_PLANNER = "planner";
    private static final String CARD_CUSTODIAN = "custodian";

    private final DesktopController controller;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);
    private final JLabel savedBadge = pillLabel("0", AppTheme.WARM_100, AppTheme.TEXT_SECONDARY);
    private final JLabel pendingBadge = pillLabel("0", AppTheme.AMBER_50, AppTheme.AMBER_700);
    private final JPanel pendingPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    private final JToggleButton onlineToggle = new JToggleButton("● Online");
    private final JToggleButton facilitatorToggle = new JToggleButton("Facilitator");
    private final JToggleButton custodianToggle = new JToggleButton("Custodian");
    private final SyncStatusBar syncStatusBar = new SyncStatusBar();
    private final CatalogPanel catalogPanel;
    private final CatalogPanel savedPanel;
    private final PlannerPanel plannerPanel;
    private final CustodyPanel custodyPanel;
    private final JLabel roleStatus = new JLabel();

    public DesktopShellFrame(DesktopController controller) {
        super("STEMCraft - Facilitator Studio");
        this.controller = Objects.requireNonNull(controller, "controller");
        this.catalogPanel = new CatalogPanel(false);
        this.savedPanel = new CatalogPanel(true);
        this.plannerPanel = new PlannerPanel();
        this.custodyPanel = new CustodyPanel();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 780));
        setSize(1360, 880);
        setLocationByPlatform(true);
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        add(buildContent(), BorderLayout.CENTER);

        content.add(catalogPanel, CARD_CATALOG);
        content.add(savedPanel, CARD_SAVED);
        content.add(plannerPanel, CARD_PLANNER);
        content.add(custodyPanel, CARD_CUSTODIAN);

        controller.addChangeListener(this::refreshAll);
        plannerPanel.loadDraft(controller.getCurrentDraft());
        refreshAll();
    }

    public void showCatalog() {
        cardLayout.show(content, CARD_CATALOG);
    }

    public void showSaved() {
        cardLayout.show(content, CARD_SAVED);
    }

    public void showPlanner() {
        cardLayout.show(content, CARD_PLANNER);
    }

    public void showCustodian() {
        cardLayout.show(content, CARD_CUSTODIAN);
    }

    public void refreshAll() {
        pendingBadge.setText(Integer.toString(controller.pendingCount()));
        pendingPanel.setVisible(controller.pendingCount() > 0);
        savedBadge.setText(Integer.toString(controller.findSavedActivities(new ActivityCriteria(null, null, null, List.of())).size()));
        onlineToggle.setSelected(controller.isOnline());
        onlineToggle.setText(controller.isOnline() ? "● Online" : "● Offline");
        onlineToggle.setForeground(controller.isOnline() ? AppTheme.EMERALD_700 : AppTheme.AMBER_700);
        syncStatusBar.showStatus(controller.getLastSyncStatus());
        facilitatorToggle.setSelected(controller.getRole() == DesktopController.Role.FACILITATOR);
        custodianToggle.setSelected(controller.getRole() == DesktopController.Role.CUSTODIAN);
        roleStatus.setText(controller.getRole() == DesktopController.Role.FACILITATOR ? "Facilitator" : "Custodian");
        catalogPanel.refresh();
        savedPanel.refresh();
        plannerPanel.refreshPlanList();
        custodyPanel.refresh();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 8));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER),
                BorderFactory.createEmptyBorder(14, 22, 10, 22)));
        header.setBackground(AppTheme.SURFACE);

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);
        JLabel mark = new JLabel("S", JLabel.CENTER);
        mark.setOpaque(true);
        mark.setBackground(AppTheme.BRAND_600);
        mark.setForeground(Color.WHITE);
        mark.setPreferredSize(new Dimension(32, 32));
        mark.setFont(AppTheme.UI_BOLD.deriveFont(14f));
        brand.add(mark);
        JLabel title = new JLabel("STEMCraft");
        title.setFont(AppTheme.UI_BOLD.deriveFont(16f));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        brand.add(title);
        header.add(brand, BorderLayout.WEST);

        JPanel center = new JPanel(new GridLayout(2, 1, 0, 6));
        center.setOpaque(false);
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        topRow.setOpaque(false);
        styleHeaderButton(onlineToggle);
        onlineToggle.addActionListener(e -> controller.setOnline(onlineToggle.isSelected()));
        topRow.add(onlineToggle);
        pendingPanel.setOpaque(false);
        pendingPanel.add(pendingBadge);
        JButton syncButton = new JButton("Sync");
        AppTheme.stylePrimaryButton(syncButton);
        syncButton.addActionListener(e -> controller.syncNow());
        pendingPanel.add(syncButton);
        topRow.add(pendingPanel);
        center.add(topRow);
        center.add(syncStatusBar);
        header.add(center, BorderLayout.CENTER);

        JPanel rolePanel = new JPanel(new BorderLayout(8, 0));
        rolePanel.setOpaque(false);
        JPanel roleButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        roleButtons.setOpaque(false);
        ButtonGroup roleGroup = new ButtonGroup();
        roleGroup.add(facilitatorToggle);
        roleGroup.add(custodianToggle);
        AppTheme.styleSecondaryButton(facilitatorToggle);
        AppTheme.styleSecondaryButton(custodianToggle);
        facilitatorToggle.addActionListener(e -> {
            controller.setRole(DesktopController.Role.FACILITATOR);
            showPlanner();
        });
        custodianToggle.addActionListener(e -> {
            controller.setRole(DesktopController.Role.CUSTODIAN);
            showCustodian();
        });
        roleButtons.add(facilitatorToggle);
        roleButtons.add(custodianToggle);
        roleStatus.setFont(AppTheme.META_BOLD);
        roleStatus.setForeground(AppTheme.TEXT_SECONDARY);
        rolePanel.add(roleButtons, BorderLayout.NORTH);
        rolePanel.add(roleStatus, BorderLayout.SOUTH);
        header.add(rolePanel, BorderLayout.EAST);
        return header;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBackground(AppTheme.WARM_50);
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, AppTheme.BORDER),
                BorderFactory.createEmptyBorder(20, 16, 16, 16)));
        sidebar.setLayout(new BorderLayout());

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new javax.swing.BoxLayout(nav, javax.swing.BoxLayout.Y_AXIS));
        nav.add(navButton("Activity Catalog", e -> showCatalog()));
        nav.add(space(8));
        JPanel savedRow = new JPanel(new BorderLayout(8, 0));
        savedRow.setOpaque(false);
        savedRow.add(navButton("Saved Offline", e -> showSaved()), BorderLayout.CENTER);
        savedRow.add(savedBadge, BorderLayout.EAST);
        nav.add(savedRow);
        nav.add(space(8));
        nav.add(navButton("Session Planner", e -> showPlanner()));
        nav.add(space(8));
        nav.add(navButton("Equipment Custody", e -> showCustodian()));

        JPanel footer = cardPanel();
        footer.setLayout(new BorderLayout(0, 8));
        JLabel label = new JLabel("<html><b>Offline-ready</b><br/>Local catalogue, drafts, kits, and sync logs are available without a network.</html>");
        label.setFont(AppTheme.UI_SMALL);
        label.setForeground(AppTheme.TEXT_SECONDARY);
        footer.add(label, BorderLayout.CENTER);

        sidebar.add(nav, BorderLayout.NORTH);
        sidebar.add(footer, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel buildContent() {
        content.setBackground(AppTheme.CANVAS);
        return content;
    }

    private void styleHeaderButton(JToggleButton toggle) {
        AppTheme.styleSecondaryButton(toggle);
        toggle.setFocusPainted(true);
    }

    private JButton navButton(String text, ActionListener listener) {
        return navButton(text, listener, null);
    }

    private JButton navButton(String text, ActionListener listener, JLabel badge) {
        JButton button = new JButton(text);
        AppTheme.styleSecondaryButton(button);
        button.setHorizontalAlignment(JButton.LEFT);
        button.addActionListener(listener);
        if (badge != null) {
            JPanel wrapper = new JPanel(new BorderLayout(8, 0));
            wrapper.setOpaque(false);
            wrapper.add(button, BorderLayout.CENTER);
            wrapper.add(badge, BorderLayout.EAST);
            button.putClientProperty("wrapper", wrapper);
        }
        return button;
    }

    private static JPanel space(int height) {
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        spacer.setPreferredSize(new Dimension(1, height));
        return spacer;
    }

    private static JLabel pillLabel(String text, Color background, Color foreground) {
        JLabel label = new JLabel(text, JLabel.CENTER);
        label.setOpaque(true);
        label.setBackground(background);
        label.setForeground(foreground);
        label.setFont(AppTheme.META_BOLD);
        label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        return label;
    }

    private static JPanel cardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(AppTheme.SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppTheme.BORDER, 16, 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        return panel;
    }

    private static JPanel pagePanel() {
        JPanel panel = new JPanel();
        panel.setBackground(AppTheme.CANVAS);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        panel.setLayout(new BorderLayout(0, 16));
        return panel;
    }

    private static JLabel screenTitle(String title, String subtitle) {
        JLabel label = new JLabel("<html><h1 style='margin:0'>" + title + "</h1><div style='color:#73736C'>" + subtitle + "</div></html>");
        label.setFont(AppTheme.HEADING);
        label.setForeground(AppTheme.TEXT_PRIMARY);
        return label;
    }

    private static JPanel emptyState(String title, String subtitle) {
        JPanel panel = cardPanel();
        panel.setLayout(new GridLayout(2, 1, 0, 6));
        JLabel heading = new JLabel(title, JLabel.CENTER);
        heading.setFont(AppTheme.UI_BOLD.deriveFont(14f));
        heading.setForeground(AppTheme.TEXT_PRIMARY);
        JLabel body = new JLabel(subtitle, JLabel.CENTER);
        body.setFont(AppTheme.UI_SMALL);
        body.setForeground(AppTheme.TEXT_SECONDARY);
        panel.add(heading);
        panel.add(body);
        return panel;
    }

    private final class CatalogPanel extends JPanel {
        private final boolean savedOnly;
        private final JTextField search = new JTextField();
        private final JComboBox<String> level = new JComboBox<>(new String[]{"All Levels", "Primary", "Middle School", "High School"});
        private final JComboBox<String> topic = new JComboBox<>(new String[]{"All Topics", "Physics", "Environmental", "Robotics", "Chemistry"});
        private final JComboBox<String> duration = new JComboBox<>(new String[]{"Any Duration", "Under 30 mins", "Under 45 mins", "Under 60 mins"});
        private final JComboBox<String> materials = new JComboBox<>(new String[]{"Any Materials", "Cardboard", "Basic Electronics", "Water", "Microcontrollers", "Soil Samples", "pH Kits"});
        private final JPanel grid = new JPanel(new GridLayout(0, 3, 16, 16));

        private CatalogPanel(boolean savedOnly) {
            super(new BorderLayout(0, 16));
            this.savedOnly = savedOnly;
            setBackground(AppTheme.CANVAS);
            build();
        }

        private void build() {
            JPanel header = cardPanel();
            header.setLayout(new BorderLayout(16, 0));
            JPanel titleBlock = new JPanel();
            titleBlock.setOpaque(false);
            titleBlock.setLayout(new javax.swing.BoxLayout(titleBlock, javax.swing.BoxLayout.Y_AXIS));
            titleBlock.add(screenTitle(savedOnly ? "Offline Activity Vault" : "STEM Activity Catalog",
                    savedOnly ? "Activities stored locally on this device for offline facilitation." :
                            "Explore activities aligned with your curriculum with step-by-step guidance."));
            header.add(titleBlock, BorderLayout.WEST);
            if (!savedOnly) {
                JPanel searchWrap = new JPanel(new BorderLayout());
                searchWrap.setOpaque(false);
                search.setPreferredSize(new Dimension(280, 32));
                search.setToolTipText("Search topics, keywords, or materials");
                searchWrap.add(search, BorderLayout.CENTER);
                header.add(searchWrap, BorderLayout.EAST);
            } else {
                JLabel badge = pillLabel("Storage ready", AppTheme.EMERALD_50, AppTheme.EMERALD_700);
                header.add(badge, BorderLayout.EAST);
            }
            add(header, BorderLayout.NORTH);

            JPanel filterCard = cardPanel();
            filterCard.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(0, 0, 0, 12);
            c.gridy = 0;
            addFilter(filterCard, c, "Level", level);
            addFilter(filterCard, c, "Topic", topic);
            addFilter(filterCard, c, "Duration", duration);
            addFilter(filterCard, c, "Materials", materials);
            JButton reset = new JButton("Reset all");
            AppTheme.styleSecondaryButton(reset);
            reset.addActionListener(e -> {
                search.setText("");
                level.setSelectedIndex(0);
                topic.setSelectedIndex(0);
                duration.setSelectedIndex(0);
                materials.setSelectedIndex(0);
                refresh();
            });
            c.gridy = 1;
            c.gridx = 4;
            filterCard.add(reset, c);
            add(filterCard, BorderLayout.NORTH);

            grid.setOpaque(false);
            JScrollPane scrollPane = new JScrollPane(grid);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            add(scrollPane, BorderLayout.CENTER);

            DocumentListener listener = new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { refresh(); }
                @Override public void removeUpdate(DocumentEvent e) { refresh(); }
                @Override public void changedUpdate(DocumentEvent e) { refresh(); }
            };
            search.getDocument().addDocumentListener(listener);
            level.addActionListener(e -> refresh());
            topic.addActionListener(e -> refresh());
            duration.addActionListener(e -> refresh());
            materials.addActionListener(e -> refresh());
        }

        private void addFilter(JPanel panel, GridBagConstraints c, String label, JComboBox<String> combo) {
            JPanel wrapper = new JPanel();
            wrapper.setOpaque(false);
            wrapper.setLayout(new javax.swing.BoxLayout(wrapper, javax.swing.BoxLayout.Y_AXIS));
            JLabel filterLabel = new JLabel(label);
            filterLabel.setFont(AppTheme.UI_SMALL);
            filterLabel.setForeground(AppTheme.TEXT_PRIMARY);
            wrapper.add(filterLabel);
            combo.setFont(AppTheme.UI_SMALL);
            wrapper.add(combo);
            c.gridx++;
            panel.add(wrapper, c);
        }

        void refresh() {
            grid.removeAll();
            List<Activity> activities = savedOnly
                    ? controller.findSavedActivities(filterCriteria())
                    : controller.findActivities(filterCriteria());
            String query = search.getText().trim().toLowerCase();
            List<Activity> filtered = new ArrayList<>();
            for (Activity activity : activities) {
                if (query.isBlank()
                        || activity.topic().toLowerCase().contains(query)
                        || activity.level().toLowerCase().contains(query)
                        || activity.materials().stream().anyMatch(material -> material.name().toLowerCase().contains(query))) {
                    filtered.add(activity);
                }
            }
            if (filtered.isEmpty()) {
                grid.setLayout(new GridLayout(1, 1));
                grid.add(emptyState(
                        savedOnly ? "No offline activities saved" : "No activities match your filters",
                        savedOnly ? "Save an activity from the catalog to see it here." : "Try adjusting the filters or search terms."));
            } else {
                grid.setLayout(new GridLayout(0, 3, 16, 16));
                for (Activity activity : filtered) {
                    grid.add(new ActivityCardPanel(activity, !savedOnly));
                }
            }
            grid.revalidate();
            grid.repaint();
        }

        private ActivityCriteria filterCriteria() {
            String levelValue = level.getSelectedIndex() == 0 ? null : (String) level.getSelectedItem();
            String topicValue = topic.getSelectedIndex() == 0 ? null : (String) topic.getSelectedItem();
            Integer durationValue = switch (duration.getSelectedIndex()) {
                case 1 -> 30;
                case 2 -> 45;
                case 3 -> 60;
                default -> null;
            };
            List<String> materialValues = materials.getSelectedIndex() == 0
                    ? List.of()
                    : List.of((String) Objects.requireNonNull(materials.getSelectedItem()));
            return new ActivityCriteria(levelValue, topicValue, durationValue, materialValues);
        }
    }

    private final class ActivityCardPanel extends JPanel {
        private ActivityCardPanel(Activity activity, boolean allowPlanAction) {
            setLayout(new BorderLayout(0, 10));
            setBackground(AppTheme.SURFACE);
            setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(AppTheme.BORDER, 16, 1),
                    BorderFactory.createEmptyBorder(16, 16, 16, 16)));

            JLabel meta = new JLabel(activity.topic() + "  •  " + activity.level() + "  •  " + activity.durationMinutes() + " min");
            meta.setFont(AppTheme.META_BOLD);
            meta.setForeground(AppTheme.TEXT_SECONDARY);
            add(meta, BorderLayout.NORTH);

            JPanel body = new JPanel();
            body.setOpaque(false);
            body.setLayout(new javax.swing.BoxLayout(body, javax.swing.BoxLayout.Y_AXIS));
            JLabel title = new JLabel(activity.title());
            title.setFont(AppTheme.UI_BOLD.deriveFont(15f));
            title.setForeground(AppTheme.TEXT_PRIMARY);
            body.add(title);
            body.add(space(6));
            JLabel description = new JLabel("<html><body style='width:210px'>" + activity.description() + "</body></html>");
            description.setFont(AppTheme.UI_SMALL);
            description.setForeground(AppTheme.TEXT_SECONDARY);
            body.add(description);
            body.add(space(10));
            body.add(tagLine(activity));
            add(body, BorderLayout.CENTER);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            actions.setOpaque(false);
            JButton offline = new JButton(controller.isSavedOffline(activity.activityId()) ? "Remove" : "Save Offline");
            AppTheme.styleSecondaryButton(offline);
            offline.addActionListener(e -> {
                controller.toggleOffline(activity);
                refreshAll();
            });
            actions.add(offline);
            if (allowPlanAction) {
                JButton create = new JButton("Create Plan");
                AppTheme.stylePrimaryButton(create);
                create.addActionListener(e -> {
                    plannerPanel.loadDraft(controller.createDraftFromActivity(activity));
                    showPlanner();
                });
                actions.add(create);
            }
            add(actions, BorderLayout.SOUTH);
        }
    }

    private JPanel tagLine(Activity activity) {
        JPanel tags = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        tags.setOpaque(false);
        for (Material material : activity.materials()) {
            JLabel tag = pillLabel(material.name(), AppTheme.BRAND_50, AppTheme.BRAND_700);
            tags.add(tag);
        }
        return tags;
    }

    private final class PlannerPanel extends JPanel {
        private final JTextField titleField = new JTextField();
        private final JComboBox<ActivityChoice> activityCombo = new JComboBox<>();
        private final JTextField targetGroupField = new JTextField();
        private final JPanel stepsContainer = new JPanel();
        private final JTextArea materialsArea = new JTextArea(3, 20);
        private final JTextArea safetyArea = new JTextArea(3, 20);
        private final JTextArea inclusionArea = new JTextArea(3, 20);
        private final JLabel statusBadge = pillLabel("Saved", AppTheme.BRAND_50, AppTheme.BRAND_700);
        private final JLabel errorLabel = new JLabel(" ");
        private final Timer autosaveTimer;
        private boolean loading;
        private SessionPlan draft;
        private final DefaultListModel<SessionPlan> savedPlansModel = new DefaultListModel<>();
        private final JList<SessionPlan> savedPlansList = new JList<>(savedPlansModel);
        private final JLabel savedPlansCount = pillLabel("0", AppTheme.WARM_100, AppTheme.TEXT_SECONDARY);

        private PlannerPanel() {
            super(new BorderLayout(16, 16));
            setBackground(AppTheme.CANVAS);
            autosaveTimer = new Timer(700, e -> autosaveDraft());
            autosaveTimer.setRepeats(false);
            build();
        }

        private void build() {
            JPanel header = cardPanel();
            header.setLayout(new BorderLayout(16, 0));
            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
            text.add(screenTitle("Session Plan Studio", "Design structured facilitation plans including timings, safety, and inclusion strategies."));
            header.add(text, BorderLayout.WEST);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actions.setOpaque(false);
            actions.add(statusBadge);
            JButton duplicate = new JButton("Duplicate draft");
            AppTheme.styleSecondaryButton(duplicate);
            duplicate.addActionListener(e -> duplicateDraft());
            actions.add(duplicate);
            JButton save = new JButton("Save & finalize");
            AppTheme.stylePrimaryButton(save);
            save.addActionListener(e -> finalizeDraft());
            actions.add(save);
            header.add(actions, BorderLayout.EAST);
            add(header, BorderLayout.NORTH);

            JPanel main = new JPanel(new BorderLayout(16, 16));
            main.setOpaque(false);

            JPanel form = cardPanel();
            form.setLayout(new BorderLayout(0, 16));
            form.add(buildMainForm(), BorderLayout.CENTER);
            errorLabel.setForeground(AppTheme.RED_700);
            errorLabel.setFont(AppTheme.UI_SMALL);
            form.add(errorLabel, BorderLayout.SOUTH);
            main.add(form, BorderLayout.CENTER);

            JPanel savedList = cardPanel();
            savedList.setLayout(new BorderLayout(0, 10));
            JPanel savedHeader = new JPanel(new BorderLayout());
            savedHeader.setOpaque(false);
            JLabel label = new JLabel("Saved session plans");
            label.setFont(AppTheme.UI_BOLD);
            label.setForeground(AppTheme.TEXT_PRIMARY);
            savedHeader.add(label, BorderLayout.WEST);
            savedHeader.add(savedPlansCount, BorderLayout.EAST);
            savedList.add(savedHeader, BorderLayout.NORTH);
            savedPlansList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            savedPlansList.setCellRenderer(new SavedPlanRenderer());
            savedPlansList.setVisibleRowCount(10);
            savedPlansList.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        SessionPlan selected = savedPlansList.getSelectedValue();
                        if (selected != null) {
                            loadDraft(controller.loadPlanAsDraft(selected.planId()));
                            showPlanner();
                        }
                    }
                }
            });
            savedList.add(new JScrollPane(savedPlansList), BorderLayout.CENTER);
            main.add(savedList, BorderLayout.EAST);

            add(main, BorderLayout.CENTER);
        }

        private JPanel buildMainForm() {
            JPanel form = new JPanel(new GridBagLayout());
            form.setOpaque(false);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 0, 12, 0);

            form.add(labeledField("Session plan title", titleField), c);
            c.gridy++;
            form.add(labeledField("Associated activity", activityCombo), c);
            c.gridy++;
            form.add(labeledField("Target group", targetGroupField), c);
            c.gridy++;

            JPanel stepsHeader = new JPanel(new BorderLayout());
            stepsHeader.setOpaque(false);
            JLabel stepsLabel = new JLabel("Activity steps & timing breakdown");
            stepsLabel.setFont(AppTheme.UI_BOLD);
            stepsLabel.setForeground(AppTheme.TEXT_PRIMARY);
            JButton addStep = new JButton("Add step");
            AppTheme.styleSecondaryButton(addStep);
            addStep.addActionListener(e -> {
                stepsContainer.add(new StepRowPanel(new PlanStep("", 0, "")));
                stepsContainer.revalidate();
                stepsContainer.repaint();
                scheduleAutosave();
            });
            stepsHeader.add(stepsLabel, BorderLayout.WEST);
            stepsHeader.add(addStep, BorderLayout.EAST);
            form.add(stepsHeader, c);
            c.gridy++;

            stepsContainer.setLayout(new javax.swing.BoxLayout(stepsContainer, javax.swing.BoxLayout.Y_AXIS));
            form.add(stepsContainer, c);
            c.gridy++;
            form.add(labeledArea("Required materials & kits", materialsArea, null), c);
            c.gridy++;
            form.add(labeledArea("Safety notes & risk controls", safetyArea, AppTheme.RED_50), c);
            c.gridy++;
            form.add(labeledArea("Inclusion & universal design prompts", inclusionArea, AppTheme.INDIGO_50), c);

            titleField.getDocument().addDocumentListener(documentListener());
            targetGroupField.getDocument().addDocumentListener(documentListener());
            materialsArea.getDocument().addDocumentListener(documentListener());
            safetyArea.getDocument().addDocumentListener(documentListener());
            inclusionArea.getDocument().addDocumentListener(documentListener());
            activityCombo.addActionListener(e -> {
                if (!loading && activityCombo.getSelectedItem() instanceof ActivityChoice choice) {
                    loadDraft(controller.createDraftFromActivity(choice.activity()));
                }
            });
            return form;
        }

        private JComponent labeledField(String label, JComponent field) {
            JPanel wrapper = new JPanel();
            wrapper.setOpaque(false);
            wrapper.setLayout(new javax.swing.BoxLayout(wrapper, javax.swing.BoxLayout.Y_AXIS));
            JLabel title = new JLabel(label);
            title.setFont(AppTheme.UI_SMALL);
            title.setForeground(AppTheme.TEXT_PRIMARY);
            wrapper.add(title);
            field.setFont(AppTheme.UI_SMALL);
            wrapper.add(field);
            return wrapper;
        }

        private JComponent labeledArea(String label, JTextArea area, Color tint) {
            JPanel wrapper = new JPanel();
            wrapper.setOpaque(false);
            wrapper.setLayout(new javax.swing.BoxLayout(wrapper, javax.swing.BoxLayout.Y_AXIS));
            JLabel title = new JLabel(label);
            title.setFont(AppTheme.UI_SMALL);
            title.setForeground(AppTheme.TEXT_PRIMARY);
            wrapper.add(title);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setFont(AppTheme.UI_SMALL);
            area.setRows(3);
            area.setBorder(new RoundedBorder(AppTheme.BORDER, 12, 1));
            if (tint != null) {
                area.setBackground(tint);
            }
            wrapper.add(new JScrollPane(area));
            return wrapper;
        }

        private DocumentListener documentListener() {
            return new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { scheduleAutosave(); }
                @Override public void removeUpdate(DocumentEvent e) { scheduleAutosave(); }
                @Override public void changedUpdate(DocumentEvent e) { scheduleAutosave(); }
            };
        }

        private void scheduleAutosave() {
            if (!loading) {
                statusBadge.setText("Saving...");
                statusBadge.setBackground(AppTheme.AMBER_50);
                statusBadge.setForeground(AppTheme.AMBER_700);
                autosaveTimer.restart();
            }
        }

        private void autosaveDraft() {
            if (loading) {
                return;
            }
            try {
                controller.autosaveDraft(buildDraft(false));
                statusBadge.setText("Saved");
                statusBadge.setBackground(AppTheme.BRAND_50);
                statusBadge.setForeground(AppTheme.BRAND_700);
                errorLabel.setText(" ");
                refreshAll();
            } catch (RuntimeException exception) {
                errorLabel.setText(exception.getMessage());
            }
        }

        private void finalizeDraft() {
            try {
                controller.finalizePlan(buildDraft(true));
                statusBadge.setText("Saved");
                statusBadge.setBackground(AppTheme.EMERALD_50);
                statusBadge.setForeground(AppTheme.EMERALD_700);
                errorLabel.setText("Plan saved locally and queued for sync.");
                refreshAll();
            } catch (ValidationException exception) {
                errorLabel.setText(exception.getMessage());
            }
        }

        private void duplicateDraft() {
            SessionPlan duplicated = controller.duplicatePlan(draft.planId());
            if (duplicated != null) {
                loadDraft(duplicated);
                showPlanner();
            }
        }

        private SessionPlan buildDraft(boolean forFinalize) {
            if (activityCombo.getSelectedItem() == null) {
                throw new ValidationException("Select an activity before saving.");
            }
            ActivityChoice selected = (ActivityChoice) activityCombo.getSelectedItem();
            List<PlanStep> steps = new ArrayList<>();
            List<Integer> timings = new ArrayList<>();
            List<Material> materials = parseLines(materialsArea.getText()).stream().map(text -> new Material(text, "as required")).toList();
            List<SafetyNote> safety = parseLines(safetyArea.getText()).stream().map(SafetyNote::new).toList();
            List<InclusionPrompt> prompts = parseLines(inclusionArea.getText()).stream().map(InclusionPrompt::new).toList();
            for (Component component : stepsContainer.getComponents()) {
                if (component instanceof StepRowPanel row) {
                    steps.add(row.step());
                    timings.add(row.duration());
                }
            }
            if (!forFinalize) {
                return new SessionPlan(
                        draft.planId(),
                        selected.activity().activityId(),
                        titleField.getText().trim(),
                        targetGroupField.getText().trim(),
                        steps,
                        timings,
                        materials,
                        safety,
                        prompts,
                        SyncStatus.WAITING_TO_SYNC,
                        draft.createdAt(),
                        Instant.now());
            }
            return new SessionPlan(
                    draft.planId(),
                    selected.activity().activityId(),
                    titleField.getText().trim(),
                    targetGroupField.getText().trim(),
                    steps,
                    timings,
                    materials,
                    safety,
                    prompts,
                    SyncStatus.WAITING_TO_SYNC,
                    draft.createdAt(),
                    Instant.now());
        }

        private List<String> parseLines(String text) {
            return text == null ? List.of() : text.lines().map(String::trim).filter(line -> !line.isBlank()).toList();
        }

        void loadDraft(SessionPlan draft) {
            this.draft = Objects.requireNonNull(draft, "draft");
            loading = true;
            try {
                titleField.setText(nullToEmpty(draft.title()));
                targetGroupField.setText(nullToEmpty(draft.targetGroup()));
                materialsArea.setText(joinMaterials(draft.materials()));
                safetyArea.setText(joinLines(draft.safetyNotes().stream().map(SafetyNote::text).toList()));
                inclusionArea.setText(joinLines(draft.inclusionPrompts().stream().map(InclusionPrompt::prompt).toList()));
                stepsContainer.removeAll();
                for (PlanStep step : draft.steps()) {
                    stepsContainer.add(new StepRowPanel(step));
                }
                rebuildActivityChoices(draft.activityId());
                statusBadge.setText("Saved");
                statusBadge.setBackground(AppTheme.BRAND_50);
                statusBadge.setForeground(AppTheme.BRAND_700);
                errorLabel.setText(" ");
            } finally {
                loading = false;
            }
            stepsContainer.revalidate();
            stepsContainer.repaint();
        }

        private void rebuildActivityChoices(UUID selectedId) {
            activityCombo.removeAllItems();
            for (Activity activity : controller.findActivities(new ActivityCriteria(null, null, null, List.of()))) {
                activityCombo.addItem(new ActivityChoice(activity));
            }
            for (int i = 0; i < activityCombo.getItemCount(); i++) {
                ActivityChoice choice = activityCombo.getItemAt(i);
                if (choice.activity().activityId().equals(selectedId)) {
                    activityCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        private String joinLines(List<String> values) {
            return String.join(System.lineSeparator(), values);
        }

        private String joinMaterials(List<Material> values) {
            return values.stream().map(Material::name).reduce((left, right) -> left + System.lineSeparator() + right).orElse("");
        }

        void refreshPlanList() {
            savedPlansModel.clear();
            for (SessionPlan plan : controller.savedPlans()) {
                savedPlansModel.addElement(plan);
            }
            savedPlansCount.setText(Integer.toString(savedPlansModel.size()));
        }

        private String nullToEmpty(String value) {
            return value == null ? "" : value;
        }

        private final class SavedPlanRenderer extends JPanel implements ListCellRenderer<SessionPlan> {
            private final JLabel title = new JLabel();
            private final JLabel meta = new JLabel();

            private SavedPlanRenderer() {
                setLayout(new BorderLayout(0, 4));
                setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(AppTheme.BORDER, 12, 1),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)));
                add(title, BorderLayout.NORTH);
                add(meta, BorderLayout.SOUTH);
            }

            @Override
            public Component getListCellRendererComponent(JList<? extends SessionPlan> list, SessionPlan value, int index, boolean isSelected, boolean cellHasFocus) {
                setBackground(isSelected ? AppTheme.BRAND_50 : AppTheme.SURFACE);
                title.setText(value.title());
                title.setFont(AppTheme.UI_BOLD);
                title.setForeground(AppTheme.TEXT_PRIMARY);
                meta.setText(value.targetGroup() + " • " + value.syncStatus());
                meta.setFont(AppTheme.META);
                meta.setForeground(AppTheme.TEXT_SECONDARY);
                return this;
            }
        }
    }

    private final class StepRowPanel extends JPanel {
        private final JTextField nameField = new JTextField();
        private final JSpinner minutesField = new JSpinner(new javax.swing.SpinnerNumberModel(0, 0, 480, 5));
        private final JTextField descriptionField = new JTextField();

        private StepRowPanel(PlanStep step) {
            setLayout(new GridBagLayout());
            setOpaque(false);
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 0, 8, 8);
            c.weightx = 1;
            nameField.setText(step.instruction());
            minutesField.setValue(step.durationMinutes());
            descriptionField.setText(step.description());
            add(column("Step name", nameField), cAt(c, 0));
            add(column("Timing (min)", minutesField), cAt(c, 1));
            add(column("Description", descriptionField), cAt(c, 2));
            JButton remove = new JButton("Remove");
            AppTheme.styleSecondaryButton(remove);
            remove.addActionListener(e -> {
                java.awt.Container parent = getParent();
                if (parent != null) {
                    parent.remove(this);
                    parent.revalidate();
                    parent.repaint();
                    SwingUtilities.invokeLater(plannerPanel::scheduleAutosave);
                }
            });
            c.gridx = 3;
            c.weightx = 0;
            add(remove, c);
            nameField.getDocument().addDocumentListener(plannerPanel.documentListener());
            descriptionField.getDocument().addDocumentListener(plannerPanel.documentListener());
            minutesField.addChangeListener(e -> plannerPanel.scheduleAutosave());
        }

        private GridBagConstraints cAt(GridBagConstraints base, int x) {
            GridBagConstraints c = (GridBagConstraints) base.clone();
            c.gridx = x;
            c.gridy = 0;
            return c;
        }

        private JPanel column(String label, JComponent component) {
            JPanel panel = new JPanel();
            panel.setOpaque(false);
            panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
            JLabel fieldLabel = new JLabel(label);
            fieldLabel.setFont(AppTheme.UI_SMALL);
            fieldLabel.setForeground(AppTheme.TEXT_PRIMARY);
            panel.add(fieldLabel);
            panel.add(component);
            return panel;
        }

        private PlanStep step() {
            return new PlanStep(nameField.getText().trim(), ((Number) minutesField.getValue()).intValue(), descriptionField.getText().trim());
        }

        private int duration() {
            return ((Number) minutesField.getValue()).intValue();
        }
    }

    private final class ActivityChoice {
        private final Activity activity;

        private ActivityChoice(Activity activity) {
            this.activity = activity;
        }

        private Activity activity() {
            return activity;
        }

        @Override
        public String toString() {
            return activity.topic() + " • " + activity.level();
        }
    }

    private final class CustodyPanel extends JPanel {
        private final KitTableModel tableModel = new KitTableModel();
        private final JTable table = new JTable(tableModel);

        private CustodyPanel() {
            super(new BorderLayout(0, 16));
            setBackground(AppTheme.CANVAS);
            build();
        }

        private void build() {
            JPanel header = cardPanel();
            header.setLayout(new BorderLayout());
            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
            text.add(screenTitle("Equipment Kit Custody", "Track intended checkout dates, responsible facilitators, and kit return status."));
            header.add(text, BorderLayout.WEST);
            JButton log = new JButton("Log kit checkout");
            AppTheme.stylePrimaryButton(log);
            log.addActionListener(e -> openKitDialog(null));
            header.add(log, BorderLayout.EAST);
            add(header, BorderLayout.NORTH);

            table.setRowHeight(36);
            table.setFillsViewportHeight(true);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.getColumnModel().getColumn(4).setCellRenderer(new ActionRenderer());
            table.getColumnModel().getColumn(4).setCellEditor(new ActionEditor());
            add(new JScrollPane(table), BorderLayout.CENTER);
        }

        void refresh() {
            tableModel.setRecords(controller.kitRecords());
        }

        private void openKitDialog(EquipmentKitRecord record) {
            KitDialog dialog = new KitDialog(DesktopShellFrame.this, record, saved -> {
                controller.saveKit(saved.kitId(), saved.kitName(), saved.responsiblePerson(), saved.intendedDate(), saved.status());
                refreshAll();
            });
            dialog.setVisible(true);
        }

        private final class KitTableModel extends AbstractTableModel {
            private final String[] columns = {"Kit name / ID", "Responsible person", "Intended date", "Status", "Actions"};
            private List<EquipmentKitRecord> records = List.of();

            void setRecords(List<EquipmentKitRecord> records) {
                this.records = List.copyOf(records);
                fireTableDataChanged();
            }

            @Override
            public int getRowCount() {
                return records.size();
            }

            @Override
            public int getColumnCount() {
                return columns.length;
            }

            @Override
            public String getColumnName(int column) {
                return columns[column];
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                EquipmentKitRecord record = records.get(rowIndex);
                return switch (columnIndex) {
                    case 0 -> record.kitName();
                    case 1 -> record.responsiblePerson();
                    case 2 -> record.intendedDate();
                    case 3 -> record.status();
                    default -> record;
                };
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == 4;
            }
        }

        private final class ActionRenderer extends JPanel implements TableCellRenderer {
            private final JButton edit = new JButton("Edit");
            private final JButton delete = new JButton("Delete");

            private ActionRenderer() {
                setOpaque(true);
                setLayout(new FlowLayout(FlowLayout.RIGHT, 4, 0));
                AppTheme.styleSecondaryButton(edit);
                AppTheme.styleSecondaryButton(delete);
                add(edit);
                add(delete);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return this;
            }
        }

        private final class ActionEditor extends DefaultCellEditor implements TableCellRenderer, TableCellEditor {
            private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            private final JButton edit = new JButton("Edit");
            private final JButton delete = new JButton("Delete");
            private EquipmentKitRecord record;

            private ActionEditor() {
                super(new JCheckBox());
                AppTheme.styleSecondaryButton(edit);
                AppTheme.styleSecondaryButton(delete);
                edit.addActionListener(e -> {
                    openKitDialog(record);
                    fireEditingStopped();
                });
                delete.addActionListener(e -> {
                    controller.deleteKit(record.kitId());
                    fireEditingStopped();
                    refreshAll();
                });
                panel.add(edit);
                panel.add(delete);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return panel;
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                record = (EquipmentKitRecord) value;
                return panel;
            }

            @Override
            public Object getCellEditorValue() {
                return record;
            }
        }
    }

    private final class KitDialog extends JDialog {
        private final JTextField kitNameField = new JTextField(24);
        private final JTextField responsibleField = new JTextField(24);
        private final JSpinner dateField = new JSpinner(new SpinnerDateModel());
        private final JComboBox<String> statusField = new JComboBox<>(new String[]{"Checked Out", "Returned", "Overdue"});
        private final JLabel kitError = new JLabel(" ");
        private final JLabel personError = new JLabel(" ");
        private final JLabel dateError = new JLabel(" ");
        private final JLabel statusError = new JLabel(" ");
        private final Consumer<EquipmentKitRecord> saveAction;
        private final UUID kitId;

        private KitDialog(JFrame owner, EquipmentKitRecord existing, Consumer<EquipmentKitRecord> saveAction) {
            super(owner, true);
            this.saveAction = saveAction;
            this.kitId = existing == null ? UUID.randomUUID() : existing.kitId();
            setTitle(existing == null ? "Log Kit Checkout" : "Edit Kit Checkout");
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setLayout(new BorderLayout());
            add(buildForm(existing), BorderLayout.CENTER);
            pack();
            setLocationRelativeTo(owner);
        }

        private JPanel buildForm(EquipmentKitRecord existing) {
            JPanel panel = cardPanel();
            panel.setLayout(new BorderLayout(0, 12));
            JPanel form = new JPanel(new GridBagLayout());
            form.setOpaque(false);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 0, 10, 0);
            form.add(fieldWithError("Kit name / identifier", kitNameField, kitError), c);
            c.gridy++;
            form.add(fieldWithError("Responsible person", responsibleField, personError), c);
            c.gridy++;
            form.add(fieldWithError("Intended usage date", dateField, dateError), c);
            c.gridy++;
            form.add(fieldWithError("Status", statusField, statusError), c);
            panel.add(form, BorderLayout.CENTER);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actions.setOpaque(false);
            JButton cancel = new JButton("Cancel");
            AppTheme.styleSecondaryButton(cancel);
            cancel.addActionListener(e -> dispose());
            JButton save = new JButton("Save kit record");
            AppTheme.stylePrimaryButton(save);
            save.addActionListener(e -> saveKit());
            actions.add(cancel);
            actions.add(save);
            panel.add(actions, BorderLayout.SOUTH);
            if (existing != null) {
                kitNameField.setText(existing.kitName());
                responsibleField.setText(existing.responsiblePerson());
                dateField.setValue(Date.from(existing.intendedDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
                statusField.setSelectedItem(existing.status());
            }
            kitNameField.requestFocusInWindow();
            return panel;
        }

        private JComponent fieldWithError(String label, JComponent field, JLabel errorLabel) {
            JPanel wrapper = new JPanel();
            wrapper.setOpaque(false);
            wrapper.setLayout(new javax.swing.BoxLayout(wrapper, javax.swing.BoxLayout.Y_AXIS));
            JLabel caption = new JLabel(label);
            caption.setFont(AppTheme.UI_SMALL);
            caption.setForeground(AppTheme.TEXT_PRIMARY);
            wrapper.add(caption);
            wrapper.add(field);
            errorLabel.setForeground(AppTheme.RED_700);
            errorLabel.setFont(AppTheme.META);
            wrapper.add(errorLabel);
            return wrapper;
        }

        private void saveKit() {
            clearErrors();
            boolean valid = true;
            if (kitNameField.getText().isBlank()) {
                showError(kitNameField, kitError, "Kit name is required.");
                valid = false;
            }
            if (responsibleField.getText().isBlank()) {
                showError(responsibleField, personError, "Responsible person is required.");
                valid = false;
            }
            Date selectedDate = (Date) dateField.getValue();
            if (selectedDate == null) {
                showError(dateField, dateError, "Date is required.");
                valid = false;
            }
            if (statusField.getSelectedItem() == null) {
                showError(statusField, statusError, "Status is required.");
                valid = false;
            }
            if (!valid) {
                return;
            }
            LocalDate intendedDate = Instant.ofEpochMilli(selectedDate.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
            saveAction.accept(new EquipmentKitRecord(
                    kitId,
                    kitNameField.getText().trim(),
                    responsibleField.getText().trim(),
                    intendedDate,
                    (String) statusField.getSelectedItem(),
                    "Returned".equalsIgnoreCase((String) statusField.getSelectedItem())));
            dispose();
        }

        private void clearErrors() {
            kitError.setText(" ");
            personError.setText(" ");
            dateError.setText(" ");
            statusError.setText(" ");
            resetBorder(kitNameField);
            resetBorder(responsibleField);
            resetBorder(dateField);
            resetBorder(statusField);
        }

        private void showError(JComponent component, JLabel label, String message) {
            label.setText(message);
            component.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.RED_500, 1),
                    component.getBorder()));
        }

        private void resetBorder(JComponent component) {
            component.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(AppTheme.BORDER, 12, 1),
                    BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        }
    }
}
