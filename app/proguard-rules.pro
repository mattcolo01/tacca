# kotlinx.serialization: keep generated serializers of our @Serializable classes
# (backup DTOs and type-safe navigation routes).
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class it.matteocolombo.tacca.**$$serializer { *; }
-keepclassmembers class it.matteocolombo.tacca.** {
    *** Companion;
}
-keepclasseswithmembers class it.matteocolombo.tacca.** {
    kotlinx.serialization.KSerializer serializer(...);
}
