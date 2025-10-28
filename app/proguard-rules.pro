# ===============================
# proguard-rules.pro
# ===============================

# Add project specific ProGuard rules here.

# Keep Hilt components
-keep class dagger.hilt.android.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-dontwarn dagger.hilt.android.**

# Keep Room entities and DAO
-keep class net.calvuz.qreport.data.local.entity.** { *; }
-keep class net.calvuz.qreport.data.local.dao.** { *; }

# Keep domain models for serialization
-keep class net.calvuz.qreport.domain.model.** { *; }

# Apache POI
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.apache.commons.codec.**

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Kotlinx Datetime
-keep class kotlinx.datetime.** { *; }
-dontwarn kotlinx.datetime.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
