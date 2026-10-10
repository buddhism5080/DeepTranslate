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

    /** 用 root 直接删除目标 App 的缓存数据库（最可靠，不依赖 App 是否在运行）。 */
    private fun clearCacheWithRoot(pkg: String) {
        val safe = safePackage(pkg) ?: return
        Thread {
            try {
                val cmd = "rm -f /data/data/$safe/databases/deeptranslate_cache.db /data/data/$safe/databases/deeptranslate_cache.db-*"
                val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                proc.waitFor()
                Log.d(TAG, "root cache clear for $pkg: exit=${proc.exitValue()}")
            } catch (e: Exception) {
                Log.w(TAG, "root cache clear failed for $pkg: ${e.message}")
            }
        }.start()
    }

    /** 发送广播到目标 App 进程，让 Hook 清空 SQLite 缓存。 */
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
                    val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
                    result.success(prefs.getLong("flutter.pref_cache_count", 0L).toInt())
                }

                "getCacheDetails" -> {
                    val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
                    val jsonStr = prefs.getString(CACHE_PREFS_KEY, "{}") ?: "{}"
                    try {
                        val jsonObj = JSONObject(jsonStr)
                        val list = mutableListOf<Map<String, Any>>()
                        for (key in jsonObj.keys()) {
                            val count = jsonObj.getInt(key)
                            val (appName, iconBytes) = getAppInfo(key)
                            list.add(mapOf("package" to key, "count" to count, "name" to appName, "icon" to iconBytes))
                        }
                        result.success(list)
                    } catch (e: Exception) {
                        result.success(emptyList<Map<String, Any>>())
                    }
                }

                "clearAppCache" -> {
                    val pkg = call.argument<String>("package") ?: ""
                    if (pkg.isNotEmpty()) {
                        sendClearBroadcast(pkg)
                        clearCacheWithRoot(pkg)
                        // 清空本地统计
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
                        } catch (_: Exception) {}
                        result.success(true)
                    } else {
                        result.success(false)
                    }
                }

                "clearAllCache" -> {
                    val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
                    val jsonStr = prefs.getString(CACHE_PREFS_KEY, "{}") ?: "{}"
                    try {
                        val jsonObj = JSONObject(jsonStr)
                        for (key in jsonObj.keys()) {
                            sendClearBroadcast(key)
                            clearCacheWithRoot(key)
                        }
                    } catch (_: Exception) {}
                    prefs.edit()
                        .putString(CACHE_PREFS_KEY, "{}")
                        .putLong("flutter.pref_cache_count", 0L)
                        .apply()
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
                    val pkg = call.argument<String>("package")?.let { safePackage(it) }
                    if (call.argument<String>("package").isNullOrEmpty()) {
                        ErrorLogStore.clear(this, null)
                        result.success(true)
                    } else if (pkg != null) {
                        ErrorLogStore.clear(this, pkg)
                        result.success(true)
                    } else {
                        result.success(false)
                    }
                }

                else -> result.notImplemented()
            }
        }
    }
}
