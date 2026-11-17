package com.github.brane08.fx.takebreak.inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brane08.fx.takebreak.Constants;
import com.github.brane08.fx.takebreak.domain.BreakConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Injector {

    private static final Map<String, Object> INSTANCES = new ConcurrentHashMap<>();

    private Injector() {

    }

    public static void register(Object instance) {
        INSTANCES.put(instance.getClass().getName(), instance);
    }

    public static void registerNamed(String name, Object instance) {
        INSTANCES.put(name, instance);
    }

    @SuppressWarnings("unchecked")
    public static <T> T resolve(Class<T> type) {
        T instance = (T) INSTANCES.get(type.getName());
        if (instance == null) {
            throw new IllegalArgumentException("No registered instance found for type: " + type.getName());
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    public static <T> T resolveNamed(String name) {
        T instance = (T) INSTANCES.get(name);
        if (instance == null) {
            throw new IllegalArgumentException("No registered instance found for name: " + name);
        }
        return instance;
    }

    public static void initDefault() {
        registerNamed(Constants.DI_JSON_MAPPER, new ObjectMapper());
        registerNamed(Constants.DI_BREAK_CONFIG, BreakConfig.fromFile());
    }
}
