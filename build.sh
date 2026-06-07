#!/usr/bin/env bash
# ============================================================
#  build.sh — compiles NetScout and packages it into a fat jar
#  Usage: ./build.sh
# ============================================================

set -e

SRC_DIR="src/main/java"
OUT_DIR="out"
JAR_NAME="netscout.jar"
MAIN_CLASS="com.netscout.NetScout"

echo "==> Cleaning build output..."
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

echo "==> Compiling Java sources..."
# Try to find javac — it lives in the JDK, not always on PATH
JAVAC=$(which javac 2>/dev/null || find /usr/lib/jvm -name "javac" 2>/dev/null | head -1)
if [ -z "$JAVAC" ]; then
    echo "ERROR: javac not found. Install the JDK:"
    echo "  Ubuntu/Debian: sudo apt install default-jdk"
    echo "  macOS:         brew install openjdk"
    echo "  Windows:       https://adoptium.net"
    exit 1
fi
echo "    Using: $JAVAC"
find "$SRC_DIR" -name "*.java" | xargs "$JAVAC" -d "$OUT_DIR"

echo "==> Packaging jar..."
cd "$OUT_DIR"
echo "Main-Class: $MAIN_CLASS" > manifest.txt
jar cfm "../$JAR_NAME" manifest.txt .
cd ..

echo ""
echo "Build complete!  Run with:"
echo "  java -jar $JAR_NAME <CIDR> [options]"
echo "  java -jar $JAR_NAME --help"
echo ""
