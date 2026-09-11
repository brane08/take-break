package com.github.brane08.fx.takebreak.controllers;

import com.github.brane08.fx.takebreak.Constants;
import com.github.brane08.fx.takebreak.domain.BreakConfig;
import com.github.brane08.fx.takebreak.inject.Injector;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Slider;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class ConfigController implements Initializable {

    @FXML Slider smallBreakSlider;
    @FXML Slider longBreakSlider;
    @FXML Slider spacingSlider;
    @FXML Slider warningSlider;
    @FXML Slider idleSlider;

    private Consumer<BreakConfig> rescheduleCallback;

    public void setRescheduleCallback(Consumer<BreakConfig> callback) {
        this.rescheduleCallback = callback;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        BreakConfig config = Injector.resolveNamed(Constants.DI_BREAK_CONFIG);
        smallBreakSlider.setValue(config.smallBreak());
        longBreakSlider.setValue(config.longBreak());
        spacingSlider.setValue(config.spacing());
        warningSlider.setValue(config.warningTime());
        idleSlider.setValue(config.idleThreshold());
    }

    @FXML
    public void resetDefaults() {
        smallBreakSlider.setValue(BreakConfig.DEFAULT_SMALL);
        longBreakSlider.setValue(BreakConfig.DEFAULT_LONG);
        spacingSlider.setValue(BreakConfig.DEFAULT_SPACING);
        warningSlider.setValue(BreakConfig.DEFAULT_WARNING);
        idleSlider.setValue(BreakConfig.DEFAULT_IDLE);
    }

    @FXML
    public void saveConfig() {
        int small = (int) smallBreakSlider.getValue();
        int large = (int) longBreakSlider.getValue();
        int spacing = (int) spacingSlider.getValue();
        int warning = (int) warningSlider.getValue();
        int idle = (int) idleSlider.getValue();
        if (warning >= spacing) {
            warning = Math.max(0, spacing - 10);
            warningSlider.setValue(warning);
        }
        BreakConfig updated = new BreakConfig(small, large, spacing, warning, idle);
        updated.save();
        Injector.registerNamed(Constants.DI_BREAK_CONFIG, updated);
        if (rescheduleCallback != null) {
            rescheduleCallback.accept(updated);
        }
        if (smallBreakSlider.getScene() != null
                && smallBreakSlider.getScene().getWindow() != null) {
            smallBreakSlider.getScene().getWindow().hide();
        }
    }
}
