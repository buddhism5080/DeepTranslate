import 'dart:typed_data';
import 'package:flutter/material.dart';
import '../l10n/generated/app_localizations.dart';
import '../services/app_info_service.dart';
import '../widgets/blur_app_bar.dart';

class ErrorLogPage extends StatefulWidget {
  const ErrorLogPage({super.key});

  @override
  State<ErrorLogPage> createState() => _ErrorLogPageState();
}

class _ErrorLogPageState extends State<ErrorLogPage> {
  List<Map<String, dynamic>> _apps = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final apps = await AppInfoService.listErrorLogs();
      if (mounted) setState(() { _apps = apps; _loading = false; });
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _clearAll() async {
    final l10n = AppLocalizations.of(context)!;
    final confirmed = await _confirm(l10n.clearAllErrors, l10n.clearAllErrorsContent);
    if (confirmed != true) return;
    await AppInfoService.clearErrorLogs();
    await _load();
    _toast(l10n.errorsCleared);
  }

  Future<bool?> _confirm(String title, String body) {
    final l10n = AppLocalizations.of(context)!;
    return showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(title),
        content: Text(body),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: Text(l10n.cancel)),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: Text(l10n.confirm)),
        ],
      ),
    );
  }

  void _toast(String text) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(text), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10))),
    );
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      backgroundColor: cs.surface,
      body: BlurAppBarHost(
        title: l10n.errorLog,
        largeTitle: true,
        actions: [
          IconButton(
            icon: const Icon(Icons.delete_sweep),
            tooltip: l10n.clearAll,
            onPressed: _apps.isEmpty ? null : _clearAll,
          ),
        ],
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            sliver: _loading
                ? const SliverFillRemaining(child: Center(child: CircularProgressIndicator()))
                : _apps.isEmpty
                    ? SliverFillRemaining(
                        child: Center(
                          child: Column(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.inbox_outlined, size: 48, color: Colors.grey),
                              const SizedBox(height: 12),
                              Text(l10n.noErrorLog),
                            ],
                          ),
                        ),
                      )
                    : SliverList(
                        delegate: SliverChildBuilderDelegate(
                          (context, index) {
                            final app = _apps[index];
                            final entries = (app['entries'] as List?) ?? const [];
                            return ListTile(
                              leading: _icon(app['icon'] as Uint8List?, cs),
                              title: Text(app['name'] as String? ?? '', maxLines: 1, overflow: TextOverflow.ellipsis),
                              subtitle: Text(
                                l10n.errorLogEntry(entries.length, app['package'] as String? ?? ''),
                                style: TextStyle(fontSize: 11, fontFamily: 'monospace', color: cs.onSurfaceVariant),
                              ),
                              trailing: const Icon(Icons.chevron_right),
                              tileColor: cs.surfaceContainerHighest,
                              shape: _radius(index == 0, index == _apps.length - 1),
                              onTap: () async {
                                await Navigator.push(context, MaterialPageRoute(
                                  builder: (_) => _ErrorLogDetail(packageName: app['package'] as String),
                                ));
                                _load();
                              },
                            );
                          },
                          childCount: _apps.length,
                        ),
                      ),
          ),
        ],
      ),
    );
  }

  Widget _icon(Uint8List? bytes, ColorScheme cs) {
    if (bytes != null && bytes.isNotEmpty) {
      return ClipRRect(
        borderRadius: BorderRadius.circular(8),
        child: Image.memory(bytes, width: 40, height: 40, fit: BoxFit.cover),
      );
    }
    return CircleAvatar(
      backgroundColor: cs.primaryContainer,
      child: Icon(Icons.android, color: cs.onPrimaryContainer, size: 20),
    );
  }
}

class _ErrorLogDetail extends StatefulWidget {
  final String packageName;
  const _ErrorLogDetail({required this.packageName});

  @override
  State<_ErrorLogDetail> createState() => _ErrorLogDetailState();
}

class _ErrorLogDetailState extends State<_ErrorLogDetail> {
  List<Map<String, dynamic>> _entries = [];
  final Set<String> _selected = {};
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final apps = await AppInfoService.listErrorLogs();
    Map<String, dynamic>? match;
    for (final app in apps) {
      if (app['package'] == widget.packageName) match = app;
    }
    final raw = (match?['entries'] as List?) ?? const [];
    if (!mounted) return;
    setState(() {
      _entries = raw.map((e) => Map<String, dynamic>.from(e as Map)).toList();
      _selected.removeWhere((id) => _entries.every((e) => e['id'] != id));
      _loading = false;
    });
  }

  Future<void> _delete(Set<String> ids) async {
    if (ids.isEmpty) return;
    await AppInfoService.deleteErrorLogs(ids.toList());
    _selected.clear();
    await _load();
  }

  Future<void> _clearApp() async {
    final l10n = AppLocalizations.of(context)!;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.clearAppErrors),
        content: Text(l10n.clearAppErrorsContent(widget.packageName)),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: Text(l10n.cancel)),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: Text(l10n.confirm)),
        ],
      ),
    );
    if (confirmed != true) return;
    await AppInfoService.clearErrorLogs(widget.packageName);
    await _load();
  }

  Future<void> _open(Map<String, dynamic> entry) async {
    final l10n = AppLocalizations.of(context)!;
    final full = await AppInfoService.getErrorLog(entry['id'] as String? ?? '') ?? entry;
    if (!mounted) return;
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(_title(l10n, full)),
        content: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(full['message'] as String? ?? ''),
              const SizedBox(height: 16),
              Text(l10n.errorRequest, style: Theme.of(ctx).textTheme.labelLarge),
              const SizedBox(height: 4),
              SelectableText(full['request'] as String? ?? ''),
              const SizedBox(height: 16),
              Text(l10n.errorResponse, style: Theme.of(ctx).textTheme.labelLarge),
              const SizedBox(height: 4),
              SelectableText(full['response'] as String? ?? ''),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: Text(l10n.confirm)),
        ],
      ),
    );
  }

  String _title(AppLocalizations l10n, Map<String, dynamic> entry) {
    final kind = entry['kind'] as String? ?? '';
    final status = entry['status'] as int? ?? 0;
    final when = _fmt(entry['time'] as int? ?? 0);
    final label = switch (kind) {
      'network' => l10n.errorKindNetwork,
      'http' => l10n.errorKindHttp(status),
      _ => l10n.errorKindParse,
    };
    return '$label · $when';
  }

  String _fmt(int ms) {
    if (ms <= 0) return '';
    final t = DateTime.fromMillisecondsSinceEpoch(ms);
    String two(int n) => n.toString().padLeft(2, '0');
    return '${t.year}-${two(t.month)}-${two(t.day)} ${two(t.hour)}:${two(t.minute)}:${two(t.second)}';
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final l10n = AppLocalizations.of(context)!;
    final selecting = _selected.isNotEmpty;
    return Scaffold(
      backgroundColor: cs.surface,
      body: BlurAppBarHost(
        title: widget.packageName,
        largeTitle: false,
        actions: [
          if (selecting)
            IconButton(
              icon: const Icon(Icons.delete),
              tooltip: l10n.deleteSelected,
              onPressed: () => _delete(_selected.toSet()),
            )
          else
            IconButton(
              icon: const Icon(Icons.delete_sweep),
              tooltip: l10n.clearAppErrors,
              onPressed: _entries.isEmpty ? null : _clearApp,
            ),
        ],
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            sliver: _loading
                ? const SliverFillRemaining(child: Center(child: CircularProgressIndicator()))
                : _entries.isEmpty
                    ? SliverFillRemaining(child: Center(child: Text(l10n.noErrorLog)))
                    : SliverList(
                        delegate: SliverChildBuilderDelegate(
                          (context, index) {
                            final entry = _entries[index];
                            final id = entry['id'] as String? ?? '';
                            final checked = _selected.contains(id);
                            return ListTile(
                              onLongPress: () => setState(() {
                                if (checked) {
                                  _selected.remove(id);
                                } else {
                                  _selected.add(id);
                                }
                              }),
                              onTap: selecting
                                  ? () => setState(() {
                                      if (checked) {
                                        _selected.remove(id);
                                      } else {
                                        _selected.add(id);
                                      }
                                    })
                                  : () => _open(entry),
                              leading: selecting ? Checkbox(value: checked, onChanged: (_) {
                                setState(() {
                                  if (checked) {
                                    _selected.remove(id);
                                  } else {
                                    _selected.add(id);
                                  }
                                });
                              }) : null,
                              title: Text(_title(l10n, entry), maxLines: 1, overflow: TextOverflow.ellipsis),
                              subtitle: Text(
                                entry['message'] as String? ?? '',
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                              ),
                              trailing: selecting
                                  ? null
                                  : IconButton(
                                      icon: Icon(Icons.delete_outline, color: cs.error.withValues(alpha: 0.7), size: 20),
                                      onPressed: () => _delete({id}),
                                    ),
                              tileColor: cs.surfaceContainerHighest,
                              shape: _radius(index == 0, index == _entries.length - 1),
                            );
                          },
                          childCount: _entries.length,
                        ),
                      ),
          ),
        ],
      ),
    );
  }
}

RoundedRectangleBorder? _radius(bool first, bool last) {
  if (first && last) return RoundedRectangleBorder(borderRadius: BorderRadius.circular(16));
  if (first) return const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(16)));
  if (last) return const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(bottom: Radius.circular(16)));
  return null;
}
