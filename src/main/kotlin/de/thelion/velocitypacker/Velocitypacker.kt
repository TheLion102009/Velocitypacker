package de.thelion.velocitypacker

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import org.slf4j.Logger
import java.io.File
import java.nio.file.Path

@Plugin(
    id = "velocitypacker",
    name = "Velocitypacker",
    version = BuildConstants.VERSION,
    description = "Velocity plugin to send resource packs to players",
    authors = ["thelion"]
)
class Velocitypacker @Inject constructor(
    val logger: Logger,
    val server: ProxyServer,
    @DataDirectory private val dataDirectory: Path
) {
    private lateinit var config: Config
    private lateinit var database: DatabaseManager
    private lateinit var listener: ResourcePackListener

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        logger.info("Initializing Velocitypacker...")

        // Load configuration
        val configFile = File(dataDirectory.toFile(), "config.yml")
        config = Config.load(configFile)
        logger.info("Configuration loaded")

        // Initialize database
        database = DatabaseManager(dataDirectory.toFile())
        logger.info("Database initialized")

        // Register event listener
        listener = ResourcePackListener(this, config, database, logger)
        server.eventManager.register(this, listener)
        logger.info("Event listener registered")

        logger.info("Velocitypacker successfully initialized!")
        logger.info("Resource Pack URL: ${config.resourcePackUrl}")
        logger.info("Resource Pack SHA-1: ${if (config.resourcePackSha1.isNotBlank()) config.resourcePackSha1 else "Not set"}")
        logger.info("Kick on decline: ${config.kickOnDecline}")
        logger.info("Kick on failed download: ${config.kickOnFailedDownload}")
        logger.info("Only on first join: ${config.onlyOnFirstJoin}")
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        logger.info("Shutting down Velocitypacker...")
        
        // Cleanup
        listener.cleanup()
        database.close()
        
        logger.info("Velocitypacker shut down successfully")
    }
}
