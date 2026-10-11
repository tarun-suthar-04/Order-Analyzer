# Add project specific ProGuard rules here.

# Keep Apache POI classes
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**

# Keep OpenCSV
-keep class com.opencsv.** { *; }
-dontwarn com.opencsv.**

# Keep XML / SAX
-dontwarn javax.xml.**
-dontwarn org.xml.**
-dontwarn org.w3c.**
-dontwarn org.xmlpull.**
