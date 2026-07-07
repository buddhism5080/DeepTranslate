import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../controllers/settings_controller.dart';
import '../l10n/generated/app_localizations.dart';
import 'home_page.dart';
import 'ai_config_page.dart';
import 'settings_page.dart';

class MainPage extends StatefulWidget {
  const MainPage({super.key});

  @override
  State<MainPage> createState() => _MainPageState();
}

class _MainPageState extends State<MainPage> {
  int _currentIndex = 0;
  final _ctrl = SettingsController.instance;

  final _pages = const [HomePage(), AiConfigPage(), SettingsPage()];

  @override
  void initState() {
    super.initState();
    _ctrl.addListener(_onChanged);
  }

  @override
  void dispose() {
    _ctrl.removeListener(_onChanged);
    super.dispose();
  }

  void _onChanged() {
    if (!mounted) return;
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    final blurBars = _ctrl.blurBars;
    final l10n = AppLocalizations.of(context)!;

    Widget navBar = NavigationBar(
      selectedIndex: _currentIndex,
      onDestinationSelected: (index) {
        FocusScope.of(context).unfocus();
        setState(() => _currentIndex = index);
      },
      destinations: [
        NavigationDestination(icon: const Icon(Icons.home_outlined), selectedIcon: const Icon(Icons.home), label: l10n.navHome),
        NavigationDestination(icon: const Icon(Icons.translate_outlined), selectedIcon: const Icon(Icons.translate), label: l10n.navTranslateConfig),
        NavigationDestination(icon: const Icon(Icons.settings_outlined), selectedIcon: const Icon(Icons.settings), label: l10n.navSettings),
      ],
    );

    if (blurBars) {
      navBar = ClipRect(
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 20, sigmaY: 20),
          child: navBar,
        ),
      );
    }

    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) {
        if (didPop) return;
        SystemNavigator.pop();
      },
      child: Scaffold(
        extendBody: blurBars,
        body: AnimatedSwitcher(
          duration: const Duration(milliseconds: 180),
          switchInCurve: Curves.easeOut,
          switchOutCurve: Curves.easeIn,
          child: KeyedSubtree(
            key: ValueKey('${_currentIndex}_${_ctrl.locale}'),
            child: _pages[_currentIndex],
          ),
        ),
        bottomNavigationBar: navBar,
      ),
    );
  }
}
