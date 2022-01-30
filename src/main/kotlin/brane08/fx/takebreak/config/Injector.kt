package brane08.fx.takebreak.config

import brane08.fx.takebreak.domain.BreakConfig
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Paths

object Injector {
	private val objectMap: Map<String, *>

	init {
		val jsonMapper = ObjectMapper()
		val dbFilesLocation = Paths.get(System.getProperty("user.home"), "/.take-break")
		if (!Files.exists(dbFilesLocation)) {
			Files.createDirectory(dbFilesLocation)
		}
		val configFile = dbFilesLocation.resolve("config.json")
		if (!Files.exists(configFile)) {
			val config = BreakConfig()
			config.smallBreak = 1
			config.longBreak = 2
			config.spacing = 1
			val json = jsonMapper.createObjectNode()
			json.put("version", "0.1")
			json.putPOJO("breakConfig", config)
			jsonMapper.writeValue(configFile.toFile(), json)
		}
		val appConfig = jsonMapper.readTree(configFile.toFile())
		val breakConfig = jsonMapper.treeToValue(appConfig.get("breakConfig"), BreakConfig::class.java)
		objectMap = mapOf(
			"configLoc" to dbFilesLocation,
			"jsonMapper" to jsonMapper,
			"appConfig" to appConfig,
			"breakConfig" to breakConfig
		)
	}

	fun <T> get(name: String): T {
		return objectMap[name] as T
	}
}