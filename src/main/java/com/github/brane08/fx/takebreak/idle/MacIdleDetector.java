package com.github.brane08.fx.takebreak.idle;

import com.sun.jna.Library;
import com.sun.jna.Native;

public final class MacIdleDetector implements IdleDetector {

    interface CoreGraphics extends Library {
        CoreGraphics INSTANCE = Native.load("CoreGraphics", CoreGraphics.class);
        // kCGEventSourceStateHIDSystemState=1, kCGAnyInputEventType=0xFFFFFFFF
        double CGEventSourceSecondsSinceLastEventType(int stateId, int eventType);
    }

    @Override
    public long getIdleSeconds() {
        return (long) CoreGraphics.INSTANCE
                .CGEventSourceSecondsSinceLastEventType(1, 0xFFFFFFFF);
    }
}
