package brane08.fx.takebreak.config

import brane08.fx.takebreak.domain.BreakConfig
import io.jsondb.JsonDBTemplate
import java.nio.file.Files
import java.nio.file.Paths

object Injector {
	private val objectMap: Map<String, *>

	init {
		val dbFilesLocation = Paths.get(System.getProperty("user.home"), "/.take-break")
		if (!Files.exists(dbFilesLocation))
			Files.createDirectory(dbFilesLocation)
		val baseScanPackage = "brane08.fx.takebreak.domain"

		val dbTemplate = JsonDBTemplate(dbFilesLocation.toString(), baseScanPackage)
		objectMap = mapOf(
			"configLoc" to dbFilesLocation,
			"template" to dbTemplate,
			"config" to loadConfig(dbTemplate)
		)
	}

	fun <T> get(beanClass: Class<T>): T {
		return objectMap[beanClass.name] as T
	}

	private fun loadConfig(dbTemplate: JsonDBTemplate): BreakConfig {
		if (!dbTemplate.collectionExists(BreakConfig::class.java)) {
			dbTemplate.createCollection(BreakConfig::class.java)
		}

		val jxQuery = String.format("/.[id='%s']", "01")
		val configs = dbTemplate.find(jxQuery, BreakConfig::class.java)
		if (configs.size == 0) {
			println("Configuration not created, creating default")
			val config = BreakConfig()
			config.smallBreak = 5
			config.longBreak = 5
			config.spacing = 5
			dbTemplate.insert<BreakConfig>(config)
			return config
		}
		return configs.first()
	}

}