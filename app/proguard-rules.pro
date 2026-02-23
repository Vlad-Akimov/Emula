# Keep Parcelable implementations (сохраняем Parcelable)
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# Keep data classes (сохраняем данные)
-keep class com.example.nfctagemulator.data.model.** { *; }

# Keep NFC service (сохраняем NFC сервис)
-keep class com.example.nfctagemulator.nfc.emulator.TagHostApduService

# Remove logging (удаляем логи в релизе)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Keep Compose runtime
-keep class androidx.compose.runtime.** { *; }

# Keep your main activity
-keep class com.example.nfctagemulator.ui.MainActivity

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}