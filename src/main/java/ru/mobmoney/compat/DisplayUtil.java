package ru.mobmoney.compat;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Version-safe display helpers.
 *
 * Action Bar:
 *   All versions:  player.spigot().sendMessage(ACTION_BAR, ...)  — available since Spigot 1.8
 *   Paper 1.20.6+: also tries player.sendActionBar() (no-ops on old builds)
 *
 * Title:
 *   1.9+:  player.sendTitle(title, subtitle, fadeIn, stay, fadeOut)
 *   1.8:   player.sendTitle(title, subtitle)  — only 2 params
 *   &lt;1.8 / pure CraftBukkit without title method: silently skipped
 */
public final class DisplayUtil {

    /**
     * Cached reference to the 5-param sendTitle method (1.9+).
     * null means we fall back to 2-param (1.8) or skip.
     */
    private static final Method SEND_TITLE_FULL;
    private static final Method SEND_TITLE_SHORT;

    static {
        Method full  = null;
        Method short2 = null;
        try {
            full = Player.class.getMethod("sendTitle",
                    String.class, String.class, int.class, int.class, int.class);
        } catch (NoSuchMethodException ignored) { /* 1.8 */ }

        if (full == null) {
            try {
                short2 = Player.class.getMethod("sendTitle", String.class, String.class);
            } catch (NoSuchMethodException ignored) { /* very old / no title support */ }
        }

        SEND_TITLE_FULL  = full;
        SEND_TITLE_SHORT = short2;
    }

    private DisplayUtil() { }

    // ── Action Bar ────────────────────────────────────────────────────────────

    /**
     * Sends an Action Bar message (the coloured text above the hotbar).
     * Works on Spigot/Paper/Purpur from 1.8 through 1.21.
     */
    public static void sendActionBar(Player player, String message) {
        try {
            // BungeeCord chat API — available in Spigot 1.8+
            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(message)
            );
        } catch (Exception e) {
            // Last resort: plain chat message
            player.sendMessage(message);
        }
    }

    // ── Title ─────────────────────────────────────────────────────────────────

    /**
     * Sends a full-screen Title + Subtitle.
     *
     * @param fadeIn  ticks to fade in  (ignored on 1.8)
     * @param stay    ticks to stay on screen
     * @param fadeOut ticks to fade out (ignored on 1.8)
     */
    public static void sendTitle(Player player,
                                 String title, String subtitle,
                                 int fadeIn, int stay, int fadeOut) {
        if (SEND_TITLE_FULL != null) {
            // 1.9+ — full params
            try {
                SEND_TITLE_FULL.invoke(player, title, subtitle, fadeIn, stay, fadeOut);
                return;
            } catch (Exception ignored) { }
        }

        if (SEND_TITLE_SHORT != null) {
            // 1.8 — no timing params; just title + subtitle
            try {
                SEND_TITLE_SHORT.invoke(player, title, subtitle);
                return;
            } catch (Exception ignored) { }
        }

        // No title support (very old pure CraftBukkit) — fallback to chat
        if (title != null   && !title.isEmpty())    player.sendMessage(title);
        if (subtitle != null && !subtitle.isEmpty()) player.sendMessage(subtitle);
    }

    // ── Chat ──────────────────────────────────────────────────────────────────

    /** Simple coloured chat message — always available. */
    public static void sendMessage(Player player, String message) {
        player.sendMessage(message);
    }
}
