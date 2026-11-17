package com.github.brane08.fx.takebreak;

import com.github.brane08.fx.takebreak.domain.BreakConfig;
import com.github.brane08.fx.takebreak.inject.Injector;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Slider;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class ConfigController implements Initializable {

    @FXML
    private Slider smallBreakSlider;
    @FXML
    private Slider longBreakSlider;
    @FXML
    private Slider spacingSlider;

    private Consumer<BreakConfig> rescheduleCallback;

    public void setRescheduleCallback(Consumer<BreakConfig> callback) {
        this.rescheduleCallback = callback;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        BreakConfig config = Injector.resolveNamed("breakConfig");
        smallBreakSlider.setValue(config.smallBreak());
        longBreakSlider.setValue(config.longBreak());
        spacingSlider.setValue(config.spacing());
    }

    @FXML
    public void resetDefaults() {
        smallBreakSlider.setValue(BreakConfig.DEFAULT_SMALL);
        longBreakSlider.setValue(BreakConfig.DEFAULT_LONG);
        spacingSlider.setValue(BreakConfig.DEFAULT_SPACING);
    }

    @FXML
    public void saveConfig() {
        int small = (int) smallBreakSlider.getValue();
        int large = (int) longBreakSlider.getValue();
        int spacing = (int) spacingSlider.getValue();
        BreakConfig updated = new BreakConfig(small, large, spacing, "1.0");
        updated.save();
        Injector.registerNamed("breakConfig", updated);
        if (rescheduleCallback != null) {
            rescheduleCallback.accept(updated);
        }
        smallBreakSlider.getScene().getWindow().hide();
    }
}
