package com.github.brane08.fx.takebreak.controllers;

import com.github.brane08.fx.takebreak.FxHelper;
import com.github.brane08.fx.takebreak.domain.BreakConfig;
import com.github.brane08.fx.takebreak.inject.Injector;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigControllerTest {

    @BeforeAll
    static void startFx() throws InterruptedException {
        Injector.initDefault();
        FxHelper.startOnce();
    }

    private ConfigController loadController() throws Exception {
        return FxHelper.onFxThread(() -> {
            FXMLLoader loader = new FXMLLoader(
                ConfigControllerTest.class.getResource("/views/config.fxml"));
            loader.load();
            return loader.getController();
        });
    }

    @Test
    void slidersInitializeFromConfig() throws Exception {
        BreakConfig config = Injector.resolveNamed("breakConfig");
        ConfigController ctrl = loadController();
        assertEquals(config.smallBreak(),    (int) ctrl.smallBreakSlider.getValue());
        assertEquals(config.longBreak(),     (int) ctrl.longBreakSlider.getValue());
        assertEquals(config.spacing(),       (int) ctrl.spacingSlider.getValue());
        assertEquals(config.warningTime(),   (int) ctrl.warningSlider.getValue());
        assertEquals(config.idleThreshold(), (int) ctrl.idleSlider.getValue());
    }

    @Test
    void resetDefaultsRestoresConstants() throws Exception {
        ConfigController ctrl = loadController();
        FxHelper.onFxThread(() -> { ctrl.resetDefaults(); return null; });
        assertEquals(BreakConfig.DEFAULT_SMALL,   (int) ctrl.smallBreakSlider.getValue());
        assertEquals(BreakConfig.DEFAULT_LONG,    (int) ctrl.longBreakSlider.getValue());
        assertEquals(BreakConfig.DEFAULT_SPACING, (int) ctrl.spacingSlider.getValue());
        assertEquals(BreakConfig.DEFAULT_WARNING, (int) ctrl.warningSlider.getValue());
        assertEquals(BreakConfig.DEFAULT_IDLE,    (int) ctrl.idleSlider.getValue());
    }

    @Test
    void saveConfigWritesCorrectRecord() throws Exception {
        ConfigController ctrl = loadController();
        FxHelper.onFxThread(() -> {
            ctrl.smallBreakSlider.setValue(20);
            ctrl.longBreakSlider.setValue(120);
            ctrl.spacingSlider.setValue(660);
            ctrl.warningSlider.setValue(30);
            ctrl.idleSlider.setValue(240);
            ctrl.saveConfig();
            return null;
        });
        BreakConfig saved = Injector.resolveNamed("breakConfig");
        assertEquals(20,  saved.smallBreak());
        assertEquals(120, saved.longBreak());
        assertEquals(660, saved.spacing());
        assertEquals(30,  saved.warningTime());
        assertEquals(240, saved.idleThreshold());
    }
}
