# Vendroid Canary

[Vencord/Vendroid](https://github.com/Vencord/Vendroid) with **one** change:

- loads `https://canary.discord.com/app` instead of `https://discord.com/app`

Everything else follows upstream Vendroid (GPL-3.0).

## Build

**Actions** → **Build APK** → download the zip → install the `.apk` inside.

Or: `./gradlew app:assembleDebug`
