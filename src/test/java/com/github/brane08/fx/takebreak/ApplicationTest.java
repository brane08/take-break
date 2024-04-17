package com.github.brane08.fx.takebreak;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

public class ApplicationTest extends Application {

    @Test
    public void testFxmlShow() {
        Application.launch(ApplicationTest.class);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        var parent = FXMLLoader.load(getClass().getResource("/views/main.fxml"));
    }
}
