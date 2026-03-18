# Guardian ProGuard Rules
-keep class com.guardian.app.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
