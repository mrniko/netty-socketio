/**
 * Copyright (c) 2012-2025 Nikita Koksharov
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio.smoketest;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import org.HdrHistogram.Histogram;

/**
 * Client-specific metrics collection for performance testing.
 */
public class ClientMetrics {

    private final LongAdder totalMessagesSent = new LongAdder();
    private final LongAdder totalMessagesReceived = new LongAdder();
    private final LongAdder totalBytesSent = new LongAdder();
    private final LongAdder totalBytesReceived = new LongAdder();
    private final LongAdder totalErrors = new LongAdder();

    // Histogram for latency percentiles
    private final Histogram latencyHistogram = new Histogram(1, 60000, 2); // 1ms to 60s, 2 significant digits

    private volatile long startTime = 0;
    private volatile long endTime = 0;

    public void start() {
        startTime = System.currentTimeMillis();
    }

    public void stop() {
        endTime = System.currentTimeMillis();
    }

    public void recordMessageSent(int bytes) {
        totalMessagesSent.increment();
        totalBytesSent.add(bytes);
    }

    public void recordMessageReceived(int bytes) {
        totalMessagesReceived.increment();
        totalBytesReceived.add(bytes);
    }

    public void recordError() {
        totalErrors.increment();
    }

    public void recordLatency(long latencyMs) {
        // Record in histogram
        latencyHistogram.recordValue(latencyMs);
    }

    // Getters
    public long getTotalMessagesSent() {
        return totalMessagesSent.sum();
    }

    public long getTotalMessagesReceived() {
        return totalMessagesReceived.sum();
    }

    public long getTotalBytesSent() {
        return totalBytesSent.sum();
    }

    public long getTotalBytesReceived() {
        return totalBytesReceived.sum();
    }

    public long getTotalErrors() {
        return totalErrors.sum();
    }

    public long getMinLatency() {
        return latencyHistogram.getMinValue();
    }

    public long getMaxLatency() {
        return latencyHistogram.getMaxValue();
    }

    public double getAverageLatency() {
        return latencyHistogram.getMean();
    }

    public long getLatencyPercentile(double percentile) {
        if (latencyHistogram.getTotalCount() == 0) {
            return 0;
        }
        return latencyHistogram.getValueAtPercentile(percentile);
    }

    public long getLatencyP50() {
        return getLatencyPercentile(50.0);
    }

    public long getLatencyP90() {
        return getLatencyPercentile(90.0);
    }

    public long getLatencyP99() {
        return getLatencyPercentile(99.0);
    }

    public long getTestDuration() {
        if (endTime == 0) {
            return System.currentTimeMillis() - startTime;
        }
        return endTime - startTime;
    }

    public double getMessagesPerSecond() {
        long duration = getTestDuration();
        return duration == 0 ? 0.0 : (double) getTotalMessagesSent() * 1000 / duration;
    }

    public double getBytesPerSecond() {
        long duration = getTestDuration();
        return duration == 0 ? 0.0 : (double) getTotalBytesSent() * 1000 / duration;
    }

    public double getErrorRate() {
        long total = getTotalMessagesSent() + getTotalErrors();
        return total == 0 ? 0.0 : (double) getTotalErrors() / total;
    }

    public double getMessageLossRate() {
        long sent = getTotalMessagesSent();
        long received = getTotalMessagesReceived();
        return sent == 0 ? 0.0 : (double) (sent - received) / sent;
    }

    public void reset() {
        totalMessagesSent.reset();
        totalMessagesReceived.reset();
        totalBytesSent.reset();
        totalBytesReceived.reset();
        totalErrors.reset();
        latencyHistogram.reset();
        startTime = 0;
        endTime = 0;
    }

    @Override
    public String toString() {
        return String.format(
                "ClientMetrics{messagesSent=%d, messagesReceived=%d, bytesSent=%d, bytesReceived=%d, " +
                        "errors=%d, " +
                        "minLatency=%dms, maxLatency=%dms, avgLatency=%.2fms, " +
                        "p50Latency=%dms, p90Latency=%dms, p99Latency=%dms, " +
                        "duration=%dms, msgPerSec=%.2f, bytesPerSec=%.2f, errorRate=%.4f, lossRate=%.4f}",
                getTotalMessagesSent(), getTotalMessagesReceived(), getTotalBytesSent(), getTotalBytesReceived(),
                getTotalErrors(),
                getMinLatency(), getMaxLatency(), getAverageLatency(),
                getLatencyP50(), getLatencyP90(), getLatencyP99(),
                getTestDuration(), getMessagesPerSecond(), getBytesPerSecond(), getErrorRate(), getMessageLossRate()
        );
    }
}
