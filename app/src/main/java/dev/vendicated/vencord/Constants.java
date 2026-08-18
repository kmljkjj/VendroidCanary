package dev.vendicated.vencord;

public final class Constants {
    private Constants() {}

    /** Vencord browser build (dev) */
    public static final String JS_BUNDLE_URL =
            "https://github.com/Vendicated/Vencord/releases/download/devbuild/browser.js";

    /** Discord Canary — desktop web app (mobile web breaks User Settings) */
    public static final String DISCORD_APP_URL = "https://canary.discord.com/channels/@me";

    /**
     * Desktop Chrome UA. Mobile UA forces Discord's broken mobile web layout
     * (User Settings → "Discord a cessé de fonctionner").
     */
    public static final String DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    public static final String[] DISCORD_HOSTS = {
            "canary.discord.com",
            "discord.com",
            "discordapp.com",
            "cdn.discordapp.com",
            "media.discordapp.net",
            "gateway.discord.gg"
    };

    public static boolean isDiscordHost(String host) {
        if (host == null) return false;
        String h = host.toLowerCase();
        for (String allowed : DISCORD_HOSTS) {
            if (h.equals(allowed) || h.endsWith("." + allowed)) return true;
        }
        return h.endsWith(".discord.com")
                || h.endsWith(".discordapp.com")
                || h.endsWith(".discord.gg");
    }
}
