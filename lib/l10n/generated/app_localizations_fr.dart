// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for French (`fr`).
class AppLocalizationsFr extends AppLocalizations {
  AppLocalizationsFr([String locale = 'fr']) : super(locale);

  @override
  String get appTitle => 'DeepTranslate';

  @override
  String get navHome => 'Accueil';

  @override
  String get navTranslateConfig => 'Config';

  @override
  String get navSettings => 'Paramètres';

  @override
  String get moduleStatus => 'Statut';

  @override
  String get activated => 'Activé';

  @override
  String get notActivated => 'Désactivé';

  @override
  String get enableInLSPosed => 'Activer dans LSPosed et sélectionner les apps';

  @override
  String get translationStatus => 'Traduction';

  @override
  String get globalTranslation => 'Traduction Globale';

  @override
  String get globalTranslationDesc =>
      'Traduire le texte dans les apps sélectionnées';

  @override
  String get accountBalance => 'Solde';

  @override
  String get noApiKey => 'Clé API non configurée';

  @override
  String get fetchFailed => 'Échec';

  @override
  String get totalTokens => 'Tokens utilisés';

  @override
  String get resetStats => 'Réinitialiser';

  @override
  String get resetTokenTitle => 'Réinitialiser les tokens';

  @override
  String get resetTokenContent => 'Réinitialiser le compteur ? Irréversible.';

  @override
  String get cancel => 'Annuler';

  @override
  String get confirm => 'Confirmer';

  @override
  String get usageNotes => 'Notes';

  @override
  String get note1 => '1. Set an OpenAI-compatible API URL and model in Config';

  @override
  String get note2 => '2. Activer le module dans LSPosed';

  @override
  String get note3 => '3. Redémarrer les apps';

  @override
  String get note4 => '4. Traductions mises en cache';

  @override
  String get note5 => '5. Mode batch pour le contexte';

  @override
  String get refresh => 'Actualiser';

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
  String get save => 'Enregistrer';

  @override
  String get configSaved => 'Enregistré';

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
  String get cacheManagement => 'Gestion du cache';

  @override
  String cachedCount(Object count) {
    return '$count entrées · détails';
  }

  @override
  String get clearAllCache => 'Vider le cache';

  @override
  String get clearAllCacheTitle => 'Vider le cache';

  @override
  String get clearAllCacheContent => 'Supprimer tout le cache de traduction ?';

  @override
  String get clearAllCacheConfirm => 'Tout supprimer';

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
  String get detectingStatus => 'Détection...';

  @override
  String get language => 'Langue';

  @override
  String get languageTitle => 'Langue de l\'interface';

  @override
  String get settings => 'Paramètres';

  @override
  String get themeSystem => 'Suivre le système';

  @override
  String get themeLight => 'Clair';

  @override
  String get themeDark => 'Sombre';

  @override
  String get cachedTranslations => 'Traductions en cache';

  @override
  String get clearCacheFailed => 'Échec de la suppression';

  @override
  String get appSubtitle => 'Module de traduction temps réel LSPosed';

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
      'Plus bas = plus déterministe, plus haut = plus créatif';

  @override
  String get maxTokensDesc => 'Longueur max de réponse';

  @override
  String get batchConfig => 'Configuration par lot';

  @override
  String get batchSize => 'Taille du lot';

  @override
  String get batchSizeDesc => 'Max. de textes par requête API';

  @override
  String get batchWindow => 'Fenêtre de lot';

  @override
  String get batchWindowDesc => 'Temps d\'attente pour plus de texte (ms)';

  @override
  String get translationCache => 'Cache de traduction';

  @override
  String get translationCacheDesc =>
      'Mettre en cache localement pour éviter les appels API répétés';

  @override
  String get deepseekInfo => 'Any OpenAI-compatible API. Use a base URL (https://host/v1) or the full /chat/completions URL. Fetch models from /models, or type a model id. API key can be empty for local servers.';

  @override
  String get promptHint => 'Appuyez pour modifier le prompt de traduction...';

  @override
  String get restoreDefault => 'Rétablir par défaut';

  @override
  String get promptEditHint => 'Saisissez le prompt de traduction...';

  @override
  String get apiUrlEmpty => 'L\'URL de l\'API ne peut pas être vide';

  @override
  String get cacheDetail => 'Détails du cache';

  @override
  String get clearAll => 'Tout supprimer';

  @override
  String get noCacheData => 'Aucune donnée en cache';

  @override
  String get clearAppCache => 'Vider le cache de l\'app';

  @override
  String clearAppCacheContent(Object package) {
    return 'Supprimer les stats de cache de $package ?';
  }

  @override
  String appCacheCleared(Object package) {
    return 'Cache vidé pour $package';
  }

  @override
  String get clearAllCacheStatsContent =>
      'Supprimer toutes les stats de cache des apps ?';

  @override
  String cacheEntry(Object count, Object package) {
    return '$count entrées · $package';
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
  String modelsFetched(Object count) {
    return 'Fetched $count models';
  }
}
