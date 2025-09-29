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
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
echo "Using Java version: $JAVA_VERSION"

# Build the project
echo "Building smoke test module..."
cd netty-socketio-smoke-test
mvn clean package -DskipTests

# Run performance test
echo "Running performance test..."
java -Xmx256m -XX:+UseG1GC -XX:+AlwaysPreTouch \
     -cp target/netty-socketio-smoke-test.jar:target/dependency/* \
     com.corundumstudio.socketio.smoketest.PerformanceTestRunner \
     8899 10 1000 128

echo "Performance test completed!"
echo "Results saved in: performance-results/"
echo "Report updated: PERFORMANCE_REPORT.md"
