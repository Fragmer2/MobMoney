package ru.mobmoney.compat;

import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Safely resolves EntityType names from config/lang files across all MC versions.
 *
 * Many mob enum names changed between versions:
 *
 *  ┌─────────────────────┬──────────────────────────────────────────────────────┐
 *  │  Modern name        │  Old name(s)                                         │
 *  ├─────────────────────┼──────────────────────────────────────────────────────┤
 *  │  ZOMBIFIED_PIGLIN   │  PIG_ZOMBIE  (≤1.15)                                 │
 *  │  MOOSHROOM          │  MUSHROOM_COW  (some old builds)                     │
 *  │  VINDICATOR         │  VINDICATION_ILLAGER / VINDICATOR                    │
 *  │  EVOKER             │  EVOCATION_ILLAGER (≤1.11)                           │
 *  │  ILLUSIONER         │  ILLUSION_ILLAGER  (≤1.11)                           │
 *  │  PILLAGER           │  (1.14+, no old alias)                               │
 *  │  RAVAGER            │  (1.14+, no old alias)                               │
 *  └─────────────────────┴──────────────────────────────────────────────────────┘
 *
 * Resolution order for a given string key:
 *   1. Direct EntityType.valueOf(key)
 *   2. Alias lookup (modern → old)
 *   3. Alias lookup (old → modern)
 *   4. null  (skip silently with a warning)
 */
public final class EntityTypeHelper {

    /**
     * Map: canonical/modern name → fallback name used on old servers.
     * Also contains the reverse so both directions work.
     */
    private static final Map<String, String> ALIASES = new HashMap<String, String>();

    static {
        // 1.16 rename
        ALIASES.put("ZOMBIFIED_PIGLIN",     "PIG_ZOMBIE");
        ALIASES.put("PIG_ZOMBIE",           "ZOMBIFIED_PIGLIN");

        // Some old CraftBukkit builds used MUSHROOM_COW
        ALIASES.put("MOOSHROOM",            "MUSHROOM_COW");
        ALIASES.put("MUSHROOM_COW",         "MOOSHROOM");

        // Illager renames (1.12 → 1.13)
        ALIASES.put("EVOKER",               "EVOCATION_ILLAGER");
        ALIASES.put("EVOCATION_ILLAGER",    "EVOKER");

        ALIASES.put("VINDICATOR",           "VINDICATION_ILLAGER");
        ALIASES.put("VINDICATION_ILLAGER",  "VINDICATOR");

        ALIASES.put("ILLUSIONER",           "ILLUSION_ILLAGER");
        ALIASES.put("ILLUSION_ILLAGER",     "ILLUSIONER");

        // 1.14 rename: OCELOT lost the CAT subtype; CAT became its own EntityType
        // No direct alias needed — both exist in modern builds.

        // Some builds spell it differently
        ALIASES.put("ZOMBIFIED_PIGLIN",     "ZOMBIE_PIGMAN");
        ALIASES.put("ZOMBIE_PIGMAN",        "ZOMBIFIED_PIGLIN");
    }

    private EntityTypeHelper() { }

    /**
     * Resolves a string to an EntityType, trying aliases if the primary name fails.
     *
     * @param key    Upper-cased enum name from config (e.g. "ZOMBIFIED_PIGLIN")
     * @param logger Logger for warnings; pass {@code null} to silence
     * @return resolved EntityType, or {@code null} if not available on this version
     */
    public static EntityType resolve(String key, Logger logger) {
        // 1. Direct lookup
        EntityType direct = tryValueOf(key);
        if (direct != null) return direct;

        // 2. Alias lookup
        String alias = ALIASES.get(key.toUpperCase());
        if (alias != null) {
            EntityType aliased = tryValueOf(alias);
            if (aliased != null) return aliased;
        }

        // 3. Not available on this server version — not an error, just skip
        if (logger != null) {
            logger.fine("EntityType '" + key + "' not available on this server version — skipping.");
        }
        return null;
    }

    /** @return name to use in config for this EntityType (always the modern/canonical name). */
    public static String canonicalName(EntityType type) {
        return type.name();
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static EntityType tryValueOf(String name) {
        try {
            return EntityType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
