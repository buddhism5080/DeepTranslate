// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for German (`de`).
class AppLocalizationsDe extends AppLocalizations {
  AppLocalizationsDe([String locale = 'de']) : super(locale);

  @override
  String get appTitle => 'DeepTranslate';

  @override
  String get navHome => 'Start';

  @override
  String get navTranslateConfig => 'Konfig';

  @override
  String get navSettings => 'Einstellungen';

  @override
  String get moduleStatus => 'Modulstatus';

  @override
  String get activated => 'Aktiviert';

  @override
  String get notActivated => 'Deaktiviert';

  @override
  String get enableInLSPosed => 'In LSPosed aktivieren und Apps auswählen';

  @override
  String get translationStatus => 'Übersetzung';

  @override
  String get globalTranslation => 'Globale Übersetzung';

  @override
  String get globalTranslationDesc => 'Text in ausgewählten Apps übersetzen';

  @override
  String get accountBalance => 'Kontostand';

  @override
  String get noApiKey => 'API-Key nicht konfiguriert';

  @override
  String get fetchFailed => 'Abruf fehlgeschlagen';

  @override
  String get totalTokens => 'Tokens verbraucht';

  @override
  String get resetStats => 'Zurücksetzen';

  @override
  String get resetTokenTitle => 'Token-Statistik zurücksetzen';

  @override
  String get resetTokenContent =>
      'Token-Zähler zurücksetzen? Kann nicht rückgängig gemacht werden.';

  @override
  String get cancel => 'Abbrechen';

  @override
  String get confirm => 'Bestätigen';

  @override
  String get usageNotes => 'Hinweise';

  @override
  String get note1 => '1. Set an OpenAI-compatible API URL and model in Config';

  @override
  String get note2 => '2. Modul in LSPosed aktivieren';

  @override
  String get note3 => '3. Apps neu starten';

  @override
  String get note4 => '4. Übersetzungen werden gecached';

  @override
  String get note5 => '5. Batch-Modus verhindert Kontextfehler';

  @override
  String get refresh => 'Aktualisieren';

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
  String get save => 'Speichern';

  @override
  String get configSaved => 'Gespeichert';

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
  String get cacheManagement => 'Cache-Verwaltung';

  @override
  String cachedCount(Object count) {
    return '$count Einträge · Details';
  }

  @override
  String get clearAllCache => 'Cache löschen';

  @override
  String get clearAllCacheTitle => 'Cache löschen';

  @override
  String get clearAllCacheContent => 'Gesamten Übersetzungs-Cache löschen?';

  @override
  String get clearAllCacheConfirm => 'Alle löschen';

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
  String get detectingStatus => 'Erkennung...';

  @override
  String get language => 'Sprache';

  @override
  String get languageTitle => 'Oberflächensprache';

  @override
  String get settings => 'Einstellungen';

  @override
  String get themeSystem => 'System folgen';

  @override
  String get themeLight => 'Hell';

  @override
  String get themeDark => 'Dunkel';

  @override
  String get cachedTranslations => 'Zwischengespeicherte Übersetzungen';

  @override
  String get clearCacheFailed => 'Löschen fehlgeschlagen';

  @override
  String get appSubtitle => 'LSPosed Globales Echtzeit-Übersetzungsmodul';

  @override
  String get engineDesc => 'OpenAI-compatible API';

  @override
  String get apiUrlHint => 'https://api.deepseek.com/v1 or .../chat/completions';

  @override
  String get apiKeyHint => 'optional for local servers';

  @override
  String timeoutSeconds(Object seconds) {
    return '$seconds s';
  }

  @override
  String get temperatureDesc =>
      'Niedriger = deterministischer, höher = kreativer';

  @override
  String get maxTokensDesc => 'Maximale Antwortlänge';

  @override
  String get batchConfig => 'Batch-Konfiguration';

  @override
  String get batchSize => 'Batch-Größe';

  @override
  String get batchSizeDesc => 'Max. Texte pro API-Anfrage';

  @override
  String get batchWindow => 'Batch-Fenster';

  @override
  String get batchWindowDesc => 'Wartezeit auf mehr Text (ms)';

  @override
  String get translationCache => 'Übersetzungs-Cache';

  @override
  String get translationCacheDesc =>
      'Übersetzungen lokal cachen, um wiederholte API-Aufrufe zu vermeiden';

  @override
  String get deepseekInfo => 'Any OpenAI-compatible API. Use a base URL (https://host/v1) or the full /chat/completions URL. Fetch models from /models, or type a model id. API key can be empty for local servers.';

  @override
  String get promptHint => 'Tippen, um Übersetzungs-Prompt zu bearbeiten...';

  @override
  String get restoreDefault => 'Standard wiederherstellen';

  @override
  String get promptEditHint => 'Übersetzungs-Prompt eingeben...';

  @override
  String get apiUrlEmpty => 'API URL darf nicht leer sein';

  @override
  String get cacheDetail => 'Cache-Details';

  @override
  String get clearAll => 'Alle löschen';

  @override
  String get noCacheData => 'Keine Cache-Daten';

  @override
  String get clearAppCache => 'App-Cache leeren';

  @override
  String clearAppCacheContent(Object package) {
    return 'Cache-Statistiken für $package löschen?';
  }

  @override
  String appCacheCleared(Object package) {
    return 'Cache für $package geleert';
  }

  @override
  String get clearAllCacheStatsContent => 'Alle App-Cache-Statistiken löschen?';

  @override
  String cacheEntry(Object count, Object package) {
    return '$count Einträge · $package';
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
