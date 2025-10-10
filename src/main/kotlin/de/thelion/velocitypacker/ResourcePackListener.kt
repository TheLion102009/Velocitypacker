package de.thelion.velocitypacker

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.slf4j.Logger
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class ResourcePackListener(
    private val plugin: Velocitypacker,
    private val config: Config,
    private val database: DatabaseManager,
    private val logger: Logger
) {
    // Tracking players who are waiting for resource pack acceptance
    private val pendingPlayers = ConcurrentHashMap<UUID, Long>()
    
    // Tracking players who have successfully loaded the pack in this session
    private val acceptedThisSession = ConcurrentHashMap<UUID, Boolean>()

    @Subscribe(order = PostOrder.FIRST)
    fun onPostLogin(event: PostLoginEvent) {
        val player = event.player
        
        // Always send pack on proxy join (not on server switch)
        // Mark player as pending and send resource pack
        pendingPlayers[player.uniqueId] = System.currentTimeMillis()
        sendResourcePack(player)
    }

    @Subscribe(order = PostOrder.FIRST)
    fun onServerPreConnect(event: ServerPreConnectEvent) {
        val player = event.player
        
        // Allow connection if player has accepted the pack in this session
        if (acceptedThisSession.containsKey(player.uniqueId)) {
            return
        }
        
        // Check if player is pending resource pack acceptance
        if (pendingPlayers.containsKey(player.uniqueId)) {
            // Block connection until pack is accepted or declined
            // Player stays in the resource pack screen
            event.result = ServerPreConnectEvent.ServerResult.denied()
            return
        }
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
                acceptedThisSession[player.uniqueId] = true
                
                // Auto-connect player to first available server
                connectToFirstServer(player)
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
                    kickPlayer(player, "§cResource pack download failed!")
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
    
    private fun connectToFirstServer(player: Player) {
        // Get the first available server
        val servers = plugin.server.allServers
        if (servers.isNotEmpty()) {
            val firstServer = servers.first()
            player.createConnectionRequest(firstServer).fireAndForget()
            logger.info("Connecting ${player.username} to ${firstServer.serverInfo.name}")
        } else {
            logger.warn("No servers available to connect ${player.username}")
        }
    }
    
    @Subscribe
    fun onDisconnect(event: DisconnectEvent) {
        val player = event.player
        // Clean up session data when player disconnects
        pendingPlayers.remove(player.uniqueId)
        acceptedThisSession.remove(player.uniqueId)
        logger.debug("Cleaned up session data for ${player.username}")
    }

    private fun sendResourcePack(player: Player) {
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
        acceptedThisSession.clear()
    }
}
