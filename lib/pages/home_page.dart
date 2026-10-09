import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:url_launcher/url_launcher.dart';
import '../controllers/settings_controller.dart';
import '../services/app_info_service.dart';
import '../widgets/blur_app_bar.dart';
import '../widgets/section_label.dart';
import '../l10n/generated/app_localizations.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final _ctrl = SettingsController.instance;
  String _version = '';
  bool? _moduleActive;

  @override
  void initState() {
    super.initState();
    _ctrl.addListener(_onChanged);
    _loadInfo();
  }

  @override
  void dispose() {
    _ctrl.removeListener(_onChanged);
    super.dispose();
  }

  void _onChanged() {
    if (mounted) setState(() {});
  }

  void _loadInfo() async {
    final version = await AppInfoService.getVersion();
    final buildTime = await AppInfoService.getBuildTime();
    if (mounted) setState(() => _version = 'v$version-$buildTime');

    try {
      final status = await AppInfoService.getModuleStatus();
      if (mounted) setState(() => _moduleActive = status['active'] as bool? ?? false);
    } catch (_) {
      if (mounted) setState(() => _moduleActive = false);
    }
  }

  void _showLanguagePicker() {
    final l10n = AppLocalizations.of(context)!;
    final languages = {
      'zh': '中文', 'en': 'English', 'ja': '日本語',
      'es': 'Español', 'de': 'Deutsch', 'fr': 'Français',
      'ko': '한국어', 'ru': 'Русский',
    };

    showDialog(
      context: context,
      builder: (ctx) => SimpleDialog(
        title: Text(l10n.languageTitle),
        children: [
          RadioGroup<Locale?>(
            groupValue: _ctrl.locale,
            onChanged: (v) {
              _ctrl.setLocale(v);
              Navigator.pop(ctx);
            },
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                RadioListTile<Locale?>(title: const Text('跟随系统'), value: null),
                ...languages.entries.map((e) => RadioListTile<Locale?>(
                  title: Text(e.value),
                  value: Locale(e.key),
                )),
              ],
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final l10n = AppLocalizations.of(context)!;
    final bottomPad = _ctrl.blurBars ? 80.0 : 0.0;

    return Scaffold(
      backgroundColor: cs.surface,
      body: BlurAppBarHost(
        title: l10n.appTitle,
        titleWidget: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(l10n.appTitle, style: Theme.of(context).textTheme.headlineMedium?.copyWith(color: cs.onSurface)),
            if (_version.isNotEmpty)
              Text(_version, style: Theme.of(context).textTheme.bodySmall?.copyWith(color: cs.onSurfaceVariant)),
          ],
        ),
        largeTitle: true,
        actions: [
          IconButton(
            icon: const FaIcon(FontAwesomeIcons.language, size: 18),
            onPressed: _showLanguagePicker,
            tooltip: l10n.language,
          ),
          IconButton(
            icon: FaIcon(FontAwesomeIcons.github, color: cs.onSurfaceVariant.withValues(alpha: 0.7), size: 20),
            onPressed: () => launchUrl(Uri.parse('https://github.com/sakukir'), mode: LaunchMode.externalApplication),
            tooltip: 'GitHub',
          ),
        ],
        bottomPadding: bottomPad,
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            sliver: SliverList(
              delegate: SliverChildListDelegate([
                _ModuleStatusCard(active: _moduleActive, l10n: l10n),
                const SizedBox(height: 16),

                SectionLabel(l10n.translationStatus),
                const SizedBox(height: 8),
                Card(
                  elevation: 0, color: cs.surfaceContainerHighest,
                  child: SwitchListTile(
                    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                    title: Text(l10n.globalTranslation),
                    subtitle: Text(l10n.globalTranslationDesc),
                    value: _ctrl.translationEnabled,
                    onChanged: (value) async => _ctrl.setTranslationEnabled(value),
                  ),
                ),
                const SizedBox(height: 24),

                SectionLabel(l10n.usageNotes),
                const SizedBox(height: 8),
                const _NotesCard(),
                const SizedBox(height: 24),
              ]),
            ),
          ),
        ],
      ),
    );
  }
}

class _ModuleStatusCard extends StatelessWidget {
  final bool? active;
  final AppLocalizations l10n;
  const _ModuleStatusCard({this.active, required this.l10n});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    if (active == null) {
      return Card(elevation: 0, color: cs.surfaceContainerHighest,
        child: Padding(padding: const EdgeInsets.all(20),
          child: Row(children: [
            const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2)),
            const SizedBox(width: 16), Text(l10n.detectingStatus),
          ]),
        ),
      );
    }
    final bool isActive = active!;
    final color = isActive ? Colors.green : cs.error;
    final bgColor = isActive ? Colors.green.withValues(alpha: 0.12) : cs.errorContainer;
    return Card(elevation: 0, color: bgColor,
      child: Padding(padding: const EdgeInsets.all(20),
        child: Row(children: [
          Container(width: 48, height: 48,
            decoration: BoxDecoration(color: color.withValues(alpha: 0.15), shape: BoxShape.circle),
            child: Icon(isActive ? Icons.check_circle : Icons.cancel, color: color, size: 28),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(l10n.moduleStatus, style: Theme.of(context).textTheme.labelLarge?.copyWith(color: color.withValues(alpha: 0.8))),
              const SizedBox(height: 2),
              Text(isActive ? l10n.activated : l10n.notActivated,
                  style: Theme.of(context).textTheme.titleLarge?.copyWith(color: color, fontWeight: FontWeight.bold)),
              if (!isActive) ...[
                const SizedBox(height: 4),
                Text(l10n.enableInLSPosed, style: Theme.of(context).textTheme.bodySmall?.copyWith(color: color.withValues(alpha: 0.7))),
              ],
            ]),
          ),
        ]),
      ),
    );
  }
}

class _NotesCard extends StatelessWidget {
  const _NotesCard();
  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final l10n = AppLocalizations.of(context)!;
    final items = [l10n.note1, l10n.note2, l10n.note3, l10n.note4, l10n.note5];
    return Card(elevation: 0, color: cs.surfaceContainerHighest,
      child: Padding(padding: const EdgeInsets.all(16),
        child: Column(
          children: items.map((text) => Padding(
            padding: const EdgeInsets.symmetric(vertical: 6),
            child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Icon(Icons.arrow_right, size: 20, color: cs.onSurfaceVariant),
              const SizedBox(width: 4),
              Expanded(child: Text(text, style: Theme.of(context).textTheme.bodyMedium?.copyWith(color: cs.onSurfaceVariant))),
            ]),
          )).toList(),
        ),
      ),
    );
  }
}
