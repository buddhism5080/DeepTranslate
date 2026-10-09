// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Spanish Castilian (`es`).
class AppLocalizationsEs extends AppLocalizations {
  AppLocalizationsEs([String locale = 'es']) : super(locale);

  @override
  String get appTitle => 'DeepTranslate';

  @override
  String get navHome => 'Inicio';

  @override
  String get navTranslateConfig => 'Config';

  @override
  String get navSettings => 'Ajustes';

  @override
  String get moduleStatus => 'Estado';

  @override
  String get activated => 'Activado';

  @override
  String get notActivated => 'No activado';

  @override
  String get enableInLSPosed => 'Activar en LSPosed y seleccionar apps';

  @override
  String get translationStatus => 'Traducción';

  @override
  String get globalTranslation => 'Traducción Global';

  @override
  String get globalTranslationDesc => 'Traducir texto en apps seleccionadas';

  @override
  String get accountBalance => 'Saldo';

  @override
  String get noApiKey => 'API Key no configurada';

  @override
  String get fetchFailed => 'Error al obtener';

  @override
  String get totalTokens => 'Tokens usados';

  @override
  String get resetStats => 'Restablecer';

  @override
  String get resetTokenTitle => 'Restablecer Tokens';

  @override
  String get resetTokenContent =>
      '¿Restablecer el conteo de tokens? No se puede deshacer.';

  @override
  String get cancel => 'Cancelar';

  @override
  String get confirm => 'Confirmar';

  @override
  String get usageNotes => 'Notas';

  @override
  String get note1 => '1. Set an OpenAI-compatible API URL and model in Config';

  @override
  String get note2 => '2. Activa el módulo en LSPosed';

  @override
  String get note3 => '3. Reinicia las apps, se traducirán';

  @override
  String get note4 => '4. Se almacena en caché';

  @override
  String get note5 => '5. El modo por lotes evita errores';

  @override
  String get refresh => 'Actualizar';

  @override
  String get apiConfig => 'API';

  @override
  String get apiUrl => 'API URL';

  @override
  String get apiKey => 'API Key';

  @override
  String get model => 'Modelo';

  @override
  String get testConn => 'Probar';

  @override
  String get testing => 'Probando...';

  @override
  String get fetchModels => 'Obtener modelos';

  @override
  String get translatePrompt => 'Prompt';

  @override
  String get timeout => 'Timeout';

  @override
  String get temperature => 'Temp.';

  @override
  String get maxTokens => 'Max Tokens';

  @override
  String get save => 'Guardar';

  @override
  String get configSaved => 'Configuración guardada';

  @override
  String get appearance => 'Apariencia';

  @override
  String get themeColor => 'Color del tema';

  @override
  String get blurGlass => 'Vidrio esmerilado';

  @override
  String get blurGlassDesc => 'Difuminar barras';

  @override
  String get translationBehavior => 'Comportamiento';

  @override
  String get toastNotification => 'Notificación';

  @override
  String get toastNotificationDesc => 'Mostrar al traducir';

  @override
  String get debugLog => 'Registro';

  @override
  String get debugLogDesc => 'Salida detallada a logcat';

  @override
  String get cacheManagement => 'Gestión de caché';

  @override
  String cachedCount(Object count) {
    return '$count entradas · detalles';
  }

  @override
  String get clearAllCache => 'Borrar caché';

  @override
  String get clearAllCacheTitle => 'Borrar caché';

  @override
  String get clearAllCacheContent => '¿Borrar toda la caché de traducción?';

  @override
  String get clearAllCacheConfirm => 'Borrar todo';

  @override
  String get cacheCleared => 'Cache cleared';

  @override
  String get clearAllCacheDone => 'All caches cleared';

  @override
  String get about => 'Acerca de';

  @override
  String get engine => 'Motor';

  @override
  String get version => 'Versión';

  @override
  String get detectingStatus => 'Detectando...';

  @override
  String get language => 'Idioma';

  @override
  String get languageTitle => 'Idioma de la interfaz';

  @override
  String get settings => 'Ajustes';

  @override
  String get themeSystem => 'Seguir sistema';

  @override
  String get themeLight => 'Claro';

  @override
  String get themeDark => 'Oscuro';

  @override
  String get cachedTranslations => 'Traducciones en caché';

  @override
  String get clearCacheFailed => 'Error al borrar';

  @override
  String get appSubtitle =>
      'Módulo de traducción global en tiempo real LSPosed';

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
      'Más bajo = más determinista, más alto = más creativo';

  @override
  String get maxTokensDesc => 'Longitud máxima de respuesta';

  @override
  String get batchConfig => 'Config. de lotes';

  @override
  String get batchSize => 'Tamaño de lote';

  @override
  String get batchSizeDesc => 'Máx. de textos por solicitud API';

  @override
  String get batchWindow => 'Ventana de lote';

  @override
  String get batchWindowDesc => 'Tiempo de espera para más texto (ms)';

  @override
  String get translationCache => 'Caché de traducción';

  @override
  String get translationCacheDesc =>
      'Guardar en caché para evitar llamadas repetidas a la API';

  @override
  String get deepseekInfo => 'Any OpenAI-compatible API. Use a base URL (https://host/v1) or the full /chat/completions URL. Fetch models from /models, or type a model id. API key can be empty for local servers.';

  @override
  String get promptHint => 'Toca para editar el prompt de traducción...';

  @override
  String get restoreDefault => 'Restaurar predeterminado';

  @override
  String get promptEditHint => 'Introduce el prompt de traducción...';

  @override
  String get apiUrlEmpty => 'La URL de la API no puede estar vacía';

  @override
  String get cacheDetail => 'Detalles de caché';

  @override
  String get clearAll => 'Borrar todo';

  @override
  String get noCacheData => 'Sin datos en caché';

  @override
  String get clearAppCache => 'Borrar caché de la app';

  @override
  String clearAppCacheContent(Object package) {
    return '¿Borrar estadísticas de caché de $package?';
  }

  @override
  String appCacheCleared(Object package) {
    return 'Caché borrada para $package';
  }

  @override
  String get clearAllCacheStatsContent =>
      '¿Borrar todas las estadísticas de caché?';

  @override
  String cacheEntry(Object count, Object package) {
    return '$count entradas · $package';
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
