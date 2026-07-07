# DeepTranslate

> LSPosed 全局实时翻译模块，基于 DeepSeek API，Hook 原生 Android 页面文字自动翻译。

[English](README_EN.md)

## 效果展示

| 翻译前 | 翻译后 |
|--------|--------|
| ![origin](assets/images/origin.jpg) | ![trans](assets/images/trans.jpg) |

| App 界面 (中文) | App 界面 (English) |
|----------------|-------------------|
| ![app_zh](assets/images/app_zh.jpg) | ![app_en](assets/images/app_en.jpg) |

## 功能

- **全局文字翻译** -- Hook `TextView.setText()` 拦截外文文字，替换为目标语言
- **语境批处理** -- 聚合同一页面的多条文本一次性发送给 AI，结合上下文理解语义，避免逐词机翻
- **SQLite 翻译缓存** -- 翻译结果持久化，命中缓存零 API 调用、零延迟
- **自动语言识别** -- DeepSeek API 自动检测源语言，无需指定
- **Span 样式保留** -- 翻译后保留原文字的颜色、粗细、字号、对齐等视觉样式
- **Token/额度统计** -- 首页显示 DeepSeek 账户余额和累计 Token 消耗，翻译时弹 Toast
- **缓存详情** -- 按应用显示缓存条数，支持单独或全部清空（通过 root 直删数据库文件）
- **Material 3 UI** -- Flutter 配置面板，毛玻璃效果，自动拉取可用模型列表，8 种语言支持

## 适用范围

| 支持 | 不支持 |
|--------|----------|
| 原生 Android App（Twitter、Telegram、Reddit、Gmail 等） | Flutter App（Skia 直接渲染，无 TextView） |
| LayoutInflater 生成的 UI | React Native / Weex |
| View-based 界面 | WebView 内嵌网页 |

## 使用

1. 安装 APK，在 LSPosed 中启用模块
2. 在 LSPosed 作用域中勾选要翻译的目标 App
3. 打开 DeepTranslate App -> 翻译配置 -> 填入 DeepSeek API Key
4. 首页开启"全局翻译"开关
5. 重启目标 App，页面文字自动翻译

> 首次翻译会触发上下文批处理（收集 100ms 内的多条文本），稍等即见翻译结果。

## 构建

```
flutter pub get
flutter build apk --release
```

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
  +-- TextViewHook          核心: Hook TextView.setText()
  +-- TextBatcher           语境批处理（时间窗口 + 数量上限聚合）
  +-- TranslationEngine     DeepSeek API 调用（OpenAI 兼容协议）
  +-- TranslationCache      SQLite 持久化缓存
  +-- LanguageDetector      快速语言检测（CJK 字符比例）
```

## DeepSeek API

| 项 | 值 |
|---|-----|
| Endpoint | `https://api.deepseek.com/v1/chat/completions` |
| 默认模型 | `deepseek-v4-flash` |
| 协议 | OpenAI 兼容，Bearer Token |
| 价格 | 1 CNY/百万输入 token，2 CNY/百万输出 token |

## 许可

MIT

---

**GitHub**: [github.com/sakukir](https://github.com/sakukir)
