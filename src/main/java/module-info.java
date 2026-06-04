module brane.fx.takebreak {
	requires java.desktop;
	requires javafx.controls;
	requires javafx.fxml;
    requires javafx.graphics;
requires org.slf4j;
    requires com.dustinredmond.fxtrayicon;
    requires javafx.base;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    opens com.github.brane08.fx.takebreak.idle;

    opens com.github.brane08.fx.takebreak;
	exports com.github.brane08.fx.takebreak;
	exports com.github.brane08.fx.takebreak.domain;
    exports com.github.brane08.fx.takebreak.controllers;
    opens com.github.brane08.fx.takebreak.controllers;
}
