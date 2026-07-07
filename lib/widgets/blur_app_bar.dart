import 'dart:ui';
import 'package:flutter/material.dart';
import '../controllers/settings_controller.dart';

/// 毛玻璃顶栏容器。抄自 HyperIsland。
class BlurAppBarHost extends StatefulWidget {
  final String title;
  final Widget? titleWidget;
  final bool largeTitle;
  final List<Widget> slivers;
  final List<Widget>? actions;
  final Widget? leading;
  final ScrollPhysics? physics;
  final bool automaticallyImplyLeading;
  final double bottomPadding;
  final ScrollController? scrollController;

  const BlurAppBarHost({
    super.key,
    required this.title,
    this.titleWidget,
    this.largeTitle = false,
    required this.slivers,
    this.actions,
    this.leading,
    this.physics,
    this.automaticallyImplyLeading = true,
    this.bottomPadding = 0,
    this.scrollController,
  });

  @override
  State<BlurAppBarHost> createState() => _BlurAppBarHostState();
}

class _BlurAppBarHostState extends State<BlurAppBarHost> {
  final _ctrl = SettingsController.instance;
  late final ScrollController _scrollController;
  double _scrollOffset = 0;
  bool _ownsController = false;

  static const _largeExpandedHeight = 152.0;
  static const _largeCollapsedHeight = 64.0;

  @override
  void initState() {
    super.initState();
    _ctrl.addListener(_onSettingsChanged);
    if (widget.scrollController != null) {
      _scrollController = widget.scrollController!;
      _ownsController = false;
    } else {
      _scrollController = ScrollController();
      _ownsController = true;
    }
    _scrollController.addListener(_onScroll);
  }

  @override
  void dispose() {
    _ctrl.removeListener(_onSettingsChanged);
    _scrollController.removeListener(_onScroll);
    if (_ownsController) _scrollController.dispose();
    super.dispose();
  }

  void _onSettingsChanged() {
    if (mounted) setState(() {});
  }

  @override
  void didUpdateWidget(covariant BlurAppBarHost oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.scrollController != oldWidget.scrollController) {
      _scrollController.removeListener(_onScroll);
      if (_ownsController) _scrollController.dispose();
      if (widget.scrollController != null) {
        _scrollController = widget.scrollController!;
        _ownsController = false;
      } else {
        _scrollController = ScrollController();
        _ownsController = true;
      }
      _scrollController.addListener(_onScroll);
    }
  }

  void _onScroll() {
    if (!mounted) return;
    final offset = _scrollController.offset;
    if (offset != _scrollOffset) {
      setState(() => _scrollOffset = offset);
    }
  }

  Widget _buildLeading(BuildContext context, {double opacity = 1.0}) {
    if (widget.leading != null) {
      return Opacity(opacity: opacity, child: widget.leading!);
    }
    if (!widget.automaticallyImplyLeading) return const SizedBox(width: 16);
    final canPop = ModalRoute.of(context)?.canPop ?? false;
    if (!canPop) return const SizedBox(width: 16);
    return Opacity(
      opacity: opacity,
      child: IconButton(
        icon: const Icon(Icons.arrow_back),
        onPressed: () => Navigator.maybePop(context),
      ),
    );
  }

  Widget _buildLargeTitleLayout({
    required BuildContext context,
    required ColorScheme cs,
    required double statusBarHeight,
    required double barHeight,
    required double largeOpacity,
    required double smallOpacity,
  }) {
    return Column(
      children: [
        SizedBox(height: statusBarHeight),
        SizedBox(
          height: barHeight,
          child: Stack(
            children: [
              Positioned(
                left: 16,
                right: 16,
                bottom: 28,
                child: Opacity(
                  opacity: largeOpacity,
                  child: widget.titleWidget ??
                      Text(
                        widget.title,
                        style:
                            Theme.of(context).textTheme.headlineMedium?.copyWith(
                                  color: cs.onSurface,
                                ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                ),
              ),
              Positioned(
                left: 0,
                right: 0,
                top: 0,
                height: _largeCollapsedHeight,
                child: Row(
                  children: [
                    _buildLeading(context),
                    Expanded(
                      child: Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 16),
                        child: Opacity(
                          opacity: smallOpacity,
                          child: Text(
                            widget.title,
                            style: Theme.of(context)
                                .textTheme
                                .titleLarge
                                ?.copyWith(
                                  color: cs.onSurface,
                                ),
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ),
                    ),
                    if (widget.actions != null) ...widget.actions!,
                  ],
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildSmallTitleLayout({
    required BuildContext context,
    required ColorScheme cs,
    required double statusBarHeight,
  }) {
    return Column(
      children: [
        SizedBox(height: statusBarHeight),
        SizedBox(
          height: kToolbarHeight,
          child: Row(
            children: [
              _buildLeading(context),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  child: Text(
                    widget.title,
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(
                          color: cs.onSurface,
                        ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ),
              if (widget.actions != null) ...widget.actions!,
            ],
          ),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final brightness = Theme.of(context).brightness;
    final statusBarHeight = MediaQuery.of(context).padding.top;
    final blurEnabled = _ctrl.blurBars;

    final expandedHeight = widget.largeTitle ? _largeExpandedHeight : 0.0;
    final collapsedHeight =
        widget.largeTitle ? _largeCollapsedHeight : kToolbarHeight;

    final maxScroll = expandedHeight - collapsedHeight;
    final expandedRatio = maxScroll > 0
        ? (1 - _scrollOffset / maxScroll).clamp(0.0, 1.0)
        : 0.0;

    final barHeight = widget.largeTitle
        ? (expandedHeight - _scrollOffset)
            .clamp(collapsedHeight, expandedHeight)
        : collapsedHeight;
    final totalHeight = barHeight + statusBarHeight;

    final scrollProgress = (1 - expandedRatio).clamp(0.0, 1.0);
    double sigma;
    double alpha;
    if (blurEnabled) {
      final blurAlpha = brightness == Brightness.light ? 0.7 : 0.6;
      sigma = 20 * scrollProgress;
      alpha = 1.0 - scrollProgress * (1.0 - blurAlpha);
    } else {
      sigma = 0;
      alpha = 1.0;
    }
    final bgColor = cs.surface.withValues(alpha: alpha);

    final largeOpacity = widget.largeTitle
        ? ((expandedRatio - 0.2) / 0.3).clamp(0.0, 1.0)
        : 0.0;
    final smallOpacity = widget.largeTitle
        ? ((0.2 - expandedRatio) / 0.2).clamp(0.0, 1.0)
        : 1.0;

    final allSlivers = <Widget>[
      ...widget.slivers,
      if (widget.bottomPadding > 0)
        SliverToBoxAdapter(
          child: SizedBox(height: widget.bottomPadding),
        ),
    ];

    return Stack(
      children: [
        CustomScrollView(
          controller: _scrollController,
          physics: widget.physics,
          slivers: [
            SliverAppBar(
              pinned: true,
              expandedHeight: widget.largeTitle ? expandedHeight : null,
              toolbarHeight: collapsedHeight,
              forceMaterialTransparency: true,
              primary: true,
              automaticallyImplyLeading: false,
            ),
            ...allSlivers,
          ],
        ),
        Positioned(
          top: 0,
          left: 0,
          right: 0,
          child: ClipRect(
            child: BackdropFilter(
              filter: ImageFilter.blur(sigmaX: sigma, sigmaY: sigma),
              child: Container(
                height: totalHeight,
                color: bgColor,
                child: widget.largeTitle
                    ? _buildLargeTitleLayout(
                        context: context,
                        cs: cs,
                        statusBarHeight: statusBarHeight,
                        barHeight: barHeight,
                        largeOpacity: largeOpacity,
                        smallOpacity: smallOpacity,
                      )
                    : _buildSmallTitleLayout(
                        context: context,
                        cs: cs,
                        statusBarHeight: statusBarHeight,
                      ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}
