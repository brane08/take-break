module brane.fx.takebreak {
	requires java.desktop;
	requires java.sql;
	requires javafx.controls;
	requires javafx.fxml;
	requires eu.hansolo.tilesfx;
	requires kotlin.stdlib;
	requires org.slf4j;
	requires com.fasterxml.jackson.databind;

	opens brane08.fx.takebreak.domain;
	exports brane08.fx.takebreak;
	exports brane08.fx.takebreak.domain;
}