package io.github.kirby.deeptranslate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import org.json.JSONObject

class MainActivity : FlutterActivity() {
    private val CHANNEL = "io.github.kirby.deeptranslate/channel"
    private val TAG = "DeepTranslate"
    private val CACHE_PREFS_KEY = "flutter.pref_cache_details"

    private val packageNamePattern = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

    private fun safePackage(pkg: String): String? = pkg.takeIf { packageNamePattern.matches(it) }

    /** API 34+ 能看出广播是谁发的。发送方必须就是 extras 里的那个包。 */
    private fun BroadcastReceiver.senderOwns(pkg: String): Boolean {
        if (android.os.Build.VERSION.SDK_INT < 34) return true
        return sentFromPackage == pkg
    }

    private val tokenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val cached = intent?.getIntExtra("cached", 0) ?: 0
            val pkg = safePackage(intent?.getStringExtra("package") ?: "") ?: return
            if (cached !in 1..1000) return
            if (!tokenOk(intent)) return
            if (!senderOwns(pkg)) return
            val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            try {
                val jsonObj = JSONObject(prefs.getString(CACHE_PREFS_KEY, "{}") ?: "{}")
                jsonObj.put(pkg, jsonObj.optInt(pkg, 0) + cached)
                val next = (prefs.getLong("flutter.pref_cache_count", 0L) + cached).coerceAtMost(Int.MAX_VALUE.toLong())
                prefs.edit()
                    .putString(CACHE_PREFS_KEY, jsonObj.toString())
                    .putLong("flutter.pref_cache_count", next)
                    .apply()
            } catch (e: Exception) {
                Log.e(TAG, "cache update failed: ${e.message}")
            }
        }
    }

    private val cacheClearedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val pkg = safePackage(intent?.getStringExtra("package") ?: "") ?: return
            if (!tokenOk(intent)) return
            if (!senderOwns(pkg)) return
            val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(CACHE_PREFS_KEY, "{}") ?: "{}"
            try {
                val jsonObj = JSONObject(jsonStr)
                val removed = jsonObj.optInt(pkg, 0)
                jsonObj.remove(pkg)
                prefs.edit()
                    .putString(CACHE_PREFS_KEY, jsonObj.toString())
                    .putLong("flutter.pref_cache_count", maxOf(0L, prefs.getLong("flutter.pref_cache_count", 0L) - removed))
                    .apply()
                Log.d(TAG, "cache cleared for $pkg, -$removed entries")
            } catch (_: Exception) {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(tokenReceiver, IntentFilter("io.github.kirby.deeptranslate.TOKEN_UPDATE"), Context.RECEIVER_EXPORTED)
        registerReceiver(cacheClearedReceiver, IntentFilter("io.github.kirby.deeptranslate.CACHE_CLEARED"), Context.RECEIVER_EXPORTED)
    }

    override fun onDestroy() {
        unregisterReceiver(tokenReceiver)
        unregisterReceiver(cacheClearedReceiver)
        super.onDestroy()
    }

    private fun tokenOk(intent: Intent?): Boolean {
        val expected = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            .getString("flutter.pref_broadcast_token", "") ?: ""
        return ModuleBroadcast.tokenMatches(intent, expected)
    }

    /** 获取应用名和图标字节。 */
    private fun getAppInfo(pkg: String): Pair<String, ByteArray> {
        return try {
            val info = packageManager.getApplicationInfo(pkg, 0)
            val name = packageManager.getApplicationLabel(info).toString()
            val drawable = packageManager.getApplicationIcon(pkg)
            val bitmap = if (drawable is BitmapDrawable) drawable.bitmap
                        else {
                            val bmp = android.graphics.Bitmap.createBitmap(
                                drawable.intrinsicWidth.coerceAtLeast(64),
                                drawable.intrinsicHeight.coerceAtLeast(64),
                                android.graphics.Bitmap.Config.ARGB_8888
                            )
                            val canvas = android.graphics.Canvas(bmp)
                            drawable.setBounds(0, 0, bmp.width, bmp.height)
                            drawable.draw(canvas)
                            bmp
                        }
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 80, stream)
            Pair(name, stream.toByteArray())
        } catch (e: Exception) {
            Pair(pkg, ByteArray(0))
        }
    }

    /** 让还在运行的目标进程丢掉内存里的旧结果。磁盘只在模块这边。 */
    private fun sendClearBroadcast(pkg: String) {
        val safe = safePackage(pkg) ?: return
        val intent = Intent("io.github.kirby.deeptranslate.CLEAR_CACHE").apply {
            setPackage(safe)
        }
        sendBroadcast(intent)
        Log.d(TAG, "sent CLEAR_CACHE broadcast to $pkg")
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "getModuleStatus" -> {
                    val active = XposedPrefsSyncApp.awaitReady()
                    result.success(mapOf(
                        "active" to active,
                        "apiVersion" to XposedPrefsSyncApp.getApiVersion(),
                        "frameworkName" to XposedPrefsSyncApp.getFrameworkName(),
                        "frameworkVersion" to XposedPrefsSyncApp.getFrameworkVersion()
                    ))
                }

                "getCacheCount" -> {
                    result.success(ModuleCacheStore.count(this))
                }

                "getCacheDetails" -> {
                    val list = ModuleCacheStore.packages(this).map { (pkg, count) ->
                        val (appName, iconBytes) = getAppInfo(pkg)
                        mapOf("package" to pkg, "count" to count, "name" to appName, "icon" to iconBytes)
                    }
                    result.success(list)
                }

                "clearAppCache" -> {
                    val pkg = safePackage(call.argument<String>("package") ?: "")
                    if (pkg != null) {
                        ModuleCacheStore.clearPackage(this, pkg)
                        sendClearBroadcast(pkg)
                        result.success(true)
                    } else {
                        result.success(false)
                    }
                }

                "clearAllCache" -> {
                    val pkgs = ModuleCacheStore.packages(this).map { it.first }
                    for (pkg in pkgs) sendClearBroadcast(pkg)
                    ModuleCacheStore.clearAll(this)
                    Toast.makeText(this, "已清空所有缓存", Toast.LENGTH_SHORT).show()
                    result.success(true)
                }

                "getAppVersion" -> {
                    try {
                        result.success(packageManager.getPackageInfo(packageName, 0).versionName)
                    } catch (e: Exception) { result.success("unknown") }
                }

                "getBuildTime" -> { result.success(BuildConfig.BUILD_TIME) }

                "listErrorLogs" -> {
                    val list = ErrorLogStore.grouped(this).map { (pkg, entries) ->
                        val (appName, iconBytes) = getAppInfo(pkg)
                        mapOf("package" to pkg, "name" to appName, "icon" to iconBytes, "entries" to entries)
                    }
                    result.success(list)
                }

                "deleteErrorLogs" -> {
                    val ids = call.argument<List<*>>("ids")?.mapNotNull { it as? String }?.toSet() ?: emptySet()
                    ErrorLogStore.delete(this, ids)
                    result.success(true)
                }

                "clearErrorLogs" -> {
                    val raw = call.argument<String>("package")
                    val pkg = raw?.let { safePackage(it) }
                    if (raw.isNullOrEmpty()) {
                        ErrorLogStore.clear(this, null)
                        result.success(true)
                    } else if (pkg != null) {
                        ErrorLogStore.clear(this, pkg)
                        result.success(true)
                    } else {
                        result.success(false)
                    }
                }

                "getErrorLog" -> {
                    val id = call.argument<String>("id") ?: ""
                    result.success(ErrorLogStore.entry(this, id))
                }

                "listCacheRows" -> {
                    val pkg = safePackage(call.argument<String>("package") ?: "")
                    if (pkg == null) result.success(emptyList<Map<String, Any>>())
                    else result.success(ModuleCacheStore.list(this, pkg, call.argument<String>("query") ?: ""))
                }

                "getCacheRow" -> {
                    val pkg = safePackage(call.argument<String>("package") ?: "")
                    val hash = call.argument<String>("hash") ?: ""
                    if (pkg == null || !hash.matches(Regex("^[0-9a-f]{64}$"))) result.success(null)
                    else result.success(ModuleCacheStore.detail(this, pkg, hash))
                }

                "deleteCacheRows" -> {
                    val pkg = safePackage(call.argument<String>("package") ?: "")
                    val hashes = call.argument<List<*>>("hashes")?.mapNotNull { it as? String }
                        ?.filter { it.matches(Regex("^[0-9a-f]{64}$")) } ?: emptyList()
                    if (pkg == null || hashes.isEmpty()) result.success(false)
                    else {
                        ModuleCacheStore.delete(this, pkg, hashes)
                        val intent = Intent("io.github.kirby.deeptranslate.DELETE_CACHE").apply {
                            setPackage(pkg)
                            putStringArrayListExtra("hashes", ArrayList(hashes))
                        }
                        sendBroadcast(intent)
                        result.success(true)
                    }
                }

                else -> result.notImplemented()
            }
        }
    }
}
