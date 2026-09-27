package com.github.brane08.fx.takebreak;

import com.github.brane08.fx.takebreak.inject.Injector;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(ApplicationExtension.class)
class BreakApplicationTest {

    private BreakApplication app;
    private Stage stage;

    @BeforeAll
    static void init() {
        // Surefire runs with -Djava.awt.headless=true for Monocle; BreakApplication's
        // skipItemRef field eagerly constructs a MenuItem, which rejects that. Flip it
        // back before touching AWT — reuseForks=false gives this class its own JVM fork,
        // and -Dtake-break.test=true (also set by Surefire) makes start() skip
        // systemTray(), so no real AWT SystemTray/FXTrayIcon is ever created here.
        System.setProperty("java.awt.headless", "false");
        Injector.initDefault();
    }

    @Start
    void start(Stage stage) throws Exception {
        this.stage = stage;
        app = new BreakApplication();
        app.start(stage);
    }

    @Test
    void startWiresStageAndScene() {
        assertNotNull(stage.getScene());
        assertEquals(BreakApplication.WIDTH, stage.getScene().getWidth());
        assertEquals(BreakApplication.HEIGHT, stage.getScene().getHeight());
        assertFalse(stage.isResizable());
    }

    @Test
    void stopDoesNotThrow() {
        assertDoesNotThrow(() -> app.stop());
    }
}
