# Preserve stack trace metadata for crash diagnostics
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep data models, Room entities, and Moshi adapters for Retrofit serialization
-keep class com.julcer26.tasave.data.** { *; }
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class **JsonAdapter { *; }

# Keep Ultra enums & models used via valueOf() and manual JSON serialization
-keep class com.julcer26.tasave.ultra.** { *; }
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep AppWidgets, QuickCalc, and Billing callbacks intact under R8
-keep class com.julcer26.tasave.widget.** { *; }
-keep class com.julcer26.tasave.quick.** { *; }
-keep class com.julcer26.tasave.billing.** { *; }

# Suppress harmless warnings from third-party libraries
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
