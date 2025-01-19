-dontnote **
-dontwarn **
-keep class org.apache.** { *; }


-keep class ** extends com.sun.jna.Structure { *; } # JNA C struct
-keep class ** extends com.sun.jna.Library { *; } # JNA

-keep enum com.sun.jna.** { *; } # ProGuard bug https://github.com/Guardsquare/proguard/issues/450
-keep class com.jthemedetecor.** { *; } #1404 OsThemeDetector
-keep class oshi.** { *; } #1404 OsThemeDetector
