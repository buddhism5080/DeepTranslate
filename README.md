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
- **语境批处理** -- 界面短文本整条打进同一请求；正文按段落和字数切开后再发出去
- **SQLite 翻译缓存** -- 按原文记住译文，相同文本不再请求。双语只在显示时拼上原文，不另存一份
- **双语显示** -- 设置里可打开。短控件是「译文（原文）」，正文是译文后空一行再放原文
- **回退渠道** -- 主渠道按设置重试 0–3 次后，改走另一套 API。模型少返回的条目会单独再试，不把整批丢掉
- **Span 样式保留** -- `TextView.setText` 路径保留颜色、粗细、字号、对齐
- **Material 3 UI** -- 配置面板可分别开关布局文本、WebView、Compose

## 适用范围

| 支持 | 不支持 |
|--------|----------|
| 原生 TextView（Twitter、Telegram、Reddit、Gmail 等） | Flutter（字在 libflutter.so 里由 Skia/Impeller 画出，Java 层没有这段文字） |
| StaticLayout / BoringLayout | Weex |
| React Native 旧 TextView，以及 Fabric 的 PreparedLayoutTextView | |
| Compose 文本（类存在时） | |
| WebView 可见正文 | |

## 使用

1. 安装 APK，在 LSPosed 中启用模块
2. 在 LSPosed 作用域中勾选要翻译的目标 App
3. 打开 DeepTranslate -> 翻译配置。接口可以填 `https://host/v1`，也可以填完整的 `/chat/completions`。API Key 本地服务可留空。模型可以手填，也可以点「获取模型」
4. 首页开启「全局翻译」。设置页可以分别关掉布局文本、WebView、Compose
5. 重启目标 App，页面文字自动翻译

> 首次翻译会等一个很短的聚合窗口。超长正文会拆成多段请求，稍等即见翻译结果。并发、单次控件数、段落数和字符数在「翻译配置」里。

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
  +-- TextBatcher           界面文本和正文分开打包，超长正文按段落/字符切块
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
