# NhatKyDuongHuyet release rules

# Keep runtime annotations and generic signatures used by serializers and Room.
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations,Signature,*AnnotationDefault

# Keep Room entities and DAOs referenced by generated code.
-keep class com.example.nhatkyduonghuyet.data.local.entity.** { *; }
-keep interface com.example.nhatkyduonghuyet.data.local.dao.** { *; }

# Keep backend JSON payload classes and public result types.
-keep class com.example.nhatkyduonghuyet.data.remote.** { *; }
-keep class com.example.nhatkyduonghuyet.domain.usecase.** { *; }

# Keep TensorFlow Lite model/runtime entry points.
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# CameraX and ML Kit ship their own consumer rules; suppress only optional warnings.
-dontwarn com.google.mlkit.**
