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
| 2025-10-13 01:34:56 | 21.0.8 | Linux 6.11.0-1018-azure | 4 | 101,112.23 | 1011.63 | 1463 | 0.0000 | 256 | -Xms256m -Xmx256m -XX:+UseZGC -XX:+AlwaysPreTouch | master | 2.0.14-SNAPSHOT | 1978 |
| 2025-10-12 09:12:37 | 21.0.8 | Linux 6.11.0-1018-azure | 4 | 99,651.22 | 1159.05 | 1663 | 0.0000 | 256 | -Xms256m -Xmx256m -XX:+UseZGC -XX:+AlwaysPreTouch | master | 2.0.14-SNAPSHOT | 2007 |
| 2025-10-12 09:10:22 | 17.0.16 | Linux 6.11.0-1018-azure | 4 | 90,252.71 | 1142.77 | 1615 | 0.0000 | 256 | -Xms256m -Xmx256m -XX:+UseZGC -XX:+AlwaysPreTouch | master | 2.0.14-SNAPSHOT | 2216 |
