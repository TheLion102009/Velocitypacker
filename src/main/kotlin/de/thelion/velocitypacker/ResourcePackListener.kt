package de.thelion.velocitypacker

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.slf4j.Logger
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ResourcePackListener(
    private val plugin: Velocitypacker,
    private val config: Config,
    private val database: DatabaseManager,
    private val logger: Logger
) {
    // Tracking players who are currently being sent the resource pack
    private val pendingPlayers = ConcurrentHashMap<UUID, Boolean>()
    
    // Tracking first join to proxy (not backend server switch)
    private val firstJoinToProxy = ConcurrentHashMap<UUID, Boolean>()

    @Subscribe(order = PostOrder.FIRST)
    fun onPostLogin(event: PostLoginEvent) {
        val player = event.player
        firstJoinToProxy[player.uniqueId] = true
        
        // Check if player has already accepted the pack
        if (config.onlyOnFirstJoin && database.hasAccepted(player.uniqueId)) {
            logger.info("Player ${player.username} has already accepted the resource pack")
            return
        }

        // Send resource pack
        sendResourcePack(player)
    }

    @Subscribe
    fun onServerConnected(event: ServerConnectedEvent) {
        val player = event.player
        
        // Skip if this is the first join to proxy (already handled in PostLoginEvent)
        if (firstJoinToProxy.remove(player.uniqueId) == true) {
            return
        }
        
        // Skip if player has already accepted and onlyOnFirstJoin is enabled
        if (config.onlyOnFirstJoin && database.hasAccepted(player.uniqueId)) {
            return
        }

        // Don't send pack again when switching backend servers if already accepted
        if (database.hasAccepted(player.uniqueId)) {
            logger.info("Player ${player.username} switching servers - pack already accepted")
            return
        }

        // Send resource pack if not yet accepted
        sendResourcePack(player)
    }

    @Subscribe
    fun onResourcePackStatus(event: PlayerResourcePackStatusEvent) {
        val player = event.player
        val status = event.status

        // Only handle if we're tracking this player
        if (!pendingPlayers.containsKey(player.uniqueId)) {
            return
        }

        when (status) {
            PlayerResourcePackStatusEvent.Status.SUCCESSFUL -> {
                logger.info("Player ${player.username} successfully downloaded the resource pack")
                database.setAccepted(player.uniqueId, true)
                pendingPlayers.remove(player.uniqueId)
            }

            PlayerResourcePackStatusEvent.Status.DECLINED -> {
                logger.info("Player ${player.username} declined the resource pack")
                pendingPlayers.remove(player.uniqueId)
                
                if (config.kickOnDecline) {
                    kickPlayer(player, config.kickMessage)
                }
            }

            PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD -> {
                logger.info("Player ${player.username} failed to download the resource pack")
                pendingPlayers.remove(player.uniqueId)
                
                if (config.kickOnFailedDownload) {
                    kickPlayer(player, "§cResourcepack Download fehlgeschlagen!")
                }
            }

            PlayerResourcePackStatusEvent.Status.ACCEPTED -> {
                logger.info("Player ${player.username} accepted the resource pack")
                // Don't remove from pending yet, wait for SUCCESSFUL
            }

            else -> {
                // Handle other statuses if needed
            }
        }
    }

    private fun sendResourcePack(player: Player) {
        pendingPlayers[player.uniqueId] = true
        
        try {
            val promptComponent = LegacyComponentSerializer.legacySection()
                .deserialize(config.resourcePackPrompt)
            
            // Use the proxy's resource pack builder
            val packBuilder = plugin.server.createResourcePackBuilder(config.resourcePackUrl)
                .setPrompt(promptComponent)
                .setShouldForce(config.kickOnDecline || config.kickOnFailedDownload)
            
            // Add hash if provided (SHA-1 hash as byte array)
            if (config.resourcePackSha1.isNotBlank()) {
                try {
                    val hashBytes = config.resourcePackSha1.chunked(2)
                        .map { it.toInt(16).toByte() }
                        .toByteArray()
                    packBuilder.setHash(hashBytes)
                } catch (e: Exception) {
                    logger.warn("Invalid SHA-1 hash format in config, ignoring hash")
                }
            }
            
            player.sendResourcePackOffer(packBuilder.build())
            logger.info("Sent resource pack to player ${player.username}")
        } catch (e: Exception) {
            logger.error("Failed to send resource pack to player ${player.username}", e)
            pendingPlayers.remove(player.uniqueId)
        }
    }

    private fun kickPlayer(player: Player, message: String) {
        val kickMessage = LegacyComponentSerializer.legacySection().deserialize(message)
        player.disconnect(kickMessage)
    }

    fun cleanup() {
        pendingPlayers.clear()
        firstJoinToProxy.clear()
    }
}
