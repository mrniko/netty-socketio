#!/bin/bash

# Netty SocketIO Performance Test Runner
# This script runs performance tests for different Java versions

set -e

echo "Starting Netty SocketIO Performance Test..."

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    exit 1
fi

# Get Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | sed 's/^1\.//' | cut -d'.' -f1)
echo "Using Java version: $JAVA_VERSION"

# Build the project
echo "Building whole module..."
mvn clean package -DskipTests
echo "Go to smoke test module..."
cd netty-socketio-smoke-test

# Determine GC flags based on Java version
if [ "$JAVA_VERSION" -ge 17 ]; then
  GC_OPTS="-XX:+UseZGC"
else
  GC_OPTS="-XX:+UseG1GC"
fi

# Run performance test
echo "Running performance test..."
java -Xms256m -Xmx256m $GC_OPTS -XX:+AlwaysPreTouch \
     -cp target/netty-socketio-smoke-test.jar:target/dependency/* \
     com.corundumstudio.socketio.smoketest.PerformanceTestRunner \
     8899 10 20000 128

echo "Performance test completed!"
echo "Results saved in: netty-socketio-smoke-test/performance-results/"
echo "Report updated: netty-socketio-smoke-test/PERFORMANCE_REPORT.md"

