# Netty SocketIO Performance Test Report

This report contains daily performance test results for Netty SocketIO.

## Test Configuration
- Server Port: 8899
- Client Count: 10
- Messages per Client: 20000
- Message Size: 128 bytes
- Server Max Memory: 256 MB

## Test Results

*Results will be automatically updated daily by GitHub Actions*

---

## Historical Results

| Date | Java Version | OS | CPU Cores | Messages/sec | Avg Latency (ms) | P99 Latency (ms) | Error Rate (%) | Max Heap (MB) | JVM Args | Git Branch | Version | Test Duration (ms) |
|------|-------------|----|-----------|--------------|------------------|------------------|----------------|---------------|-----------|------------|---------|-------------------|
| 2025-10-14 11:43:04 | 17.0.16 | Linux 6.11.0-1018-azure | 4 | 95,831.34 | 1095.46 | 1479 | 0.0000 | 256 | -Xms256m -Xmx256m -XX:+UseZGC -XX:+AlwaysPreTouch | master | 2.0.14-SNAPSHOT | 2087 |
| 2025-10-14 11:30:19 | 17.0.16 | Linux 6.11.0-1018-azure | 4 | 93,852.65 | 1077.50 | 1583 | 0.0000 | 256 | -Xms256m -Xmx256m -XX:+UseZGC -XX:+AlwaysPreTouch | master | 2.0.14-SNAPSHOT | 2131 |
