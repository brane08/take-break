package com.github.brane08.fx.takebreak.call;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WindowsCallDetector implements CallDetector {

    // Two queries at up to (WAIT + JOIN) each stay well inside the caller's budget.
    private static final long WAIT_SECONDS = 2;
    private static final long JOIN_MILLIS = 500;
    private static final Pattern KEY_LINE = Pattern.compile("^HKEY_", Pattern.CASE_INSENSITIVE);
    private static final Pattern START = Pattern.compile("LastUsedTimeStart\\s+REG_QWORD\\s+0x([0-9a-fA-F]+)");
    private static final Pattern STOP = Pattern.compile("LastUsedTimeStop\\s+REG_QWORD\\s+0x([0-9a-fA-F]+)");

    @Override
    public boolean isCallActive() {
        return keyReportsInUse("webcam") || keyReportsInUse("microphone");
    }

    /**
     * An app is using the device when its ConsentStore subkey has a non-zero LastUsedTimeStart and
     * a zero LastUsedTimeStop. Apps that were granted access but never used have both at zero.
     */
    static boolean anySubkeyInUse(String regQueryOutput) {
        Long start = null;
        Long stop = null;
        for (String line : regQueryOutput.split("\\R")) {
            String trimmed = line.trim();
            if (KEY_LINE.matcher(trimmed).find()) {
                start = null;
                stop = null;
                continue;
            }
            Matcher m = START.matcher(trimmed);
            if (m.find()) {
                start = parseQword(m.group(1));
            } else if ((m = STOP.matcher(trimmed)).find()) {
                stop = parseQword(m.group(1));
            }
            if (start != null && stop != null && start != 0 && stop == 0) {
                return true;
            }
        }
        return false;
    }

    private static Long parseQword(String hex) {
        try {
            return Long.parseUnsignedLong(hex, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean keyReportsInUse(String device) {
        Process p = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("reg", "query",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\CapabilityAccessManager\\ConsentStore\\" + device,
                    "/s");
            pb.redirectErrorStream(true);
            p = pb.start();

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            Thread reader = startDrainThread(p.getInputStream(), buffer);

            if (!p.waitFor(WAIT_SECONDS, TimeUnit.SECONDS)) {
                return false; // reg query hung — never skip breaks
            }
            reader.join(JOIN_MILLIS);
            return anySubkeyInUse(buffer.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // let the scheduler see the cancellation
            return false;
        } catch (Exception e) {
            return false; // detection unavailable — never skip breaks
        } finally {
            if (p != null) {
                p.destroyForcibly();
            }
        }
    }

    private static Thread startDrainThread(InputStream in, ByteArrayOutputStream buffer) {
        Thread t = new Thread(() -> {
            try {
                in.transferTo(buffer);
            } catch (Exception e) {
                // stream closed/interrupted — buffer holds whatever was read so far
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }
}
