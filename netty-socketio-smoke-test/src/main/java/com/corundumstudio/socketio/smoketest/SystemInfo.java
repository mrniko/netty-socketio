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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Map;

/**
 * System information collector for performance testing environment.
 */
public class SystemInfo {
    
    private static final Logger log = LoggerFactory.getLogger(SystemInfo.class);
    
    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;
    private final RuntimeMXBean runtimeBean;
    
    public SystemInfo() {
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.runtimeBean = ManagementFactory.getRuntimeMXBean();
    }
    
    public void printSystemInfo() {
        log.info("=== System Environment Information ===");
        printOperatingSystemInfo();
        printJvmInfo();
        printMemoryInfo();
        printJvmArguments();
        log.info("=====================================");
    }
    
    private void printOperatingSystemInfo() {
        log.info("Operating System:");
        log.info("  Name: {}", osBean.getName());
        log.info("  Version: {}", osBean.getVersion());
        log.info("  Architecture: {}", osBean.getArch());
        log.info("  Available Processors: {}", osBean.getAvailableProcessors());
        
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
            log.info("  Total Physical Memory: {} MB", sunOsBean.getTotalPhysicalMemorySize() / (1024 * 1024));
            log.info("  Free Physical Memory: {} MB", sunOsBean.getFreePhysicalMemorySize() / (1024 * 1024));
            log.info("  Committed Virtual Memory: {} MB", sunOsBean.getCommittedVirtualMemorySize() / (1024 * 1024));
        }
    }
    
    private void printJvmInfo() {
        log.info("Java Virtual Machine:");
        log.info("  Name: {}", runtimeBean.getVmName());
        log.info("  Version: {}", runtimeBean.getVmVersion());
        log.info("  Vendor: {}", runtimeBean.getVmVendor());
        log.info("  Uptime: {} ms", runtimeBean.getUptime());
        log.info("  Input Arguments Count: {}", runtimeBean.getInputArguments().size());
    }
    
    private void printMemoryInfo() {
        log.info("Memory Information:");
        
        // Heap Memory
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        long heapCommitted = memoryBean.getHeapMemoryUsage().getCommitted();
        
        log.info("  Heap Memory:");
        log.info("    Used: {} MB", heapUsed / (1024 * 1024));
        log.info("    Committed: {} MB", heapCommitted / (1024 * 1024));
        log.info("    Max: {} MB", heapMax / (1024 * 1024));
        log.info("    Usage: {:.2f}%", (double) heapUsed / heapMax * 100);
        
        // Non-Heap Memory
        long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
        long nonHeapMax = memoryBean.getNonHeapMemoryUsage().getMax();
        long nonHeapCommitted = memoryBean.getNonHeapMemoryUsage().getCommitted();
        
        log.info("  Non-Heap Memory:");
        log.info("    Used: {} MB", nonHeapUsed / (1024 * 1024));
        log.info("    Committed: {} MB", nonHeapCommitted / (1024 * 1024));
        log.info("    Max: {} MB", nonHeapMax == -1 ? "Unlimited" : String.valueOf(nonHeapMax / (1024 * 1024)));
    }
    
    private void printJvmArguments() {
        List<String> inputArgs = runtimeBean.getInputArguments();
        if (!inputArgs.isEmpty()) {
            log.info("JVM Arguments:");
            for (String arg : inputArgs) {
                log.info("  {}", arg);
            }
        }
        
        // System Properties
        log.info("Key System Properties:");
        java.util.Properties sysProps = System.getProperties();
        String[] keyProps = {
            "java.version", "java.vendor", "java.home",
            "os.name", "os.version", "os.arch",
            "user.name", "user.home", "user.dir",
            "file.encoding", "java.io.tmpdir"
        };
        
        for (String key : keyProps) {
            String value = sysProps.getProperty(key);
            if (value != null) {
                log.info("  {}: {}", key, value);
            }
        }
    }
    
    public int getAvailableProcessors() {
        return osBean.getAvailableProcessors();
    }
    
    public long getTotalPhysicalMemory() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getTotalPhysicalMemorySize();
        }
        return -1;
    }
    
    public long getFreePhysicalMemory() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getFreePhysicalMemorySize();
        }
        return -1;
    }
    
    public long getHeapUsed() {
        return memoryBean.getHeapMemoryUsage().getUsed();
    }
    
    public long getHeapMax() {
        return memoryBean.getHeapMemoryUsage().getMax();
    }
    
    public String getJvmName() {
        return runtimeBean.getVmName();
    }
    
    public String getJvmVersion() {
        return runtimeBean.getVmVersion();
    }
}
