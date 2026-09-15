# Lexora Service release rules.
# Keep this file minimal: Room and Compose publish consumer rules where needed.
# Add explicit keep rules here only when a concrete release-build issue requires them.

# Preserve source/line information for useful crash diagnostics without keeping debug code.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
