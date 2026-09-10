# ==============================================================================
# Regras do ProGuard / R8 para o King of Bozó
# ==============================================================================

# 1. Preservar atributos de reflexão, genéricos, assinaturas e anotações
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# 2. Preservar todas as Data Classes e Enums do modelo (usadas pelo Gson e Room)
-keep class com.wendellugalds.kingofbozo.model.** { *; }
-keepclassmembers class com.wendellugalds.kingofbozo.model.** { *; }

# Preservar membros de Enums (valores, valueOf, etc.)
-keepclassmembers enum com.wendellugalds.kingofbozo.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 3. Preservar suporte total ao Gson, TypeTokens e anotações @SerializedName
-keep class com.google.gson.annotations.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 4. Preservar Banco de Dados Room, Converters e DAOs
-keep class * extends androidx.room.RoomDatabase
-keep class com.wendellugalds.kingofbozo.database.** { *; }
-keepclassmembers class com.wendellugalds.kingofbozo.database.** { *; }
-keep class com.wendellugalds.kingofbozo.model.Converters { *; }

# 5. Preservar anotação @Keep da AndroidX
-keep class androidx.annotation.Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# 6. Preservar classes Serializable
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
