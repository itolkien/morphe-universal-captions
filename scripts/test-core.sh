#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/classes
javac -d build/classes src/main/java/dev/itolkien/captions/*.java src/test/java/dev/itolkien/captions/*.java
java -cp build/classes dev.itolkien.captions.LanguageResolverTest
java -cp build/classes dev.itolkien.captions.CaptionResolverTest
# Test the actual Android-shipped target policy, not a second copy.
javac -d build/classes extensions/extension/src/main/java/dev/itolkien/captions/extension/TranslationTargets.java tests/java/dev/itolkien/captions/extension/TranslationTargetsTest.java
java -cp build/classes dev.itolkien.captions.extension.TranslationTargetsTest
javac -d build/classes extensions/extension/src/main/java/dev/itolkien/captions/extension/CaptionMenuOrder.java tests/java/dev/itolkien/captions/extension/CaptionMenuOrderTest.java
java -cp build/classes dev.itolkien.captions.extension.CaptionMenuOrderTest
sed 's/^package dev\.itolkien\.captions\.extension;/package dev.itolkien.captions;/' extensions/extension/src/main/java/dev/itolkien/captions/extension/CaptionResolver.java > build/shipped-CaptionResolver.java
diff -u src/main/java/dev/itolkien/captions/CaptionResolver.java build/shipped-CaptionResolver.java
