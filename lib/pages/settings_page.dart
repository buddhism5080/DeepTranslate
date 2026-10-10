import 'package:flutter/material.dart';
import '../controllers/settings_controller.dart';
import '../l10n/generated/app_localizations.dart';
import '../services/app_info_service.dart';
import '../widgets/blur_app_bar.dart';
import '../widgets/section_label.dart';
import 'cache_detail_page.dart';
import 'error_log_page.dart';

class SettingsPage extends StatefulWidget {
  const SettingsPage({super.key});

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> with WidgetsBindingObserver {
  final _ctrl = SettingsController.instance;
  int _cacheCount = 0;
  String _version = '';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _ctrl.addListener(_onChanged);
    _loadCacheCount();
    AppInfoService.getVersion().then((version) {
      if (mounted && version.isNotEmpty) setState(() => _version = version);
    });
  }

  void _loadCacheCount() async {
    try {
      final count = await AppInfoService.getCacheCount();
      if (mounted) setState(() => _cacheCount = count);
    } catch (_) {}
  }

  void _editCacheLimit() async {
    final l10n = AppLocalizations.of(context)!;
    final controller = TextEditingController(text: _ctrl.cacheLimit.toString());
    final next = await showDialog<int>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.cacheLimit),
        content: TextField(
          controller: controller,
          keyboardType: TextInputType.number,
          decoration: InputDecoration(hintText: l10n.cacheLimitHint),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: Text(l10n.cancel)),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, int.tryParse(controller.text.trim())),
            child: Text(l10n.confirm),
          ),
        ],
      ),
    );
    if (next == null) return;
    await _ctrl.setCacheLimit(next);
  }

  void _clearCache() async {
    final l10n = AppLocalizations.of(context)!;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.clearAllCacheTitle),
        content: Text(l10n.clearAllCacheContent),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: Text(l10n.cancel)),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: Text(l10n.clearAllCacheConfirm)),
        ],
      ),
    );
    if (confirmed != true) return;

    try {
      await AppInfoService.clearAllCache();
      if (mounted) {
        setState(() => _cacheCount = 0);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(l10n.clearAllCacheDone),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('${l10n.clearCacheFailed}: $e')));
      }
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _ctrl.removeListener(_onChanged);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _loadCacheCount();
  }

  void _onChanged() {
    if (!mounted) return;
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final l10n = AppLocalizations.of(context)!;
    final bottomPad = _ctrl.blurBars ? 80.0 : 0.0;

    return Scaffold(
      backgroundColor: cs.surface,
      body: BlurAppBarHost(
        title: l10n.settings,
        largeTitle: true,
        physics: const ClampingScrollPhysics(),
        bottomPadding: bottomPad,
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            sliver: SliverList(
              delegate: SliverChildListDelegate([
                const SizedBox(height: 8),

                // ── 外观 ──
                SectionLabel(l10n.appearance),
                const SizedBox(height: 8),
                Card(
                  elevation: 0,
                  color: cs.surfaceContainerHighest,
                  child: Column(
                    children: [
                      ListTile(
                        leading: const Icon(Icons.color_lens_outlined),
                        title: Text(l10n.themeColor),
                        subtitle: Text(_themeLabel(l10n)),
                        trailing: const Icon(Icons.chevron_right),
                        shape: const RoundedRectangleBorder(
                          borderRadius: BorderRadius.vertical(
                            top: Radius.circular(16),
                          ),
                        ),
                        onTap: _showThemeColorDialog,
                      ),
                      const Divider(height: 1, indent: 16, endIndent: 16),
                      SwitchListTile(
                        secondary: const Icon(Icons.blur_on),
                        title: Text(l10n.blurGlass),
                        subtitle: Text(l10n.blurGlassDesc),
                        value: _ctrl.blurBars,
                        onChanged: (v) => _ctrl.setBlurBars(v),
                        shape: const RoundedRectangleBorder(
                          borderRadius: BorderRadius.vertical(
                            bottom: Radius.circular(16),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 8),

                // ── 翻译行为 ──
                SectionLabel(l10n.translationBehavior),
                const SizedBox(height: 8),
                Card(
                  elevation: 0,
                  color: cs.surfaceContainerHighest,
                  child: Column(
                    children: [
                      SwitchListTile(
                        secondary: const Icon(Icons.notifications),
                        title: Text(l10n.toastNotification),
                        subtitle: Text(l10n.toastNotificationDesc),
                        value: _ctrl.translateToast,
                        onChanged: (v) => _ctrl.setTranslateToast(v),
                        shape: const RoundedRectangleBorder(
                          borderRadius: BorderRadius.vertical(
                            top: Radius.circular(16),
                          ),
                        ),
                      ),
                      const Divider(height: 1, indent: 16, endIndent: 16),
                      SwitchListTile(
                        secondary: const Icon(Icons.subtitles_outlined),
                        title: Text(l10n.bilingual),
                        subtitle: Text(l10n.bilingualDesc),
                        value: _ctrl.bilingual,
                        onChanged: (v) => _ctrl.setBilingual(v),
                      ),
                      const Divider(height: 1, indent: 16, endIndent: 16),
                      SwitchListTile(
                        secondary: const Icon(Icons.bug_report),
                        title: Text(l10n.debugLog),
                        subtitle: Text(l10n.debugLogDesc),
                        value: _ctrl.debugLog,
                        onChanged: (v) => _ctrl.setDebugLog(v),
                        shape: const RoundedRectangleBorder(
                          borderRadius: BorderRadius.vertical(
                            bottom: Radius.circular(16),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 8),

                SectionLabel(l10n.coverageTitle),
                const SizedBox(height: 8),
                Card(
                  elevation: 0,
                  color: cs.surfaceContainerHighest,
                  child: Column(
                    children: [
                      SwitchListTile(
                        secondary: const Icon(Icons.view_agenda_outlined),
                        title: Text(l10n.hookLayout),
                        subtitle: Text(l10n.hookLayoutDesc),
                        value: _ctrl.hookLayout,
                        onChanged: (v) => _ctrl.setHookLayout(v),
                      ),
                      const Divider(height: 1, indent: 16, endIndent: 16),
                      SwitchListTile(
                        secondary: const Icon(Icons.public),
                        title: Text(l10n.hookWebView),
                        subtitle: Text(l10n.hookWebViewDesc),
                        value: _ctrl.hookWebView,
                        onChanged: (v) => _ctrl.setHookWebView(v),
                      ),
                      const Divider(height: 1, indent: 16, endIndent: 16),
                      SwitchListTile(
                        secondary: const Icon(Icons.layers_outlined),
                        title: Text(l10n.hookCompose),
                        subtitle: Text(l10n.hookComposeDesc),
                        value: _ctrl.hookCompose,
                        onChanged: (v) => _ctrl.setHookCompose(v),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 8),

                // ── 缓存管理 ──
                SectionLabel(l10n.cacheManagement),
                const SizedBox(height: 8),
                Card(
                  elevation: 0,
                  color: cs.surfaceContainerHighest,
                  child: Column(
                    children: [
                      ListTile(
                        leading: const Icon(Icons.storage),
                        title: Text(l10n.cachedTranslations),
                        subtitle: Text(l10n.cachedCount(_cacheCount)),
                        trailing: const Icon(Icons.chevron_right),
                        shape: const RoundedRectangleBorder(
                          borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
                        ),
                        onTap: () {
                          Navigator.push(context,
                            MaterialPageRoute(builder: (_) => const CacheDetailPage()),
                          ).then((_) => _loadCacheCount());
                        },
                      ),
                      const Divider(height: 1, indent: 16, endIndent: 16),
                      ListTile(
                        leading: const Icon(Icons.filter_alt_outlined),
                        title: Text(l10n.cacheLimit),
                        subtitle: Text(l10n.cacheLimitValue(_ctrl.cacheLimit)),
                        onTap: _editCacheLimit,
                      ),
                      const Divider(height: 1, indent: 16, endIndent: 16),
                      ListTile(
                        leading: const Icon(Icons.delete_outline, color: Colors.red),
                        title: Text(l10n.clearAllCache),
                        shape: const RoundedRectangleBorder(
                          borderRadius: BorderRadius.vertical(bottom: Radius.circular(16)),
                        ),
                        onTap: _clearCache,
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 8),

                SectionLabel(l10n.errorLog),
                const SizedBox(height: 8),
                Card(
                  elevation: 0,
                  color: cs.surfaceContainerHighest,
                  child: ListTile(
                    leading: const Icon(Icons.error_outline),
                    title: Text(l10n.errorLog),
                    trailing: const Icon(Icons.chevron_right),
                    onTap: () {
                      Navigator.push(context, MaterialPageRoute(builder: (_) => const ErrorLogPage()));
                    },
                  ),
                ),
                const SizedBox(height: 8),

                // ── 关于 ──
                SectionLabel(l10n.about),
                const SizedBox(height: 8),
                Card(
                  elevation: 0,
                  color: cs.surfaceContainerHighest,
                  child: Column(
                    children: [
                      ListTile(
                        leading: const Icon(Icons.info_outline),
                        title: Text(l10n.appTitle),
                        subtitle: Text(l10n.appSubtitle),
                        shape: const RoundedRectangleBorder(
                          borderRadius: BorderRadius.vertical(
                            top: Radius.circular(16),
                          ),
                        ),
                      ),
                      const Divider(height: 1, indent: 16, endIndent: 16),
                      ListTile(
                        leading: const Icon(Icons.psychology),
                        title: Text(l10n.engine),
                        subtitle: Text(l10n.engineDesc),
                      ),
                      const Divider(height: 1, indent: 16, endIndent: 16),
                      ListTile(
                        leading: const Icon(Icons.code),
                        title: Text(l10n.version),
                        subtitle: Text(_version.isEmpty ? '…' : _version),
                        shape: const RoundedRectangleBorder(
                          borderRadius: BorderRadius.vertical(
                            bottom: Radius.circular(16),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 32),
              ]),
            ),
          ),
        ],
      ),
    );
  }

  String _themeLabel(AppLocalizations l10n) {
    return switch (_ctrl.themeMode) {
      ThemeMode.system => l10n.themeSystem,
      ThemeMode.light => l10n.themeLight,
      ThemeMode.dark => l10n.themeDark,
    };
  }

  void _showThemeColorDialog() {
    final l10n = AppLocalizations.of(context)!;
    final colors = [
      0xFF6750A4, // 紫
      0xFF0061A4, // 蓝
      0xFF006C4C, // 绿
      0xFFB3261E, // 红
      0xFFFF6D00, // 橙
      0xFF8E24AA, // 深紫
      0xFF0D47A1, // 深蓝
    ];

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.themeColor),
        content: Wrap(
          spacing: 12,
          runSpacing: 12,
          children: colors.map((color) {
            final selected = _ctrl.themeSeedColor == color;
            return GestureDetector(
              onTap: () {
                _ctrl.setThemeSeedColor(color);
                Navigator.pop(ctx);
              },
              child: Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: Color(color),
                  shape: BoxShape.circle,
                  border: selected
                      ? Border.all(color: Theme.of(context).colorScheme.primary, width: 3)
                      : null,
                ),
                child: selected
                    ? const Icon(Icons.check, color: Colors.white)
                    : null,
              ),
            );
          }).toList(),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: Text(l10n.cancel),
          ),
        ],
      ),
    );
  }
}
