# Add project specific ProGuard rules here.
# By default, the noise in SQL queries is kept out of obfuscation:
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
