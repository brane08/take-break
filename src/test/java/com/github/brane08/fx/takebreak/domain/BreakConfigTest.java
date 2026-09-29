package com.github.brane08.fx.takebreak.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BreakConfigTest {

    @Test
    void defaultConstantsAreDefined() {
        assertEquals(60,   BreakConfig.DEFAULT_SMALL);
        assertEquals(300,  BreakConfig.DEFAULT_LONG);
        assertEquals(1080, BreakConfig.DEFAULT_SPACING);
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
    void normalizedRaisesSpacingAboveLongestBreak() {
        var fixed = new BreakConfig(60, 300, 120, 30, 300).normalized();
        assertEquals(310, fixed.spacing());
        assertEquals(30, fixed.warningTime());
    }

    @Test
    void normalizedKeepsWarningBeforeBreak() {
        var fixed = new BreakConfig(60, 300, 600, 900, 300).normalized();
        assertEquals(600, fixed.spacing());
        assertEquals(590, fixed.warningTime());
    }

    @Test
    void normalizedReturnsSameInstanceWhenValid() {
        var valid = new BreakConfig(60, 300, 1200, 30, 300);
        assertSame(valid, valid.normalized());
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

    @Test
    void defaultOffsetsGive18_1_18_1_17_5Hour() {
        var c = new BreakConfig(60, 300, 1080, 30, 300);
        assertArrayEquals(new int[]{1080, 2220, 3300}, c.breakOffsets());
    }
}
