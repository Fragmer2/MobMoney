package ru.mobmoney.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import ru.mobmoney.MobMoney;
import ru.mobmoney.compat.EntityTypeHelper;

import java.util.EnumMap;
import java.util.Map;

public class ConfigManager {

    private final MobMoney plugin;

    // EntityType → [min, max]  rewards
    private final Map<EntityType, int[]> mobRewards = new EnumMap<EntityType, int[]>(EntityType.class);

    // ── Settings fields ───────────────────────────────────────────────────────
    private String  language;
    private boolean actionBarEnabled;
    private boolean titleOnBoss;
    private boolean spawnerMobsReward;
    private boolean logKills;
    private double  globalMultiplier;
    private String  currencySymbol;

    public ConfigManager(MobMoney plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        load();
    }

    public void reload() {
        plugin.reloadConfig();
        mobRewards.clear();
        load();
    }

    private void load() {
        FileConfiguration cfg = plugin.getConfig();

        language          = cfg.getString("settings.language",        "ru");
        actionBarEnabled  = cfg.getBoolean("settings.action-bar",     true);
        titleOnBoss       = cfg.getBoolean("settings.title-on-boss",  true);
        spawnerMobsReward = cfg.getBoolean("settings.spawner-mobs-reward", false);
        logKills          = cfg.getBoolean("settings.log-kills",      false);
        globalMultiplier  = cfg.getDouble("settings.global-multiplier", 1.0);
        currencySymbol    = cfg.getString("settings.currency-symbol", "\u20B4"); // ₴

        ConfigurationSection mobs = cfg.getConfigurationSection("mobs");
        if (mobs == null) {
            plugin.getLogger().warning("No 'mobs' section in config.yml!");
            return;
        }

        int loaded = 0, skipped = 0;
        for (String key : mobs.getKeys(false)) {
            EntityType type = EntityTypeHelper.resolve(key, plugin.getLogger());
            if (type == null) {
                skipped++;
                continue;
            }
            int min = mobs.getInt(key + ".min", 0);
            int max = mobs.getInt(key + ".max", 0);
            if (min > max) {
                plugin.getLogger().warning("Config: mob '" + key + "' has min > max, swapping.");
                int tmp = min; min = max; max = tmp;
            }
            mobRewards.put(type, new int[]{min, max});
            loaded++;
        }
        plugin.getLogger().info("Loaded " + loaded + " mob reward(s)"
                + (skipped > 0 ? " (" + skipped + " skipped — not on this version)" : "") + ".");
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Map<EntityType, int[]> getMobRewards()  { return mobRewards; }
    public String  getLanguage()                   { return language; }
    public boolean isActionBarEnabled()            { return actionBarEnabled; }
    public boolean isTitleOnBoss()                 { return titleOnBoss; }
    public boolean isSpawnerMobsReward()           { return spawnerMobsReward; }
    public boolean isLogKills()                    { return logKills; }
    public double  getGlobalMultiplier()           { return globalMultiplier; }
    public String  getCurrencySymbol()             { return currencySymbol; }
}
