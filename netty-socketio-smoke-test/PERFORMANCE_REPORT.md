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
| 2025-10-15 03:16:58 | 25 | Linux 6.11.0-1018-azure | 4 | 221,729.49 | 1161.82 | 1759 | 0.0000 | 256 | -Xms256m -Xmx256m -XX:+UseG1GC -XX:+AlwaysPreTouch | master | 2.0.14-SNAPSHOT | 2255 |
| 2025-10-15 02:32:46 | 21.0.8 | Linux 6.11.0-1018-azure | 4 | 208,507.09 | 1344.59 | 1863 | 0.0000 | 256 | -Xms256m -Xmx256m -XX:+UseG1GC -XX:+AlwaysPreTouch | master | 2.0.14-SNAPSHOT | 2398 |
| 2025-10-15 01:34:51 | 17.0.16 | Linux 6.11.0-1018-azure | 4 | 196,155.36 | 1500.28 | 2127 | 0.0000 | 256 | -Xms256m -Xmx256m -XX:+UseG1GC -XX:+AlwaysPreTouch | master | 2.0.14-SNAPSHOT | 2549 |
| 2025-10-15 00:02:00 | 11.0.28 | Linux 6.11.0-1018-azure | 4 | 187,828.70 | 1479.06 | 2207 | 0.0000 | 256 | -Xms256m -Xmx256m -XX:+UseG1GC -XX:+AlwaysPreTouch | master | 2.0.14-SNAPSHOT | 2662 |
