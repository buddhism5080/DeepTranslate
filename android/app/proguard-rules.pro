# Keep LSPosed module entry class
-keep class io.github.kirby.deeptranslate.** { *; }
-keep class io.github.libxposed.** { *; }

# Keep Xposed metadata
-keep class META-INF.xposed.** { *; }

-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class io.github.kirby.deeptranslate.xposed.hook.WebViewHook$JsBridge {
    @android.webkit.JavascriptInterface <methods>;
}
