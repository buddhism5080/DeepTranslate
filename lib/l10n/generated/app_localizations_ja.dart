// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Japanese (`ja`).
class AppLocalizationsJa extends AppLocalizations {
  AppLocalizationsJa([String locale = 'ja']) : super(locale);

  @override
  String get appTitle => 'DeepTranslate';

  @override
  String get navHome => 'ホーム';

  @override
  String get navTranslateConfig => '翻訳設定';

  @override
  String get navSettings => '設定';

  @override
  String get moduleStatus => 'モジュール状態';

  @override
  String get activated => '有効';

  @override
  String get notActivated => '無効';

  @override
  String get enableInLSPosed => 'LSPosedで有効にして対象アプリを選択してください';

  @override
  String get translationStatus => '翻訳状態';

  @override
  String get globalTranslation => 'グローバル翻訳';

  @override
  String get globalTranslationDesc => '有効にすると選択したアプリのテキストを翻訳します';

  @override
  String get accountBalance => 'アカウント残高';

  @override
  String get noApiKey => 'APIキー未設定';

  @override
  String get fetchFailed => '取得失敗';

  @override
  String get totalTokens => '累計トークン使用量';

  @override
  String get resetStats => '統計リセット';

  @override
  String get resetTokenTitle => 'トークン統計をリセット';

  @override
  String get resetTokenContent => '累計トークン数をリセットしますか？この操作は元に戻せません。';

  @override
  String get cancel => 'キャンセル';

  @override
  String get confirm => '確認';

  @override
  String get usageNotes => '使い方';

  @override
  String get note1 => '1. Set an OpenAI-compatible API URL and model in Config';

  @override
  String get note2 => '2. LSPosedでモジュールを有効にし、対象アプリを選択';

  @override
  String get note3 => '3. 対象アプリを再起動すると自動的に翻訳されます';

  @override
  String get note4 => '4. 翻訳結果はキャッシュされ、繰り返しAPIを呼び出しません';

  @override
  String get note5 => '5. バッチ処理で文脈を理解し、単語単位の誤訳を防止';

  @override
  String get refresh => '更新';

  @override
  String get apiConfig => 'API設定';

  @override
  String get apiUrl => 'API URL';

  @override
  String get apiKey => 'APIキー';

  @override
  String get model => 'モデル';

  @override
  String get testConn => '接続テスト';

  @override
  String get testing => 'テスト中...';

  @override
  String get fetchModels => 'モデル取得';

  @override
  String get translatePrompt => '翻訳プロンプト';

  @override
  String get timeout => 'タイムアウト';

  @override
  String get temperature => '温度';

  @override
  String get maxTokens => '最大トークン';

  @override
  String get save => '保存';

  @override
  String get configSaved => '設定を保存しました';

  @override
  String get appearance => '外観';

  @override
  String get themeColor => 'テーマカラー';

  @override
  String get blurGlass => 'ガラス効果';

  @override
  String get blurGlassDesc => '上下バーの背景をぼかす';

  @override
  String get translationBehavior => '翻訳動作';

  @override
  String get toastNotification => '翻訳通知';

  @override
  String get toastNotificationDesc => '翻訳時に通知を表示';

  @override
  String get debugLog => 'デバッグログ';

  @override
  String get debugLogDesc => '詳細なフックログをlogcatに出力';

  @override
  String get cacheManagement => 'キャッシュ管理';

  @override
  String cachedCount(Object count) {
    return '$count 件 · タップで詳細';
  }

  @override
  String get clearAllCache => '全キャッシュ削除';

  @override
  String get clearAllCacheTitle => '全キャッシュ削除';

  @override
  String get clearAllCacheContent => 'すべての翻訳キャッシュを削除しますか？この操作は元に戻せません。';

  @override
  String get clearAllCacheConfirm => 'すべて削除';

  @override
  String get cacheCleared => 'キャッシュを削除しました';

  @override
  String get clearAllCacheDone => 'すべてのキャッシュを削除しました';

  @override
  String get about => 'について';

  @override
  String get engine => '翻訳エンジン';

  @override
  String get version => 'バージョン';

  @override
  String get detectingStatus => 'モジュール状態を検出中...';

  @override
  String get language => '言語';

  @override
  String get languageTitle => 'インターフェース言語';

  @override
  String get settings => '設定';

  @override
  String get themeSystem => 'システムに従う';

  @override
  String get themeLight => 'ライト';

  @override
  String get themeDark => 'ダーク';

  @override
  String get cachedTranslations => 'キャッシュ翻訳';

  @override
  String get clearCacheFailed => '削除失敗';

  @override
  String get appSubtitle => 'LSPosed グローバルリアルタイム翻訳モジュール';

  @override
  String get engineDesc => 'OpenAI-compatible API';

  @override
  String get apiUrlHint => 'https://api.deepseek.com/v1 or .../chat/completions';

  @override
  String get apiKeyHint => 'optional for local servers';

  @override
  String timeoutSeconds(Object seconds) {
    return '$seconds秒';
  }

  @override
  String get temperatureDesc => '低いほど確定的、高いほど創造的';

  @override
  String get maxTokensDesc => '1回の応答の最大長';

  @override
  String get batchConfig => 'バッチ設定';

  @override
  String get batchSize => 'バッチサイズ';

  @override
  String get batchSizeDesc => 'APIリクエストあたりの最大テキスト数';

  @override
  String get batchWindow => 'バッチウィンドウ';

  @override
  String get batchWindowDesc => 'テキストを待機する時間（ミリ秒）';

  @override
  String get translationCache => '翻訳キャッシュ';

  @override
  String get translationCacheDesc => '翻訳結果をローカルにキャッシュし、API呼び出しを繰り返さない';

  @override
  String get deepseekInfo => 'Any OpenAI-compatible API. Use a base URL (https://host/v1) or the full /chat/completions URL. Fetch models from /models, or type a model id. API key can be empty for local servers.';

  @override
  String get promptHint => 'タップして翻訳プロンプトを編集...';

  @override
  String get restoreDefault => 'デフォルトに戻す';

  @override
  String get promptEditHint => '翻訳プロンプトを入力...';

  @override
  String get apiUrlEmpty => 'API URLを入力してください';

  @override
  String get cacheDetail => 'キャッシュ詳細';

  @override
  String get clearAll => 'すべて削除';

  @override
  String get noCacheData => 'キャッシュデータがありません';

  @override
  String get clearAppCache => 'アプリキャッシュを削除';

  @override
  String clearAppCacheContent(Object package) {
    return '$package のキャッシュ統計を削除しますか？';
  }

  @override
  String appCacheCleared(Object package) {
    return '$package のキャッシュを削除しました';
  }

  @override
  String get clearAllCacheStatsContent => 'すべてのアプリのキャッシュ統計を削除しますか？';

  @override
  String cacheEntry(Object count, Object package) {
    return '$count 件 · $package';
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
