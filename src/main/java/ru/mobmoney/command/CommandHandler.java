package ru.mobmoney.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import ru.mobmoney.MobMoney;
import ru.mobmoney.compat.EntityTypeHelper;
import ru.mobmoney.compat.VersionHelper;
import ru.mobmoney.manager.ConfigManager;
import ru.mobmoney.manager.LanguageManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommandHandler implements CommandExecutor, TabCompleter {

    private final MobMoney plugin;

    public CommandHandler(MobMoney plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {

        LanguageManager lang = plugin.getLanguageManager();

        if (args.length == 0) {
            sendHelp(sender, lang);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("reload")) {
            handleReload(sender, lang);

        } else if (sub.equals("info")) {
            if (args.length >= 2) {
                showMobInfo(sender, args[1], lang);
            } else {
                showGeneralInfo(sender, lang);
            }

        } else {
            sendHelp(sender, lang);
        }

        return true;
    }

    // ── Sub-command handlers ──────────────────────────────────────────────────

    private void handleReload(CommandSender sender, LanguageManager lang) {
        if (!sender.hasPermission("mobmoney.admin")) {
            sender.sendMessage(lang.get("cmd.no-permission"));
            return;
        }
        try {
            plugin.reload();
            sender.sendMessage(lang.get("cmd.reload-success"));
        } catch (Exception e) {
            sender.sendMessage(lang.get("cmd.reload-fail"));
            plugin.getLogger().severe("Reload error: " + e.getMessage());
        }
    }

    private void showGeneralInfo(CommandSender sender, LanguageManager lang) {
        ConfigManager cfg = plugin.getConfigManager();
        sender.sendMessage(lang.get("cmd.info-header",
                "version", plugin.getDescription().getVersion()));
        sender.sendMessage(lang.get("cmd.info-platform",
                "platform", VersionHelper.getPlatformName()
                        + " 1." + VersionHelper.getMinor()
                        + (VersionHelper.getPatch() > 0 ? "." + VersionHelper.getPatch() : "")));
        sender.sendMessage(lang.get("cmd.info-lang",
                "lang", cfg.getLanguage().toUpperCase(Locale.ROOT)));
        sender.sendMessage(lang.get("cmd.info-mobs",
                "count", cfg.getMobRewards().size()));
        sender.sendMessage(lang.get("cmd.info-multiplier",
                "mult", cfg.getGlobalMultiplier()));
    }

    private void showMobInfo(CommandSender sender, String mobArg, LanguageManager lang) {
        ConfigManager cfg = plugin.getConfigManager();

        // Use the alias-aware resolver
        EntityType type = EntityTypeHelper.resolve(
                mobArg.toUpperCase(Locale.ROOT), plugin.getLogger());

        if (type == null) {
            sender.sendMessage(lang.get("cmd.mob-unknown", "mob", mobArg));
            return;
        }

        int[] range = cfg.getMobRewards().get(type);
        if (range == null) {
            sender.sendMessage(lang.get("cmd.mob-unknown", "mob", mobArg));
            return;
        }

        NumberFormat nf = NumberFormat.getIntegerInstance();
        sender.sendMessage(lang.get("cmd.mob-reward",
                "mob",    type.name(),
                "min",    nf.format(range[0]),
                "max",    nf.format(range[1]),
                "symbol", cfg.getCurrencySymbol()));
    }

    private void sendHelp(CommandSender sender, LanguageManager lang) {
        sender.sendMessage(lang.get("cmd.usage"));
    }

    // ── Tab completion ────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        List<String> result = new ArrayList<String>();

        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            for (String sub : new String[]{"reload", "info"}) {
                if (sub.startsWith(input)) result.add(sub);
            }

        } else if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            String input = args[1].toLowerCase(Locale.ROOT);
            for (EntityType t : plugin.getConfigManager().getMobRewards().keySet()) {
                String name = t.name().toLowerCase(Locale.ROOT);
                if (name.startsWith(input)) result.add(name);
            }
        }

        return result;
    }
}
