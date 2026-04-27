package ru.mobmoney.compat;

import org.bukkit.Bukkit;

/**
 * Detects the running Minecraft version and server platform at startup.
 *
 * Supports:
 *   Bukkit / CraftBukkit 1.8 – 1.21.x
 *   Spigot  1.8 – 1.21.x
 *   Paper   1.8 – 1.21.x  (destroystokyo era + modern io.papermc)
 *   Purpur, Pufferfish, Leaves  (Paper forks — detected as Paper)
 *   Folia   (Paper's threaded-regions fork)
 */
public final class VersionHelper {

    /* e.g.  1.21.4  →  minor = 21, patch = 4
             1.8.8   →  minor = 8,  patch = 8
             1.16    →  minor = 16, patch = 0  */
    private static final int MINOR;
    private static final int PATCH;

    private static final boolean IS_PAPER;
    private static final boolean IS_FOLIA;

    static {
        // getBukkitVersion() → "1.21.4-R0.1-SNAPSHOT"
        String raw = Bukkit.getBukkitVersion().split("-")[0]; // "1.21.4"
        String[] parts = raw.split("\\.");
        int minor = 8, patch = 0;
        try { minor = Integer.parseInt(parts[1]); } catch (Exception ignored) { /* stay at 8 */ }
        try { patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0; } catch (Exception ignored) { }
        MINOR = minor;
        PATCH = patch;

        // --- Paper detection (covers all forks that inherit Paper) ---
        boolean paper = false;
        // Modern Paper (1.19+)
        if (classExists("io.papermc.paper.configuration.Configuration")) paper = true;
        // Paper 1.14–1.18
        if (!paper && classExists("com.destroystokyo.paper.PaperConfig"))  paper = true;
        // Very old Paper (1.8/1.9 era forks)
        if (!paper && classExists("com.destroystokyo.paper.Title"))         paper = true;
        IS_PAPER = paper;

        // --- Folia detection ---
        IS_FOLIA = classExists("io.papermc.paper.threadedregions.RegionisedServer");
    }

    private VersionHelper() { }

    // ── Public API ────────────────────────────────────────────────────────────

    /** True if running on Minecraft minor version ≥ {@code minor} (e.g. 16 for 1.16+). */
    public static boolean isAtLeast(int minor) {
        return MINOR >= minor;
    }

    /** True if running on exactly ≥ minor.patch  (e.g. isAtLeast(9,0) for 1.9+). */
    public static boolean isAtLeast(int minor, int patch) {
        return MINOR > minor || (MINOR == minor && PATCH >= patch);
    }

    public static int getMinor()   { return MINOR; }
    public static int getPatch()   { return PATCH; }
    public static boolean isPaper() { return IS_PAPER; }
    public static boolean isFolia() { return IS_FOLIA; }

    /** Human-readable platform string for logs. */
    public static String getPlatformName() {
        if (IS_FOLIA)  return "Folia";
        if (IS_PAPER)  return "Paper";
        // Distinguish Spigot from pure CraftBukkit
        if (classExists("org.spigotmc.SpigotConfig")) return "Spigot";
        return "CraftBukkit";
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
