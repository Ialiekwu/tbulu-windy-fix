#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/test-classes
javac -encoding UTF-8 -cp '.tools/test-libs/*' -d build/test-classes \
  app/src/main/java/dev/local/windyfix/TilePolicy.java \
  app/src/main/java/dev/local/windyfix/RequestRewriter.java tests/RequestRewriterTest.java
java -cp 'build/test-classes:.tools/test-libs/*' RequestRewriterTest
python3 -m unittest discover -s tests -p 'test_*.py' -v
