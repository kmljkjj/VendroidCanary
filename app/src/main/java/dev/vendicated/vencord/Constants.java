package dev.vendicated.vencord;

public final class Constants {
    private Constants() {}

    public static final String JS_BUNDLE_URL =
            "https://github.com/Vendicated/Vencord/releases/download/devbuild/browser.js";

    /** Same entry as first working build */
    public static final String DISCORD_APP_URL = "https://canary.discord.com/app";

    public static boolean isDiscordHost(String host) {
        if (host == null) return false;
        String h = host.toLowerCase();
        return h.equals("canary.discord.com")
                || h.equals("discord.com")
                || h.equals("discordapp.com")
                || h.endsWith(".discord.com")
                || h.endsWith(".discordapp.com")
                || h.endsWith(".discord.gg");
    }
}
