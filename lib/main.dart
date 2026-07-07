import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'l10n/generated/app_localizations.dart';
import 'controllers/settings_controller.dart';
import 'pages/main_page.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final _ctrl = SettingsController.instance;
  late ThemeMode _themeMode;
  int _seedColor = 0xFF6750A4;
  bool _blurBars = false;
  bool _settingsLoading = true;
  Locale? _locale;

  @override
  void initState() {
    super.initState();
    _themeMode = _ctrl.themeMode;
    _seedColor = _ctrl.themeSeedColor;
    _blurBars = _ctrl.blurBars;
    _settingsLoading = _ctrl.loading;
    _locale = _ctrl.locale;
    _ctrl.addListener(_onSettingsChanged);
  }

  @override
  void dispose() {
    _ctrl.removeListener(_onSettingsChanged);
    super.dispose();
  }

  void _onSettingsChanged() {
    final nextThemeMode = _ctrl.themeMode;
    final nextSeedColor = _ctrl.themeSeedColor;
    final nextBlurBars = _ctrl.blurBars;
    final nextLoading = _ctrl.loading;
    final nextLocale = _ctrl.locale;
    if (nextThemeMode == _themeMode &&
        nextSeedColor == _seedColor &&
        nextBlurBars == _blurBars &&
        nextLoading == _settingsLoading &&
        nextLocale == _locale) return;
    if (!mounted) return;
    setState(() {
      _themeMode = nextThemeMode;
      _seedColor = nextSeedColor;
      _blurBars = nextBlurBars;
      _settingsLoading = nextLoading;
      _locale = nextLocale;
    });
  }

  ThemeData _buildTheme({required Color seedColor, required Brightness brightness, required bool blur}) {
    final colorScheme = ColorScheme.fromSeed(seedColor: seedColor, brightness: brightness);
    return ThemeData(
      colorScheme: colorScheme,
      useMaterial3: true,
      dialogTheme: DialogThemeData(shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28))),
      bottomSheetTheme: BottomSheetThemeData(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(28))),
      ),
      cardTheme: CardThemeData(shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16))),
      inputDecorationTheme: InputDecorationTheme(
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)), filled: true,
        fillColor: colorScheme.surfaceContainerHighest,
      ),
      popupMenuTheme: PopupMenuThemeData(shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12))),
      menuTheme: MenuThemeData(
        style: MenuStyle(shape: WidgetStatePropertyAll(RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)))),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12))),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12))),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12))),
      ),
      snackBarTheme: SnackBarThemeData(shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)), behavior: SnackBarBehavior.floating),
      chipTheme: ChipThemeData(shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8))),
      appBarTheme: AppBarTheme(
        backgroundColor: blur ? Colors.transparent : colorScheme.surface,
        scrolledUnderElevation: blur ? 0 : null,
        systemOverlayStyle: blur
            ? (brightness == Brightness.light ? SystemUiOverlayStyle.dark : SystemUiOverlayStyle.light)
            : null,
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: blur ? colorScheme.surface.withValues(alpha: 0.7) : colorScheme.surface,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final seedColor = Color(_seedColor);
    return MaterialApp(
      title: 'DeepTranslate',
      debugShowCheckedModeBanner: false,
      localizationsDelegates: const [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: AppLocalizations.supportedLocales,
      locale: _locale,
      theme: _buildTheme(seedColor: seedColor, brightness: Brightness.light, blur: _blurBars),
      darkTheme: _buildTheme(seedColor: seedColor, brightness: Brightness.dark, blur: _blurBars),
      themeMode: _themeMode,
      home: _settingsLoading
          ? const Scaffold(body: Center(child: CircularProgressIndicator()))
          : const MainPage(),
    );
  }
}
