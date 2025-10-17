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
package com.corundumstudio.socketio.benchmark;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.corundumstudio.socketio.protocol.PacketEncoder;

/**
 * JMH Benchmark for longToBytes performance comparison
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class LongToBytesBenchmark {

    @Param({"0", "1", "123", "12345", "123456789", "1760666224123"})
    public long testValue;

    /**
     * Original implementation for performance comparison
     * This is the old version that had issues with zero values
     */
    public static byte[] longToBytesOriginal(long number) {
        // Handle zero case for original method to avoid exception
        if (number == 0) {
            return new byte[]{0};
        }
        
        // TODO optimize - this is the original implementation
        int length = (int) (Math.log10(number) + 1);
        byte[] res = new byte[length];
        int i = length;
        while (number > 0) {
            res[--i] = (byte) (number % 10);
            number = number / 10;
        }
        return res;
    }

    @Benchmark
    public byte[] optimizedMethod() {
        return PacketEncoder.longToBytes(testValue);
    }

    @Benchmark
    public byte[] originalMethod() {
        return longToBytesOriginal(testValue);
    }

    /**
     * Main method to run the benchmark
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(LongToBytesBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
