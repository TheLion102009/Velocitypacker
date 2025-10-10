package de.thelion.velocitypacker

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileReader
import java.io.FileWriter

data class Config(
    val resourcePackUrl: String = "https://example.com/resourcepack.zip",
    val resourcePackSha1: String = "",
    val resourcePackPrompt: String = "§aBitte akzeptiere das Resourcepack um zu spielen!",
    val kickOnDecline: Boolean = true,
    val kickOnFailedDownload: Boolean = true,
    val kickMessage: String = "§cDu musst das Resourcepack akzeptieren um zu spielen!",
    val onlyOnFirstJoin: Boolean = true
) {
    companion object {
        private val yaml: Yaml by lazy {
            val options = DumperOptions().apply {
                defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
                isPrettyFlow = true
            }
            Yaml(options)
        }

        fun load(file: File): Config {
            if (!file.exists()) {
                val defaultConfig = Config()
                save(file, defaultConfig)
                return defaultConfig
            }

            return try {
                FileReader(file).use { reader ->
                    val data = yaml.load<Map<String, Any>>(reader) ?: emptyMap()
                    Config(
                        resourcePackUrl = data["resourcePackUrl"] as? String ?: "https://example.com/resourcepack.zip",
                        resourcePackSha1 = data["resourcePackSha1"] as? String ?: "",
                        resourcePackPrompt = data["resourcePackPrompt"] as? String ?: "§aBitte akzeptiere das Resourcepack um zu spielen!",
                        kickOnDecline = data["kickOnDecline"] as? Boolean ?: true,
                        kickOnFailedDownload = data["kickOnFailedDownload"] as? Boolean ?: true,
                        kickMessage = data["kickMessage"] as? String ?: "§cDu musst das Resourcepack akzeptieren um zu spielen!",
                        onlyOnFirstJoin = data["onlyOnFirstJoin"] as? Boolean ?: true
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Config()
            }
        }

        fun save(file: File, config: Config) {
            file.parentFile?.mkdirs()
            FileWriter(file).use { writer ->
                val data = mapOf(
                    "resourcePackUrl" to config.resourcePackUrl,
                    "resourcePackSha1" to config.resourcePackSha1,
                    "resourcePackPrompt" to config.resourcePackPrompt,
                    "kickOnDecline" to config.kickOnDecline,
                    "kickOnFailedDownload" to config.kickOnFailedDownload,
                    "kickMessage" to config.kickMessage,
                    "onlyOnFirstJoin" to config.onlyOnFirstJoin
                )
                yaml.dump(data, writer)
            }
        }
    }
}
