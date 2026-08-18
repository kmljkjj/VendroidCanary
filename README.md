# Vendroid Canary

Fork of [Vencord/Vendroid](https://github.com/Vencord/Vendroid) aimed at **Discord Canary** (`canary.discord.com`) with WebView tweaks for smoother use.

> Upstream Vendroid is a proof-of-concept (web Discord + Vencord injection). Discord’s mobile website remains limited compared to native mods ([Aliucord](https://github.com/Aliucord/Aliucord), etc.).

## Changes vs upstream

- Loads **Canary** instead of stable
- Allows `canary.discord.com` / `discord.com` navigation inside the WebView
- WebView: hardware layer, DOM storage, cache, media autoplay, wider viewport
- Safer network path (no blanket `StrictMode` permitAll)
- Debuggable WebView only on debug builds
- App id: `app.vendroid.canary`
- CI builds **debug APK** without a release keystore

## Build

```bash
./gradlew app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Or **Actions** → **Build APK** → download artifact.

## License

GPL-3.0 — same as [Vencord/Vendroid](https://github.com/Vencord/Vendroid).
