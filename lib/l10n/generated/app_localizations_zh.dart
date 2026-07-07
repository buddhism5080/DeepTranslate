// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Chinese (`zh`).
class AppLocalizationsZh extends AppLocalizations {
  AppLocalizationsZh([String locale = 'zh']) : super(locale);

  @override
  String get appTitle => 'DeepTranslate';

  @override
  String get navHome => '首页';

  @override
  String get navTranslateConfig => '翻译配置';

  @override
  String get navSettings => '设置';

  @override
  String get moduleStatus => '模块状态';

  @override
  String get activated => '已激活';

  @override
  String get notActivated => '未激活';

  @override
  String get enableInLSPosed => '请在 LSPosed 中启用模块并勾选目标 App 作用域';

  @override
  String get translationStatus => '翻译状态';

  @override
  String get globalTranslation => '全局翻译';

  @override
  String get globalTranslationDesc => '开启后 Hook 所有勾选 App 的页面文字';

  @override
  String get accountBalance => '账户余额';

  @override
  String get noApiKey => '未配置 API Key';

  @override
  String get fetchFailed => '获取失败';

  @override
  String get totalTokens => '累计消耗 Token';

  @override
  String get resetStats => '重置统计';

  @override
  String get resetTokenTitle => '重置 Token 统计';

  @override
  String get resetTokenContent => '确定重置累计消耗 Token？此操作不可撤销。';

  @override
  String get cancel => '取消';

  @override
  String get confirm => '确定';

  @override
  String get usageNotes => '使用说明';

  @override
  String get note1 => '1. 在「翻译配置」页填入 DeepSeek API Key';

  @override
  String get note2 => '2. 在 LSPosed 中启用模块，勾选要翻译的 App';

  @override
  String get note3 => '3. 重启目标 App，页面文字将自动翻译';

  @override
  String get note4 => '4. 翻译结果会缓存，下次不重复翻译';

  @override
  String get note5 => '5. 批处理模式自动聚合上下文，避免机翻失真';

  @override
  String get refresh => '刷新';

  @override
  String get apiConfig => 'API 配置';

  @override
  String get apiUrl => 'API URL';

  @override
  String get apiKey => 'API Key';

  @override
  String get model => '模型';

  @override
  String get testConn => '测试连接';

  @override
  String get testing => '测试中...';

  @override
  String get fetchModels => '获取模型';

  @override
  String get translatePrompt => '翻译 Prompt';

  @override
  String get timeout => '超时时间';

  @override
  String get temperature => '温度';

  @override
  String get maxTokens => '最大 Token';

  @override
  String get save => '保存';

  @override
  String get configSaved => '配置已保存';

  @override
  String get appearance => '外观';

  @override
  String get themeColor => '主题颜色';

  @override
  String get blurGlass => '毛玻璃效果';

  @override
  String get blurGlassDesc => '顶栏与底栏模糊背景';

  @override
  String get translationBehavior => '翻译行为';

  @override
  String get toastNotification => '翻译 Toast 提示';

  @override
  String get toastNotificationDesc => '翻译时显示提示';

  @override
  String get debugLog => '调试日志';

  @override
  String get debugLogDesc => '输出详细 Hook 日志到 logcat';

  @override
  String get cacheManagement => '缓存管理';

  @override
  String cachedCount(Object count) {
    return '$count 条 · 点击查看详情';
  }

  @override
  String get clearAllCache => '清空所有缓存';

  @override
  String get clearAllCacheTitle => '清空所有缓存';

  @override
  String get clearAllCacheContent => '确定清空所有应用的翻译缓存？此操作不可撤销。';

  @override
  String get clearAllCacheConfirm => '确定清空';

  @override
  String get cacheCleared => '缓存已清空';

  @override
  String get clearAllCacheDone => '已清空所有缓存';

  @override
  String get about => '关于';

  @override
  String get engine => '翻译引擎';

  @override
  String get version => '版本';

  @override
  String get detectingStatus => '正在检测模块状态...';

  @override
  String get language => '语言';

  @override
  String get languageTitle => '界面语言';

  @override
  String get settings => '设置';

  @override
  String get themeSystem => '跟随系统';

  @override
  String get themeLight => '浅色';

  @override
  String get themeDark => '深色';

  @override
  String get cachedTranslations => '已缓存翻译';

  @override
  String get clearCacheFailed => '清空失败';

  @override
  String get appSubtitle => 'LSPosed 全局实时翻译模块';

  @override
  String get engineDesc => 'DeepSeek API (OpenAI 兼容)';

  @override
  String get apiUrlHint => 'https://api.deepseek.com/v1/chat/completions';

  @override
  String get apiKeyHint => 'sk-xxxxxxxxxxxxxxxx';

  @override
  String timeoutSeconds(Object seconds) {
    return '$seconds 秒';
  }

  @override
  String get temperatureDesc => '越低越确定，越高越有创造性';

  @override
  String get maxTokensDesc => '单次响应上限';

  @override
  String get batchConfig => '批处理配置';

  @override
  String get batchSize => '批量大小';

  @override
  String get batchSizeDesc => '每次 API 请求的最大文本数';

  @override
  String get batchWindow => '聚合窗口';

  @override
  String get batchWindowDesc => '等待更多文本的时间（毫秒）';

  @override
  String get translationCache => '翻译缓存';

  @override
  String get translationCacheDesc => '翻译结果缓存到本地，避免重复调用 API';

  @override
  String get deepseekInfo =>
      'DeepSeek API 兼容 OpenAI 协议。默认使用 deepseek-chat 模型，支持 prompt caching 可降低重复请求成本。批处理模式会将同一页面的多条文本聚合并附带上下文发送，避免单词条翻译的机翻失真。';

  @override
  String get promptHint => '点击编辑翻译指令...';

  @override
  String get restoreDefault => '恢复默认';

  @override
  String get promptEditHint => '输入翻译指令...';

  @override
  String get apiUrlEmpty => 'API URL 不能为空';

  @override
  String get cacheDetail => '缓存详情';

  @override
  String get clearAll => '清空所有';

  @override
  String get noCacheData => '暂无缓存数据';

  @override
  String get clearAppCache => '清空该应用缓存';

  @override
  String clearAppCacheContent(Object package) {
    return '确定清空 $package 的翻译缓存统计？';
  }

  @override
  String appCacheCleared(Object package) {
    return '已清空 $package 的缓存';
  }

  @override
  String get clearAllCacheStatsContent => '确定清空所有应用的翻译缓存统计？';

  @override
  String cacheEntry(Object count, Object package) {
    return '$count 条缓存 · $package';
  }
}
