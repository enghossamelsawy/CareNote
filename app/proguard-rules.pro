# R8 with packageScope moved StringsKt__StringBuilderKt out of kotlin.text, away from its
# package-private parent, and the app crashed at startup with an IllegalAccessError.
# Keeping the names keeps this chain of classes in one package.
-keep,allowshrinking,allowoptimization class kotlin.text.StringsKt__*

# Removes log calls from the code R8 processes, which is only the packages in packageScope.
# Libraries outside it, such as Google Play services and OkHttp, can still log in release builds.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static int println(...);
}
