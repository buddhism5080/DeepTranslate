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
  String get note1 => '1. Ingresa tu API Key de DeepSeek';

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
  String get engineDesc => 'DeepSeek API (Compatible con OpenAI)';

  @override
  String get apiUrlHint => 'https://api.deepseek.com/v1/chat/completions';

  @override
  String get apiKeyHint => 'sk-xxxxxxxxxxxxxxxx';

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
  String get deepseekInfo =>
      'DeepSeek API es compatible con el protocolo OpenAI. Usa el modelo deepseek-chat por defecto, admite prompt caching para reducir costes. El modo lote agrupa textos de una misma página con contexto para evitar traducciones literales.';

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
}
