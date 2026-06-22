# Keep JNI methods
-keepclasseswithmembernames class com.szyx.ai.engine.llm.LlamaEngine {
    native <methods>;
}

# Keep engine classes accessed by JNI
-keep class com.szyx.ai.engine.llm.StreamingCallback { *; }
-keep class com.szyx.ai.engine.llm.LlamaConfig { *; }

# General JNI rule
-keepclasseswithmembernames class ** {
    native <methods>;
}

# Keep Room entities
-keep class com.szyx.ai.data.db.entity.** { *; }

# Keep MediaPipe
-keep class com.google.mediapipe.** { *; }

# Keep Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
