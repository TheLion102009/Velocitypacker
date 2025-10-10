package de.thelion.velocitypacker

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileReader
import java.io.FileWriter

data class Config(
    val resourcePackUrl: String = "https://example.com/resourcepack.zip",
    val resourcePackSha1: String = "",
    val resourcePackPrompt: String = "§aPlease accept the resource pack to play!",
    val kickOnDecline: Boolean = true,
    val kickOnFailedDownload: Boolean = true,
    val kickMessage: String = "§cYou must accept the resource pack to play!"
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
                        resourcePackPrompt = data["resourcePackPrompt"] as? String ?: "§aPlease accept the resource pack to play!",
                        kickOnDecline = data["kickOnDecline"] as? Boolean ?: true,
                        kickOnFailedDownload = data["kickOnFailedDownload"] as? Boolean ?: true,
                        kickMessage = data["kickMessage"] as? String ?: "§cYou must accept the resource pack to play!"
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
                    "kickMessage" to config.kickMessage
                )
                yaml.dump(data, writer)
            }
        }
    }
}
