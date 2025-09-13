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
package com.corundumstudio.socketio.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SchedulerKey Tests")
class SchedulerKeyTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create SchedulerKey with valid type and sessionId")
        void shouldCreateSchedulerKeyWithValidParameters() {
            // Given
            SchedulerKey.Type type = SchedulerKey.Type.PING;
            String sessionId = "test-session-123";

            // When
            SchedulerKey schedulerKey = new SchedulerKey(type, sessionId);

            // Then
            assertThat(schedulerKey).isNotNull();
            // Test equality instead of direct field access
            SchedulerKey expectedKey = new SchedulerKey(type, sessionId);
            assertThat(schedulerKey).isEqualTo(expectedKey);
        }

        @Test
        @DisplayName("Should create SchedulerKey with null sessionId")
        void shouldCreateSchedulerKeyWithNullSessionId() {
            // Given
            SchedulerKey.Type type = SchedulerKey.Type.ACK_TIMEOUT;

            // When
            SchedulerKey schedulerKey = new SchedulerKey(type, null);

            // Then
            assertThat(schedulerKey).isNotNull();
            // Test equality instead of direct field access
            SchedulerKey expectedKey = new SchedulerKey(type, null);
            assertThat(schedulerKey).isEqualTo(expectedKey);
        }

        @Test
        @DisplayName("Should create SchedulerKey with null type")
        void shouldCreateSchedulerKeyWithNullType() {
            // Given
            String sessionId = "test-session-456";

            // When
            SchedulerKey schedulerKey = new SchedulerKey(null, sessionId);

            // Then
            assertThat(schedulerKey).isNotNull();
            // Test equality instead of direct field access
            SchedulerKey expectedKey = new SchedulerKey(null, sessionId);
            assertThat(schedulerKey).isEqualTo(expectedKey);
        }

        @Test
        @DisplayName("Should create SchedulerKey with both null values")
        void shouldCreateSchedulerKeyWithBothNullValues() {
            // When
            SchedulerKey schedulerKey = new SchedulerKey(null, null);

            // Then
            assertThat(schedulerKey).isNotNull();
            // Test equality instead of direct field access
            SchedulerKey expectedKey = new SchedulerKey(null, null);
            assertThat(schedulerKey).isEqualTo(expectedKey);
        }
    }

    @Nested
    @DisplayName("Type Enum Tests")
    class TypeEnumTests {

        @Test
        @DisplayName("Should have all expected enum values")
        void shouldHaveAllExpectedEnumValues() {
            // When
            SchedulerKey.Type[] types = SchedulerKey.Type.values();

            // Then
            assertThat(types).hasSize(4);
            assertThat(types).contains(
                SchedulerKey.Type.PING,
                SchedulerKey.Type.PING_TIMEOUT,
                SchedulerKey.Type.ACK_TIMEOUT,
                SchedulerKey.Type.UPGRADE_TIMEOUT
            );
        }

        @ParameterizedTest
        @EnumSource(SchedulerKey.Type.class)
        @DisplayName("Should create SchedulerKey with each enum type")
        void shouldCreateSchedulerKeyWithEachEnumType(SchedulerKey.Type type) {
            // Given
            String sessionId = "test-session";

            // When
            SchedulerKey schedulerKey = new SchedulerKey(type, sessionId);

            // Then
            // Test equality instead of direct field access
            SchedulerKey expectedKey = new SchedulerKey(type, sessionId);
            assertThat(schedulerKey).isEqualTo(expectedKey);
        }
    }

    @Nested
    @DisplayName("Equals Tests")
    class EqualsTests {

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            // Given
            SchedulerKey schedulerKey = new SchedulerKey(SchedulerKey.Type.PING, "session-1");

            // When & Then
            assertThat(schedulerKey).isEqualTo(schedulerKey);
        }

        @Test
        @DisplayName("Should be equal to another SchedulerKey with same values")
        void shouldBeEqualToAnotherSchedulerKeyWithSameValues() {
            // Given
            SchedulerKey key1 = new SchedulerKey(SchedulerKey.Type.PING, "session-1");
            SchedulerKey key2 = new SchedulerKey(SchedulerKey.Type.PING, "session-1");

            // When & Then
            assertThat(key1).isEqualTo(key2);
            assertThat(key2).isEqualTo(key1);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            // Given
            SchedulerKey schedulerKey = new SchedulerKey(SchedulerKey.Type.PING, "session-1");

            // When & Then
            assertThat(schedulerKey).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should not be equal to different class")
        void shouldNotBeEqualToDifferentClass() {
            // Given
            SchedulerKey schedulerKey = new SchedulerKey(SchedulerKey.Type.PING, "session-1");
            Object differentObject = "different";

            // When & Then
            assertThat(schedulerKey).isNotEqualTo(differentObject);
        }

        @Test
        @DisplayName("Should not be equal when types are different")
        void shouldNotBeEqualToWhenTypesAreDifferent() {
            // Given
            SchedulerKey key1 = new SchedulerKey(SchedulerKey.Type.PING, "session-1");
            SchedulerKey key2 = new SchedulerKey(SchedulerKey.Type.PING_TIMEOUT, "session-1");

            // When & Then
            assertThat(key1).isNotEqualTo(key2);
            assertThat(key2).isNotEqualTo(key1);
        }

        @Test
        @DisplayName("Should not be equal when sessionIds are different")
        void shouldNotBeEqualToWhenSessionIdsAreDifferent() {
            // Given
            SchedulerKey key1 = new SchedulerKey(SchedulerKey.Type.PING, "session-1");
            SchedulerKey key2 = new SchedulerKey(SchedulerKey.Type.PING, "session-2");

            // When & Then
            assertThat(key1).isNotEqualTo(key2);
            assertThat(key2).isNotEqualTo(key1);
        }

        @Test
        @DisplayName("Should be equal when both values are null")
        void shouldBeEqualToWhenBothValuesAreNull() {
            // Given
            SchedulerKey key1 = new SchedulerKey(null, null);
            SchedulerKey key2 = new SchedulerKey(null, null);

            // When & Then
            assertThat(key1).isEqualTo(key2);
            assertThat(key2).isEqualTo(key1);
        }

        @Test
        @DisplayName("Should be equal when type is null but sessionId is same")
        void shouldBeEqualToWhenTypeIsNullButSessionIdIsSame() {
            // Given
            SchedulerKey key1 = new SchedulerKey(null, "session-1");
            SchedulerKey key2 = new SchedulerKey(null, "session-1");

            // When & Then
            assertThat(key1).isEqualTo(key2);
            assertThat(key2).isEqualTo(key1);
        }

        @Test
        @DisplayName("Should be equal when sessionId is null but type is same")
        void shouldBeEqualToWhenSessionIdIsNullButTypeIsSame() {
            // Given
            SchedulerKey key1 = new SchedulerKey(SchedulerKey.Type.PING, null);
            SchedulerKey key2 = new SchedulerKey(SchedulerKey.Type.PING, null);

            // When & Then
            assertThat(key1).isEqualTo(key2);
            assertThat(key2).isEqualTo(key1);
        }

        @Test
        @DisplayName("Should not be equal when one type is null and other is not")
        void shouldNotBeEqualToWhenOneTypeIsNullAndOtherIsNot() {
            // Given
            SchedulerKey key1 = new SchedulerKey(null, "session-1");
            SchedulerKey key2 = new SchedulerKey(SchedulerKey.Type.PING, "session-1");

            // When & Then
            assertThat(key1).isNotEqualTo(key2);
            assertThat(key2).isNotEqualTo(key1);
        }

        @Test
        @DisplayName("Should not be equal when one sessionId is null and other is not")
        void shouldNotBeEqualToWhenOneSessionIdIsNullAndOtherIsNot() {
            // Given
            SchedulerKey key1 = new SchedulerKey(SchedulerKey.Type.PING, null);
            SchedulerKey key2 = new SchedulerKey(SchedulerKey.Type.PING, "session-1");

            // When & Then
            assertThat(key1).isNotEqualTo(key2);
            assertThat(key2).isNotEqualTo(key1);
        }
    }

    @Nested
    @DisplayName("HashCode Tests")
    class HashCodeTests {

        @Test
        @DisplayName("Should have same hash code for equal objects")
        void shouldHaveSameHashCodeForEqualObjects() {
            // Given
            SchedulerKey key1 = new SchedulerKey(SchedulerKey.Type.PING, "session-1");
            SchedulerKey key2 = new SchedulerKey(SchedulerKey.Type.PING, "session-1");

            // When & Then
            assertThat(key1).hasSameHashCodeAs(key2);
        }

        @Test
        @DisplayName("Should have same hash code when called multiple times")
        void shouldHaveSameHashCodeWhenCalledMultipleTimes() {
            // Given
            SchedulerKey schedulerKey = new SchedulerKey(SchedulerKey.Type.PING, "session-1");

            // When
            int hashCode1 = schedulerKey.hashCode();
            int hashCode2 = schedulerKey.hashCode();
            int hashCode3 = schedulerKey.hashCode();

            // Then
            assertThat(hashCode1).isEqualTo(hashCode2);
            assertThat(hashCode2).isEqualTo(hashCode3);
            assertThat(hashCode1).isEqualTo(hashCode3);
        }

        @Test
        @DisplayName("Should handle null type in hash code")
        void shouldHandleNullTypeInHashCode() {
            // Given
            SchedulerKey schedulerKey = new SchedulerKey(null, "session-1");

            // When
            int hashCode = schedulerKey.hashCode();

            // Then
            assertThat(hashCode).isNotEqualTo(0);
        }

        @Test
        @DisplayName("Should handle null sessionId in hash code")
        void shouldHandleNullSessionIdInHashCode() {
            // Given
            SchedulerKey schedulerKey = new SchedulerKey(SchedulerKey.Type.PING, null);

            // When
            int hashCode = schedulerKey.hashCode();

            // Then
            assertThat(hashCode).isNotEqualTo(0);
        }

        @Test
        @DisplayName("Should handle both null values in hash code")
        void shouldHandleBothNullValuesInHashCode() {
            // Given
            SchedulerKey schedulerKey = new SchedulerKey(null, null);

            // When
            int hashCode = schedulerKey.hashCode();

            // Then
            assertThat(hashCode).isNotEqualTo(0);
            // The actual value depends on the hash calculation, but should be consistent
            assertThat(hashCode).isEqualTo(schedulerKey.hashCode());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle empty string sessionId")
        void shouldHandleEmptyStringSessionId() {
            // Given
            SchedulerKey key1 = new SchedulerKey(SchedulerKey.Type.PING, "");
            SchedulerKey key2 = new SchedulerKey(SchedulerKey.Type.PING, "");

            // When & Then
            assertThat(key1).isEqualTo(key2);
            assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
        }

        @Test
        @DisplayName("Should handle very long sessionId")
        void shouldHandleVeryLongSessionId() {
            // Given
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                sb.append("a");
            }
            String longSessionId = sb.toString();
            SchedulerKey key1 = new SchedulerKey(SchedulerKey.Type.PING, longSessionId);
            SchedulerKey key2 = new SchedulerKey(SchedulerKey.Type.PING, longSessionId);

            // When & Then
            assertThat(key1).isEqualTo(key2);
            assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
        }

        @Test
        @DisplayName("Should handle special characters in sessionId")
        void shouldHandleSpecialCharactersInSessionId() {
            // Given
            String specialSessionId = "!@#$%^&*()_+-=[]{}|;':\",./<>?";
            SchedulerKey key1 = new SchedulerKey(SchedulerKey.Type.PING, specialSessionId);
            SchedulerKey key2 = new SchedulerKey(SchedulerKey.Type.PING, specialSessionId);

            // When & Then
            assertThat(key1).isEqualTo(key2);
            assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
        }

        @Test
        @DisplayName("Should handle unicode characters in sessionId")
        void shouldHandleUnicodeCharactersInSessionId() {
            // Given
            String unicodeSessionId = "测试会话ID-123-🚀-🌟";
            SchedulerKey key1 = new SchedulerKey(SchedulerKey.Type.PING, unicodeSessionId);
            SchedulerKey key2 = new SchedulerKey(SchedulerKey.Type.PING, unicodeSessionId);

            // When & Then
            assertThat(key1).isEqualTo(key2);
            assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
        }
    }
}
