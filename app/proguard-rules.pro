# R8 with packageScope moved StringsKt__StringBuilderKt out of kotlin.text, away from its
# package-private parent, and the app crashed at startup with an IllegalAccessError.
# Keeping the names keeps this chain of classes in one package.
-keep,allowshrinking,allowoptimization class kotlin.text.StringsKt__*

# Release builds log nothing: this removes log calls from the code R8 processes.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static int println(...);
}
