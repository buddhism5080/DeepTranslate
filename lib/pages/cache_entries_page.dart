import 'package:flutter/material.dart';
import '../l10n/generated/app_localizations.dart';
import '../services/app_info_service.dart';
import '../widgets/blur_app_bar.dart';

class CacheEntriesPage extends StatefulWidget {
  final String packageName;
  final String appName;
  const CacheEntriesPage({super.key, required this.packageName, required this.appName});

  @override
  State<CacheEntriesPage> createState() => _CacheEntriesPageState();
}

class _CacheEntriesPageState extends State<CacheEntriesPage> {
  final _query = TextEditingController();
  List<Map<String, dynamic>> _rows = [];
  final Set<String> _selected = {};
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _query.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    final rows = await AppInfoService.listCacheRows(widget.packageName, _query.text);
    if (!mounted) return;
    setState(() {
      _rows = rows;
      _selected.removeWhere((id) => rows.every((row) => row['hash'] != id));
      _loading = false;
    });
  }

  Future<void> _delete(Set<String> hashes) async {
    if (hashes.isEmpty) return;
    await AppInfoService.deleteCacheRows(widget.packageName, hashes.toList());
    _selected.clear();
    await _load();
  }

  String _fmt(int ms) {
    if (ms <= 0) return '';
    final t = DateTime.fromMillisecondsSinceEpoch(ms);
    String two(int n) => n.toString().padLeft(2, '0');
    return '${t.year}-${two(t.month)}-${two(t.day)} ${two(t.hour)}:${two(t.minute)}:${two(t.second)}';
  }

  Future<void> _open(Map<String, dynamic> row) async {
    final l10n = AppLocalizations.of(context)!;
    final full = await AppInfoService.getCacheRow(widget.packageName, row['hash'] as String? ?? '') ?? row;
    if (!mounted) return;
    final time = _fmt(full['time'] as int? ?? 0);
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(time),
        content: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(l10n.cacheOriginal, style: Theme.of(ctx).textTheme.labelLarge),
              const SizedBox(height: 4),
              SelectableText(full['original'] as String? ?? ''),
              const SizedBox(height: 16),
              Text(l10n.cacheTranslated, style: Theme.of(ctx).textTheme.labelLarge),
              const SizedBox(height: 4),
              SelectableText(full['translated'] as String? ?? ''),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: Text(l10n.confirm)),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final l10n = AppLocalizations.of(context)!;
    final selecting = _selected.isNotEmpty;
    return Scaffold(
      backgroundColor: cs.surface,
      body: BlurAppBarHost(
        title: widget.appName,
        slivers: [
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
              child: TextField(
                controller: _query,
                decoration: InputDecoration(
                  hintText: l10n.cacheSearch,
                  prefixIcon: const Icon(Icons.search),
                  isDense: true,
                ),
                onSubmitted: (_) => _load(),
              ),
            ),
          ),
          if (selecting)
            SliverToBoxAdapter(
              child: Align(
                alignment: Alignment.centerRight,
                child: TextButton.icon(
                  onPressed: () => _delete(_selected.toSet()),
                  icon: const Icon(Icons.delete_outline),
                  label: Text(l10n.deleteSelected),
                ),
              ),
            ),
          SliverPadding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            sliver: _loading
                ? const SliverFillRemaining(child: Center(child: CircularProgressIndicator()))
                : _rows.isEmpty
                    ? SliverFillRemaining(child: Center(child: Text(l10n.cacheEmpty)))
                    : SliverList(
                        delegate: SliverChildBuilderDelegate(
                          (context, index) {
                            final row = _rows[index];
                            final hash = row['hash'] as String? ?? '';
                            final checked = _selected.contains(hash);
                            return ListTile(
                              onTap: selecting
                                  ? () => setState(() {
                                      if (checked) {
                                        _selected.remove(hash);
                                      } else {
                                        _selected.add(hash);
                                      }
                                    })
                                  : () => _open(row),
                              onLongPress: () => setState(() {
                                if (checked) {
                                  _selected.remove(hash);
                                } else {
                                  _selected.add(hash);
                                }
                              }),
                              leading: selecting
                                  ? Checkbox(
                                      value: checked,
                                      onChanged: (_) => setState(() {
                                        if (checked) {
                                          _selected.remove(hash);
                                        } else {
                                          _selected.add(hash);
                                        }
                                      }),
                                    )
                                  : null,
                              title: Text(row['translated'] as String? ?? '', maxLines: 2, overflow: TextOverflow.ellipsis),
                              subtitle: Text(
                                '${_fmt(row['time'] as int? ?? 0)}\n${row['original'] ?? ''}',
                                maxLines: 3,
                                overflow: TextOverflow.ellipsis,
                              ),
                              trailing: selecting
                                  ? null
                                  : IconButton(
                                      icon: Icon(Icons.delete_outline, color: cs.error.withValues(alpha: 0.7), size: 20),
                                      onPressed: () => _delete({hash}),
                                    ),
                              tileColor: cs.surfaceContainerHighest,
                            );
                          },
                          childCount: _rows.length,
                        ),
                      ),
          ),
        ],
      ),
    );
  }
}
