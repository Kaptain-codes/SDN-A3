import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps the outbox in a text file on the laptop, one item per line,
 * so nothing waiting to sync is lost if the app closes or the power goes.
 *
 * File: <home folder>/.stemmate/outbox.txt (change with STEMMATE_OUTBOX_FILE)
 */
public class Outbox {

    private final Path file;

    public Outbox() {
        String custom = System.getenv("STEMMATE_OUTBOX_FILE");
        file = (custom != null && !custom.isBlank())
                ? Path.of(custom)
                : Path.of(System.getProperty("user.home"), ".stemmate", "outbox.txt");
    }

    public List<String> load() {
        try {
            if (Files.exists(file)) return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Could not read outbox: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    /** Writes to a temp file first, then swaps it in, so a crash mid-save never leaves a broken file. */
    public void save(List<String> lines) {
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling("outbox.tmp");
            Files.write(temp, lines, StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            System.err.println("Could not save outbox: " + e.getMessage());
        }
    }
}
