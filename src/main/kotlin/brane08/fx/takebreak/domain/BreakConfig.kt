package brane08.fx.takebreak.domain

import java.io.Serializable
import kotlin.properties.Delegates

class BreakConfig : Serializable {
	var id: String = "01"
	var smallBreak by Delegates.notNull<Int>()
	var longBreak by Delegates.notNull<Int>()
	var spacing by Delegates.notNull<Int>()

	fun getBreakTime(instance: Int): Int {
		return if ((instance % 3) == 0) longBreak else smallBreak
	}
}
