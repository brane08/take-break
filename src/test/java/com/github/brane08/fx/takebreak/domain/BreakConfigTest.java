package com.github.brane08.fx.takebreak.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BreakConfigTest {

    @Test
    void defaultConstantsAreDefined() {
        assertEquals(60,   BreakConfig.DEFAULT_SMALL);
        assertEquals(300,  BreakConfig.DEFAULT_LONG);
        assertEquals(1200, BreakConfig.DEFAULT_SPACING);
        assertEquals(30,   BreakConfig.DEFAULT_WARNING);
        assertEquals(300,  BreakConfig.DEFAULT_IDLE);
    }

    @Test
    void shortBreakOnNonMultipleOfThree() {
        var config = new BreakConfig(5, 10, 600, 30, 300);
        assertEquals(5, config.getBreakTime(1));
        assertEquals(5, config.getBreakTime(2));
        assertEquals(5, config.getBreakTime(4));
        assertEquals(5, config.getBreakTime(5));
    }

    @Test
    void longBreakOnMultipleOfThree() {
        var config = new BreakConfig(5, 10, 600, 30, 300);
        assertEquals(10, config.getBreakTime(3));
        assertEquals(10, config.getBreakTime(6));
        assertEquals(10, config.getBreakTime(9));
    }

    @Test
    void saveAndLoadRoundTrip() {
        var original = new BreakConfig(15, 120, 300, 45, 240);
        original.save();

        var loaded = BreakConfig.fromFile();

        assertEquals(original.smallBreak(),    loaded.smallBreak());
        assertEquals(original.longBreak(),     loaded.longBreak());
        assertEquals(original.spacing(),       loaded.spacing());
        assertEquals(original.warningTime(),   loaded.warningTime());
        assertEquals(original.idleThreshold(), loaded.idleThreshold());
    }
}
