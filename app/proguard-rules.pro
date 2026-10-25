# Vosk usa JNA (reflexión): sin estas reglas R8 rompe el reconocimiento de voz.
-keep class com.sun.jna.** { *; }
-keep class org.vosk.** { *; }
-keepclassmembers class * extends com.sun.jna.** { public *; }
-dontwarn java.awt.**
-dontwarn com.sun.jna.**
