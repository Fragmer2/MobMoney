package ru.mobmoney.listener;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;
import ru.mobmoney.MobMoney;
import ru.mobmoney.compat.DisplayUtil;
import ru.mobmoney.compat.VersionHelper;
import ru.mobmoney.manager.ConfigManager;
import ru.mobmoney.manager.LanguageManager;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MobListener implements Listener {

    /** Metadata key used to mark mobs that spawned from a spawner block. */
    private static final String SPAWNER_META = "mm_spawner";

    /**
     * Boss-tier mobs: get a full-screen title in addition to the action bar.
     * Built using Arrays.asList for Java 8 compatibility (no Set.of).
     */
    private static final Set<EntityType> BOSS_TYPES;
    static {
        // Gather known boss types; types that don't exist on old servers are handled
        // gracefully — EntityType.valueOf will throw and we simply skip them.
        Set<EntityType> s = new HashSet<EntityType>();
        addSafe(s, "ENDER_DRAGON");
        addSafe(s, "WITHER");
        addSafe(s, "WARDEN");        // 1.19+
        addSafe(s, "ELDER_GUARDIAN"); // 1.8+
        BOSS_TYPES = s;
    }

    private static void addSafe(Set<EntityType> set, String name) {
        try {
            set.add(EntityType.valueOf(name));
        } catch (IllegalArgumentException ignored) { /* not on this version */ }
    }

    private final MobMoney plugin;
    private final Random    random = new Random();

    public MobListener(MobMoney plugin) {
        this.plugin = plugin;
    }

    // ── Spawner detection ─────────────────────────────────────────────────────

    /**
     * Tag mobs that come from spawner blocks.
     * CreatureSpawnEvent + SpawnReason.SPAWNER exists since CraftBukkit 1.8.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            event.getEntity().setMetadata(
                    SPAWNER_META,
                    new FixedMetadataValue(plugin, Boolean.TRUE)
            );
        }
    }

    // ── Kill reward ───────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {

        // Must be killed by a player
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        // Permission check
        if (!killer.hasPermission("mobmoney.receive")) return;

        ConfigManager  cfg  = plugin.getConfigManager();
        LanguageManager lang = plugin.getLanguageManager();

        // Skip spawner mobs (anti-farm) if configured
        Entity entity = event.getEntity();
        if (!cfg.isSpawnerMobsReward() && entity.hasMetadata(SPAWNER_META)) return;

        EntityType type = entity.getType();
        Map<EntityType, int[]> rewards = cfg.getMobRewards();
        if (!rewards.containsKey(type)) return;

        // ── Calculate reward ─────────────────────────────────────────────────
        int[] range   = rewards.get(type);
        int   min     = range[0];
        int   max     = range[1];
        int   base    = (min >= max) ? min : min + random.nextInt(max - min + 1);
        long  reward  = Math.round(base * cfg.getGlobalMultiplier());

        // Give money via Vault
        plugin.getEconomy().depositPlayer(killer, reward);

        // Console log
        if (cfg.isLogKills()) {
            plugin.getLogger().info(
                    killer.getName() + " killed " + type.name()
                    + " → +" + reward + " " + cfg.getCurrencySymbol()
            );
        }

        // ── Display ──────────────────────────────────────────────────────────
        String symbol   = cfg.getCurrencySymbol();
        String amount   = formatAmount(reward);
        String mobName  = getMobDisplayName(type, lang);
        boolean isBoss  = BOSS_TYPES.contains(type);

        // Action Bar — shown for every kill
        if (cfg.isActionBarEnabled()) {
            String bar = lang.get("reward.action-bar",
                    "amount", amount,
                    "symbol", symbol,
                    "mob",    mobName);
            DisplayUtil.sendActionBar(killer, bar);
        }

        // Title + chat — only for bosses
        if (isBoss && cfg.isTitleOnBoss()) {
            String title    = lang.get("reward.boss-title");
            String subtitle = lang.get("reward.boss-subtitle",
                    "amount", amount,
                    "symbol", symbol,
                    "mob",    mobName);
            DisplayUtil.sendTitle(killer, title, subtitle, 10, 70, 20);
        }

        // Boss chat message
        if (isBoss) {
            String chat = lang.get("reward.boss-chat",
                    "amount", amount,
                    "symbol", symbol,
                    "mob",    mobName);
            DisplayUtil.sendMessage(killer, chat);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the localised mob name.
     * Falls back to a prettified enum string if the lang file has no entry.
     */
    private String getMobDisplayName(EntityType type, LanguageManager lang) {
        String key        = "mobs." + type.name().toLowerCase();
        String translated = lang.get(key);
        // LanguageManager returns the key itself when nothing is found
        return translated.equals(key) ? formatEnumName(type.name()) : translated;
    }

    /** "WITHER_SKELETON" → "Wither Skeleton" */
    private String formatEnumName(String name) {
        String[] parts = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1));
                sb.append(' ');
            }
        }
        return sb.toString().trim();
    }

    /** 1234567 → "1,234,567" (locale-independent grouping). */
    private String formatAmount(long amount) {
        // NumberFormat is Java 1.1+ — always safe
        NumberFormat nf = NumberFormat.getIntegerInstance();
        return nf.format(amount);
    }
}
