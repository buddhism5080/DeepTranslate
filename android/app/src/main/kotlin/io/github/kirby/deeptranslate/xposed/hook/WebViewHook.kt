package io.github.kirby.deeptranslate.xposed.hook

import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import io.github.kirby.deeptranslate.xposed.ConfigManager
import io.github.kirby.deeptranslate.xposed.TextBatcher
import io.github.kirby.deeptranslate.xposed.TextGate
import io.github.kirby.deeptranslate.xposed.TextKinds
import io.github.kirby.deeptranslate.xposed.TranslationSession
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Translates visible text nodes inside WebView. Inputs, scripts and style are skipped.
 * Flutter Skia and React Native are still out of reach.
 */
object WebViewHook : BaseHook() {

    override fun getTag() = "DeepTranslate[WebView]"

    private val hookedClients = ConcurrentHashMap.newKeySet<String>()
    private val bridged = Collections.synchronizedMap(WeakHashMap<WebView, Boolean>())

    override fun onInit(module: XposedModule, param: PackageLoadedParam) {
        val batcher = TranslationSession.batcher(module, param.packageName)
        ConfigManager.addChangeListener {
            val on = ConfigManager.isTranslationEnabled() && ConfigManager.isHookWebView()
            val views = synchronized(bridged) { bridged.keys.toList() }
            for (view in views) {
                view.post {
                    try {
                        if (on) inject(view)
                        else view.evaluateJavascript("window.__dtPaused=true; window.__dtAbort && window.__dtAbort();", null)
                    } catch (_: Throwable) {
                    }
                }
            }
        }
        val loader = param.defaultClassLoader
        val webView = load(loader, "android.webkit.WebView") ?: return

        for (ctor in webView.declaredConstructors) {
            module.hook(ctor).intercept { chain ->
                val result = chain.proceed()
                val view = chain.thisObject as? WebView
                if (view != null) attachBridge(view, batcher)
                result
            }
        }

        val client = load(loader, "android.webkit.WebViewClient")
        val onFinished = client?.declaredMethods?.firstOrNull {
            it.name == "onPageFinished" && it.parameterTypes.size == 2
        }
        if (onFinished != null) {
            module.hook(onFinished).intercept { chain ->
                val result = chain.proceed()
                (chain.args.getOrNull(0) as? WebView)?.let { inject(it) }
                result
            }
        }

        val setClient = webView.declaredMethods.firstOrNull {
            it.name == "setWebViewClient" && it.parameterTypes.size == 1
        }
        if (setClient != null) {
            module.hook(setClient).intercept { chain ->
                val result = chain.proceed()
                val concrete = chain.args.getOrNull(0)?.javaClass
                if (concrete != null && concrete != WebViewClient::class.java) {
                    hookConcreteClient(module, concrete)
                }
                result
            }
        }

        for (method in webView.declaredMethods) {
            if (method.name != "loadUrl" && method.name != "loadData" && method.name != "loadDataWithBaseURL") continue
            module.hook(method).intercept { chain ->
                val result = chain.proceed()
                val view = chain.thisObject as? WebView
                if (view != null) {
                    view.postDelayed({ inject(view) }, 600)
                    view.postDelayed({ inject(view) }, 1800)
                }
                result
            }
        }
        log(module, "WebView hooks installed for ${param.packageName}")
    }

    private fun hookConcreteClient(module: XposedModule, clazz: Class<*>) {
        if (!hookedClients.add(clazz.name)) return
        val method = try {
            clazz.getDeclaredMethod("onPageFinished", WebView::class.java, String::class.java)
        } catch (_: Throwable) {
            return
        }
        if (method.declaringClass == WebViewClient::class.java) return
        module.hook(method).intercept { chain ->
            val result = chain.proceed()
            (chain.args.getOrNull(0) as? WebView)?.let { inject(it) }
            result
        }
    }

    private fun attachBridge(webView: WebView, batcher: TextBatcher) {
        if (bridged.put(webView, true) != null) return
        try {
            webView.addJavascriptInterface(JsBridge(webView, batcher), "DeepTranslate")
        } catch (_: Throwable) {
        }
    }

    private fun inject(webView: WebView) {
        if (!ConfigManager.isTranslationEnabled() || !ConfigManager.isHookWebView()) return
        try {
            webView.evaluateJavascript(BOOT_SCRIPT, null)
        } catch (_: Throwable) {
        }
    }

    private fun load(loader: ClassLoader, name: String): Class<*>? =
        try {
            Class.forName(name, false, loader)
        } catch (_: Throwable) {
            null
        }

    class JsBridge(
        webView: WebView,
        private val batcher: TextBatcher,
    ) {
        private val viewRef = WeakReference(webView)

        @JavascriptInterface
        fun offer(payload: String) {
            val texts = try {
                val arr = JSONArray(payload)
                val collected = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    if (collected.size >= 40) break
                    val text = arr.optString(i, "")
                    if (text.isNotBlank()) collected.add(text)
                }
                collected
            } catch (_: Throwable) {
                abort()
                return
            }
            if (!ConfigManager.isTranslationEnabled() || !ConfigManager.isHookWebView()) {
                finish(emptyMap(), texts)
                return
            }
            if (texts.isEmpty()) return
            val todo = texts.filter { TextGate.shouldTranslate(it) }
            if (todo.isEmpty()) {
                finish(emptyMap(), texts)
                return
            }
            val pending = AtomicInteger(todo.size)
            val translated = ConcurrentHashMap<String, String>()
            for (text in todo) {
                batcher.submit(text, TextKinds.of(text)) { result ->
                    if (ConfigManager.isTranslationEnabled() && result != text) translated[text] = result
                    if (pending.decrementAndGet() == 0) finish(translated, texts)
                }
            }
        }

        private fun abort() {
            val view = viewRef.get() ?: return
            view.post {
                try {
                    view.evaluateJavascript("window.__dtAbort && window.__dtAbort()", null)
                } catch (_: Throwable) {
                }
            }
        }

        private fun finish(ok: Map<String, String>, settle: List<String>) {
            val view = viewRef.get() ?: return
            val payload = JSONObject()
            payload.put("ok", JSONObject(ok))
            payload.put("settle", JSONArray(settle))
            val encoded = JSONObject.quote(payload.toString())
            view.post {
                try {
                    view.evaluateJavascript("window.__dtFinish && window.__dtFinish($encoded)", null)
                } catch (_: Throwable) {
                }
            }
        }
    }

    private val BOOT_SCRIPT = """
        (function(){
          function boot(){
            if (!document.body || !window.DeepTranslate) return;
            window.__dtPaused = false;
            if (window.__dtBooted) { if (window.__dtCollect) window.__dtCollect(); return; }
            window.__dtAbort = function(){
              if (!document.body) return;
              var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null);
              var n;
              while ((n = walker.nextNode())) { n.__dtPending = false; }
            };
            window.__dtBooted = true;
            function skip(el){
              if (!el) return true;
              var node = el;
              while (node) {
                var t = node.tagName;
                if (t === 'SCRIPT' || t === 'STYLE' || t === 'NOSCRIPT' || t === 'TEXTAREA' || t === 'INPUT' || t === 'CODE') return true;
                if (node.isContentEditable) return true;
                node = node.parentElement;
              }
              return false;
            }
            function collect(){
              if (window.__dtPaused || !document.body || !window.DeepTranslate) return;
              var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null);
              var batch = [];
              var n;
              while ((n = walker.nextNode())) {
                if (n.__dtLock || n.__dtPending) continue;
                if (skip(n.parentElement)) continue;
                var raw = n.nodeValue;
                if (!raw || raw.trim().length < 2) continue;
                n.__dtPending = true;
                batch.push(raw);
                if (batch.length >= 40) break;
              }
              if (batch.length) window.DeepTranslate.offer(JSON.stringify(batch));
            }
            window.__dtFinish = function(encoded){
              var msg;
              try { msg = JSON.parse(encoded); } catch (e) { collect(); return; }
              var ok = msg.ok || {};
              var settle = msg.settle || [];
              var settleSet = {};
              for (var i = 0; i < settle.length; i++) settleSet[settle[i]] = true;
              if (!document.body) return;
              var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null);
              var n;
              while ((n = walker.nextNode())) {
                if (skip(n.parentElement)) continue;
                var v = n.nodeValue;
                if (!v) continue;
                if (Object.prototype.hasOwnProperty.call(ok, v)) {
                  n.nodeValue = ok[v];
                  n.__dtLock = true;
                  n.__dtPending = false;
                } else if (settleSet[v]) {
                  n.__dtPending = false;
                }
              }
              collect();
            };
            var timer = null;
            new MutationObserver(function(){
              if (timer) return;
              timer = setTimeout(function(){ timer = null; collect(); }, 150);
            }).observe(document.body, {subtree:true, childList:true, characterData:true});
            window.__dtCollect = collect;
            collect();
          }
          boot();
        })();
    """.trimIndent()
}
