# Regras do ProGuard / R8 para o King of Bozó

# 1. Preservar todas as Data Classes do modelo (usadas pelo Gson e Room)
-keep class com.wendellugalds.kingofbozo.model.** { *; }
-keepclassmembers class com.wendellugalds.kingofbozo.model.** { *; }

# 2. Preservar o Banco de Dados Room e DAOs
-keep class * extends androidx.room.RoomDatabase
-keep class com.wendellugalds.kingofbozo.database.** { *; }
-keepclassmembers class com.wendellugalds.kingofbozo.database.** { *; }

# 3. Preservar suporte a anotações e reflexão do Gson
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 4. Preservar anotação @Keep da AndroidX
-keep class androidx.annotation.Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
