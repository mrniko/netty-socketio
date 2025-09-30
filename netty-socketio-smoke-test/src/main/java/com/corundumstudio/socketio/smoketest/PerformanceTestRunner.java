/**
 * Copyright (c) 2012-2025 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio.smoketest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Comparator;

/**
 * Performance test runner that executes smoke tests and records results.
 */
public class PerformanceTestRunner {
    
    private static final Logger log = LoggerFactory.getLogger(PerformanceTestRunner.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final SystemInfo systemInfo;
    private final int port;
    private final int clientCount;
    private final int eachMsgCount;
    private final int eachMsgSize;
    private final String javaVersion;
    private final String jvmArgs;
    private final String gitBranch;
    private final String version;
    
    public PerformanceTestRunner(int port, int clientCount, int eachMsgCount, int eachMsgSize) {
        this.systemInfo = new SystemInfo();
        this.port = port;
        this.clientCount = clientCount;
        this.eachMsgCount = eachMsgCount;
        this.eachMsgSize = eachMsgSize;
        this.javaVersion = System.getProperty("java.version");
        this.jvmArgs = String.join(" ", ManagementFactory.getRuntimeMXBean().getInputArguments());
        this.gitBranch = getGitBranch();
        this.version = getVersion();
    }
    
    public void runTest() throws Exception {
        log.info("Starting performance test...");
        log.info("Java Version: {}", javaVersion);
        log.info("JVM Args: {}", jvmArgs);

        ClientMetrics clientMetrics = new ClientMetrics();

        // Start server
        ServerMain server = new ServerMain(clientMetrics);
        server.start(port);
        
        try {
            // Run client test, preheating
            ClientMain client = new ClientMain(port, clientCount, eachMsgCount, eachMsgSize, clientMetrics);
            client.start();

            // Wait a bit before actual measurement, for JIT optimizations
            Thread.sleep(5000);

            // Run client test, actual measurement
            client = new ClientMain(port, clientCount, eachMsgCount, eachMsgSize, clientMetrics);
            client.start();
            
            // Collect metrics
            ClientMetrics metrics = client.getMetrics();
            PerformanceResult result = collectPerformanceResult(metrics);
            
            // Save results
            saveResults(result);
            
        } finally {
            server.stop();
        }
    }
    
    private PerformanceResult collectPerformanceResult(ClientMetrics metrics) {
        PerformanceResult result = new PerformanceResult();
        
        // Test metadata
        result.timestamp = LocalDateTime.now().format(formatter);
        result.javaVersion = javaVersion;
        result.jvmArgs = jvmArgs;
        result.gitBranch = gitBranch;
        result.version = version;
        result.operatingSystem = System.getProperty("os.name") + " " + System.getProperty("os.version");
        result.architecture = System.getProperty("os.arch");
        result.cpuCount = systemInfo.getAvailableProcessors();
        result.totalMemory = systemInfo.getTotalPhysicalMemory();
        result.freeMemory = systemInfo.getFreePhysicalMemory();
        
        // Test configuration
        result.port = port;
        result.clientCount = clientCount;
        result.eachMsgCount = eachMsgCount;
        result.eachMsgSize = eachMsgSize;
        
        // Performance metrics
        result.messagesSent = metrics.getTotalMessagesSent();
        result.messagesReceived = metrics.getTotalMessagesReceived();
        result.bytesSent = metrics.getTotalBytesSent();
        result.bytesReceived = metrics.getTotalBytesReceived();
        result.errors = metrics.getTotalErrors();
        result.minLatency = metrics.getMinLatency();
        result.maxLatency = metrics.getMaxLatency();
        result.avgLatency = metrics.getAverageLatency();
        result.p50Latency = metrics.getLatencyP50();
        result.p90Latency = metrics.getLatencyP90();
        result.p99Latency = metrics.getLatencyP99();
        result.testDuration = metrics.getTestDuration();
        result.messagesPerSecond = metrics.getMessagesPerSecond();
        result.bytesPerSecond = metrics.getBytesPerSecond();
        result.errorRate = metrics.getErrorRate();
        result.messageLossRate = metrics.getMessageLossRate();
        
        // Memory usage
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        result.heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        result.heapMax = memoryBean.getHeapMemoryUsage().getMax();
        result.heapCommitted = memoryBean.getHeapMemoryUsage().getCommitted();
        
        return result;
    }
    
    private void saveResults(PerformanceResult result) throws IOException {
        // Create results directory if it doesn't exist
        File resultsDir = new File("performance-results");
        if (!resultsDir.exists()) {
            resultsDir.mkdirs();
        }
        
        // Save JSON result
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String filename = String.format("performance-result-%s-%s.json", javaVersion.replace(".", "_"), timestamp);
        File jsonFile = new File(resultsDir, filename);
        
        ObjectNode jsonResult = mapper.createObjectNode();
        jsonResult.put("timestamp", result.timestamp);
        jsonResult.put("javaVersion", result.javaVersion);
        jsonResult.put("jvmArgs", result.jvmArgs);
        jsonResult.put("gitBranch", result.gitBranch);
        jsonResult.put("version", result.version);
        jsonResult.put("operatingSystem", result.operatingSystem);
        jsonResult.put("architecture", result.architecture);
        jsonResult.put("cpuCount", result.cpuCount);
        jsonResult.put("totalMemory", result.totalMemory);
        jsonResult.put("freeMemory", result.freeMemory);
        jsonResult.put("port", result.port);
        jsonResult.put("clientCount", result.clientCount);
        jsonResult.put("eachMsgCount", result.eachMsgCount);
        jsonResult.put("eachMsgSize", result.eachMsgSize);
        jsonResult.put("messagesSent", result.messagesSent);
        jsonResult.put("messagesReceived", result.messagesReceived);
        jsonResult.put("bytesSent", result.bytesSent);
        jsonResult.put("bytesReceived", result.bytesReceived);
        jsonResult.put("errors", result.errors);
        jsonResult.put("minLatency", result.minLatency);
        jsonResult.put("maxLatency", result.maxLatency);
        jsonResult.put("avgLatency", result.avgLatency);
        jsonResult.put("p50Latency", result.p50Latency);
        jsonResult.put("p90Latency", result.p90Latency);
        jsonResult.put("p99Latency", result.p99Latency);
        jsonResult.put("testDuration", result.testDuration);
        jsonResult.put("messagesPerSecond", result.messagesPerSecond);
        jsonResult.put("bytesPerSecond", result.bytesPerSecond);
        jsonResult.put("errorRate", result.errorRate);
        jsonResult.put("messageLossRate", result.messageLossRate);
        jsonResult.put("heapUsed", result.heapUsed);
        jsonResult.put("heapMax", result.heapMax);
        jsonResult.put("heapCommitted", result.heapCommitted);
        
        mapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, jsonResult);
        log.info("Results saved to: {}", jsonFile.getAbsolutePath());
        
        // Regenerate markdown report from all JSON files
        regenerateMarkdownReportFromJson();
    }
    
    private void updateMarkdownReport(PerformanceResult result) throws IOException {
        File reportFile = new File("PERFORMANCE_REPORT.md");
        StringBuilder report = new StringBuilder();
        
        if (reportFile.exists()) {
            // Read existing content
            String existingContent = new String(java.nio.file.Files.readAllBytes(reportFile.toPath()));
            
            // Check if this is the first result (no table rows yet)
            if (existingContent.contains("| 2025-") && !existingContent.contains("| " + result.timestamp)) {
                // Insert new row before the closing of Historical Results section
                String[] lines = existingContent.split("\n");
                boolean inHistoricalSection = false;
                boolean tableFound = false;
                
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    
                    if (line.contains("## Historical Results")) {
                        inHistoricalSection = true;
                        report.append(line).append("\n\n");
                        continue;
                    }
                    
                    if (inHistoricalSection && line.startsWith("| Date |")) {
                        tableFound = true;
                        report.append(line).append("\n");
                        continue;
                    }
                    
                    if (inHistoricalSection && line.startsWith("|------")) {
                        report.append(line).append("\n");
                        continue;
                    }
                    
                    if (inHistoricalSection && tableFound && line.startsWith("| 2025-")) {
                        // Add new row before existing rows
                        String newRow = String.format("| %s | %s | %s | %d | %,.2f | %.2f | %d | %.4f | %d | %s | %d |",
                                result.timestamp,
                                result.javaVersion,
                                result.operatingSystem,
                                result.cpuCount,
                                result.messagesPerSecond,
                                result.avgLatency,
                                result.p99Latency,
                                result.errorRate * 100,
                                result.heapMax / (1024 * 1024),
                                result.jvmArgs,
                                result.testDuration);
                        report.append(newRow).append("\n");
                        report.append(line).append("\n");
                        continue;
                    }
                    
                    if (inHistoricalSection && tableFound && !line.startsWith("|") && !line.trim().isEmpty()) {
                        // End of table, add new row before this line
                        String newRow = String.format("| %s | %s | %s | %d | %,.2f | %.2f | %d | %.4f | %d | %s | %d |",
                                result.timestamp,
                                result.javaVersion,
                                result.operatingSystem,
                                result.cpuCount,
                                result.messagesPerSecond,
                                result.avgLatency,
                                result.p99Latency,
                                result.errorRate * 100,
                                result.heapMax / (1024 * 1024),
                                result.jvmArgs,
                                result.testDuration);
                        report.append(newRow).append("\n\n");
                        report.append(line).append("\n");
                        continue;
                    }
                    
                    report.append(line).append("\n");
                }
            } else {
                // First result or no table structure yet, create new report
                report.append(existingContent);
                
                // Replace the placeholder with actual table
                if (report.toString().contains("*This section will be populated with daily test results*")) {
                    String newRow = String.format("| %s | %s | %s | %d | %,.2f | %.2f | %d | %.4f | %d | %s | %d |",
                            result.timestamp,
                            result.javaVersion,
                            result.operatingSystem,
                            result.cpuCount,
                            result.messagesPerSecond,
                            result.avgLatency,
                            result.p99Latency,
                            result.errorRate * 100,
                            result.heapMax / (1024 * 1024),
                            result.jvmArgs,
                            result.testDuration);
                    
                    report = new StringBuilder(report.toString().replace(
                            "*This section will be populated with daily test results*",
                            newRow));
                }
            }
        } else {
            // Create new report
            report.append("# Netty SocketIO Performance Test Report\n\n");
            report.append("This report contains daily performance test results for Netty SocketIO.\n\n");
            report.append("## Test Configuration\n");
            report.append("- Server Port: ").append(port).append("\n");
            report.append("- Client Count: ").append(clientCount).append("\n");
            report.append("- Messages per Client: ").append(eachMsgCount).append("\n");
            report.append("- Message Size: ").append(eachMsgSize).append(" bytes\n");
            report.append("- Server Max Memory: 256 MB\n\n");
            report.append("## Test Results\n\n");
            report.append("*Results will be automatically updated daily by GitHub Actions*\n\n");
            report.append("---\n\n");
            report.append("## Historical Results\n\n");
            report.append("| Date | Java Version | OS | CPU Cores | Messages/sec | Avg Latency (ms) | P99 Latency (ms) | Error Rate (%) | Max Heap (MB) | JVM Args | Test Duration (ms) |\n");
            report.append("|------|-------------|----|-----------|--------------|------------------|------------------|----------------|---------------|-----------|-------------------|\n");
            
            String newRow = String.format("| %s | %s | %s | %d | %,.2f | %.2f | %d | %.4f | %d | %s | %d |",
                    result.timestamp,
                    result.javaVersion,
                    result.operatingSystem,
                    result.cpuCount,
                    result.messagesPerSecond,
                    result.avgLatency,
                    result.p99Latency,
                    result.errorRate * 100,
                    result.heapMax / (1024 * 1024),
                    result.jvmArgs,
                    result.testDuration);
            report.append(newRow).append("\n");
        }
        
        // Write updated report
        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write(report.toString());
        }
        
        log.info("Performance report updated: {}", reportFile.getAbsolutePath());
    }
    
    private String getGitBranch() {
        try {
            Process process = Runtime.getRuntime().exec("git rev-parse --abbrev-ref HEAD");
            process.waitFor();
            if (process.exitValue() == 0) {
                java.util.Scanner scanner = new java.util.Scanner(process.getInputStream()).useDelimiter("\\A");
                return scanner.hasNext() ? scanner.next().trim() : "unknown";
            }
        } catch (Exception e) {
            log.debug("Failed to get git branch", e);
        }
        return "unknown";
    }
    
    private String getVersion() {
        try {
            // Try to get version from pom.xml
            Process process = Runtime.getRuntime().exec("mvn help:evaluate -Dexpression=project.version -q -DforceStdout");
            process.waitFor();
            if (process.exitValue() == 0) {
                java.util.Scanner scanner = new java.util.Scanner(process.getInputStream()).useDelimiter("\\A");
                String version = scanner.hasNext() ? scanner.next().trim() : "unknown";
                if (!version.isEmpty() && !version.contains("null")) {
                    return version;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get version from maven", e);
        }
        
        // Fallback to system property
        String version = System.getProperty("project.version");
        if (version != null && !version.isEmpty()) {
            return version;
        }
        
        return "unknown";
    }
    
    private void regenerateMarkdownReportFromJson() throws IOException {
        File resultsDir = new File("performance-results");
        if (!resultsDir.exists()) {
            log.warn("Performance results directory does not exist");
            return;
        }
        
        // Read all JSON files
        List<PerformanceResult> results = new ArrayList<>();
        File[] jsonFiles = resultsDir.listFiles((dir, name) -> name.endsWith(".json"));
        
        if (jsonFiles == null || jsonFiles.length == 0) {
            log.warn("No JSON result files found");
            return;
        }
        
        for (File jsonFile : jsonFiles) {
            try {
                PerformanceResult result = mapper.readValue(jsonFile, PerformanceResult.class);
                results.add(result);
            } catch (Exception e) {
                log.warn("Failed to read JSON file: {}", jsonFile.getName(), e);
            }
        }
        
        // Sort by timestamp (newest first)
        results.sort(Comparator.comparing((PerformanceResult r) -> r.timestamp).reversed());
        
        // Generate markdown report
        generateMarkdownReport(results);
    }
    
    private void generateMarkdownReport(List<PerformanceResult> results) throws IOException {
        File reportFile = new File("PERFORMANCE_REPORT.md");
        
        StringBuilder report = new StringBuilder();
        report.append("# Netty SocketIO Performance Test Report\n\n");
        report.append("This report contains daily performance test results for Netty SocketIO.\n\n");
        report.append("## Test Configuration\n");
        report.append("- Server Port: ").append(port).append("\n");
        report.append("- Client Count: ").append(clientCount).append("\n");
        report.append("- Messages per Client: ").append(eachMsgCount).append("\n");
        report.append("- Message Size: ").append(eachMsgSize).append(" bytes\n");
        report.append("- Server Max Memory: 256 MB\n\n");
        report.append("## Test Results\n\n");
        report.append("*Results will be automatically updated daily by GitHub Actions*\n\n");
        report.append("---\n\n");
        report.append("## Historical Results\n\n");
        
        // Table header
        report.append("| Date | Java Version | OS | CPU Cores | Messages/sec | Avg Latency (ms) | P99 Latency (ms) | Error Rate (%) | Max Heap (MB) | JVM Args | Git Branch | Version | Test Duration (ms) |\n");
        report.append("|------|-------------|----|-----------|--------------|------------------|------------------|----------------|---------------|-----------|------------|---------|-------------------|\n");
        
        // Add data rows
        for (PerformanceResult result : results) {
            String row = String.format("| %s | %s | %s | %d | %,.2f | %.2f | %d | %.4f | %d | %s | %s | %s | %d |",
                    result.timestamp,
                    result.javaVersion,
                    result.operatingSystem,
                    result.cpuCount,
                    result.messagesPerSecond,
                    result.avgLatency,
                    result.p99Latency,
                    result.errorRate * 100,
                    result.heapMax / (1024 * 1024),
                    result.jvmArgs,
                    result.gitBranch,
                    result.version,
                    result.testDuration);
            report.append(row).append("\n");
        }
        
        // Write report
        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write(report.toString());
        }
        
        log.info("Markdown report regenerated from {} JSON files: {}", results.size(), reportFile.getAbsolutePath());
    }
    
    public static void main(String[] args) {
        try {
            int port = 8899;
            int clientCount = 10;
            int eachMsgCount = 1000;
            int eachMsgSize = 128;
            
            if (args.length >= 4) {
                port = Integer.parseInt(args[0]);
                clientCount = Integer.parseInt(args[1]);
                eachMsgCount = Integer.parseInt(args[2]);
                eachMsgSize = Integer.parseInt(args[3]);
            }
            
            PerformanceTestRunner runner = new PerformanceTestRunner(port, clientCount, eachMsgCount, eachMsgSize);
            runner.runTest();
            
        } catch (Exception e) {
            log.error("Performance test failed", e);
            System.exit(1);
        }
    }
    
    public static class PerformanceResult {
        public String timestamp;
        public String javaVersion;
        public String jvmArgs;
        public String gitBranch;
        public String version;
        public String operatingSystem;
        public String architecture;
        public int cpuCount;
        public long totalMemory;
        public long freeMemory;
        public int port;
        public int clientCount;
        public int eachMsgCount;
        public int eachMsgSize;
        public long messagesSent;
        public long messagesReceived;
        public long bytesSent;
        public long bytesReceived;
        public long errors;
        public long minLatency;
        public long maxLatency;
        public double avgLatency;
        public long p50Latency;
        public long p90Latency;
        public long p99Latency;
        public long testDuration;
        public double messagesPerSecond;
        public double bytesPerSecond;
        public double errorRate;
        public double messageLossRate;
        public long heapUsed;
        public long heapMax;
        public long heapCommitted;
    }
}
