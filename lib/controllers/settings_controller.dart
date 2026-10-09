import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

// ── Pref Keys ──────────────────────────────────────────────────────────────

const kPrefTranslationEnabled = 'pref_translation_enabled';
const kPrefAiUrl = 'pref_ai_url';
const kPrefAiApiKey = 'pref_ai_api_key';
const kPrefAiModel = 'pref_ai_model';
const kPrefAiPrompt = 'pref_ai_prompt';
const kPrefAiTargetLang = 'pref_ai_target_lang';
const kPrefAiTimeout = 'pref_ai_timeout';
const kPrefAiTemperature = 'pref_ai_temperature';
const kPrefAiMaxTokens = 'pref_ai_max_tokens';
const kPrefBatchSize = 'pref_batch_size';
const kPrefBatchWindowMs = 'pref_batch_window_ms';
const kPrefConcurrency = 'pref_concurrency';
const kPrefMaxParagraphs = 'pref_max_paragraphs';
const kPrefMaxChars = 'pref_max_chars';
const kPrefBilingual = 'pref_bilingual';
const kPrefCacheEnabled = 'pref_cache_enabled';
const kPrefTranslateToast = 'pref_translate_toast';
const kPrefHookLayout = 'pref_hook_layout';
const kPrefHookWebView = 'pref_hook_webview';
const kPrefHookCompose = 'pref_hook_compose';
const kPrefAppWhitelist = 'pref_app_whitelist';
const kPrefThemeMode = 'pref_theme_mode';
const kPrefThemeSeedColor = 'pref_theme_seed_color';
const kPrefBlurBars = 'pref_blur_bars';
const kPrefDebugLog = 'pref_debug_log';
const kPrefOnboardingCompleted = 'pref_onboarding_completed';

const kDefaultAiUrl = 'https://api.deepseek.com/v1';
const kDefaultAiModel = 'deepseek-v4-flash';
const kDefaultTargetLang = '中文';
const kDefaultPrompt = '''你是一个专业翻译引擎。请将用户提供的文本翻译为中文。

规则：
1. 自动识别每条文本的源语言
2. 结合整批文本的上下文理解语境，确保翻译准确自然
3. 不要逐字翻译，要理解完整语义后再翻译
4. 保持原文的格式（换行、标点风格）
5. 如果文本已经是中文，原样返回
6. 如果文本是专有名词、品牌名、代码、URL，保持不变
7. 只返回 JSON 数组，不要包含任何其他文字

输出格式（严格 JSON）：
[
  {"id": 0, "translation": "译文", "lang": "源语言代码"},
  {"id": 1, "translation": "译文", "lang": "源语言代码"}
]''';

class SettingsController extends ChangeNotifier {
  static final SettingsController instance = SettingsController._();
  SharedPreferences? _prefs;

  SettingsController._() {
    _load();
  }

  // ── 状态字段 ──────────────────────────────────────────────────────────────
  bool translationEnabled = false;
  String aiUrl = kDefaultAiUrl;
  String aiApiKey = '';
  String aiModel = kDefaultAiModel;
  String aiPrompt = kDefaultPrompt;
  String aiTargetLang = kDefaultTargetLang;
  int aiTimeout = 10;
  double aiTemperature = 0.1;
  int aiMaxTokens = 4096;
  int batchSize = 20;
  int batchWindowMs = 100;
  int concurrency = 3;
  int maxParagraphs = 6;
  int maxChars = 1800;
  bool bilingual = false;
  bool cacheEnabled = true;
  bool translateToast = true;
  bool hookLayout = true;
  bool hookWebView = true;
  bool hookCompose = true;
  List<String> appWhitelist = [];
  ThemeMode themeMode = ThemeMode.system;
  int themeSeedColor = 0xFF6750A4;
  bool blurBars = true;
  bool debugLog = false;
  bool onboardingCompleted = false;
  Locale? locale;
  bool loading = true;

  Future<SharedPreferences> _getPrefs() async {
    final cached = _prefs;
    if (cached != null) return cached;
    final prefs = await SharedPreferences.getInstance();
    _prefs = prefs;
    return prefs;
  }

  Future<void> _load() async {
    final prefs = await _getPrefs();
    translationEnabled = prefs.getBool(kPrefTranslationEnabled) ?? false;
    aiUrl = prefs.getString(kPrefAiUrl) ?? kDefaultAiUrl;
    aiApiKey = prefs.getString(kPrefAiApiKey) ?? '';
    aiModel = prefs.getString(kPrefAiModel) ?? kDefaultAiModel;
    aiPrompt = prefs.getString(kPrefAiPrompt) ?? kDefaultPrompt;
    aiTargetLang = prefs.getString(kPrefAiTargetLang) ?? kDefaultTargetLang;
    aiTimeout = prefs.getInt(kPrefAiTimeout) ?? 10;
    aiTemperature = prefs.getDouble(kPrefAiTemperature) ?? 0.1;
    aiMaxTokens = prefs.getInt(kPrefAiMaxTokens) ?? 4096;
    batchSize = prefs.getInt(kPrefBatchSize) ?? 20;
    batchWindowMs = prefs.getInt(kPrefBatchWindowMs) ?? 100;
    concurrency = prefs.getInt(kPrefConcurrency) ?? 3;
    maxParagraphs = prefs.getInt(kPrefMaxParagraphs) ?? 6;
    maxChars = prefs.getInt(kPrefMaxChars) ?? 1800;
    bilingual = prefs.getBool(kPrefBilingual) ?? false;
    cacheEnabled = prefs.getBool(kPrefCacheEnabled) ?? true;
    translateToast = prefs.getBool(kPrefTranslateToast) ?? true;
    hookLayout = prefs.getBool(kPrefHookLayout) ?? true;
    hookWebView = prefs.getBool(kPrefHookWebView) ?? true;
    hookCompose = prefs.getBool(kPrefHookCompose) ?? true;
    appWhitelist = prefs.getStringList(kPrefAppWhitelist) ?? [];
    themeMode = ThemeMode.values[prefs.getInt(kPrefThemeMode) ?? 0];
    themeSeedColor = prefs.getInt(kPrefThemeSeedColor) ?? 0xFF6750A4;
    blurBars = prefs.getBool(kPrefBlurBars) ?? true;
    debugLog = prefs.getBool(kPrefDebugLog) ?? false;
    onboardingCompleted = prefs.getBool(kPrefOnboardingCompleted) ?? false;
    final localeStr = prefs.getString('pref_locale');
    locale = localeStr != null && localeStr.isNotEmpty ? Locale(localeStr) : null;
    loading = false;
    notifyListeners();
  }

  // ── Setters ──────────────────────────────────────────────────────────────

  Future<void> setTranslationEnabled(bool value) async {
    if (translationEnabled == value) return;
    translationEnabled = value;
    final prefs = await _getPrefs();
    await prefs.setBool(kPrefTranslationEnabled, value);
    notifyListeners();
  }

  Future<void> setAiUrl(String value) async {
    if (aiUrl == value) return;
    aiUrl = value;
    final prefs = await _getPrefs();
    await prefs.setString(kPrefAiUrl, value);
    notifyListeners();
  }

  Future<void> setAiApiKey(String value) async {
    if (aiApiKey == value) return;
    aiApiKey = value;
    final prefs = await _getPrefs();
    await prefs.setString(kPrefAiApiKey, value);
    notifyListeners();
  }

  Future<void> setAiModel(String value) async {
    if (aiModel == value) return;
    aiModel = value;
    final prefs = await _getPrefs();
    await prefs.setString(kPrefAiModel, value);
    notifyListeners();
  }

  Future<void> setAiPrompt(String value) async {
    if (aiPrompt == value) return;
    aiPrompt = value;
    final prefs = await _getPrefs();
    await prefs.setString(kPrefAiPrompt, value);
    notifyListeners();
  }

  Future<void> setAiTargetLang(String value) async {
    if (aiTargetLang == value) return;
    aiTargetLang = value;
    final prefs = await _getPrefs();
    await prefs.setString(kPrefAiTargetLang, value);
    notifyListeners();
  }

  Future<void> setAiTimeout(int value) async {
    if (aiTimeout == value) return;
    aiTimeout = value;
    final prefs = await _getPrefs();
    await prefs.setInt(kPrefAiTimeout, value);
    notifyListeners();
  }

  Future<void> setAiTemperature(double value) async {
    if (aiTemperature == value) return;
    aiTemperature = value;
    final prefs = await _getPrefs();
    await prefs.setDouble(kPrefAiTemperature, value);
    notifyListeners();
  }

  Future<void> setAiMaxTokens(int value) async {
    if (aiMaxTokens == value) return;
    aiMaxTokens = value;
    final prefs = await _getPrefs();
    await prefs.setInt(kPrefAiMaxTokens, value);
    notifyListeners();
  }

  Future<void> setBatchSize(int value) async {
    if (batchSize == value) return;
    batchSize = value;
    final prefs = await _getPrefs();
    await prefs.setInt(kPrefBatchSize, value);
    notifyListeners();
  }

  Future<void> setBatchWindowMs(int value) async {
    if (batchWindowMs == value) return;
    batchWindowMs = value;
    final prefs = await _getPrefs();
    await prefs.setInt(kPrefBatchWindowMs, value);
    notifyListeners();
  }

  Future<void> setConcurrency(int value) async {
    if (concurrency == value) return;
    concurrency = value;
    final prefs = await _getPrefs();
    await prefs.setInt(kPrefConcurrency, value);
    notifyListeners();
  }

  Future<void> setMaxParagraphs(int value) async {
    if (maxParagraphs == value) return;
    maxParagraphs = value;
    final prefs = await _getPrefs();
    await prefs.setInt(kPrefMaxParagraphs, value);
    notifyListeners();
  }

  Future<void> setMaxChars(int value) async {
    if (maxChars == value) return;
    maxChars = value;
    final prefs = await _getPrefs();
    await prefs.setInt(kPrefMaxChars, value);
    notifyListeners();
  }

  Future<void> setBilingual(bool value) async {
    if (bilingual == value) return;
    bilingual = value;
    final prefs = await _getPrefs();
    await prefs.setBool(kPrefBilingual, value);
    notifyListeners();
  }

  Future<void> setCacheEnabled(bool value) async {
    if (cacheEnabled == value) return;
    cacheEnabled = value;
    final prefs = await _getPrefs();
    await prefs.setBool(kPrefCacheEnabled, value);
    notifyListeners();
  }

  Future<void> setTranslateToast(bool value) async {
    if (translateToast == value) return;
    translateToast = value;
    final prefs = await _getPrefs();
    await prefs.setBool(kPrefTranslateToast, value);
    notifyListeners();
  }

  Future<void> setHookLayout(bool value) async {
    if (hookLayout == value) return;
    hookLayout = value;
    final prefs = await _getPrefs();
    await prefs.setBool(kPrefHookLayout, value);
    notifyListeners();
  }

  Future<void> setHookWebView(bool value) async {
    if (hookWebView == value) return;
    hookWebView = value;
    final prefs = await _getPrefs();
    await prefs.setBool(kPrefHookWebView, value);
    notifyListeners();
  }

  Future<void> setHookCompose(bool value) async {
    if (hookCompose == value) return;
    hookCompose = value;
    final prefs = await _getPrefs();
    await prefs.setBool(kPrefHookCompose, value);
    notifyListeners();
  }

  Future<void> setAppWhitelist(List<String> value) async {
    appWhitelist = value;
    final prefs = await _getPrefs();
    await prefs.setStringList(kPrefAppWhitelist, value);
    notifyListeners();
  }

  Future<void> setThemeMode(ThemeMode value) async {
    themeMode = value;
    final prefs = await _getPrefs();
    await prefs.setInt(kPrefThemeMode, value.index);
    notifyListeners();
  }

  Future<void> setThemeSeedColor(int value) async {
    themeSeedColor = value;
    final prefs = await _getPrefs();
    await prefs.setInt(kPrefThemeSeedColor, value);
    notifyListeners();
  }

  Future<void> setBlurBars(bool value) async {
    blurBars = value;
    final prefs = await _getPrefs();
    await prefs.setBool(kPrefBlurBars, value);
    notifyListeners();
  }

  Future<void> setDebugLog(bool value) async {
    debugLog = value;
    final prefs = await _getPrefs();
    await prefs.setBool(kPrefDebugLog, value);
    notifyListeners();
  }

  Future<void> setLocale(Locale? loc) async {
    locale = loc;
    final prefs = await _getPrefs();
    await prefs.setString('pref_locale', loc?.languageCode ?? '');
    notifyListeners();
  }

  Future<void> completeOnboarding() async {
    onboardingCompleted = true;
    final prefs = await _getPrefs();
    await prefs.setBool(kPrefOnboardingCompleted, true);
    notifyListeners();
  }
}
