#!/usr/bin/env bash
set -e

echo "=================================================="
echo "    HEX Audiophile Player — ARM64-v8a Port       "
echo "=================================================="
echo "Architecture: arm64-v8a (64-bit ARM)"
echo "C++ DSP:      NEON SIMD RBJ Biquad Filter"
echo "Audio Output: AndroidX Media3 / AAudio float PCM"
echo "Min SDK:      26 (Android 8.0 Oreo)"
echo "Target SDK:   34 (Android 14 Upside Down Cake)"
echo "=================================================="

# Check for gradlew
if [ ! -f "./gradlew" ]; then
    echo "Error: ./gradlew not found. Run this script from the android/ directory."
    exit 1
fi

chmod +x ./gradlew

# Auto-detect Java 17+ if not set
if [ -z "$JAVA_HOME" ] || [ ! -d "$JAVA_HOME" ]; then
    if [ -d "/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home" ]; then
        export JAVA_HOME="/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home"
        export PATH="$JAVA_HOME/bin:$PATH"
    elif [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
        export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
        export PATH="$JAVA_HOME/bin:$PATH"
    fi
fi

# Check for Java
if ! command -v java &> /dev/null; then
    echo "Warning: Java runtime is not found in PATH."
    echo "Please ensure JDK 17+ and ANDROID_HOME are installed and configured."
    echo "You can import this directory directly into Android Studio."
    exit 0
fi

echo "Building release APK for arm64-v8a..."
./gradlew assembleRelease -x lint --stacktrace

echo "=================================================="
echo "Build complete! APKs generated at:"
echo "android/app/build/outputs/apk/release/"
echo "=================================================="
