# Call-In-Progress Break Skip Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Skip a scheduled break when the user is on a camera/mic call (Zoom, Teams, etc.), independent of idle detection.

**Architecture:** New `com.github.brane08.fx.takebreak.call` package mirrors the existing `idle` package: a `CallDetector` interface, a `CallDetectorFactory` picking an OS-specific impl by `os.name`, and Mac/Windows/Linux implementations that shell out to a platform tool. `BreakSchedule.run()` gets a `CallDetector` and skips (logs + returns) before the idle check if `isCallActive()` is true. All impls fail-open (return `false` = never skip) on any error, matching `LinuxIdleDetector`'s existing philosophy.

**Tech Stack:** Java 21, `ProcessBuilder` (no new dependencies — JNA is not needed here, detection is via OS CLI tools).

**Spec:** N/A — scope agreed inline with user (see conversation): Mac checks for the camera-assistant helper process; Windows checks the CapabilityAccessManager consent-store registry for webcam/microphone; Linux best-effort checks `/dev/video*` via `fuser`.

## Global Constraints
- Never throw from `isCallActive()` — every impl must catch and fail open (`return false`).
- Every subprocess call must have a timeout and `destroyForcibly()` in a `finally`, exactly like `LinuxIdleDetector.getIdleSeconds()`.
- No new `BreakConfig` field or UI toggle — this is an always-on skip, same as the existing idle-skip.
- Mirror existing file/class naming in `idle/`: `CallDetector`, `CallDetectorFactory`, `MacCallDetector`, `WindowsCallDetector`, `LinuxCallDetector`.

---

### Task 1: CallDetector interface + factory + platform impls

**Files:**
- Create: `src/main/java/com/github/brane08/fx/takebreak/call/CallDetector.java`
- Create: `src/main/java/com/github/brane08/fx/takebreak/call/CallDetectorFactory.java`
- Create: `src/main/java/com/github/brane08/fx/takebreak/call/MacCallDetector.java`
- Create: `src/main/java/com/github/brane08/fx/takebreak/call/WindowsCallDetector.java`
- Create: `src/main/java/com/github/brane08/fx/takebreak/call/LinuxCallDetector.java`
- Modify: `src/main/java/module-info.java`
- Test: `src/test/java/com/github/brane08/fx/takebreak/call/CallDetectorFactoryTest.java`

**Interfaces:**
- Produces: `CallDetector.isCallActive(): boolean`; `CallDetectorFactory.create(): CallDetector` — both consumed by Task 2.

- [ ] **Step 1: Write the failing test**

```java
package com.github.brane08.fx.takebreak.call;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CallDetectorFactoryTest {

    @Test
    void factoryReturnsNonNull() {
        assertNotNull(CallDetectorFactory.create());
    }

    @Test
    void factoryReturnsPlatformCorrectType() {
        CallDetector detector = CallDetectorFactory.create();
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            assertInstanceOf(MacCallDetector.class, detector);
        } else if (os.contains("win")) {
            assertInstanceOf(WindowsCallDetector.class, detector);
        } else {
            assertInstanceOf(LinuxCallDetector.class, detector);
        }
    }

    @Test
    void linuxDetectorNeverThrowsWhenToolsAbsent() {
        LinuxCallDetector detector = new LinuxCallDetector();
        assertDoesNotThrow(detector::isCallActive);
    }
}
```

- [ ] **Step 2: Run test to verify it fails (classes don't exist yet)**

Run: `mvn -q test -Dtest="CallDetectorFactoryTest" 2>&1 | tail -30`
Expected: compile error — `CallDetector`/`CallDetectorFactory`/`MacCallDetector`/`WindowsCallDetector`/`LinuxCallDetector` do not exist.

- [ ] **Step 3: Create the interface**

```java
package com.github.brane08.fx.takebreak.call;

public interface CallDetector {
    boolean isCallActive();
}
```

- [ ] **Step 4: Create the Mac implementation**

```java
package com.github.brane08.fx.takebreak.call;

import java.util.concurrent.TimeUnit;

public final class MacCallDetector implements CallDetector {

    private static final long TIMEOUT_SECONDS = 2;

    @Override
    public boolean isCallActive() {
        Process p = null;
        try {
            p = new ProcessBuilder("ps", "-A", "-o", "comm=").start();
            if (!p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                return false; // ps hung — never skip breaks
            }
            String out = new String(p.getInputStream().readAllBytes());
            return out.contains("VDCAssistant") || out.contains("AppleCameraAssistant");
        } catch (Exception e) {
            return false; // detection unavailable — never skip breaks
        } finally {
            if (p != null) {
                p.destroyForcibly();
            }
        }
    }
}
```

- [ ] **Step 5: Create the Windows implementation**

```java
package com.github.brane08.fx.takebreak.call;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class WindowsCallDetector implements CallDetector {

    private static final long TIMEOUT_SECONDS = 3;
    private static final Pattern IN_USE = Pattern.compile("LastUsedTimeStop\\s+REG_QWORD\\s+0x0\\b");

    @Override
    public boolean isCallActive() {
        return keyReportsInUse("webcam") || keyReportsInUse("microphone");
    }

    private boolean keyReportsInUse(String device) {
        Process p = null;
        try {
            p = new ProcessBuilder("reg", "query",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\CapabilityAccessManager\\ConsentStore\\" + device,
                    "/s").start();
            if (!p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                return false; // reg query hung — never skip breaks
            }
            String out = new String(p.getInputStream().readAllBytes());
            return IN_USE.matcher(out).find();
        } catch (Exception e) {
            return false; // detection unavailable — never skip breaks
        } finally {
            if (p != null) {
                p.destroyForcibly();
            }
        }
    }
}
```

- [ ] **Step 6: Create the Linux implementation**

```java
package com.github.brane08.fx.takebreak.call;

import java.io.File;
import java.util.concurrent.TimeUnit;

public final class LinuxCallDetector implements CallDetector {

    private static final long TIMEOUT_SECONDS = 2;

    @Override
    public boolean isCallActive() {
        File[] videoDevices = new File("/dev").listFiles((dir, name) -> name.startsWith("video"));
        if (videoDevices == null) {
            return false; // no video devices — never skip breaks
        }
        for (File device : videoDevices) {
            if (isInUse(device)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInUse(File device) {
        Process p = null;
        try {
            p = new ProcessBuilder("fuser", device.getAbsolutePath()).start();
            if (!p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                return false; // fuser hung — never skip breaks
            }
            return p.exitValue() == 0; // fuser exits 0 iff a process holds the device open
        } catch (Exception e) {
            return false; // fuser not installed — never skip breaks
        } finally {
            if (p != null) {
                p.destroyForcibly();
            }
        }
    }
}
```

- [ ] **Step 7: Create the factory**

```java
package com.github.brane08.fx.takebreak.call;

public final class CallDetectorFactory {

    private CallDetectorFactory() {}

    public static CallDetector create() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) return new MacCallDetector();
        if (os.contains("win")) return new WindowsCallDetector();
        return new LinuxCallDetector();
    }
}
```

- [ ] **Step 8: Open the new package in module-info.java**

In `src/main/java/module-info.java`, add a line right after `opens com.github.brane08.fx.takebreak.idle;`:

```
opens com.github.brane08.fx.takebreak.call;
```

- [ ] **Step 9: Run test to verify it passes**

Run: `mvn -q test -Dtest="CallDetectorFactoryTest" 2>&1 | tail -30`
Expected: `Tests run: 3, Failures: 0, Errors: 0`.

- [ ] **Step 10: Commit**

```bash
git add src/main/java/com/github/brane08/fx/takebreak/call src/main/java/module-info.java src/test/java/com/github/brane08/fx/takebreak/call
git commit -m "feat: add CallDetector (Mac/Windows/Linux) to detect an active camera/mic call"
```

---

### Task 2: Wire CallDetector into BreakSchedule and BreakApplication

**Files:**
- Modify: `src/main/java/com/github/brane08/fx/takebreak/tasks/BreakSchedule.java`
- Modify: `src/main/java/com/github/brane08/fx/takebreak/BreakApplication.java`

**Interfaces:**
- Consumes: `CallDetector.isCallActive(): boolean`, `CallDetectorFactory.create(): CallDetector` from Task 1.
- Produces: `BreakSchedule` constructor now takes an additional `CallDetector callDetector` parameter, inserted right after the existing `IdleDetector idleDetector` parameter (before `AtomicLong currentEpoch, long myEpoch`).

- [ ] **Step 1: Add the CallDetector field, constructor param, and skip check to BreakSchedule**

In `src/main/java/com/github/brane08/fx/takebreak/tasks/BreakSchedule.java`, add the import:

```java
import com.github.brane08.fx.takebreak.call.CallDetector;
```

Change the field list to add (after `idleDetector`):

```java
    private final CallDetector callDetector;
```

Change the constructor signature to insert `CallDetector callDetector` right after `IdleDetector idleDetector`:

```java
    public BreakSchedule(AtomicInteger counter, Stage currentStage, AtomicReference<MenuItem> skipItemRef,
                         Function<Integer, Integer> startTimer, IdleDetector idleDetector, CallDetector callDetector,
                         AtomicLong currentEpoch, long myEpoch) {
        this.counter = counter;
        this.currentStage = currentStage;
        this.skipItemRef = skipItemRef;
        this.startTimer = startTimer;
        this.idleDetector = idleDetector;
        this.callDetector = callDetector;
        this.currentEpoch = currentEpoch;
        this.myEpoch = myEpoch;
    }
```

In `run()`, add the call-skip check right after the epoch check and before the idle check:

```java
            if (callDetector.isCallActive()) {
                LOG.info("Skipping break — call in progress");
                return;
            }
```

- [ ] **Step 2: Update both BreakSchedule construction sites in BreakApplication**

In `src/main/java/com/github/brane08/fx/takebreak/BreakApplication.java`, add the import:

```java
import com.github.brane08.fx.takebreak.call.CallDetector;
import com.github.brane08.fx.takebreak.call.CallDetectorFactory;
```

Add a field next to `idleDetector`:

```java
    private final CallDetector callDetector = CallDetectorFactory.create();
```

In `start()`, change:

```java
                new BreakSchedule(counter, rootStage, skipItemRef, controller::startTimer, idleDetector, epoch, epoch.get()),
```

to:

```java
                new BreakSchedule(counter, rootStage, skipItemRef, controller::startTimer, idleDetector, callDetector, epoch, epoch.get()),
```

In `reschedule()`, change:

```java
                new BreakSchedule(counter, defaultStage, skipItemRef, breakController::startTimer, idleDetector, epoch, myEpoch),
```

to:

```java
                new BreakSchedule(counter, defaultStage, skipItemRef, breakController::startTimer, idleDetector, callDetector, epoch, myEpoch),
```

- [ ] **Step 3: Compile**

Run: `mvn -q compile 2>&1 | tail -60`
Expected: no output, exit 0.

- [ ] **Step 4: Run the full scoped test suite to check no regressions**

Run: `mvn -q test -Dtest="BreakConfigTest,ConfigControllerTest,IdleDetectorFactoryTest,CallDetectorFactoryTest" 2>&1 | tail -30`
Expected: all tests pass, BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/github/brane08/fx/takebreak/tasks/BreakSchedule.java src/main/java/com/github/brane08/fx/takebreak/BreakApplication.java
git commit -m "feat: skip scheduled breaks while a camera/mic call is active"
```

---

## Self-Review

- **Spec coverage:** Mac/Windows/Linux detectors ✅ (Task 1), factory ✅ (Task 1), wiring into break scheduling ✅ (Task 2), fail-open behavior ✅ (every impl's catch/timeout branches return `false`), no new config/UI ✅ (not touched), tests mirroring `IdleDetectorFactoryTest` ✅ (Task 1).
- **Placeholder scan:** none — every step has full code.
- **Type consistency:** `CallDetector.isCallActive(): boolean` used identically in `MacCallDetector`, `WindowsCallDetector`, `LinuxCallDetector`, `CallDetectorFactory`, and `BreakSchedule.run()`. `BreakSchedule` constructor parameter order and `BreakApplication`'s two call sites match exactly.
