import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import '../controllers/settings_controller.dart';
import '../services/openai_endpoint.dart';
import '../l10n/generated/app_localizations.dart';
import '../services/interaction_haptics.dart';
import '../widgets/section_label.dart';
import '../widgets/blur_app_bar.dart';
import '../widgets/modern_slider.dart';

class AiConfigPage extends StatefulWidget {
  const AiConfigPage({super.key});

  @override
  State<AiConfigPage> createState() => _AiConfigPageState();
}

class _AiConfigPageState extends State<AiConfigPage> {
  final _ctrl = SettingsController.instance;

  late final TextEditingController _urlCtrl;
  late final TextEditingController _keyCtrl;
  late final TextEditingController _modelCtrl;
  late final TextEditingController _promptCtrl;
  late final TextEditingController _fbUrlCtrl;
  late final TextEditingController _fbKeyCtrl;
  late final TextEditingController _fbModelCtrl;

  bool _keyObscured = true;
  bool _fbKeyObscured = true;
  bool _testing = false;
  bool _fetchingModels = false;
  List<String> _availableModels = [];
  String? _modelsStatus;
  _TestResult? _testResult;

  late int _aiTimeoutDraft;
  late double _aiTemperatureDraft;
  late int _aiMaxTokensDraft;
  late int _batchSizeDraft;
  late int _batchWindowDraft;
  late int _concurrencyDraft;
  late int _maxParagraphsDraft;
  late int _maxCharsDraft;
  late int _retryDraft;

  @override
  void initState() {
    super.initState();
    _ctrl.addListener(_onCtrlChanged);
    _urlCtrl = TextEditingController(text: _ctrl.aiUrl);
    _keyCtrl = TextEditingController(text: _ctrl.aiApiKey);
    _modelCtrl = TextEditingController(text: _ctrl.aiModel);
    _promptCtrl = TextEditingController(text: _ctrl.aiPrompt);
    _fbUrlCtrl = TextEditingController(text: _ctrl.fallbackUrl);
    _fbKeyCtrl = TextEditingController(text: _ctrl.fallbackApiKey);
    _fbModelCtrl = TextEditingController(text: _ctrl.fallbackModel);
    _aiTimeoutDraft = _ctrl.aiTimeout;
    _aiTemperatureDraft = _ctrl.aiTemperature;
    _aiMaxTokensDraft = _ctrl.aiMaxTokens;
    _batchSizeDraft = _ctrl.batchSize;
    _batchWindowDraft = _ctrl.batchWindowMs.clamp(50, 500).toInt();
    _concurrencyDraft = _ctrl.concurrency.clamp(1, 8).toInt();
    _maxParagraphsDraft = _ctrl.maxParagraphs.clamp(1, 16).toInt();
    _maxCharsDraft = _ctrl.maxChars.clamp(400, 8000).toInt();
    _retryDraft = _ctrl.retryCount;
  }

  void _onCtrlChanged() {
    if (!mounted) return;
    setState(() {});
  }

  @override
  void dispose() {
    _ctrl.removeListener(_onCtrlChanged);
    _urlCtrl.dispose();
    _keyCtrl.dispose();
    _modelCtrl.dispose();
    _promptCtrl.dispose();
    _fbUrlCtrl.dispose();
    _fbKeyCtrl.dispose();
    _fbModelCtrl.dispose();
    super.dispose();
  }

  Widget _limitSlider({
    required Widget icon,
    required String title,
    required String subtitle,
    required String valueLabel,
    required double value,
    required double min,
    required double max,
    required int divisions,
    required ValueChanged<double> onChanged,
  }) {
    final textTheme = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;
    return Column(
      children: [
        const SizedBox(height: 16),
        Row(
          children: [
            icon,
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: textTheme.titleMedium),
                  Text(
                    subtitle,
                    style: textTheme.bodySmall?.copyWith(color: cs.onSurfaceVariant),
                  ),
                ],
              ),
            ),
            Text(
              valueLabel,
              style: textTheme.bodyLarge?.copyWith(
                color: cs.primary,
                fontWeight: FontWeight.bold,
              ),
            ),
          ],
        ),
        SliderTheme(
          data: ModernSliderTheme.theme(context),
          child: Slider(
            value: value,
            min: min,
            max: max,
            divisions: divisions,
            label: valueLabel,
            onChanged: onChanged,
          ),
        ),
      ],
    );
  }

  void _toastSaved() {
    if (!mounted) return;
    final l10n = AppLocalizations.of(context)!;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(l10n.configSaved),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      ),
    );
  }

  Widget _saveButton(VoidCallback onPressed) {
    final l10n = AppLocalizations.of(context)!;
    return SizedBox(
      width: double.infinity,
      child: FilledButton.icon(
        onPressed: onPressed,
        icon: const FaIcon(FontAwesomeIcons.floppyDisk, size: 16),
        label: Text(l10n.save),
        style: FilledButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 12)),
      ),
    );
  }

  Future<void> _saveApi() async {
    await _ctrl.setAiUrl(_urlCtrl.text.trim());
    await _ctrl.setAiApiKey(_keyCtrl.text.trim());
    await _ctrl.setAiModel(_modelCtrl.text.trim());
    await _ctrl.setAiPrompt(_promptCtrl.text.trim());
    await _ctrl.setAiTimeout(_aiTimeoutDraft);
    await _ctrl.setAiTemperature(_aiTemperatureDraft);
    await _ctrl.setAiMaxTokens(_aiMaxTokensDraft);
    _toastSaved();
  }

  Future<void> _saveFallback() async {
    await _ctrl.setFallbackUrl(_fbUrlCtrl.text.trim());
    await _ctrl.setFallbackApiKey(_fbKeyCtrl.text.trim());
    await _ctrl.setFallbackModel(_fbModelCtrl.text.trim());
    await _ctrl.setRetryCount(_retryDraft);
    _toastSaved();
  }

  Future<void> _saveBatch() async {
    await _ctrl.setBatchSize(_batchSizeDraft);
    await _ctrl.setBatchWindowMs(_batchWindowDraft);
    await _ctrl.setConcurrency(_concurrencyDraft);
    await _ctrl.setMaxParagraphs(_maxParagraphsDraft);
    await _ctrl.setMaxChars(_maxCharsDraft);
    _toastSaved();
  }

  Future<void> _test() async {
    final l10n = AppLocalizations.of(context)!;
    final url = _urlCtrl.text.trim();
    final key = _keyCtrl.text.trim();
    final model = _modelCtrl.text.trim();

    if (url.isEmpty) {
      setState(() => _testResult = _TestResult.fail(l10n.apiUrlEmpty));
      return;
    }
    final endpoints = OpenAiEndpoints.resolve(url);
    if (endpoints == null) {
      setState(() => _testResult = _TestResult.fail(l10n.apiUrlEmpty));
      return;
    }
    if (model.isEmpty) {
      setState(() => _testResult = _TestResult.fail(l10n.modelEmpty));
      return;
    }

    setState(() {
      _testing = true;
      _testResult = null;
    });

    try {
      final requestBody = jsonEncode({
        'model': model,
        'messages': [
          {
            'role': 'system',
            'content': 'Translate the following text to Chinese. Return only the translation.'
          },
          {'role': 'user', 'content': 'Hello, world! This is a test.'},
        ],
        'max_tokens': 100,
        'temperature': 0.1,
      });

      final response = await http
          .post(
            Uri.parse(endpoints.chat),
            headers: {
              'Content-Type': 'application/json',
              'Accept': 'application/json',
              if (key.isNotEmpty) 'Authorization': 'Bearer $key',
            },
            body: requestBody,
          )
          .timeout(Duration(seconds: _aiTimeoutDraft));

      if (response.statusCode == 200) {
        final json = jsonDecode(response.body) as Map<String, dynamic>;
        final content =
            (json['choices'] as List?)?.firstOrNull?['message']?['content']
                as String? ?? '';
        setState(() => _testResult = _TestResult.ok(content.trim()));
        _fetchModels();
      } else {
        setState(() => _testResult = _TestResult.fail(
          'HTTP ${response.statusCode}\n${response.body}',
        ));
      }
    } on Exception catch (e) {
      setState(() => _testResult = _TestResult.fail(e.toString()));
    } finally {
      if (mounted) setState(() => _testing = false);
    }
  }

  Future<void> _fetchModels() async {
    final l10n = AppLocalizations.of(context)!;
    final url = _urlCtrl.text.trim();
    final key = _keyCtrl.text.trim();
    final endpoints = OpenAiEndpoints.resolve(url);
    if (endpoints == null) {
      setState(() => _modelsStatus = l10n.apiUrlEmpty);
      return;
    }

    setState(() {
      _fetchingModels = true;
      _modelsStatus = null;
    });

    try {
      final response = await http
          .get(Uri.parse(endpoints.models),
            headers: {
              'Accept': 'application/json',
              if (key.isNotEmpty) 'Authorization': 'Bearer $key',
            })
          .timeout(const Duration(seconds: 15));

      if (response.statusCode == 200) {
        final models = parseModelIds(jsonDecode(response.body));
        if (mounted) {
          setState(() {
            _availableModels = models;
            _modelsStatus = models.isEmpty
                ? l10n.modelsFetchFailed
                : l10n.modelsFetched(models.length);
          });
        }
      } else {
        if (mounted) {
          setState(() => _modelsStatus =
              '${l10n.modelsFetchFailed} (HTTP ${response.statusCode})');
        }
      }
    } catch (e) {
      if (mounted) setState(() => _modelsStatus = '${l10n.modelsFetchFailed}: $e');
    } finally {
      if (mounted) setState(() => _fetchingModels = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    final l10n = AppLocalizations.of(context)!;
    final bottomPad = _ctrl.blurBars ? 80.0 : 0.0;

    return Scaffold(
      backgroundColor: cs.surface,
      body: BlurAppBarHost(
        title: l10n.navTranslateConfig,
        largeTitle: true,
        bottomPadding: bottomPad,
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            sliver: SliverList(
              delegate: SliverChildListDelegate([
                SectionLabel(l10n.apiConfig),
                const SizedBox(height: 8),
                Card(
                  elevation: 0,
                  color: cs.surfaceContainerHighest,
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _buildTextField(
                          controller: _urlCtrl,
                          label: l10n.apiUrl,
                          hint: l10n.apiUrlHint,
                          icon: FontAwesomeIcons.link,
                        ),
                        const SizedBox(height: 16),
                        _buildTextField(
                          controller: _keyCtrl,
                          label: l10n.apiKey,
                          hint: l10n.apiKeyHint,
                          icon: FontAwesomeIcons.key,
                          obscure: _keyObscured,
                          suffix: IconButton(
                            icon: FaIcon(
                              _keyObscured
                                  ? FontAwesomeIcons.eyeSlash
                                  : FontAwesomeIcons.eye,
                              size: 16,
                            ),
                            onPressed: () {
                              setState(() => _keyObscured = !_keyObscured);
                            },
                          ),
                        ),
                        const SizedBox(height: 8),
                        Row(
                          children: [
                            Expanded(
                              child: OutlinedButton.icon(
                                onPressed: _testing ? null : _test,
                                icon: _testing
                                    ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                                    : const FaIcon(FontAwesomeIcons.radiation, size: 14),
                                label: Text(_testing ? l10n.testing : l10n.testConn),
                                style: OutlinedButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 10)),
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: OutlinedButton.icon(
                                onPressed: _fetchingModels ? null : _fetchModels,
                                icon: _fetchingModels
                                    ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                                    : const FaIcon(FontAwesomeIcons.list, size: 14),
                                label: Text(l10n.fetchModels),
                                style: OutlinedButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 10)),
                              ),
                            ),
                          ],
                        ),
                        if (_modelsStatus != null) ...[
                          const SizedBox(height: 8),
                          Text(
                            _modelsStatus!,
                            style: textTheme.bodySmall?.copyWith(color: cs.onSurfaceVariant),
                          ),
                        ],
                        if (_testResult != null) ...[
                          const SizedBox(height: 8),
                          _TestResultCard(result: _testResult!),
                        ],
                        const SizedBox(height: 16),
                        _buildModelSelector(),
                        const SizedBox(height: 16),
                        _buildPromptTile(),
                        const SizedBox(height: 24),

                        // ── 超时 ──
                        Row(
                          children: [
                            const FaIcon(FontAwesomeIcons.clock, size: 18),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(l10n.timeout, style: textTheme.titleMedium),
                                  Text(
                                    l10n.timeoutSeconds(_aiTimeoutDraft),
                                    style: textTheme.bodySmall?.copyWith(
                                      color: cs.onSurfaceVariant,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                        SliderTheme(
                          data: ModernSliderTheme.theme(context),
                          child: Slider(
                            value: _aiTimeoutDraft.toDouble(),
                            min: 3,
                            max: 30,
                            divisions: 27,
                            label: l10n.timeoutSeconds(_aiTimeoutDraft),
                            onChanged: (v) => setState(
                              () => _aiTimeoutDraft = v.round(),
                            ),
                          ),
                        ),

                        const SizedBox(height: 16),
                        // ── 温度 ──
                        Row(
                          children: [
                            const FaIcon(FontAwesomeIcons.temperatureHalf, size: 18),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(l10n.temperature, style: textTheme.titleMedium),
                                  Text(
                                    l10n.temperatureDesc,
                                    style: textTheme.bodySmall?.copyWith(
                                      color: cs.onSurfaceVariant,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            Text(
                              _aiTemperatureDraft.toStringAsFixed(1),
                              style: textTheme.bodyLarge?.copyWith(
                                color: cs.primary,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                        SliderTheme(
                          data: ModernSliderTheme.theme(context),
                          child: Slider(
                            value: _aiTemperatureDraft,
                            min: 0,
                            max: 1,
                            divisions: 10,
                            label: _aiTemperatureDraft.toStringAsFixed(1),
                            onChanged: (v) => setState(
                              () => _aiTemperatureDraft = v,
                            ),
                          ),
                        ),

                        const SizedBox(height: 16),
                        // ── Max Tokens ──
                        Row(
                          children: [
                            const FaIcon(FontAwesomeIcons.coins, size: 18),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(l10n.maxTokens, style: textTheme.titleMedium),
                                  Text(
                                    l10n.maxTokensDesc,
                                    style: textTheme.bodySmall?.copyWith(
                                      color: cs.onSurfaceVariant,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            Text(
                              '$_aiMaxTokensDraft',
                              style: textTheme.bodyLarge?.copyWith(
                                color: cs.primary,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                        SliderTheme(
                          data: ModernSliderTheme.theme(context),
                          child: Slider(
                            value: _aiMaxTokensDraft.toDouble(),
                            min: 256,
                            max: 8192,
                            divisions: 31,
                            label: '$_aiMaxTokensDraft',
                            onChanged: (v) => setState(
                              () => _aiMaxTokensDraft = v.round(),
                            ),
                          ),
                        ),

                        const SizedBox(height: 24),
                        _saveButton(() { _saveApi(); }),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 16),

                SectionLabel(l10n.fallbackTitle),
                const SizedBox(height: 8),
                Card(
                  elevation: 0,
                  color: cs.surfaceContainerHighest,
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          l10n.fallbackHint,
                          style: textTheme.bodySmall?.copyWith(color: cs.onSurfaceVariant),
                        ),
                        const SizedBox(height: 12),
                        _buildTextField(
                          controller: _fbUrlCtrl,
                          label: l10n.fallbackUrl,
                          hint: l10n.apiUrlHint,
                          icon: FontAwesomeIcons.link,
                        ),
                        const SizedBox(height: 16),
                        _buildTextField(
                          controller: _fbKeyCtrl,
                          label: l10n.apiKey,
                          hint: l10n.apiKeyHint,
                          icon: FontAwesomeIcons.key,
                          obscure: _fbKeyObscured,
                          suffix: IconButton(
                            icon: FaIcon(
                              _fbKeyObscured ? FontAwesomeIcons.eyeSlash : FontAwesomeIcons.eye,
                              size: 16,
                            ),
                            onPressed: () => setState(() => _fbKeyObscured = !_fbKeyObscured),
                          ),
                        ),
                        const SizedBox(height: 16),
                        _buildTextField(
                          controller: _fbModelCtrl,
                          label: l10n.fallbackModel,
                          hint: l10n.modelHint,
                          icon: FontAwesomeIcons.cube,
                        ),
                        _limitSlider(
                          icon: const Icon(Icons.replay, size: 18),
                          title: l10n.retryCount,
                          subtitle: l10n.retryCountDesc,
                          valueLabel: '$_retryDraft',
                          value: _retryDraft.toDouble(),
                          min: 0,
                          max: 3,
                          divisions: 3,
                          onChanged: (v) => setState(() => _retryDraft = v.round()),
                        ),
                        const SizedBox(height: 16),
                        _saveButton(() { _saveFallback(); }),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 16),

                // ── 批处理配置 ──
                SectionLabel(l10n.batchConfig),
                const SizedBox(height: 8),
                Card(
                  elevation: 0,
                  color: cs.surfaceContainerHighest,
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            const FaIcon(FontAwesomeIcons.layerGroup, size: 18),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(l10n.batchSize, style: textTheme.titleMedium),
                                  Text(
                                    l10n.batchSizeDesc,
                                    style: textTheme.bodySmall?.copyWith(
                                      color: cs.onSurfaceVariant,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            Text(
                              '$_batchSizeDraft',
                              style: textTheme.bodyLarge?.copyWith(
                                color: cs.primary,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                        SliderTheme(
                          data: ModernSliderTheme.theme(context),
                          child: Slider(
                            value: _batchSizeDraft.toDouble(),
                            min: 1,
                            max: 50,
                            divisions: 49,
                            label: '$_batchSizeDraft',
                            onChanged: (v) => setState(
                              () => _batchSizeDraft = v.round(),
                            ),
                          ),
                        ),
                        const SizedBox(height: 16),
                        Row(
                          children: [
                            const FaIcon(FontAwesomeIcons.clock, size: 18),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(l10n.batchWindow, style: textTheme.titleMedium),
                                  Text(
                                    l10n.batchWindowDesc,
                                    style: textTheme.bodySmall?.copyWith(
                                      color: cs.onSurfaceVariant,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            Text(
                              '$_batchWindowDraft ms',
                              style: textTheme.bodyLarge?.copyWith(
                                color: cs.primary,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                        SliderTheme(
                          data: ModernSliderTheme.theme(context),
                          child: Slider(
                            value: _batchWindowDraft.toDouble(),
                            min: 50,
                            max: 500,
                            divisions: 9,
                            label: '$_batchWindowDraft ms',
                            onChanged: (v) => setState(
                              () => _batchWindowDraft = v.round(),
                            ),
                          ),
                        ),
                        _limitSlider(
                          icon: const Icon(Icons.speed, size: 18),
                          title: l10n.concurrency,
                          subtitle: l10n.concurrencyDesc,
                          valueLabel: '$_concurrencyDraft',
                          value: _concurrencyDraft.toDouble(),
                          min: 1,
                          max: 8,
                          divisions: 7,
                          onChanged: (v) => setState(() => _concurrencyDraft = v.round()),
                        ),
                        _limitSlider(
                          icon: const Icon(Icons.segment, size: 18),
                          title: l10n.maxParagraphs,
                          subtitle: l10n.maxParagraphsDesc,
                          valueLabel: '$_maxParagraphsDraft',
                          value: _maxParagraphsDraft.toDouble(),
                          min: 1,
                          max: 16,
                          divisions: 15,
                          onChanged: (v) => setState(() => _maxParagraphsDraft = v.round()),
                        ),
                        _limitSlider(
                          icon: const Icon(Icons.short_text, size: 18),
                          title: l10n.maxChars,
                          subtitle: l10n.maxCharsDesc,
                          valueLabel: '$_maxCharsDraft',
                          value: _maxCharsDraft.toDouble(),
                          min: 400,
                          max: 8000,
                          divisions: 38,
                          onChanged: (v) => setState(() => _maxCharsDraft = (v / 200).round() * 200),
                        ),
                        const SizedBox(height: 8),
                        SwitchListTile(
                          contentPadding: EdgeInsets.zero,
                          title: Text(l10n.translationCache),
                          subtitle: Text(l10n.translationCacheDesc),
                          value: _ctrl.cacheEnabled,
                          onChanged: (v) => _ctrl.setCacheEnabled(v),
                        ),
                        const SizedBox(height: 16),
                        _saveButton(() { _saveBatch(); }),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 16),

                Card(
                  elevation: 0,
                  color: cs.secondaryContainer.withValues(alpha: 0.5),
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        FaIcon(
                          FontAwesomeIcons.circleInfo,
                          color: cs.onSecondaryContainer,
                          size: 18,
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Text(
                            l10n.deepseekInfo,
                            style: Theme.of(context).textTheme.bodySmall
                                ?.copyWith(color: cs.onSecondaryContainer),
                          ),
                        ),
                      ],
                    ),
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

  Widget _buildModelSelector() {
    final cs = Theme.of(context).colorScheme;
    final l10n = AppLocalizations.of(context)!;
    final current = _modelCtrl.text;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildTextField(
          controller: _modelCtrl,
          label: l10n.model,
          hint: l10n.modelHint,
          icon: FontAwesomeIcons.lightbulb,
        ),
        if (_availableModels.isNotEmpty) ...[
          const SizedBox(height: 8),
          DropdownButtonFormField<String>(
            isExpanded: true,
            value: _availableModels.contains(current) ? current : null,
            hint: Text(l10n.fetchModels),
            items: _availableModels
                .map((m) => DropdownMenuItem(
                      value: m,
                      child: Text(m, overflow: TextOverflow.ellipsis),
                    ))
                .toList(),
            onChanged: (v) {
              if (v != null) setState(() => _modelCtrl.text = v);
            },
            decoration: InputDecoration(
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide(color: cs.outlineVariant),
              ),
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildPromptTile() {
    final cs = Theme.of(context).colorScheme;
    final l10n = AppLocalizations.of(context)!;
    final preview = _promptCtrl.text.length > 50
        ? '${_promptCtrl.text.substring(0, 50)}...'
        : _promptCtrl.text;

    return Card(
      elevation: 0,
      color: cs.surfaceContainerHighest,
      child: ListTile(
        leading: const FaIcon(FontAwesomeIcons.penToSquare, size: 18),
        title: Text(l10n.translatePrompt),
        subtitle: Text(preview.isNotEmpty ? preview : l10n.promptHint,
            maxLines: 1, overflow: TextOverflow.ellipsis,
            style: TextStyle(fontSize: 12, fontFamily: 'monospace', color: cs.onSurfaceVariant)),
        trailing: const Icon(Icons.edit, size: 18),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        onTap: _showPromptDialog,
      ),
    );
  }

  void _showPromptDialog() {
    final l10n = AppLocalizations.of(context)!;
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Row(
          children: [
            const FaIcon(FontAwesomeIcons.penToSquare, size: 16),
            const SizedBox(width: 8),
            Expanded(child: Text(l10n.translatePrompt)),
            IconButton(
              icon: const Icon(Icons.restart_alt, size: 20),
              onPressed: () {
                _promptCtrl.text = kDefaultPrompt;
              },
              tooltip: l10n.restoreDefault,
            ),
          ],
        ),
        content: SizedBox(
          width: double.maxFinite,
          child: TextField(
            controller: _promptCtrl,
            minLines: 5,
            maxLines: 20,
            style: const TextStyle(fontSize: 12, fontFamily: 'monospace'),
            decoration: InputDecoration(
              hintText: l10n.promptEditHint,
              border: const OutlineInputBorder(),
            ),
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: Text(l10n.confirm)),
        ],
      ),
    );
  }

  Widget _buildTextField({
    required TextEditingController controller,
    required String label,
    required String hint,
    required FaIconData icon,
    bool obscure = false,
    int? minLines,
    int? maxLines = 1,
    Widget? suffix,
  }) {
    final cs = Theme.of(context).colorScheme;
    return TextField(
      controller: controller,
      obscureText: obscure,
      minLines: minLines,
      maxLines: maxLines,
      decoration: InputDecoration(
        labelText: label,
        hintText: hint,
        prefixIcon: Padding(
          padding: const EdgeInsets.all(12),
          child: FaIcon(icon, size: 18),
        ),
        suffixIcon: suffix,
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: cs.outlineVariant),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: cs.primary, width: 2),
        ),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 16,
          vertical: 16,
        ),
        alignLabelWithHint: true,
      ),
      autocorrect: false,
    );
  }
}

class _TestResult {
  final bool success;
  final String message;
  const _TestResult.ok(this.message) : success = true;
  const _TestResult.fail(this.message) : success = false;
}

class _TestResultCard extends StatelessWidget {
  const _TestResultCard({required this.result});
  final _TestResult result;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final color = result.success ? cs.primaryContainer : cs.errorContainer;
    final onColor = result.success
        ? cs.onPrimaryContainer
        : cs.onErrorContainer;
    final icon = result.success
        ? Icons.check_circle_outline
        : Icons.error_outline;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: onColor, size: 18),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              result.message,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: onColor,
                fontFamily: 'monospace',
              ),
            ),
          ),
        ],
      ),
    );
  }
}
