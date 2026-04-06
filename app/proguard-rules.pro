# Cleanify — release shrinking (R8)

-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Kotlin metadata (reflection, serialization of some APIs)
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Jetpack Compose runtime / UI (conservative keeps for obfuscation)
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.foundation.** { *; }
-dontwarn androidx.compose.**

# Navigation Compose
-keep class androidx.navigation.** { *; }

# Parcelable / services referenced from manifest
-keep public class * extends android.app.Service
-keep public class * extends android.app.Activity
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
