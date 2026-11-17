package com.github.brane08.fx.takebreak.controllers;

import com.github.brane08.fx.takebreak.Constants;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(Constants.LOGGER_NAME);

    @FXML
    Slider smallBreak;
    @FXML
    Label smallLabel;
    @FXML
    Slider longBreak;
    @FXML
    Label longLabel;
    @FXML
    Slider spacing;
    @FXML
    Label spacingLabel;
    @FXML
    Button btnSave;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        smallBreak.valueProperty().addListener((observable, oldValue, newValue) -> {
           smallLabel.setText(String.format("%02d", newValue.intValue()));
        });
        longBreak.valueProperty().addListener((observable, oldValue, newValue) -> {
           longLabel.setText(String.format("%02d", newValue.intValue()));
        });
        spacing.valueProperty().addListener((observable, oldValue, newValue) -> {
           spacingLabel.setText(String.format("%02d", newValue.intValue()));
        });
    }
}
