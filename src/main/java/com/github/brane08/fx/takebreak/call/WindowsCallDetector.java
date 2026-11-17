package com.github.brane08.fx.takebreak.call;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
            ProcessBuilder pb = new ProcessBuilder("reg", "query",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\CapabilityAccessManager\\ConsentStore\\" + device,
                    "/s");
            pb.redirectErrorStream(true);
            p = pb.start();

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            Thread reader = startDrainThread(p.getInputStream(), buffer);

            boolean exited = p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            reader.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
            if (!exited) {
                return false; // reg query hung — never skip breaks
            }

            String out = buffer.toString();
            return IN_USE.matcher(out).find();
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
