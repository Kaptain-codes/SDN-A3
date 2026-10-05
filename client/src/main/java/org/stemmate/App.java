package org.stemmate;

import org.stemmate.connectivity.ConnectivityMonitor;
import org.stemmate.model.Activity;
import org.stemmate.model.EquipmentKitRecord;
import org.stemmate.model.InclusionPrompt;
import org.stemmate.model.Material;
import org.stemmate.model.PlanStep;
import org.stemmate.model.SafetyNote;
import org.stemmate.repository.LocalRemoteSessionPlanStore;
import org.stemmate.service.ActivityCatalogueService;
import org.stemmate.service.EquipmentKitService;
import org.stemmate.service.SessionPlanService;
import org.stemmate.store.LocalActivityStore;
import org.stemmate.store.LocalEquipmentKitStore;
import org.stemmate.store.LocalOutboxRepository;
import org.stemmate.store.LocalSessionPlanStore;
import org.stemmate.store.LocalSyncLogStore;
import org.stemmate.sync.SyncManager;
import org.stemmate.ui.AppTheme;
import org.stemmate.ui.DesktopShellFrame;

import javax.swing.SwingUtilities;
import java.time.Clock;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        AppTheme.install();
        SwingUtilities.invokeLater(() -> {
            LocalActivityStore activityStore = new LocalActivityStore(seedActivities());
            LocalOutboxRepository outboxRepository = new LocalOutboxRepository();
            LocalSessionPlanStore sessionPlanStore = new LocalSessionPlanStore(outboxRepository);
            LocalEquipmentKitStore kitStore = new LocalEquipmentKitStore();
            LocalSyncLogStore syncLogStore = new LocalSyncLogStore();
            ConnectivityMonitor connectivityMonitor = new ConnectivityMonitor(true);
            SyncManager syncManager = new SyncManager(
                    sessionPlanStore,
                    outboxRepository,
                    new LocalRemoteSessionPlanStore(),
                    connectivityMonitor,
                    Clock.systemUTC(),
                    syncLogStore);

            ActivityCatalogueService activityService = new ActivityCatalogueService(activityStore);
            SessionPlanService planService = new SessionPlanService(sessionPlanStore);
            EquipmentKitService kitService = new EquipmentKitService(kitStore);
            DesktopController controller = new DesktopController(
                    activityService,
                    planService,
                    kitService,
                    connectivityMonitor,
                    syncManager);
            DesktopShellFrame frame = new DesktopShellFrame(controller);
            frame.setVisible(true);
        });
    }

    private static List<Activity> seedActivities() {
        return List.of(
                activity("Middle School", 45, "Physics",
                        "Mini Wind Turbine Generator",
                        "Assemble micro wind turbines using recycled cardboard blades and small DC motors.",
                        new PlanStep[]{
                                new PlanStep("Setup and introduction", 10, "Discuss aerodynamics and clean power generation."),
                                new PlanStep("Blade construction", 20, "Cut and test cardboard pitch angles."),
                                new PlanStep("Testing and output measurement", 15, "Measure voltage output using a multimeter.")
                        },
                        new String[]{"Cardboard", "Basic Electronics", "DC Motors"},
                        new String[]{"Caution when using craft knives for cutting cardboard.", "Eye protection required during fan testing."}),
                activity("High School", 60, "Environmental",
                        "Soil Micro-Ecosystem Analysis",
                        "Analyze local soil health through moisture retention, pH reactions, and organism counting.",
                        new PlanStep[]{
                                new PlanStep("Field sample prep", 15, "Mix soil samples with distilled water."),
                                new PlanStep("pH indicator test", 25, "Observe color changes against control charts."),
                                new PlanStep("Organism count", 20, "Document visible organisms using a magnifier.")
                        },
                        new String[]{"Water", "Soil Samples", "pH Kits"},
                        new String[]{"Avoid direct skin contact with untested samples.", "Wash hands after handling soil."}),
                activity("High School", 60, "Robotics",
                        "Autonomous Obstacle Avoidance Bot",
                        "Program a sensor-driven rover to detect obstacles and navigate a maze autonomously.",
                        new PlanStep[]{
                                new PlanStep("Assembly and wiring", 20, "Connect sensors and verify battery polarity."),
                                new PlanStep("Code upload", 20, "Load the obstacle avoidance sketch."),
                                new PlanStep("Maze run", 20, "Tune sensor thresholds while the rover navigates.")
                        },
                        new String[]{"Microcontrollers", "Basic Electronics"},
                        new String[]{"Keep fingers clear of moving wheels.", "Disconnect power before rewiring."}),
                activity("Primary", 30, "Physics",
                        "Hydraulic Bridge Lift",
                        "Demonstrate Pascal's principle by creating a dual-syringe hydraulic bridge lift.",
                        new PlanStep[]{
                                new PlanStep("Introduce concept", 10, "Explain fluid pressure transfer with simple examples."),
                                new PlanStep("Build syringes", 10, "Connect the syringes with tubing and fill with water."),
                                new PlanStep("Test lifting", 10, "Compare lift force across different bridge loads.")
                        },
                        new String[]{"Cardboard", "Water", "Syringes"},
                        new String[]{"Protect desks from water spills.", "Do not overfill the syringe tubing."})
        );
    }

    private static Activity activity(
            String level,
            int duration,
            String topic,
            String title,
            String description,
            PlanStep[] steps,
            String[] materialNames,
            String[] safetyNotes) {
        return new Activity(
                UUID.nameUUIDFromBytes(title.getBytes(StandardCharsets.UTF_8)),
                title,
                description,
                level,
                duration,
                topic,
                List.of(steps),
                java.util.Arrays.stream(materialNames)
                        .map(name -> new Material(name, "as required"))
                        .toList(),
                java.util.Arrays.stream(safetyNotes)
                        .map(SafetyNote::new)
                        .toList());
    }
}
