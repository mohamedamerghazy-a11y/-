# Keep kotlinx.serialization & Firestore model classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class com.aleman.cash.model.** { *; }
