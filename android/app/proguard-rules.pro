# Rhino's standard objects resolve native members through reflected Java metadata.
-keep class org.mozilla.javascript.** { *; }
-dontwarn javax.lang.model.**
-dontwarn javax.tools.**
# Desktop-only Java interop/bytecode linker paths are unused: the engine enters
# initSafeStandardObjects(), blocks all host classes, and always uses the interpreter.
-dontwarn java.beans.**
-dontwarn jdk.dynalink.**
