/// OpenAI-compatible base URL, full chat URL, or models URL.
class OpenAiEndpoints {
  final String chat;
  final String models;
  const OpenAiEndpoints(this.chat, this.models);

  static OpenAiEndpoints? resolve(String raw) {
    var url = raw.trim();
    while (url.endsWith('/')) {
      url = url.substring(0, url.length - 1);
    }
    if (url.isEmpty || !url.contains('://')) return null;
    if (url.endsWith('/chat/completions')) {
      final base = url.substring(0, url.length - '/chat/completions'.length);
      return OpenAiEndpoints(url, '$base/models');
    }
    if (url.endsWith('/models')) {
      final base = url.substring(0, url.length - '/models'.length);
      return OpenAiEndpoints('$base/chat/completions', url);
    }
    return OpenAiEndpoints('$url/chat/completions', '$url/models');
  }
}

/// Accepts OpenAI `{data:[{id}]}`, `{models:[{id|name}]}`, or a bare list.
List<String> parseModelIds(dynamic decoded) {
  final out = <String>{};
  void take(dynamic item) {
    if (item is String && item.isNotEmpty) {
      out.add(item);
      return;
    }
    if (item is Map) {
      final id = item['id'] ?? item['name'] ?? item['model'];
      if (id is String && id.isNotEmpty) out.add(id);
    }
  }

  if (decoded is List) {
    for (final item in decoded) {
      take(item);
    }
  } else if (decoded is Map) {
    final data = decoded['data'] ?? decoded['models'];
    if (data is List) {
      for (final item in data) {
        take(item);
      }
    }
  }
  final list = out.toList()..sort();
  return list;
}
