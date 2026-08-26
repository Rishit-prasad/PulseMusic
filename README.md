<div align="center">

# 🎵 Pulse

**A free, open-source YouTube Music client for Android & Desktop**
*Your music, your pulse.*

[![License](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%20%7C%20Desktop-green.svg)]()

</div>

---

## About

Pulse is a music streaming app built on top of the excellent [SimpMusic](https://github.com/maxrave-dev/SimpMusic) project by [maxrave-dev](https://github.com/maxrave-dev). It inherits SimpMusic's full feature set — YouTube Music streaming, background playback, synced lyrics, crossfade, and more — while evolving its own UI design and feature set.

## Credits & License

This project is a derivative of [SimpMusic](https://github.com/maxrave-dev/SimpMusic).

- Upstream project: https://github.com/maxrave-dev/SimpMusic — huge thanks to [maxrave-dev](https://github.com/maxrave-dev) and all SimpMusic contributors ❤️
- Licensed under the **GNU General Public License v3.0** — see [LICENSE](LICENSE). All upstream copyright notices are preserved in the git history.
- SimpMusic itself was inspired by [InnerTune](https://github.com/z-huang/InnerTune) and [SmartTube](https://github.com/yuliskov/SmartTube).

## Features (inherited from SimpMusic)

- Play music from YouTube Music or YouTube for free, without ads, in the background
- Home, Charts, Podcasts, Moods & Genres browsing
- Spotify Canvas, synced lyrics (SimpMusic Lyrics / LRCLIB / Spotify / BetterLyrics)
- Crossfade & DJ-style transitions, sleep timer with fade-out
- Offline caching & downloads, local playlists, favorites
- AI song suggestions & lyrics translation (bring your own OpenAI/Gemini key)
- SponsorBlock, Return YouTube Dislike, Discord Rich Presence
- Android Auto support
- Desktop app for Windows, macOS & Linux (libmpv-based playback)

## Building

Requirements: **JDK 17+**, Android Studio (for Android builds).

```bash
# Clone with the `core` submodule (required — the whole core layer lives there)
git clone --recurse-submodules https://github.com/Rishit-prasad/PulseMusic.git

# Already cloned without submodules?
git submodule update --init --recursive

# Android debug APK
./gradlew :androidApp:assembleDebug

# Desktop run
./gradlew :composeApp:run
```

## Roadmap

Pulse development happens feature-by-feature on `main`:

- [ ] Rebrand: app name, launcher icon, color scheme
- [ ] Custom UI redesign (navigation, player screen, themes)
- [ ] New features, one by one

## Disclaimer

Pulse acts as a client for publicly available YouTube/YouTube Music content and hosts no media itself. This project is non-commercial and for personal/educational use. Please support artists by subscribing to YouTube Premium.
