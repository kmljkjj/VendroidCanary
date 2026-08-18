package dev.vendicated.vencord;

public final class Constants {
    private Constants() {}

    /** Vencord browser build (dev) */
    public static final String JS_BUNDLE_URL =
            "https://github.com/Vendicated/Vencord/releases/download/devbuild/browser.js";

    /** Discord Canary web app */
    public static final String DISCORD_APP_URL = "https://canary.discord.com/app";

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
        // capture *.discord.com / *.discordapp.com
        return h.endsWith(".discord.com") || h.endsWith(".discordapp.com")
                || h.endsWith(".discord.gg");
    }
}
