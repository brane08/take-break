module brane.fx.takebreak {
	requires java.desktop;
	requires java.prefs;
	requires java.sql;
	requires javafx.controls;
	requires javafx.fxml;
	requires eu.hansolo.tilesfx;
	requires org.slf4j;
	requires com.fasterxml.jackson.databind;

	opens com.github.brane08.fx.takebreak;
	exports com.github.brane08.fx.takebreak;
	exports com.github.brane08.fx.takebreak.domain;
}