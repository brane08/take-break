package com.github.brane08.fx.takebreak.call;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * Detects an active call by asking the OS whether the default microphone or any camera is being
 * used by some process ("IsRunningSomewhere"). Works on Intel and Apple Silicon, unlike matching
 * helper process names, and needs no permissions or child processes.
 * Only the default input device is checked for audio.
 */
public final class MacCallDetector implements CallDetector {

    private static final int SYSTEM_OBJECT = 1;                    // kAudioObjectSystemObject / kCMIOObjectSystemObject
    private static final int SCOPE_GLOBAL = 0x676C6F62;            // 'glob'
    private static final int ELEMENT_MAIN = 0;
    private static final int DEFAULT_INPUT_DEVICE = 0x64496E20;    // 'dIn '
    private static final int CMIO_DEVICES = 0x64657623;            // 'dev#'
    private static final int IS_RUNNING_SOMEWHERE = 0x676F6E65;    // 'gone'
    private static final int UINT32_BYTES = 4;

    interface CoreAudio extends Library {
        CoreAudio INSTANCE = Native.load("CoreAudio", CoreAudio.class);

        int AudioObjectGetPropertyData(int objectId, int[] address, int qualifierSize, Pointer qualifier,
                                       IntByReference dataSize, IntByReference data);
    }

    interface CoreMediaIO extends Library {
        CoreMediaIO INSTANCE = Native.load("CoreMediaIO", CoreMediaIO.class);

        int CMIOObjectGetPropertyDataSize(int objectId, int[] address, int qualifierSize, Pointer qualifier,
                                          IntByReference dataSize);

        int CMIOObjectGetPropertyData(int objectId, int[] address, int qualifierSize, Pointer qualifier,
                                      int dataSize, IntByReference dataUsed, int[] data);

        int CMIOObjectGetPropertyData(int objectId, int[] address, int qualifierSize, Pointer qualifier,
                                      int dataSize, IntByReference dataUsed, IntByReference data);
    }

    @Override
    public boolean isCallActive() {
        try {
            return microphoneInUse() || cameraInUse();
        } catch (Exception | LinkageError e) {
            return false; // native frameworks unavailable — never skip breaks
        }
    }

    static boolean microphoneInUse() {
        CoreAudio audio = CoreAudio.INSTANCE;
        IntByReference size = new IntByReference(UINT32_BYTES);
        IntByReference device = new IntByReference();
        if (audio.AudioObjectGetPropertyData(SYSTEM_OBJECT, address(DEFAULT_INPUT_DEVICE), 0, null, size, device) != 0
                || device.getValue() == 0) {
            return false; // no input device
        }
        size.setValue(UINT32_BYTES);
        IntByReference running = new IntByReference();
        return audio.AudioObjectGetPropertyData(device.getValue(), address(IS_RUNNING_SOMEWHERE), 0, null, size, running) == 0
                && running.getValue() != 0;
    }

    static boolean cameraInUse() {
        CoreMediaIO cmio = CoreMediaIO.INSTANCE;
        IntByReference size = new IntByReference();
        if (cmio.CMIOObjectGetPropertyDataSize(SYSTEM_OBJECT, address(CMIO_DEVICES), 0, null, size) != 0) {
            return false;
        }
        int count = size.getValue() / UINT32_BYTES;
        if (count <= 0) {
            return false; // no cameras
        }
        int[] devices = new int[count];
        IntByReference used = new IntByReference();
        if (cmio.CMIOObjectGetPropertyData(SYSTEM_OBJECT, address(CMIO_DEVICES), 0, null,
                count * UINT32_BYTES, used, devices) != 0) {
            return false;
        }
        int found = Math.min(count, used.getValue() / UINT32_BYTES);
        for (int i = 0; i < found; i++) {
            IntByReference running = new IntByReference();
            if (cmio.CMIOObjectGetPropertyData(devices[i], address(IS_RUNNING_SOMEWHERE), 0, null,
                    UINT32_BYTES, new IntByReference(), running) == 0 && running.getValue() != 0) {
                return true;
            }
        }
        return false;
    }

    // AudioObjectPropertyAddress / CMIOObjectPropertyAddress: { selector, scope, element }
    private static int[] address(int selector) {
        return new int[]{selector, SCOPE_GLOBAL, ELEMENT_MAIN};
    }
}
