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
