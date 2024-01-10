# Copyright 2019 Exotel India Pvt Ltd
# All rights reserved
# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

#-dontshrink
#-dontoptimize
#-dontobfuscate
# Preverification is irrelevant for the dex compiler and the Dalvik VM.
-dontpreverify

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable
# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile
# Keep the "Exceptions" attribute, so the compiler knows which exceptions methods may throw.
-keepattributes Exceptions
# Keep following to preserve access by Java Reflections
-keepattributes Signature,InnerClasses,MethodParameters,EnclosingMethod
# Keep annotations for compiler
-keepattributes Deprecated,Synthetic,*Annotation*
# Reduce the size of the output some more.
-repackageclasses ''
-allowaccessmodification
# Switch off some optimizations that trip older versions of the Dalvik VM.
-optimizations !code/simplification/arithmetic
# Preserve all native method names and the names of their classes.
-keepclasseswithmembernames,includedescriptorclasses class * { native <methods>; }

### Exotel Voice Client SDK
## Preserve serializable data
-keep class com.exotel.voicesample.PushNotificationData { *;}

## Android Material Design APIs
# Ignore dynamically referenced classes that should be part of Android baseport
-keepclassmembernames class com.google.android.material.** { public *; }
-dontnote com.google.android.material.**

## Misc
# Ignore dynamically referenced classes that should be part of Android baseport
-dontnote android.os.SystemProperties
-dontnote sun.net.dns.**
