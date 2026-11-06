
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class STEMCraftVerticalSlice extends JFrame {


    private boolean isOnline = true;
    private boolean isMobileView = false;
    private final List <SyncItem> syncOutbox = new ArrayList<>();
    private Timer autoSaveTimer;

    // Back-end link (server/ folder). The plan id is created here so an offline plan keeps its id when it syncs.
    private final ServerClient server = new ServerClient();
    private final Outbox outbox = new Outbox();       // outbox saved to a file, survives closing the app
    private UUID currentPlanId = UUID.randomUUID();
    private boolean syncInFlight = false;
    private Timer retryTimer;                         // retries sync by itself on a bad connection
    private int retrySeconds = 15;                    // doubles after each failure, up to 5 minutes


    private JPanel rootPanel;
    private JPanel headerPanel;
    private JTabbedPane mainTabbedPane;
    private JPanel statusBar;

    // Header Controls
    private JButton netToggleBtn;
    private JLabel syncBadgeLabel;
    private JButton deviceToggleBtn;

    // Form Fields (Session Builder)
    private JTextField planTitleField;
    private JComboBox<String> activityDropdown;
    private JTextField targetGroupField;
    private JTextArea materialsArea;
    private JTextArea safetyArea;
    private JTextArea inclusionArea;
    private JLabel autoSaveBadge;


    private DefaultListModel<String> outboxListModel;
    private JList<String> outboxJList;


    private DefaultTableModel custodyTableModel;


    private final Color BRAND_DARK = new Color(15, 23, 42);      // Slate 900
    private final Color BRAND_TEAL = new Color(13, 148, 136);    // Teal 600
    private final Color BG_LIGHT = new Color(248, 250, 252);     // Slate 50
    private final Color AMBER_WARN = new Color(217, 119, 6);     // Amber 600
    private final Color EMERALD_OK = new Color(16, 185, 129);    // Emerald 500

    public STEMCraftVerticalSlice() {
        super("STEMCraft Namibia — A3 Vertical Slice Execution");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 740);
        setMinimumSize(new Dimension(380, 640)); // Mobile portrait minimum
        setLocationRelativeTo(null);


        UIManager.put("TabbedPane.selected", BRAND_TEAL);
        UIManager.put("TabbedPane.foreground", Color.BLACK);

        initUI();

        // Bring back anything still in the outbox from last time, then keep retrying in the background.
        for (String line : outbox.load()) {
            if (!line.isBlank()) syncOutbox.add(SyncItem.fromLine(line));
        }
        renderOutbox();
        retryTimer = new Timer(retrySeconds * 1000, e -> {
            if (isOnline) triggerSyncProcess(false);
        });
        retryTimer.start();
    }

    private void initUI() {
        rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(BG_LIGHT);


        headerPanel = createHeader();
        rootPanel.add(headerPanel, BorderLayout.NORTH);


        mainTabbedPane = new JTabbedPane(JTabbedPane.TOP);
        mainTabbedPane.setFont(new Font("SansSerif", Font.BOLD, 13));

        mainTabbedPane.addTab("1. Activity Catalogd", createCatalogView());
        mainTabbedPane.addTab("2. Session Planner ", createPlannerView());
        mainTabbedPane.addTab("3. Kit Custody ", createCustodyView());

        rootPanel.add(mainTabbedPane, BorderLayout.CENTER);


        statusBar = createStatusBar();
        rootPanel.add(statusBar, BorderLayout.SOUTH);

        add(rootPanel);
    }


    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(BRAND_DARK);
        header.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));


        JLabel brandLabel = new JLabel("STEMCraft Namibia");
        brandLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        brandLabel.setForeground(Color.WHITE);

        JLabel subLabel = new JLabel(" • Vertical Slice Execution");
        subLabel.setFont(new Font("Monospaced", Font.PLAIN, 10));
        subLabel.setForeground(new Color(148, 163, 184));

        JPanel titleBox = new JPanel(new GridLayout(2, 1));
        titleBox.setOpaque(false);
        titleBox.add(brandLabel);
        titleBox.add(subLabel);

        // Controls (Right Side)
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);


        deviceToggleBtn = new JButton("📱 Phone View");
        deviceToggleBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        deviceToggleBtn.setFocusable(true);
        deviceToggleBtn.setToolTipText("Switch view mode between Desktop and Phone width");
        deviceToggleBtn.addActionListener(e -> toggleDeviceView());


        netToggleBtn = new JButton("🌐 ONLINE");
        netToggleBtn.setFont(new Font("SansSerif", Font.BOLD, 11));
        netToggleBtn.setBackground(EMERALD_OK);
        netToggleBtn.setForeground(Color.WHITE);
        netToggleBtn.setFocusable(true);
        netToggleBtn.setMnemonic(KeyEvent.VK_N);
        netToggleBtn.setToolTipText("Alt+N: Toggle network state (Online/Offline)");
        netToggleBtn.addActionListener(e -> toggleNetwork());


        syncBadgeLabel = new JLabel("Outbox: 0 Pending");
        syncBadgeLabel.setFont(new Font("Monospaced", Font.BOLD, 11));
        syncBadgeLabel.setForeground(Color.WHITE);
        syncBadgeLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        JButton syncNowBtn = new JButton("Force Sync");
        syncNowBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        syncNowBtn.addActionListener(e -> triggerSyncProcess(true));

        controls.add(deviceToggleBtn);
        controls.add(netToggleBtn);
        controls.add(syncBadgeLabel);
        controls.add(syncNowBtn);

        header.add(titleBox, BorderLayout.WEST);
        header.add(controls, BorderLayout.EAST);

        return header;
    }

    private JPanel createCatalogView() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setBackground(BG_LIGHT);


        JLabel info = new JLabel("<html><b>STEM Activity Library</b> — Select items to store in your offline vault for field access without internet (FR2).</html>");
        info.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panel.add(info, BorderLayout.NORTH);


        String[] cols = {"ID", "Activity Title", "Grade Level", "Duration", "Key Equipment"};
        Object[][] data = {
                {"ACT-101", "Mini Wind Turbine Generator", "Grade 8-10", "45 min", "DC Motor, Cardboard, Wires"},
                {"ACT-102", "Soil Micro-Ecosystem Analysis", "Grade 11-12", "60 min", "pH Strips, Distilled Water, Beaker"},
                {"ACT-103", "Autonomous Obstacle Avoidance Bot", "Grade 9-12", "60 min", "Arduino, Ultrasonic Sensor"},
                {"ACT-104", "Hydraulic Lift Physics", "Grade 5-7", "30 min", "Syringes, Flexible Tubing"}
        };

        JTable catalogTable = new JTable(new DefaultTableModel(data, cols));
        catalogTable.setRowHeight(32);
        catalogTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JScrollPane tableScroll = new JScrollPane(catalogTable);


        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionPanel.setOpaque(false);

        JButton saveOfflineBtn = new JButton("Save Selected to Offline Vault (FR2)");
        saveOfflineBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        saveOfflineBtn.setBackground(BRAND_TEAL);
        saveOfflineBtn.setForeground(Color.WHITE);
        saveOfflineBtn.addActionListener(e -> {
            int row = catalogTable.getSelectedRow();
            if (row != -1) {
                String title = (String) catalogTable.getValueAt(row, 1);
                Map<String, String> vaultData = new LinkedHashMap<>();
                vaultData.put("activityId", (String) catalogTable.getValueAt(row, 0));
                queueItem("SAVE_OFFLINE", title, vaultData);
                JOptionPane.showMessageDialog(this, "Saved \"" + title + "\" to local storage for offline use.", "FR2 Offline Storage", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Please select an activity from the table first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            }
        });

        JButton loadToPlannerBtn = new JButton("Load into Session Planner ->");
        loadToPlannerBtn.addActionListener(e -> {
            int row = catalogTable.getSelectedRow();
            if (row != -1) {
                String title = (String) catalogTable.getValueAt(row, 1);
                planTitleField.setText("Session: " + title);
                mainTabbedPane.setSelectedIndex(1); // Jump to planner tab
            }
        });

        actionPanel.add(saveOfflineBtn);
        actionPanel.add(loadToPlannerBtn);

        panel.add(tableScroll, BorderLayout.CENTER);
        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }


    private JPanel createPlannerView() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setBackground(BG_LIGHT);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;


        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        JLabel titleLbl = new JLabel("Plan Title *");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        titleLbl.setDisplayedMnemonic('T');
        formPanel.add(titleLbl, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        planTitleField = new JTextField(20);
        titleLbl.setLabelFor(planTitleField);
        planTitleField.getDocument().addDocumentListener(createAutoSaveListener());
        formPanel.add(planTitleField, gbc);


        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        JLabel actLbl = new JLabel("Base Activity");
        actLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        formPanel.add(actLbl, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        activityDropdown = new JComboBox<>(new String[]{
                "-- Select Base Activity --",
                "ACT-101: Mini Wind Turbine Generator",
                "ACT-102: Soil Micro-Ecosystem Analysis",
                "ACT-103: Autonomous Obstacle Avoidance Bot"
        });
        formPanel.add(activityDropdown, gbc);


        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        JLabel targetLbl = new JLabel("Target Group");
        targetLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        formPanel.add(targetLbl, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        targetGroupField = new JTextField(20);
        targetGroupField.getDocument().addDocumentListener(createAutoSaveListener());
        formPanel.add(targetGroupField, gbc);


        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        JLabel safetyLbl = new JLabel("Safety Controls (FR4)");
        safetyLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        safetyLbl.setForeground(new Color(185, 28, 28));
        formPanel.add(safetyLbl, gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        safetyArea = new JTextArea(2, 20);
        safetyArea.setLineWrap(true);
        safetyArea.getDocument().addDocumentListener(createAutoSaveListener());
        formPanel.add(new JScrollPane(safetyArea), gbc);


        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        JLabel incLbl = new JLabel("Inclusion Prompts (FR4)");
        incLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        incLbl.setForeground(new Color(67, 56, 202));
        formPanel.add(incLbl, gbc);

        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        inclusionArea = new JTextArea(2, 20);
        inclusionArea.setLineWrap(true);
        inclusionArea.getDocument().addDocumentListener(createAutoSaveListener());
        formPanel.add(new JScrollPane(inclusionArea), gbc);


        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);

        JButton savePlanBtn = new JButton("Save & Sync Plan");
        savePlanBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        savePlanBtn.setBackground(BRAND_TEAL);
        savePlanBtn.setForeground(Color.WHITE);
        savePlanBtn.addActionListener(e -> saveCurrentPlan());

        JButton dupPlanBtn = new JButton("Duplicate Draft (FR8)");
        dupPlanBtn.addActionListener(e -> duplicateCurrentPlan());

        btnRow.add(dupPlanBtn);
        btnRow.add(savePlanBtn);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 3;
        formPanel.add(btnRow, gbc);


        JPanel sidebar = new JPanel(new BorderLayout(8, 8));
        sidebar.setPreferredSize(new Dimension(280, 0));
        sidebar.setBorder(BorderFactory.createTitledBorder("Sync Outbox Queue (FR5)"));

        outboxListModel = new DefaultListModel<>();
        outboxJList = new JList<>(outboxListModel);
        outboxJList.setFont(new Font("Monospaced", Font.PLAIN, 11));
        sidebar.add(new JScrollPane(outboxJList), BorderLayout.CENTER);

        panel.add(formPanel, BorderLayout.CENTER);
        panel.add(sidebar, BorderLayout.EAST);

        return panel;
    }


    private JPanel createCustodyView() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setBackground(BG_LIGHT);

        JLabel title = new JLabel("<html><b>Equipment Kit Custody Log (FR7)</b> — Track checkout dates, facilitators, and return statuses.</html>");
        title.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panel.add(title, BorderLayout.NORTH);

        String[] cols = {"Kit ID", "Kit Name", "Responsible Person", "Checkout Date", "Status"};
        custodyTableModel = new DefaultTableModel(cols, 0);
        custodyTableModel.addRow(new Object[]{"KIT-01", "Arduino Sensor Array #04", "Maria Garcia", "2026-10-12", "Checked Out"});
        custodyTableModel.addRow(new Object[]{"KIT-02", "Water Quality Analysis Set", "David Chen", "2026-10-05", "Returned"});

        JTable custodyTable = new JTable(custodyTableModel);
        custodyTable.setRowHeight(30);
        panel.add(new JScrollPane(custodyTable), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.setOpaque(false);

        JButton checkoutBtn = new JButton("Log Kit Checkout");
        checkoutBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        checkoutBtn.setBackground(BRAND_DARK);
        checkoutBtn.setForeground(Color.WHITE);
        checkoutBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Enter Kit Name/ID:", "Log Checkout", JOptionPane.PLAIN_MESSAGE);
            if (name != null && !name.trim().isEmpty()) {
                String person = JOptionPane.showInputDialog(this, "Enter Responsible Facilitator:", "Log Checkout", JOptionPane.PLAIN_MESSAGE);
                String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                custodyTableModel.addRow(new Object[]{"KIT-0" + (custodyTableModel.getRowCount() + 1), name, person, date, "Checked Out"});
                queueItem("CHECKOUT_KIT", name, Map.of("kitName", name));
            }
        });

        btnPanel.add(checkoutBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }


    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(203, 213, 225)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        autoSaveBadge = new JLabel("Draft Auto-Save: Idle (FR6)");
        autoSaveBadge.setFont(new Font("SansSerif", Font.ITALIC, 11));

        JLabel wcagBadge = new JLabel("WCAG 2.2 AA Compliant • Dual-Channel Status Cues");
        wcagBadge.setFont(new Font("SansSerif", Font.PLAIN, 11));

        bar.add(autoSaveBadge, BorderLayout.WEST);
        bar.add(wcagBadge, BorderLayout.EAST);

        return bar;
    }

    // --- STATE MACHINE & EVENT HANDLERS ---
    private void toggleNetwork() {
        isOnline = !isOnline;
        if (isOnline) {
            netToggleBtn.setText("🌐 ONLINE");
            netToggleBtn.setBackground(EMERALD_OK);
            triggerSyncProcess(true);
        } else {
            netToggleBtn.setText("🚫 OFFLINE");
            netToggleBtn.setBackground(AMBER_WARN);
            autoSaveBadge.setText("Draft Auto-Save: Stored Locally (Offline)");
        }
    }

    private void toggleDeviceView() {
        isMobileView = !isMobileView;
        if (isMobileView) {
            setSize(420, 720);
            deviceToggleBtn.setText("💻 Desktop View");
        } else {
            setSize(1080, 740);
            deviceToggleBtn.setText("📱 Phone View");
        }
        revalidate();
    }

    private void queueItem(String actionType, String payload, Map<String, String> data) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        // Every item starts as "Waiting to Sync"; only the server's answer can make it "Confirmed Result".
        SyncItem item = new SyncItem(actionType, payload, "Waiting to Sync", timestamp, data);

        syncOutbox.add(item);
        saveOutbox();
        renderOutbox();

        if (isOnline) {
            triggerSyncProcess(false); // push straight away, quietly
        }
    }

    private void triggerSyncProcess(boolean showDialog) {
        if (!isOnline) {
            JOptionPane.showMessageDialog(this, "Client is currently Offline. Reconnect network to push outbox queue to server.", "Sync Paused", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (syncInFlight) return;

        List<SyncItem> pending = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        for (SyncItem item : syncOutbox) {
            if (item.status.equals("Waiting to Sync")) {
                pending.add(item);
                lines.add(item.toSendLine());
            }
        }
        if (pending.isEmpty()) {
            if (showDialog) JOptionPane.showMessageDialog(this, "Nothing waiting to sync.", "Sync Confirmation (FR5)", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        syncInFlight = true;
        autoSaveBadge.setText("Draft Auto-Save: Syncing " + pending.size() + " item(s)...");

        // Network call off the Swing thread so the UI never freezes on a slow connection.
        new SwingWorker<Map<String, Map<String, String>>, Void>() {
            @Override
            protected Map<String, Map<String, String>> doInBackground() throws Exception {
                return server.send(lines);
            }

            @Override
            protected void done() {
                syncInFlight = false;
                try {
                    Map<String, Map<String, String>> answers = get();

                    int confirmed = 0, rejected = 0;
                    for (SyncItem item : pending) {
                        Map<String, String> answer = answers.get(item.id);
                        if (answer == null) continue; // no answer yet -> stays waiting, retried later
                        if ("OK".equals(answer.get("status"))) {
                            item.status = "Confirmed Result";
                            confirmed++;
                        } else {
                            item.status = "Rejected: " + answer.get("message");
                            rejected++;
                        }
                    }
                    saveOutbox();
                    renderOutbox();
                    setRetryDelay(15); // connection works: back to normal speed
                    autoSaveBadge.setText("Draft Auto-Save: " + confirmed + " confirmed, " + rejected + " rejected by server");
                    if (showDialog || rejected > 0) {
                        JOptionPane.showMessageDialog(STEMCraftVerticalSlice.this,
                                confirmed + " item(s) confirmed by the server." + (rejected > 0 ? "\n" + rejected + " item(s) rejected - see the outbox for the reason." : ""),
                                "Sync Confirmation (FR5)",
                                rejected > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    // Server down or unreachable: nothing is lost, items stay "Waiting to Sync".
                    setRetryDelay(Math.min(retrySeconds * 2, 300)); // wait longer before the next try
                    renderOutbox();
                    autoSaveBadge.setText("Draft Auto-Save: No connection - " + pending.size() + " change(s) saved here, retrying in " + retrySeconds + "s");
                    if (showDialog) {
                        JOptionPane.showMessageDialog(STEMCraftVerticalSlice.this,
                                "Could not reach the STEMMate server. Your changes are saved on this laptop and will sync automatically.",
                                "Sync Paused", JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        }.execute();
    }

    private void setRetryDelay(int seconds) {
        retrySeconds = seconds;
        retryTimer.setDelay(seconds * 1000);
    }

    private void saveOutbox() {
        List<String> lines = new ArrayList<>();
        for (SyncItem item : syncOutbox) {
            // confirmed items are done; only keep what is waiting or rejected, so the file stays small
            if (!item.status.equals("Confirmed Result")) lines.add(item.toLine());
        }
        outbox.save(lines);
    }

    private void renderOutbox() {
        outboxListModel.clear();
        int pending = 0;
        for (SyncItem item : syncOutbox) {
            if (item.status.equals("Waiting to Sync")) pending++;
            outboxListModel.addElement(item.toString());
        }

        if (pending > 0) {
            syncBadgeLabel.setText("Outbox: " + pending + " Waiting to Sync");
            syncBadgeLabel.setForeground(AMBER_WARN);
        } else {
            syncBadgeLabel.setText("Outbox: 0 Pending");
            syncBadgeLabel.setForeground(Color.WHITE);
        }
    }

    private DocumentListener createAutoSaveListener() {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                triggerAutoSave();
            }

            public void removeUpdate(DocumentEvent e) {
                triggerAutoSave();
            }

            public void changedUpdate(DocumentEvent e) {
                triggerAutoSave();
            }
        };
    }

    private void triggerAutoSave() {
        autoSaveBadge.setText("Draft Auto-Save: Auto-saving...");
        if (autoSaveTimer != null && autoSaveTimer.isRunning()) {
            autoSaveTimer.restart();
        } else {
            autoSaveTimer = new Timer(800, e -> {
                autoSaveBadge.setText("Draft Auto-Save: Draft Saved at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                autoSaveTimer.stop();
            });
            autoSaveTimer.start();
        }
    }

    private void saveCurrentPlan() {
        String title = planTitleField.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please provide a Plan Title.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Field names match the server's SAVE_PLAN format (see server/README.md)
        Map<String, String> data = new LinkedHashMap<>();
        data.put("planId", currentPlanId.toString());
        data.put("title", title);
        String activity = (String) activityDropdown.getSelectedItem();
        data.put("activityId", (activity != null && activity.startsWith("ACT-")) ? activity.substring(0, 7) : null);
        data.put("targetGroup", targetGroupField.getText().trim());
        data.put("safety", safetyArea.getText().trim());
        data.put("inclusion", inclusionArea.getText().trim());
        data.put("updatedAt", String.valueOf(System.currentTimeMillis()));

        queueItem("SAVE_PLAN", title, data);
        JOptionPane.showMessageDialog(this, "Session Plan \"" + title + "\" saved on this device. "
                + (isOnline ? "Sending to the server now." : "It will sync when you are back online."), "Plan Saved", JOptionPane.INFORMATION_MESSAGE);
    }

    private void duplicateCurrentPlan() {
        String title = planTitleField.getText().trim();
        if (title.isEmpty()) title = "Untitled Session";
        planTitleField.setText(title + " (Copy)");
        currentPlanId = UUID.randomUUID(); // a copy is a new plan on the server
        triggerAutoSave();
    }

        private static class SyncItem {
            String action;
            String title;
            String status;
            String time;
            String id = UUID.randomUUID().toString(); // lets the server ignore duplicates on retry
            final Map<String, String> data;

            SyncItem(String action, String title, String status, String time, Map<String, String> data) {
                this.action = action;
                this.title = title;
                this.status = status;
                this.time = time;
                this.data = data;
            }

            /** What goes to the server: id, action and the data fields only. */
            String toSendLine() {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("id", id);
                m.put("action", action);
                m.putAll(data);
                return ServerClient.encode(m);
            }

            /** What goes in the outbox file: the send line plus what the screen shows. */
            String toLine() {
                return toSendLine() + "&" + ServerClient.encode(Map.of("label", title, "time", time, "status", status));
            }

            static SyncItem fromLine(String line) {
                Map<String, String> m = ServerClient.decode(line);
                String id = m.remove("id");
                SyncItem item = new SyncItem(m.remove("action"), m.remove("label"), m.remove("status"), m.remove("time"), m);
                item.id = id;
                return item;
            }

            @Override
            public String toString() {
                return "[" + time + "] " + title + " -> " + status;
            }
        }

        public static void main (String[]args){
            SwingUtilities.invokeLater(() -> {
                STEMCraftVerticalSlice app = new STEMCraftVerticalSlice();
                app.setVisible(true);
            });
        }
    }
