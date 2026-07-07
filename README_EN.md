# DeepTranslate

> LSPosed real-time translation module powered by DeepSeek API. Hooks native Android UI text for automatic translation.

[中文](README.md)

## Screenshots

| Before | After |
|--------|-------|
| ![origin](assets/images/origin.jpg) | ![trans](assets/images/trans.jpg) |

| App UI (Chinese) | App UI (English) |
|------------------|------------------|
| ![app_zh](assets/images/app_zh.jpg) | ![app_en](assets/images/app_en.jpg) |

## Features

- **Global Text Translation** -- Hooks `TextView.setText()` to intercept and replace foreign text
- **Contextual Batching** -- Aggregates multiple text fragments from the same page into one AI request, preserving semantic context and avoiding word-by-word machine translation errors
- **SQLite Translation Cache** -- Persistent caching with zero API calls and zero latency on cache hit
- **Automatic Language Detection** -- DeepSeek API auto-detects source language, no manual specification needed
- **Span Style Preservation** -- Retains original text styling (color, weight, size, alignment) after translation
- **Token/Balance Tracking** -- Home screen shows DeepSeek account balance and cumulative token usage with toast notifications
- **Cache Details** -- Per-app cache entry counts with individual or bulk clear support (root-based database deletion)
- **Material 3 UI** -- Flutter configuration panel with frosted glass effects, auto model list fetching, 8 language support

## Compatibility

| Supported | Not Supported |
|-----------|---------------|
| Native Android Apps (Twitter, Telegram, Reddit, Gmail, etc.) | Flutter Apps (Skia direct rendering, no TextView) |
| LayoutInflater-generated UI | React Native / Weex |
| View-based interfaces | WebView embedded pages |

## Usage

1. Install APK, enable the module in LSPosed
2. Select target apps in LSPosed scope settings
3. Open DeepTranslate App -> Config -> enter your DeepSeek API Key
4. Toggle "Global Translation" on the home screen
5. Restart target apps, text will translate automatically

> First translation triggers contextual batching (aggregates text within a 100ms window). Results appear shortly.

## Build

```
flutter pub get
flutter build apk --release
```

Requirements:

- Flutter SDK >= 3.9
- JDK 21
- Android SDK (compileSdk 37)
- LibXposed API 102.0.0

## Architecture

```
Flutter UI (lib/)          Configuration panel (Material 3)
     |  SharedPreferences
XposedService              RemotePreferences cross-process sync
     |
Kotlin Hook (android/)     Runs inside target app processes
  +-- DeepTranslateModule   XposedModule entry, registers broadcast receivers
  +-- TextViewHook          Core: hooks TextView.setText()
  +-- TextBatcher           Contextual batching (time window + count limit)
  +-- TranslationEngine     DeepSeek API calls (OpenAI-compatible protocol)
  +-- TranslationCache      SQLite persistent cache
  +-- LanguageDetector      Fast language detection (CJK character ratio)
```

## DeepSeek API

| Item | Value |
|------|-------|
| Endpoint | `https://api.deepseek.com/v1/chat/completions` |
| Default Model | `deepseek-v4-flash` |
| Protocol | OpenAI-compatible, Bearer Token |
| Pricing | 1 CNY/M input tokens, 2 CNY/M output tokens |

## License

MIT

---

**GitHub**: [github.com/sakukir](https://github.com/sakukir)
