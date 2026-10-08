# Dramix Proguard & R8 Rules

# Keep Moshi models and adapters
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class com.dramix.app.data.source.remote.dto.** { *; }
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# Keep Room database entities & DAOs
-keep class com.dramix.app.core.database.entity.** { *; }
-keep class com.dramix.app.core.database.dao.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.**

# Keep Domain models
-keep class com.dramix.app.domain.model.** { *; }

# Keep AndroidX Media3 (ExoPlayer & DownloadManager)
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Retrofit & OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Exceptions
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep Koin DI
-keep class * extends org.koin.core.module.Module { *; }
-dontwarn org.koin.**
