module brane.fx.takebreak {
	requires java.desktop;
	requires javafx.controls;
	requires javafx.fxml;
    requires javafx.graphics;
	requires eu.hansolo.tilesfx;
	requires org.slf4j;
	requires com.fasterxml.jackson.databind;
    requires com.dustinredmond.fxtrayicon;
    requires javafx.base;

    opens com.github.brane08.fx.takebreak;
	exports com.github.brane08.fx.takebreak;
	exports com.github.brane08.fx.takebreak.domain;
    exports com.github.brane08.fx.takebreak.controllers;
    opens com.github.brane08.fx.takebreak.controllers;
}
