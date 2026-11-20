# SQLCipher for Android (JNI bindings & native cursor reflection)
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# Room Database Entities & Generated Implementations
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepnames class com.lifeos.core.database.entity.** { *; }
