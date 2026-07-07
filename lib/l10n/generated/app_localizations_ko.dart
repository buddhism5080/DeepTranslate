// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Korean (`ko`).
class AppLocalizationsKo extends AppLocalizations {
  AppLocalizationsKo([String locale = 'ko']) : super(locale);

  @override
  String get appTitle => 'DeepTranslate';

  @override
  String get navHome => '홈';

  @override
  String get navTranslateConfig => '번역 설정';

  @override
  String get navSettings => '설정';

  @override
  String get moduleStatus => '모듈 상태';

  @override
  String get activated => '활성화됨';

  @override
  String get notActivated => '비활성화';

  @override
  String get enableInLSPosed => 'LSPosed에서 활성화하고 대상 앱을 선택하세요';

  @override
  String get translationStatus => '번역 상태';

  @override
  String get globalTranslation => '전체 번역';

  @override
  String get globalTranslationDesc => '선택한 앱의 텍스트를 번역합니다';

  @override
  String get accountBalance => '잔액';

  @override
  String get noApiKey => 'API 키 미설정';

  @override
  String get fetchFailed => '가져오기 실패';

  @override
  String get totalTokens => '누적 토큰 사용량';

  @override
  String get resetStats => '통계 초기화';

  @override
  String get resetTokenTitle => '토큰 통계 초기화';

  @override
  String get resetTokenContent => '누적 토큰 수를 초기화하시겠습니까?';

  @override
  String get cancel => '취소';

  @override
  String get confirm => '확인';

  @override
  String get usageNotes => '사용 방법';

  @override
  String get note1 => '1. DeepSeek API 키 입력';

  @override
  String get note2 => '2. LSPosed에서 모듈 활성화';

  @override
  String get note3 => '3. 대상 앱 재시작';

  @override
  String get note4 => '4. 번역 결과 캐시 저장';

  @override
  String get note5 => '5. 배치 모드로 문맥 인식';

  @override
  String get refresh => '새로고침';

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
  String get save => '저장';

  @override
  String get configSaved => '저장됨';

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
  String get cacheManagement => '캐시 관리';

  @override
  String cachedCount(Object count) {
    return '$count개 · 상세 보기';
  }

  @override
  String get clearAllCache => '캐시 삭제';

  @override
  String get clearAllCacheTitle => '캐시 삭제';

  @override
  String get clearAllCacheContent => '모든 번역 캐시를 삭제하시겠습니까?';

  @override
  String get clearAllCacheConfirm => '전체 삭제';

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
  String get detectingStatus => '감지 중...';

  @override
  String get language => '언어';

  @override
  String get languageTitle => '인터페이스 언어';

  @override
  String get settings => '설정';

  @override
  String get themeSystem => '시스템 따르기';

  @override
  String get themeLight => '라이트';

  @override
  String get themeDark => '다크';

  @override
  String get cachedTranslations => '캐시된 번역';

  @override
  String get clearCacheFailed => '삭제 실패';

  @override
  String get appSubtitle => 'LSPosed 전역 실시간 번역 모듈';

  @override
  String get engineDesc => 'DeepSeek API (OpenAI 호환)';

  @override
  String get apiUrlHint => 'https://api.deepseek.com/v1/chat/completions';

  @override
  String get apiKeyHint => 'sk-xxxxxxxxxxxxxxxx';

  @override
  String timeoutSeconds(Object seconds) {
    return '$seconds초';
  }

  @override
  String get temperatureDesc => '낮을수록 결정적, 높을수록 창의적';

  @override
  String get maxTokensDesc => '최대 응답 길이';

  @override
  String get batchConfig => '배치 설정';

  @override
  String get batchSize => '배치 크기';

  @override
  String get batchSizeDesc => 'API 요청당 최대 텍스트 수';

  @override
  String get batchWindow => '배치 창';

  @override
  String get batchWindowDesc => '더 많은 텍스트를 기다리는 시간(ms)';

  @override
  String get translationCache => '번역 캐시';

  @override
  String get translationCacheDesc => '번역을 로컬에 캐시하여 API 호출 중복 방지';

  @override
  String get deepseekInfo =>
      'DeepSeek API는 OpenAI 프로토콜과 호환됩니다. 기본적으로 deepseek-chat 모델을 사용하며, 프롬프트 캐싱을 지원하여 반복 요청 비용을 줄입니다. 배치 모드는 같은 페이지의 여러 텍스트를 컨텍스트와 함께 집계하여 단어 단위 번역의 부자연스러움을 방지합니다.';

  @override
  String get promptHint => '탭하여 번역 프롬프트 편집...';

  @override
  String get restoreDefault => '기본값 복원';

  @override
  String get promptEditHint => '번역 프롬프트 입력...';

  @override
  String get apiUrlEmpty => 'API URL을 입력하세요';

  @override
  String get cacheDetail => '캐시 상세';

  @override
  String get clearAll => '전체 삭제';

  @override
  String get noCacheData => '캐시 데이터 없음';

  @override
  String get clearAppCache => '앱 캐시 삭제';

  @override
  String clearAppCacheContent(Object package) {
    return '$package의 캐시 통계를 삭제하시겠습니까?';
  }

  @override
  String appCacheCleared(Object package) {
    return '$package의 캐시가 삭제되었습니다';
  }

  @override
  String get clearAllCacheStatsContent => '모든 앱의 캐시 통계를 삭제하시겠습니까?';

  @override
  String cacheEntry(Object count, Object package) {
    return '$count개 · $package';
  }
}
