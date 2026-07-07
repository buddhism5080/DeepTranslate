import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_de.dart';
import 'app_localizations_en.dart';
import 'app_localizations_es.dart';
import 'app_localizations_fr.dart';
import 'app_localizations_ja.dart';
import 'app_localizations_ko.dart';
import 'app_localizations_ru.dart';
import 'app_localizations_zh.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'generated/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
    : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations)!;
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
        delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('de'),
    Locale('en'),
    Locale('es'),
    Locale('fr'),
    Locale('ja'),
    Locale('ko'),
    Locale('ru'),
    Locale('zh'),
  ];

  /// No description provided for @appTitle.
  ///
  /// In en, this message translates to:
  /// **'DeepTranslate'**
  String get appTitle;

  /// No description provided for @navHome.
  ///
  /// In en, this message translates to:
  /// **'Home'**
  String get navHome;

  /// No description provided for @navTranslateConfig.
  ///
  /// In en, this message translates to:
  /// **'Config'**
  String get navTranslateConfig;

  /// No description provided for @navSettings.
  ///
  /// In en, this message translates to:
  /// **'Settings'**
  String get navSettings;

  /// No description provided for @moduleStatus.
  ///
  /// In en, this message translates to:
  /// **'Module Status'**
  String get moduleStatus;

  /// No description provided for @activated.
  ///
  /// In en, this message translates to:
  /// **'Activated'**
  String get activated;

  /// No description provided for @notActivated.
  ///
  /// In en, this message translates to:
  /// **'Not Activated'**
  String get notActivated;

  /// No description provided for @enableInLSPosed.
  ///
  /// In en, this message translates to:
  /// **'Enable in LSPosed and select target apps'**
  String get enableInLSPosed;

  /// No description provided for @translationStatus.
  ///
  /// In en, this message translates to:
  /// **'Translation'**
  String get translationStatus;

  /// No description provided for @globalTranslation.
  ///
  /// In en, this message translates to:
  /// **'Global Translation'**
  String get globalTranslation;

  /// No description provided for @globalTranslationDesc.
  ///
  /// In en, this message translates to:
  /// **'Hook and translate text in all selected apps'**
  String get globalTranslationDesc;

  /// No description provided for @accountBalance.
  ///
  /// In en, this message translates to:
  /// **'Account Balance'**
  String get accountBalance;

  /// No description provided for @noApiKey.
  ///
  /// In en, this message translates to:
  /// **'API Key not configured'**
  String get noApiKey;

  /// No description provided for @fetchFailed.
  ///
  /// In en, this message translates to:
  /// **'Fetch failed'**
  String get fetchFailed;

  /// No description provided for @totalTokens.
  ///
  /// In en, this message translates to:
  /// **'Total Tokens Used'**
  String get totalTokens;

  /// No description provided for @resetStats.
  ///
  /// In en, this message translates to:
  /// **'Reset Stats'**
  String get resetStats;

  /// No description provided for @resetTokenTitle.
  ///
  /// In en, this message translates to:
  /// **'Reset Token Stats'**
  String get resetTokenTitle;

  /// No description provided for @resetTokenContent.
  ///
  /// In en, this message translates to:
  /// **'Are you sure you want to reset the token count? This cannot be undone.'**
  String get resetTokenContent;

  /// No description provided for @cancel.
  ///
  /// In en, this message translates to:
  /// **'Cancel'**
  String get cancel;

  /// No description provided for @confirm.
  ///
  /// In en, this message translates to:
  /// **'Confirm'**
  String get confirm;

  /// No description provided for @usageNotes.
  ///
  /// In en, this message translates to:
  /// **'Notes'**
  String get usageNotes;

  /// No description provided for @note1.
  ///
  /// In en, this message translates to:
  /// **'1. Enter your DeepSeek API Key in Config'**
  String get note1;

  /// No description provided for @note2.
  ///
  /// In en, this message translates to:
  /// **'2. Enable the module in LSPosed, select target apps'**
  String get note2;

  /// No description provided for @note3.
  ///
  /// In en, this message translates to:
  /// **'3. Restart target apps, text will auto-translate'**
  String get note3;

  /// No description provided for @note4.
  ///
  /// In en, this message translates to:
  /// **'4. Translations are cached to avoid repeated API calls'**
  String get note4;

  /// No description provided for @note5.
  ///
  /// In en, this message translates to:
  /// **'5. Batch mode aggregates context to avoid word-by-word errors'**
  String get note5;

  /// No description provided for @refresh.
  ///
  /// In en, this message translates to:
  /// **'Refresh'**
  String get refresh;

  /// No description provided for @apiConfig.
  ///
  /// In en, this message translates to:
  /// **'API Config'**
  String get apiConfig;

  /// No description provided for @apiUrl.
  ///
  /// In en, this message translates to:
  /// **'API URL'**
  String get apiUrl;

  /// No description provided for @apiKey.
  ///
  /// In en, this message translates to:
  /// **'API Key'**
  String get apiKey;

  /// No description provided for @model.
  ///
  /// In en, this message translates to:
  /// **'Model'**
  String get model;

  /// No description provided for @testConn.
  ///
  /// In en, this message translates to:
  /// **'Test Connection'**
  String get testConn;

  /// No description provided for @testing.
  ///
  /// In en, this message translates to:
  /// **'Testing...'**
  String get testing;

  /// No description provided for @fetchModels.
  ///
  /// In en, this message translates to:
  /// **'Fetch Models'**
  String get fetchModels;

  /// No description provided for @translatePrompt.
  ///
  /// In en, this message translates to:
  /// **'Translation Prompt'**
  String get translatePrompt;

  /// No description provided for @timeout.
  ///
  /// In en, this message translates to:
  /// **'Timeout'**
  String get timeout;

  /// No description provided for @temperature.
  ///
  /// In en, this message translates to:
  /// **'Temperature'**
  String get temperature;

  /// No description provided for @maxTokens.
  ///
  /// In en, this message translates to:
  /// **'Max Tokens'**
  String get maxTokens;

  /// No description provided for @save.
  ///
  /// In en, this message translates to:
  /// **'Save'**
  String get save;

  /// No description provided for @configSaved.
  ///
  /// In en, this message translates to:
  /// **'Configuration saved'**
  String get configSaved;

  /// No description provided for @appearance.
  ///
  /// In en, this message translates to:
  /// **'Appearance'**
  String get appearance;

  /// No description provided for @themeColor.
  ///
  /// In en, this message translates to:
  /// **'Theme Color'**
  String get themeColor;

  /// No description provided for @blurGlass.
  ///
  /// In en, this message translates to:
  /// **'Frosted Glass'**
  String get blurGlass;

  /// No description provided for @blurGlassDesc.
  ///
  /// In en, this message translates to:
  /// **'Blur background on top and bottom bars'**
  String get blurGlassDesc;

  /// No description provided for @translationBehavior.
  ///
  /// In en, this message translates to:
  /// **'Translation Behavior'**
  String get translationBehavior;

  /// No description provided for @toastNotification.
  ///
  /// In en, this message translates to:
  /// **'Translation Toast'**
  String get toastNotification;

  /// No description provided for @toastNotificationDesc.
  ///
  /// In en, this message translates to:
  /// **'Show a toast when text is translated'**
  String get toastNotificationDesc;

  /// No description provided for @debugLog.
  ///
  /// In en, this message translates to:
  /// **'Debug Log'**
  String get debugLog;

  /// No description provided for @debugLogDesc.
  ///
  /// In en, this message translates to:
  /// **'Output detailed hook logs to logcat'**
  String get debugLogDesc;

  /// No description provided for @cacheManagement.
  ///
  /// In en, this message translates to:
  /// **'Cache Management'**
  String get cacheManagement;

  /// No description provided for @cachedCount.
  ///
  /// In en, this message translates to:
  /// **'{count} entries · tap for details'**
  String cachedCount(Object count);

  /// No description provided for @clearAllCache.
  ///
  /// In en, this message translates to:
  /// **'Clear All Cache'**
  String get clearAllCache;

  /// No description provided for @clearAllCacheTitle.
  ///
  /// In en, this message translates to:
  /// **'Clear All Cache'**
  String get clearAllCacheTitle;

  /// No description provided for @clearAllCacheContent.
  ///
  /// In en, this message translates to:
  /// **'Are you sure you want to clear all translation caches? This cannot be undone.'**
  String get clearAllCacheContent;

  /// No description provided for @clearAllCacheConfirm.
  ///
  /// In en, this message translates to:
  /// **'Clear All'**
  String get clearAllCacheConfirm;

  /// No description provided for @cacheCleared.
  ///
  /// In en, this message translates to:
  /// **'Cache cleared'**
  String get cacheCleared;

  /// No description provided for @clearAllCacheDone.
  ///
  /// In en, this message translates to:
  /// **'All caches cleared'**
  String get clearAllCacheDone;

  /// No description provided for @about.
  ///
  /// In en, this message translates to:
  /// **'About'**
  String get about;

  /// No description provided for @engine.
  ///
  /// In en, this message translates to:
  /// **'Translation Engine'**
  String get engine;

  /// No description provided for @version.
  ///
  /// In en, this message translates to:
  /// **'Version'**
  String get version;

  /// No description provided for @detectingStatus.
  ///
  /// In en, this message translates to:
  /// **'Detecting module status...'**
  String get detectingStatus;

  /// No description provided for @language.
  ///
  /// In en, this message translates to:
  /// **'Language'**
  String get language;

  /// No description provided for @languageTitle.
  ///
  /// In en, this message translates to:
  /// **'Interface Language'**
  String get languageTitle;

  /// No description provided for @settings.
  ///
  /// In en, this message translates to:
  /// **'Settings'**
  String get settings;

  /// No description provided for @themeSystem.
  ///
  /// In en, this message translates to:
  /// **'Follow System'**
  String get themeSystem;

  /// No description provided for @themeLight.
  ///
  /// In en, this message translates to:
  /// **'Light'**
  String get themeLight;

  /// No description provided for @themeDark.
  ///
  /// In en, this message translates to:
  /// **'Dark'**
  String get themeDark;

  /// No description provided for @cachedTranslations.
  ///
  /// In en, this message translates to:
  /// **'Cached Translations'**
  String get cachedTranslations;

  /// No description provided for @clearCacheFailed.
  ///
  /// In en, this message translates to:
  /// **'Clear failed'**
  String get clearCacheFailed;

  /// No description provided for @appSubtitle.
  ///
  /// In en, this message translates to:
  /// **'LSPosed Global Real-time Translation Module'**
  String get appSubtitle;

  /// No description provided for @engineDesc.
  ///
  /// In en, this message translates to:
  /// **'DeepSeek API (OpenAI Compatible)'**
  String get engineDesc;

  /// No description provided for @apiUrlHint.
  ///
  /// In en, this message translates to:
  /// **'https://api.deepseek.com/v1/chat/completions'**
  String get apiUrlHint;

  /// No description provided for @apiKeyHint.
  ///
  /// In en, this message translates to:
  /// **'sk-xxxxxxxxxxxxxxxx'**
  String get apiKeyHint;

  /// No description provided for @timeoutSeconds.
  ///
  /// In en, this message translates to:
  /// **'{seconds}s'**
  String timeoutSeconds(Object seconds);

  /// No description provided for @temperatureDesc.
  ///
  /// In en, this message translates to:
  /// **'Lower = more deterministic, higher = more creative'**
  String get temperatureDesc;

  /// No description provided for @maxTokensDesc.
  ///
  /// In en, this message translates to:
  /// **'Maximum response length'**
  String get maxTokensDesc;

  /// No description provided for @batchConfig.
  ///
  /// In en, this message translates to:
  /// **'Batch Config'**
  String get batchConfig;

  /// No description provided for @batchSize.
  ///
  /// In en, this message translates to:
  /// **'Batch Size'**
  String get batchSize;

  /// No description provided for @batchSizeDesc.
  ///
  /// In en, this message translates to:
  /// **'Max texts per API request'**
  String get batchSizeDesc;

  /// No description provided for @batchWindow.
  ///
  /// In en, this message translates to:
  /// **'Batch Window'**
  String get batchWindow;

  /// No description provided for @batchWindowDesc.
  ///
  /// In en, this message translates to:
  /// **'Time to wait for more text (ms)'**
  String get batchWindowDesc;

  /// No description provided for @translationCache.
  ///
  /// In en, this message translates to:
  /// **'Translation Cache'**
  String get translationCache;

  /// No description provided for @translationCacheDesc.
  ///
  /// In en, this message translates to:
  /// **'Cache translations locally to avoid repeated API calls'**
  String get translationCacheDesc;

  /// No description provided for @deepseekInfo.
  ///
  /// In en, this message translates to:
  /// **'DeepSeek API is compatible with the OpenAI protocol. Uses the deepseek-chat model by default, supports prompt caching to reduce repeated request costs. Batch mode aggregates multiple texts from the same page with context to avoid word-by-word translation artifacts.'**
  String get deepseekInfo;

  /// No description provided for @promptHint.
  ///
  /// In en, this message translates to:
  /// **'Tap to edit translation prompt...'**
  String get promptHint;

  /// No description provided for @restoreDefault.
  ///
  /// In en, this message translates to:
  /// **'Restore Default'**
  String get restoreDefault;

  /// No description provided for @promptEditHint.
  ///
  /// In en, this message translates to:
  /// **'Enter translation prompt...'**
  String get promptEditHint;

  /// No description provided for @apiUrlEmpty.
  ///
  /// In en, this message translates to:
  /// **'API URL cannot be empty'**
  String get apiUrlEmpty;

  /// No description provided for @cacheDetail.
  ///
  /// In en, this message translates to:
  /// **'Cache Details'**
  String get cacheDetail;

  /// No description provided for @clearAll.
  ///
  /// In en, this message translates to:
  /// **'Clear All'**
  String get clearAll;

  /// No description provided for @noCacheData.
  ///
  /// In en, this message translates to:
  /// **'No cached data'**
  String get noCacheData;

  /// No description provided for @clearAppCache.
  ///
  /// In en, this message translates to:
  /// **'Clear App Cache'**
  String get clearAppCache;

  /// No description provided for @clearAppCacheContent.
  ///
  /// In en, this message translates to:
  /// **'Clear cache stats for {package}?'**
  String clearAppCacheContent(Object package);

  /// No description provided for @appCacheCleared.
  ///
  /// In en, this message translates to:
  /// **'Cache cleared for {package}'**
  String appCacheCleared(Object package);

  /// No description provided for @clearAllCacheStatsContent.
  ///
  /// In en, this message translates to:
  /// **'Clear all apps\' cache stats?'**
  String get clearAllCacheStatsContent;

  /// No description provided for @cacheEntry.
  ///
  /// In en, this message translates to:
  /// **'{count} entries · {package}'**
  String cacheEntry(Object count, Object package);
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) => <String>[
    'de',
    'en',
    'es',
    'fr',
    'ja',
    'ko',
    'ru',
    'zh',
  ].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'de':
      return AppLocalizationsDe();
    case 'en':
      return AppLocalizationsEn();
    case 'es':
      return AppLocalizationsEs();
    case 'fr':
      return AppLocalizationsFr();
    case 'ja':
      return AppLocalizationsJa();
    case 'ko':
      return AppLocalizationsKo();
    case 'ru':
      return AppLocalizationsRu();
    case 'zh':
      return AppLocalizationsZh();
  }

  throw FlutterError(
    'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}
