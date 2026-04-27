package ru.mobmoney;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import ru.mobmoney.command.CommandHandler;
import ru.mobmoney.compat.VersionHelper;
import ru.mobmoney.listener.MobListener;
import ru.mobmoney.manager.ConfigManager;
import ru.mobmoney.manager.LanguageManager;

public class MobMoney extends JavaPlugin {

    private static MobMoney instance;
    private Economy         economy;
    private ConfigManager   configManager;
    private LanguageManager languageManager;

    @Override
    public void onEnable() {
        instance = this;

        // Detect platform and version before anything else
        getLogger().info("Platform : " + VersionHelper.getPlatformName()
                + "  MC 1." + VersionHelper.getMinor()
                + (VersionHelper.getPatch() > 0 ? "." + VersionHelper.getPatch() : "")
                + (VersionHelper.isFolia() ? "  [Folia]" : ""));

        // Load config and language (config must come first)
        configManager   = new ConfigManager(this);
        languageManager = new LanguageManager(this);

        // Vault economy
        if (!setupEconomy()) {
            getLogger().severe("================================================");
            getLogger().severe("  Vault not found or no economy plugin loaded!  ");
            getLogger().severe("  MobMoney is DISABLING itself.                 ");
            getLogger().severe("================================================");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Events
        getServer().getPluginManager().registerEvents(new MobListener(this), this);

        // Commands
        CommandHandler handler = new CommandHandler(this);
        getCommand("mobmoney").setExecutor(handler);
        getCommand("mobmoney").setTabCompleter(handler);

        getLogger().info("MobMoney v" + getDescription().getVersion() + " enabled — "
                + configManager.getMobRewards().size() + " mob(s) configured.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MobMoney disabled.");
    }

    /** Hot-reload: config + language, no server restart required. */
    public void reload() {
        configManager.reload();
        languageManager.reload();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    // ── Static accessor (safe to call from any thread) ────────────────────────

    public static MobMoney getInstance()            { return instance; }
    public Economy         getEconomy()             { return economy; }
    public ConfigManager   getConfigManager()       { return configManager; }
    public LanguageManager getLanguageManager()     { return languageManager; }
}
