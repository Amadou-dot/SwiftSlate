# SwiftSlate ProGuard Rules

# Keep the model package (data classes used with JSON serialization)
-keep class com.musheer360.swiftslate.model.** { *; }

# Keep JSON-related classes (org.json is Android built-in but keep for safety)
-keep class org.json.** { *; }

# Keep the accessibility service (referenced in AndroidManifest.xml)
-keep class com.musheer360.swiftslate.service.AssistantService { *; }
