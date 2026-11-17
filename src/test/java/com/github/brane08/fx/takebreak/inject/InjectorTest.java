package com.github.brane08.fx.takebreak.inject;

import com.github.brane08.fx.takebreak.Constants;
import com.github.brane08.fx.takebreak.domain.BreakConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InjectorTest {

    @Test
    void registerAndResolveByType() {
        String instance = "hello";
        Injector.register(instance);
        assertEquals(instance, Injector.resolve(String.class));
    }

    @Test
    void registerNamedAndResolveNamed() {
        Injector.registerNamed("myName", 42);
        assertEquals(42, (int) Injector.resolveNamed("myName"));
    }

    @Test
    void resolveThrowsWhenTypeNotRegistered() {
        assertThrows(IllegalArgumentException.class, () -> Injector.resolve(Thread.class));
    }

    @Test
    void resolveNamedThrowsWhenNameNotRegistered() {
        assertThrows(IllegalArgumentException.class, () -> Injector.resolveNamed("nope-not-registered"));
    }

    @Test
    void initDefaultRegistersBreakConfig() {
        Injector.initDefault();
        BreakConfig config = Injector.resolveNamed(Constants.DI_BREAK_CONFIG);
        assertEquals(config, Injector.resolveNamed(Constants.DI_BREAK_CONFIG));
    }
}
