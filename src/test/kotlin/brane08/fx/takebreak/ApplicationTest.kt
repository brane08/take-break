package brane08.fx.takebreak

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.stage.Stage
import kotlin.test.Test


class ApplicationTest : Application() {

    @Test
    fun testFxmlShow() {
        Application.launch(ApplicationTest::class.java)
    }

    override fun start(p0: Stage?) {
        val parent = FXMLLoader.load<Parent>(this.javaClass.getResource("/views/main.fxml"))
    }

}