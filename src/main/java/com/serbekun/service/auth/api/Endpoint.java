package com.serbekun.service.auth.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Endpoint value object
 */
public class Endpoint {

    private final String name;

    @JsonCreator
    public Endpoint(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Endpoint name cannot be null or blank");
        }
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Endpoint e && name.equals(e.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
