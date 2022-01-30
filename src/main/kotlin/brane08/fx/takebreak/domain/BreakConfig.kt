package brane08.fx.takebreak.domain

import io.jsondb.annotation.Document
import io.jsondb.annotation.Id
import java.io.Serializable
import kotlin.properties.Delegates

@Document(collection = "config", schemaVersion = "1.0")
class BreakConfig : Serializable {
    @Id
    var id: String = "01"
    var smallBreak by Delegates.notNull<Int>()
    var longBreak by Delegates.notNull<Int>()
    var spacing by Delegates.notNull<Int>()

    fun getBreakTime(instance: Int): Int {
        return if ((instance % 3) == 0) longBreak else smallBreak
    }
}
