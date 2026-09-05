package app.ezclient.shared;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Minecraft-independent filesystem logic shared by all EzClient targets. */
public final class EzClientPaths {
    private EzClientPaths() {}

    public static Path dataDirectory() {
        String appData = System.getenv("APPDATA");
        Path root = appData == null || appData.trim().isEmpty()
                ? Paths.get(System.getProperty("user.home"), ".ezclient")
                : Paths.get(appData, ".ezclient");
        try {
            Files.createDirectories(root.resolve("config"));
            Files.createDirectories(root.resolve("logs"));
            Files.createDirectories(root.resolve("cosmetics"));
        } catch (IOException exception) {
            System.err.println("[EzClient] Could not create data folders: " + exception.getMessage());
        }
        return root;
    }
}
