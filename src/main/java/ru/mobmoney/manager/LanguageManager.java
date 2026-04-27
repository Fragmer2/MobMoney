package ru.mobmoney.manager;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.mobmoney.MobMoney;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {

    private final MobMoney plugin;
    private FileConfiguration langConfig;

    /** English built-in fallback — the plugin never crashes even without a lang file. */
    private static final Map<String, String> FALLBACK = new HashMap<String, String>();
    static {
        FALLBACK.put("reward.action-bar",      "&a+{amount} {symbol} &8\u00bb &7{mob}");
        FALLBACK.put("reward.boss-title",      "&6&lBOSS SLAIN!");
        FALLBACK.put("reward.boss-subtitle",   "&e+{amount} {symbol} &7for &f{mob}");
        FALLBACK.put("reward.boss-chat",
                "&6[MobMoney] &eYou received &a{amount} {symbol} &efor killing &f{mob}&e!");
        FALLBACK.put("cmd.reload-success",  "&a[MobMoney] Config reloaded successfully.");
        FALLBACK.put("cmd.reload-fail",     "&c[MobMoney] Reload failed. Check console.");
        FALLBACK.put("cmd.no-permission",   "&c[MobMoney] You don\u0027t have permission.");
        FALLBACK.put("cmd.info-header",     "&8&m-------&r &6MobMoney &ev{version} &8&m-------");
        FALLBACK.put("cmd.info-platform",   " &8\u00bb &7Platform: &f{platform}");
        FALLBACK.put("cmd.info-lang",       " &8\u00bb &7Language: &f{lang}");
        FALLBACK.put("cmd.info-mobs",       " &8\u00bb &7Mobs loaded: &f{count}");
        FALLBACK.put("cmd.info-multiplier", " &8\u00bb &7Multiplier: &a{mult}x");
        FALLBACK.put("cmd.mob-reward",      " &8\u00bb &f{mob}: &a{min}&8-&a{max} &f{symbol}");
        FALLBACK.put("cmd.mob-unknown",     "&c[MobMoney] Unknown mob: &f{mob}");
        FALLBACK.put("cmd.usage",           "&7Usage: &f/mobmoney <reload | info [mob]>");
    }

    public LanguageManager(MobMoney plugin) {
        this.plugin = plugin;
        load();
    }

    public void reload() { load(); }

    private void load() {
        String lang     = plugin.getConfigManager().getLanguage().toLowerCase();
        String fileName = "lang/" + lang + ".yml";

        // Extract default lang files from the jar if they don't exist on disk
        for (String l : new String[]{"ru", "en", "uk"}) {
            File f = new File(plugin.getDataFolder(), "lang/" + l + ".yml");
            if (!f.exists()) {
                plugin.saveResource("lang/" + l + ".yml", false);
            }
        }

        File langFile = new File(plugin.getDataFolder(), fileName);
        if (langFile.exists()) {
            langConfig = YamlConfiguration.loadConfiguration(langFile);

            // Merge jar defaults so new keys added in updates are visible
            InputStream is = plugin.getResource(fileName);
            if (is != null) {
                try {
                    YamlConfiguration def = YamlConfiguration.loadConfiguration(
                            new InputStreamReader(is, Charset.forName("UTF-8")));
                    langConfig.setDefaults(def);
                } catch (Exception ignored) { }
            }
            plugin.getLogger().info("Language loaded: " + lang.toUpperCase());
        } else {
            plugin.getLogger().warning(
                    "Language file not found: " + fileName + " — using English fallback.");
            langConfig = null;
        }
    }

    /**
     * Returns a translated, colour-formatted string.
     *
     * @param key          dot-separated config key, e.g. "reward.action-bar"
     * @param replacements alternating key/value pairs: "amount", 500, "mob", "Zombie"
     */
    public String get(String key, Object... replacements) {
        String raw = null;

        if (langConfig != null) {
            raw = langConfig.getString(key);
        }
        if (raw == null) {
            raw = FALLBACK.get(key);
        }
        if (raw == null) {
            raw = key; // absolute last resort
        }

        // Apply placeholder replacements
        if (replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                raw = raw.replace("{" + replacements[i] + "}", String.valueOf(replacements[i + 1]));
            }
        }

        return ChatColor.translateAlternateColorCodes('&', raw);
    }
}
