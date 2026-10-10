import 'dart:typed_data';
import 'package:flutter/material.dart';
import '../l10n/generated/app_localizations.dart';
import '../services/app_info_service.dart';
import '../widgets/blur_app_bar.dart';
import 'cache_entries_page.dart';

class CacheDetailPage extends StatefulWidget {
  const CacheDetailPage({super.key});

  @override
  State<CacheDetailPage> createState() => _CacheDetailPageState();
}

class _CacheDetailPageState extends State<CacheDetailPage> {
  List<Map<String, dynamic>> _details = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _load() async {
    try {
      final details = await AppInfoService.getCacheDetails();
      details.sort((a, b) => (b['count'] as int).compareTo(a['count'] as int));
      if (mounted) {
        setState(() {
          _details = details;
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _clearApp(String pkg) async {
    final l10n = AppLocalizations.of(context)!;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.clearAppCache),
        content: Text(l10n.clearAppCacheContent(pkg)),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: Text(l10n.cancel)),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: Text(l10n.confirm)),
        ],
      ),
    );
    if (confirmed != true) return;

    await AppInfoService.clearAppCache(pkg);
    _load();
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.appCacheCleared(pkg)), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10))),
      );
    }
  }

  void _clearAll() async {
    final l10n = AppLocalizations.of(context)!;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.clearAllCacheTitle),
        content: Text(l10n.clearAllCacheStatsContent),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: Text(l10n.cancel)),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: Text(l10n.confirm)),
        ],
      ),
    );
    if (confirmed != true) return;

    await AppInfoService.clearAllCache();
    _load();
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.clearAllCacheDone), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10))),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      backgroundColor: cs.surface,
      body: BlurAppBarHost(
        title: l10n.cacheDetail,
        largeTitle: true,
        actions: [
          IconButton(
            icon: const Icon(Icons.delete_sweep),
            tooltip: l10n.clearAll,
            onPressed: _details.isEmpty ? null : _clearAll,
          ),
        ],
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            sliver: _loading
                ? const SliverFillRemaining(child: Center(child: CircularProgressIndicator()))
                : _details.isEmpty
                    ? SliverFillRemaining(
                        child: Center(
                          child: Column(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.inbox_outlined, size: 48, color: Colors.grey),
                              const SizedBox(height: 12),
                              Text(l10n.noCacheData),
                            ],
                          ),
                        ),
                      )
                    : SliverList(
                        delegate: SliverChildBuilderDelegate(
                          (context, index) {
                            final entry = _details[index];
                            return _CacheAppTile(
                              packageName: entry['package'] as String,
                              appName: entry['name'] as String,
                              iconBytes: entry['icon'] as Uint8List?,
                              count: entry['count'] as int,
                              l10n: l10n,
                              onClear: () => _clearApp(entry['package'] as String),
                              onOpen: () {
                                Navigator.push(context, MaterialPageRoute(
                                  builder: (_) => CacheEntriesPage(
                                    packageName: entry['package'] as String,
                                    appName: entry['name'] as String,
                                  ),
                                ));
                              },
                              isFirst: index == 0,
                              isLast: index == _details.length - 1,
                            );
                          },
                          childCount: _details.length,
                        ),
                      ),
          ),
        ],
      ),
    );
  }
}

class _CacheAppTile extends StatelessWidget {
  final String packageName;
  final String appName;
  final Uint8List? iconBytes;
  final int count;
  final AppLocalizations l10n;
  final VoidCallback onClear;
  final VoidCallback onOpen;
  final bool isFirst;
  final bool isLast;

  const _CacheAppTile({
    required this.packageName,
    required this.appName,
    required this.iconBytes,
    required this.count,
    required this.l10n,
    required this.onClear,
    required this.onOpen,
    this.isFirst = false,
    this.isLast = false,
  });

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    BorderRadius? radius;
    if (isFirst && isLast) {
      radius = BorderRadius.circular(16);
    } else if (isFirst) {
      radius = const BorderRadius.vertical(top: Radius.circular(16));
    } else if (isLast) {
      radius = const BorderRadius.vertical(bottom: Radius.circular(16));
    }

    Widget leading;
    if (iconBytes != null && iconBytes!.isNotEmpty) {
      leading = ClipRRect(
        borderRadius: BorderRadius.circular(8),
        child: Image.memory(iconBytes!, width: 40, height: 40, fit: BoxFit.cover),
      );
    } else {
      leading = CircleAvatar(
        backgroundColor: cs.primaryContainer,
        child: Icon(Icons.android, color: cs.onPrimaryContainer, size: 20),
      );
    }

    return Padding(
      padding: const EdgeInsets.only(bottom: 1),
      child: ListTile(
        leading: leading,
        title: Text(appName, style: Theme.of(context).textTheme.titleSmall, maxLines: 1, overflow: TextOverflow.ellipsis),
        subtitle: Text(l10n.cacheEntry(count, packageName),
            style: TextStyle(fontSize: 11, fontFamily: 'monospace', color: cs.onSurfaceVariant),
            maxLines: 1, overflow: TextOverflow.ellipsis),
        trailing: IconButton(
          icon: Icon(Icons.delete_outline, color: cs.error.withValues(alpha: 0.7), size: 20),
          onPressed: onClear,
        ),
        onTap: onOpen,
        shape: radius != null ? RoundedRectangleBorder(borderRadius: radius) : null,
        tileColor: cs.surfaceContainerHighest,
      ),
    );
  }
}
