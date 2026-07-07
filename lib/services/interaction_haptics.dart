import 'dart:async';
import 'package:flutter/services.dart';

/// 触感反馈封装。抄自 HyperIsland。
class InteractionHaptics {
  static const MethodChannel _channel = MethodChannel(
    'io.github.kirby.deeptranslate/haptics',
  );

  static DateTime? _lastSliderTickAt;

  static Future<void> button({bool force = false}) async {
    await _invoke('button');
  }

  static Future<void> toggle({bool force = false}) async {
    await _invoke('toggle');
  }

  static Future<void> sliderTick({bool force = false}) async {
    final now = DateTime.now();
    if (_lastSliderTickAt != null &&
        now.difference(_lastSliderTickAt!) < const Duration(milliseconds: 32)) {
      return;
    }
    _lastSliderTickAt = now;
    await _invoke('sliderTick');
  }

  static VoidCallback? interceptButton(
    FutureOr<void> Function()? onPressed, {
    bool force = false,
  }) {
    if (onPressed == null) return null;
    return () {
      unawaited(button(force: force));
      final result = onPressed();
      if (result is Future<void>) unawaited(result);
    };
  }

  static ValueChanged<bool>? interceptToggle(
    FutureOr<void> Function(bool value)? onChanged, {
    bool force = false,
  }) {
    if (onChanged == null) return null;
    return (value) {
      unawaited(toggle(force: force));
      final result = onChanged(value);
      if (result is Future<void>) unawaited(result);
    };
  }

  static ValueChanged<double>? interceptSlider(
    ValueChanged<double>? onChanged, {
    bool force = false,
  }) {
    if (onChanged == null) return null;
    return (value) {
      unawaited(sliderTick(force: force));
      onChanged(value);
    };
  }

  static Future<void> _invoke(String method) async {
    try {
      await _channel.invokeMethod<void>(method);
    } catch (_) {
      switch (method) {
        case 'toggle':
          await HapticFeedback.selectionClick();
          break;
        case 'sliderTick':
          await HapticFeedback.selectionClick();
          break;
        default:
          await HapticFeedback.lightImpact();
      }
    }
  }
}
