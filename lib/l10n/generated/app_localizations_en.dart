// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get appTitle => 'DeepTranslate';

  @override
  String get navHome => 'Home';

  @override
  String get navTranslateConfig => 'Config';

  @override
  String get navSettings => 'Settings';

  @override
  String get moduleStatus => 'Module Status';

  @override
  String get activated => 'Activated';

  @override
  String get notActivated => 'Not Activated';

  @override
  String get enableInLSPosed => 'Enable in LSPosed and select target apps';

  @override
  String get translationStatus => 'Translation';

  @override
  String get globalTranslation => 'Global Translation';

  @override
  String get globalTranslationDesc =>
      'Hook and translate text in all selected apps';

  @override
  String get accountBalance => 'Account Balance';

  @override
  String get noApiKey => 'API Key not configured';

  @override
  String get fetchFailed => 'Fetch failed';

  @override
  String get totalTokens => 'Total Tokens Used';

  @override
  String get resetStats => 'Reset Stats';

  @override
  String get resetTokenTitle => 'Reset Token Stats';

  @override
  String get resetTokenContent =>
      'Are you sure you want to reset the token count? This cannot be undone.';

  @override
  String get cancel => 'Cancel';

  @override
  String get confirm => 'Confirm';

  @override
  String get usageNotes => 'Notes';

  @override
  String get note1 => '1. Set an OpenAI-compatible API URL and model in Config';

  @override
  String get note2 => '2. Enable the module in LSPosed, select target apps';

  @override
  String get note3 => '3. Restart target apps, text will auto-translate';

  @override
  String get note4 => '4. Translations are cached to avoid repeated API calls';

  @override
  String get note5 =>
      '5. Batch mode aggregates context to avoid word-by-word errors';

  @override
  String get refresh => 'Refresh';

  @override
  String get apiConfig => 'API Config';

  @override
  String get apiUrl => 'API URL';

  @override
  String get apiKey => 'API Key';

  @override
  String get model => 'Model';

  @override
  String get testConn => 'Test Connection';

  @override
  String get testing => 'Testing...';

  @override
  String get fetchModels => 'Fetch Models';

  @override
  String get translatePrompt => 'Translation Prompt';

  @override
  String get timeout => 'Timeout';

  @override
  String get temperature => 'Temperature';

  @override
  String get maxTokens => 'Max Tokens';

  @override
  String get save => 'Save';

  @override
  String get configSaved => 'Configuration saved';

  @override
  String get appearance => 'Appearance';

  @override
  String get themeColor => 'Theme Color';

  @override
  String get blurGlass => 'Frosted Glass';

  @override
  String get blurGlassDesc => 'Blur background on top and bottom bars';

  @override
  String get translationBehavior => 'Translation Behavior';

  @override
  String get toastNotification => 'Translation Toast';

  @override
  String get toastNotificationDesc => 'Show a toast when text is translated';

  @override
  String get debugLog => 'Debug Log';

  @override
  String get debugLogDesc => 'Output detailed hook logs to logcat';

  @override
  String get cacheManagement => 'Cache Management';

  @override
  String cachedCount(Object count) {
    return '$count entries · tap for details';
  }

  @override
  String get clearAllCache => 'Clear All Cache';

  @override
  String get clearAllCacheTitle => 'Clear All Cache';

  @override
  String get clearAllCacheContent =>
      'Are you sure you want to clear all translation caches? This cannot be undone.';

  @override
  String get clearAllCacheConfirm => 'Clear All';

  @override
  String get cacheCleared => 'Cache cleared';

  @override
  String get clearAllCacheDone => 'All caches cleared';

  @override
  String get about => 'About';

  @override
  String get engine => 'Translation Engine';

  @override
  String get version => 'Version';

  @override
  String get detectingStatus => 'Detecting module status...';

  @override
  String get language => 'Language';

  @override
  String get languageTitle => 'Interface Language';

  @override
  String get settings => 'Settings';

  @override
  String get themeSystem => 'Follow System';

  @override
  String get themeLight => 'Light';

  @override
  String get themeDark => 'Dark';

  @override
  String get cachedTranslations => 'Cached Translations';

  @override
  String get clearCacheFailed => 'Clear failed';

  @override
  String get appSubtitle => 'LSPosed Global Real-time Translation Module';

  @override
  String get engineDesc => 'OpenAI-compatible API';

  @override
  String get apiUrlHint => 'https://api.deepseek.com/v1 or .../chat/completions';

  @override
  String get apiKeyHint => 'optional for local servers';

  @override
  String timeoutSeconds(Object seconds) {
    return '${seconds}s';
  }

  @override
  String get temperatureDesc =>
      'Lower = more deterministic, higher = more creative';

  @override
  String get maxTokensDesc => 'Maximum response length';

  @override
  String get batchConfig => 'Batch Config';

  @override
  String get batchSize => 'Max widgets per request';

  @override
  String get batchSizeDesc => 'How many controls or chunks share one request. Short UI text stays whole and is grouped together.';

  @override
  String get batchWindow => 'Batch Window';

  @override
  String get batchWindowDesc => 'Time to wait for more text (ms)';

  @override
  String get translationCache => 'Translation Cache';

  @override
  String get translationCacheDesc =>
      'Saves the translation for each source text, so the same text is not requested again. Bilingual display is added on screen and is not stored separately.';

  @override
  String get deepseekInfo => 'Any OpenAI-compatible API. Use a base URL (https://host/v1) or the full /chat/completions URL. Fetch models from /models, or type a model id. API key can be empty for local servers.';

  @override
  String get promptHint => 'Tap to edit translation prompt...';

  @override
  String get restoreDefault => 'Restore Default';

  @override
  String get promptEditHint => 'Enter translation prompt...';

  @override
  String get apiUrlEmpty => 'API URL cannot be empty';

  @override
  String get cacheDetail => 'Cache Details';

  @override
  String get clearAll => 'Clear All';

  @override
  String get noCacheData => 'No cached data';

  @override
  String get clearAppCache => 'Clear App Cache';

  @override
  String clearAppCacheContent(Object package) {
    return 'Clear cache stats for $package?';
  }

  @override
  String appCacheCleared(Object package) {
    return 'Cache cleared for $package';
  }

  @override
  String get clearAllCacheStatsContent => 'Clear all apps\' cache stats?';

  @override
  String cacheEntry(Object count, Object package) {
    return '$count entries · $package';
  }

  @override
  String get modelHint => 'Type a model id, or pick one after fetching';

  @override
  String get modelEmpty => 'Model cannot be empty';

  @override
  String get modelsFetchFailed => 'Failed to fetch models';

  @override
  String get coverageTitle => 'Coverage';

  @override
  String get hookLayout => 'Layout text';

  @override
  String get hookLayoutDesc => 'StaticLayout and BoringLayout, for text that never calls setText';

  @override
  String get hookWebView => 'WebView';

  @override
  String get hookWebViewDesc => 'Visible web page text. Inputs are skipped';

  @override
  String get hookCompose => 'Compose text';

  @override
  String get hookComposeDesc => 'Hooks Compose TextLayout / Paragraph when those classes exist';

  @override
  String get concurrency => 'Concurrency';

  @override
  String get concurrencyDesc => 'How many translation requests run at once';

  @override
  String get maxParagraphs => 'Max paragraphs';

  @override
  String get maxParagraphsDesc => 'Paragraphs from one post kept in a single chunk';

  @override
  String get maxChars => 'Max characters';

  @override
  String get maxCharsDesc => 'Longer text is split at paragraphs or sentences into more requests';

  @override
  String get bilingual => 'Bilingual display';

  @override
  String get bilingualDesc => 'Show the translation and the original together';
  @override
  String get fallbackTitle => 'Fallback channel';

  @override
  String get fallbackHint => 'Used after the main channel retries and still fails. Leave the URL or model empty to disable it.';

  @override
  String get fallbackUrl => 'Fallback API URL';

  @override
  String get fallbackModel => 'Fallback model';

  @override
  String get retryCount => 'Retries';

  @override
  String get retryCountDesc => 'Extra tries on the current channel before switching. 0 switches on the first error.';
  @override
  String modelsFetched(Object count) {
    return 'Fetched $count models';
  }
}
