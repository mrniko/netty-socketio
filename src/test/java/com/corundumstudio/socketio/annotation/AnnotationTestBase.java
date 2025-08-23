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
package com.corundumstudio.socketio.annotation;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.namespace.Namespace;
import com.github.javafaker.Faker;

public abstract class AnnotationTestBase {

    private static final Faker FAKER = new Faker();

    protected Configuration newConfiguration() {
        return new Configuration();
    }

    protected Namespace newNamespace(Configuration configuration) {
        return new Namespace(FAKER.name().name(), configuration);
    }
}
