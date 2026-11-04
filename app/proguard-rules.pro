# R8 with packageScope moved StringsKt__StringBuilderKt out of kotlin.text, away from its
# package-private parent, and the app crashed at startup with an IllegalAccessError.
# Keeping the names keeps this chain of classes in one package.
-keep,allowshrinking,allowoptimization class kotlin.text.StringsKt__*
