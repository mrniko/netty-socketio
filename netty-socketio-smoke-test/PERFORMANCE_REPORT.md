# Netty SocketIO Performance Test Report

This report contains daily performance test results for Netty SocketIO.

## Test Configuration
- Server Port: 8899
- Client Count: 10
- Messages per Client: 50000
- Message Size: 32 bytes
- Server Max Memory: 256 MB

## Test Results

*Results will be automatically updated daily by GitHub Actions*

---

## Historical Results

| Date | Java Version | OS | CPU Cores | Messages/sec | Avg Latency (ms) | P99 Latency (ms) | Error Rate (%) | Max Heap (MB) | JVM Args | Git Branch | Version | Test Duration (ms) |
|------|-------------|----|-----------|--------------|------------------|------------------|----------------|---------------|-----------|------------|---------|-------------------|
| 2025-10-14 12:18:27 | 11.0.28 | Linux 6.11.0-1018-azure | 4 | 194,704.05 | 1434.71 | 2079 | 0.0000 | 256 | -Xms256m -Xmx256m -XX:+UseG1GC -XX:+AlwaysPreTouch | master | 2.0.14-SNAPSHOT | 2568 |
