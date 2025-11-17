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

import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.corundumstudio.socketio.protocol.PacketDecoder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

import static com.corundumstudio.socketio.protocol.PacketDecoderTest.preprocessJsonOld;

/**
 * JMH benchmark comparing the performance of preprocessJson methods
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class PreprocessJsonBenchmark {

    private PacketDecoder decoder;
    private ByteBuf[] testBuffers;
    private int bufferIndex = 0;

    @Setup
    public void setup() {
        // Initialize PacketDecoder with mock dependencies
        decoder = new PacketDecoder(null, null);
        
        // Create various test cases
        String[] testCases = {
            // Simple case
            "d=2[\"hello\"]",
            
            // With escaped newlines
            "d=2[\"hello\\\\nworld\"]",
            
            // With URL encoding
            "d=2[\"hello%20world\"]",
            "d=2[\"hello+world\"]",
            
            // Complex case with multiple encodings
            "d=2[\"hello%20world%21test%22\"]",
            "d=2[\"hello+world+test+\"]",
            "d=2[\"hello%20\\\\nworld%21\"]",
            "d=2[\"hello+\\\\nworld+test\"]",
            
            // Unicode characters
            "d=2[\"hello%E4%B8%ADworld\"]",
            "d=2[\"hello%E6%96%87world\"]",
            
            // Special characters
            "d=2[\"hello%21%22%23%24%25world\"]",
            "d=2[\"hello%26%27%28%29%2Aworld\"]",
            
            // Long string with mixed encodings
            "d=2[\"hello%20world%21test%22data%23with%24various%25encodings%26and%27special%28chars%29\"]",
        };
        
        testBuffers = new ByteBuf[testCases.length];
        for (int i = 0; i < testCases.length; i++) {
            testBuffers[i] = Unpooled.copiedBuffer(testCases[i], CharsetUtil.UTF_8);
        }
    }

    @TearDown
    public void tearDown() {
        // Release all test buffers
        for (ByteBuf buffer : testBuffers) {
            buffer.release();
        }
    }

    @Benchmark
    public ByteBuf benchmarkNewMethod() throws UnsupportedEncodingException {
        ByteBuf buffer = testBuffers[bufferIndex % testBuffers.length];
        ByteBuf result = decoder.preprocessJson(1, buffer);
        bufferIndex++;
        return result;
    }

    @Benchmark
    public ByteBuf benchmarkOldMethod() throws UnsupportedEncodingException {
        ByteBuf buffer = testBuffers[bufferIndex % testBuffers.length];
        ByteBuf result = preprocessJsonOld(1, buffer);
        bufferIndex++;
        return result;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(PreprocessJsonBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
