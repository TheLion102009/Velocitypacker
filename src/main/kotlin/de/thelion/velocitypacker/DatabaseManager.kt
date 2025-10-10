package de.thelion.velocitypacker

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID

class DatabaseManager(dataFolder: File) {
    private val connection: Connection

    init {
        // Explicitly load the SQLite JDBC driver
        try {
            Class.forName("org.sqlite.JDBC")
        } catch (e: ClassNotFoundException) {
            throw RuntimeException("SQLite JDBC driver not found", e)
        }
        
        dataFolder.mkdirs()
        val dbFile = File(dataFolder, "resourcepack.db")
        connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
        createTable()
    }

    private fun createTable() {
        val statement = connection.createStatement()
        statement.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS player_resourcepack (
                uuid TEXT PRIMARY KEY,
                has_accepted INTEGER NOT NULL,
                last_updated INTEGER NOT NULL
            )
            """.trimIndent()
        )
        statement.close()
    }

    fun hasAccepted(uuid: UUID): Boolean {
        val statement = connection.prepareStatement(
            "SELECT has_accepted FROM player_resourcepack WHERE uuid = ?"
        )
        statement.setString(1, uuid.toString())
        val result = statement.executeQuery()
        
        val hasAccepted = if (result.next()) {
            result.getInt("has_accepted") == 1
        } else {
            false
        }
        
        result.close()
        statement.close()
        return hasAccepted
    }

    fun setAccepted(uuid: UUID, accepted: Boolean) {
        val statement = connection.prepareStatement(
            """
            INSERT OR REPLACE INTO player_resourcepack (uuid, has_accepted, last_updated)
            VALUES (?, ?, ?)
            """.trimIndent()
        )
        statement.setString(1, uuid.toString())
        statement.setInt(2, if (accepted) 1 else 0)
        statement.setLong(3, System.currentTimeMillis())
        statement.executeUpdate()
        statement.close()
    }

    fun removePlayer(uuid: UUID) {
        val statement = connection.prepareStatement(
            "DELETE FROM player_resourcepack WHERE uuid = ?"
        )
        statement.setString(1, uuid.toString())
        statement.executeUpdate()
        statement.close()
    }

    fun close() {
        if (!connection.isClosed) {
            connection.close()
        }
    }
}
