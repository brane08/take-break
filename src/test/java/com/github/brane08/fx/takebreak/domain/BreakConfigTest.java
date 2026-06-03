package com.github.brane08.fx.takebreak.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BreakConfigTest {

    @Test
    void defaultConstantsAreDefined() {
        assertEquals(5, BreakConfig.DEFAULT_SMALL);
        assertEquals(10, BreakConfig.DEFAULT_LONG);
        assertEquals(10, BreakConfig.DEFAULT_SPACING);
    }

    @Test
    void shortBreakOnNonMultipleOfThree() {
        var config = new BreakConfig(5, 10, 600, "1.0");
        assertEquals(5, config.getBreakTime(1));
        assertEquals(5, config.getBreakTime(2));
        assertEquals(5, config.getBreakTime(4));
        assertEquals(5, config.getBreakTime(5));
    }

    @Test
    void longBreakOnMultipleOfThree() {
        var config = new BreakConfig(5, 10, 600, "1.0");
        assertEquals(10, config.getBreakTime(3));
        assertEquals(10, config.getBreakTime(6));
        assertEquals(10, config.getBreakTime(9));
    }

    @Test
    void saveAndLoadRoundTrip() {
        var original = new BreakConfig(15, 120, 300, "1.0");
        original.save();

        var loaded = BreakConfig.fromFile();

        assertEquals(original.smallBreak(), loaded.smallBreak());
        assertEquals(original.longBreak(), loaded.longBreak());
        assertEquals(original.spacing(), loaded.spacing());
    }
}
