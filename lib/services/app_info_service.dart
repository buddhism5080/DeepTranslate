import 'package:flutter/services.dart';

class AppInfoService {
  static const _channel = MethodChannel('io.github.kirby.deeptranslate/channel');

  static Future<String> getVersion() async {
    final version = await _channel.invokeMethod<String>('getAppVersion');
    return version ?? '';
  }

  static Future<String> getBuildTime() async {
    final buildTime = await _channel.invokeMethod<String>('getBuildTime');
    return buildTime ?? '';
  }

  static Future<Map<String, dynamic>> getModuleStatus() async {
    final result = await _channel.invokeMethod<Map>('getModuleStatus');
    return Map<String, dynamic>.from(result ?? {});
  }

  static Future<int> getCacheCount() async {
    final count = await _channel.invokeMethod<int>('getCacheCount');
    return count ?? 0;
  }

  static Future<List<Map<String, dynamic>>> getCacheDetails() async {
    final result = await _channel.invokeListMethod<Map>('getCacheDetails');
    if (result == null) return [];
    return result.map((m) => Map<String, dynamic>.from(m)).toList();
  }

  static Future<bool> clearAppCache(String pkg) async {
    final r = await _channel.invokeMethod<bool>('clearAppCache', {'package': pkg});
    return r ?? false;
  }

  static Future<bool> clearAllCache() async {
    final r = await _channel.invokeMethod<bool>('clearAllCache');
    return r ?? false;
  }
}
