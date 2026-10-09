# DeepTranslate

> LSPosed 全局实时翻译模块。Hook 原生界面文字，接口是任意 OpenAI 兼容 API。

[English](README_EN.md)

## 效果展示

| 翻译前 | 翻译后 |
|--------|--------|
| ![origin](assets/images/origin.jpg) | ![trans](assets/images/trans.jpg) |

| App 界面 (中文) | App 界面 (English) |
|----------------|-------------------|
| ![app_zh](assets/images/app_zh.jpg) | ![app_en](assets/images/app_en.jpg) |

## 功能

- **全局文字翻译** -- Hook `TextView.setText()`，并补上 `StaticLayout` / `BoringLayout`
- **Compose 文本** -- 目标 App 带 Compose 时 Hook `TextLayout` / `AndroidParagraphIntrinsics`
- **WebView** -- 翻译网页里的可见文本，跳过输入框、脚本和样式
- **自定义 OpenAI 兼容接口** -- 可填 base URL（`https://host/v1`）或完整的 `/chat/completions`
- **自定义模型** -- 手填模型名，或从 `/models` 拉取后选择。API Key 对本地服务可以留空
- **语境批处理** -- 聚合同一页面的多条文本一次性发送，结合上下文翻译
- **SQLite 翻译缓存** -- 命中缓存零 API 调用。译文会记下来，避免刷新后再送去翻译
- **Span 样式保留** -- `TextView.setText` 路径保留颜色、粗细、字号、对齐
- **Material 3 UI** -- 配置面板可分别开关布局文本、WebView、Compose

## 适用范围

| 支持 | 不支持 |
|--------|----------|
| 原生 TextView（Twitter、Telegram、Reddit、Gmail 等） | Flutter（Skia 直接渲染，没有 TextView） |
| StaticLayout / BoringLayout | React Native / Weex |
| Compose 文本（类存在时） | |
| WebView 可见正文 | |

## 使用

1. 安装 APK，在 LSPosed 中启用模块
2. 在 LSPosed 作用域中勾选要翻译的目标 App
3. 打开 DeepTranslate -> 翻译配置。接口可以填 `https://host/v1`，也可以填完整的 `/chat/completions`。API Key 本地服务可留空。模型可以手填，也可以点「获取模型」
4. 首页开启「全局翻译」。设置页可以分别关掉布局文本、WebView、Compose
5. 重启目标 App，页面文字自动翻译

> 首次翻译会触发上下文批处理（收集 100ms 内的多条文本），稍等即见翻译结果。

> **切换目标语言**：修改"翻译配置 -> 翻译 Prompt"中的目标语言即可。例如将 Prompt 中的"翻译为中文"改为"翻译为日语"、"translate to Spanish" 等。

## 构建

```
flutter pub get
flutter build apk --release
```

推到 `main` 时 GitHub Actions 也会编。APK 在该次运行的 Artifacts 里，文件名 `deeptranslate-apk`，不会自动发 Release。没有签名密钥时用 debug 签名。

依赖:

- Flutter SDK >= 3.9
- JDK 21
- Android SDK (compileSdk 37)
- LibXposed API 102.0.0

## 架构

```
Flutter UI (lib/)          配置面板（Material 3）
     |  SharedPreferences
XposedService              RemotePreferences 跨进程同步
     |
Kotlin Hook (android/)     运行在被翻译 App 的进程中
  +-- DeepTranslateModule   XposedModule 入口，注册广播接收器
  +-- TextViewHook          Hook TextView.setText()
  +-- LayoutHook            StaticLayout / BoringLayout / Compose
  +-- WebViewHook           WebView 可见文本
  +-- TextBatcher           语境批处理（时间窗口 + 数量上限聚合）
  +-- TranslationEngine     OpenAI 兼容 chat/completions
  +-- TranslationCache      SQLite 持久化缓存
  +-- LanguageDetector      快速语言检测（CJK 字符比例）
```

## OpenAI 兼容接口

可填下面任意一种：

| 填写 | 实际请求 |
|---|---|
| `https://api.deepseek.com/v1` | `…/v1/chat/completions`，模型列表 `…/v1/models` |
| `https://api.openai.com/v1/chat/completions` | 原样作为对话接口，模型列表换成同级 `/models` |
| `http://127.0.0.1:11434/v1` | 本地 OpenAI 兼容服务，API Key 可留空 |

默认模型仍是 `deepseek-v4-flash`，可以改成接口返回的任意模型名。协议是 OpenAI chat completions，Bearer Token。

## 许可

MIT
