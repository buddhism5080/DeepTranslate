// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Russian (`ru`).
class AppLocalizationsRu extends AppLocalizations {
  AppLocalizationsRu([String locale = 'ru']) : super(locale);

  @override
  String get appTitle => 'DeepTranslate';

  @override
  String get navHome => 'Главная';

  @override
  String get navTranslateConfig => 'Настройки';

  @override
  String get navSettings => 'Ещё';

  @override
  String get moduleStatus => 'Статус';

  @override
  String get activated => 'Активен';

  @override
  String get notActivated => 'Не активен';

  @override
  String get enableInLSPosed => 'Включите в LSPosed и выберите приложения';

  @override
  String get translationStatus => 'Перевод';

  @override
  String get globalTranslation => 'Глобальный перевод';

  @override
  String get globalTranslationDesc =>
      'Переводить текст в выбранных приложениях';

  @override
  String get accountBalance => 'Баланс';

  @override
  String get noApiKey => 'API ключ не настроен';

  @override
  String get fetchFailed => 'Ошибка получения';

  @override
  String get totalTokens => 'Использовано токенов';

  @override
  String get resetStats => 'Сброс';

  @override
  String get resetTokenTitle => 'Сброс статистики токенов';

  @override
  String get resetTokenContent => 'Сбросить счётчик токенов? Необратимо.';

  @override
  String get cancel => 'Отмена';

  @override
  String get confirm => 'Подтвердить';

  @override
  String get usageNotes => 'Инструкция';

  @override
  String get note1 => '1. Set an OpenAI-compatible API URL and model in Config';

  @override
  String get note2 => '2. Включите модуль в LSPosed';

  @override
  String get note3 => '3. Перезапустите приложения';

  @override
  String get note4 => '4. Переводы кешируются';

  @override
  String get note5 => '5. Пакетный режим для контекста';

  @override
  String get refresh => 'Обновить';

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
  String get save => 'Сохранить';

  @override
  String get configSaved => 'Сохранено';

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
  String get cacheManagement => 'Управление кешем';

  @override
  String cachedCount(Object count) {
    return '$count записей · подробнее';
  }

  @override
  String get clearAllCache => 'Очистить кеш';

  @override
  String get clearAllCacheTitle => 'Очистить кеш';

  @override
  String get clearAllCacheContent => 'Удалить весь кеш переводов?';

  @override
  String get clearAllCacheConfirm => 'Удалить всё';

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
  String get detectingStatus => 'Определение...';

  @override
  String get language => 'Язык';

  @override
  String get languageTitle => 'Язык интерфейса';

  @override
  String get settings => 'Настройки';

  @override
  String get themeSystem => 'Как в системе';

  @override
  String get themeLight => 'Светлая';

  @override
  String get themeDark => 'Тёмная';

  @override
  String get cachedTranslations => 'Кешированные переводы';

  @override
  String get clearCacheFailed => 'Ошибка очистки';

  @override
  String get appSubtitle =>
      'Глобальный модуль перевода LSPosed в реальном времени';

  @override
  String get engineDesc => 'OpenAI-compatible API';

  @override
  String get apiUrlHint => 'https://api.deepseek.com/v1 or .../chat/completions';

  @override
  String get apiKeyHint => 'optional for local servers';

  @override
  String timeoutSeconds(Object seconds) {
    return '$seconds с';
  }

  @override
  String get temperatureDesc => 'Ниже = детерминированнее, выше = креативнее';

  @override
  String get maxTokensDesc => 'Максимальная длина ответа';

  @override
  String get batchConfig => 'Пакетная настройка';

  @override
  String get batchSize => 'Размер пакета';

  @override
  String get batchSizeDesc => 'Макс. текстов на запрос API';

  @override
  String get batchWindow => 'Окно пакета';

  @override
  String get batchWindowDesc => 'Время ожидания текста (мс)';

  @override
  String get translationCache => 'Кеш переводов';

  @override
  String get translationCacheDesc =>
      'Кешировать переводы локально, избегая повторных вызовов API';

  @override
  String get deepseekInfo => 'Any OpenAI-compatible API. Use a base URL (https://host/v1) or the full /chat/completions URL. Fetch models from /models, or type a model id. API key can be empty for local servers.';

  @override
  String get promptHint => 'Нажмите, чтобы редактировать промпт перевода...';

  @override
  String get restoreDefault => 'Восстановить умолчания';

  @override
  String get promptEditHint => 'Введите промпт перевода...';

  @override
  String get apiUrlEmpty => 'API URL не может быть пустым';

  @override
  String get cacheDetail => 'Детали кеша';

  @override
  String get clearAll => 'Очистить всё';

  @override
  String get noCacheData => 'Нет кешированных данных';

  @override
  String get clearAppCache => 'Очистить кеш приложения';

  @override
  String clearAppCacheContent(Object package) {
    return 'Очистить статистику кеша для $package?';
  }

  @override
  String appCacheCleared(Object package) {
    return 'Кеш очищен для $package';
  }

  @override
  String get clearAllCacheStatsContent =>
      'Очистить статистику кеша всех приложений?';

  @override
  String cacheEntry(Object count, Object package) {
    return '$count записей · $package';
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
  String modelsFetched(Object count) {
    return 'Fetched $count models';
  }
}
