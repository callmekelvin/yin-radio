# Add project specific R8 rules here.
# AGP will combine all keep rule files passed to proguardFiles() to pass to R8
#
# For more details, see
#   https://d.android.com/r/tools/r8/keep-rules

# ============================================================================
# Kotlinx Serialization
# ============================================================================
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class * { @kotlinx.serialization.Serializable <fields>; }
-keepclassmembers @kotlinx.serialization.Serializable class * { <init>(...); }

# Keep DTO serializer synthetic classes and companion objects
-keepclassmembers class com.yin_radio.yin_radio_android_app.data.remote.dto.** {
    static **$* *;
    ** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.yin_radio.yin_radio_android_app.data.remote.dto.** { *; }

# ============================================================================
# Room (Persistence)
# ============================================================================
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep entity classes and their constructors (Room reflects on them)
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { <init>(...); }

# Keep DAO interfaces and their methods
-keep @androidx.room.Dao class *
-keepclassmembers @androidx.room.Dao class * { *; }

# Keep TypeConverter classes if you add any later
-keep @androidx.room.TypeConverter class * { *; }

# ============================================================================
# Koin (Dependency Injection)
# ============================================================================
-keep class com.yin_radio.yin_radio_android_app.di.AppModule { *; }
-keep class com.yin_radio.yin_radio_android_app.YinRadioApplication { *; }
-keepclassmembers class * { @org.koin.core.annotation.* <methods>; }
-keepclassmembers class * { @org.koin.android.annotation.* <methods>; }

# ============================================================================
# Media3 / ExoPlayer (Playback)
# ============================================================================
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class com.yin_radio.yin_radio_android_app.service.RadioPlaybackService { *; }

# ============================================================================
# Ktor (Networking)
# ============================================================================
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class com.yin_radio.yin_radio_android_app.data.remote.api.** { *; }

# ============================================================================
# Kotlin Coroutines
# ============================================================================
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ============================================================================
# Jetpack Compose
# ============================================================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ============================================================================
# SLF4J (pulled in transitively by Ktor logging)
# ============================================================================
-dontwarn org.slf4j.impl.StaticLoggerBinder

# ============================================================================
# General Android / Kotlin
# ============================================================================
-keep class kotlin.Metadata { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
