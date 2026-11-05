# Proguard rules for TubePipe
-keep class com.tubepipe.app.data.model.** { *; }
-keep class org.schabi.newpipe.extractor.** { *; }
-keep class com.yausername.youtubedl_android.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepattributes Signature
-keepattributes *Annotation*

# ExoPlayer / Media3
-keep class androidx.media3.** { *; }
