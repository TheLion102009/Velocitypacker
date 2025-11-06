# Velocitypacker

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20--Beta1-blue.svg)](https://kotlinlang.org)
[![Velocity](https://img.shields.io/badge/Velocity-3.4.0--SNAPSHOT-blue.svg)](https://velocitypowered.com)
[![SQLite](https://img.shields.io/badge/SQLite-3.45.0.0-yellow.svg)](https://sqlite.org)

A Velocity plugin that automatically sends resource packs to players and stores their acceptance status in a SQLite database.

## ✨ Features

- 🔄 **Automatic Resource Pack Sending** - Pack is sent on every proxy join
- 📊 **SQLite Database** - Persistent storage of acceptance status
- 🚫 **Smart Blocking** - Players cannot connect to servers until pack is accepted/declined
- ⚡ **Auto-Connect** - Automatic connection to first server after successful acceptance
- 🔒 **Session Management** - No re-sending when switching between backend servers
- 🎛️ **YAML Configuration** - Easy customization of all settings
- 📋 **Detailed Logs** - Comprehensive information about pack status

## 🚀 Installation

### 1. Download Plugin
```bash
# Clone repository
git clone https://github.com/TheLion102009/Velocitypacker.git
cd Velocitypacker

# Build plugin
./gradlew clean shadowJar
```

### 2. Install JAR
You can find the compiled JAR in `build/libs/Velocitypacker-0.1.jar`.

Copy this file to the `plugins/` folder of your Velocity server.

### 3. Start Server
Restart your Velocity server. The configuration file will be created automatically.

## ⚙️ Configuration

The `config.yml` is automatically created in the `plugins/velocitypacker/` folder:

```yaml
# Resource pack URL (REQUIRED)
# Must be a direct download URL to a .zip file
resourcePackUrl: https://example.com/resourcepack.zip

# SHA-1 hash of the resource pack (OPTIONAL)
# Format: 40 hexadecimal characters (e.g. a1b2c3d4e5f6...)
# Leave empty if you don't want to use a hash
resourcePackSha1: ""

# Message shown to the player
# Supports Minecraft color codes with §
resourcePackPrompt: "§aPlease accept the resource pack to play!"

# Kick player if the resource pack is declined
kickOnDecline: true

# Kick player if the download fails
kickOnFailedDownload: true

# Kick message (supports color codes with §)
kickMessage: "§cYou must accept the resource pack to play!"
```

### Generate SHA-1 Hash

**Windows:**
```powershell
certutil -hashfile resourcepack.zip SHA1
```

**Linux/Mac:**
```bash
sha1sum resourcepack.zip
```

## 🔧 How It Works

### Player Join Process:
1. **Player connects to proxy** → Resource pack is sent immediately
2. **Server connection blocked** → Player stays in resource pack screen
3. **Pack accepted** → Automatic connection to first available server
4. **Pack declined** → Player is kicked (if configured)

### Server Switch Process:
- **Backend server switch** → No re-sending (stored in session)
- **Proxy rejoin** → Pack is sent again (new session)

### Database:
- **Location**: `plugins/velocitypacker/resourcepack.db`
- **Stored data**: UUID, acceptance status, timestamp

## 📋 Logs

The plugin provides detailed logs:

```
[INFO] Initializing Velocitypacker...
[INFO] Configuration loaded
[INFO] Database initialized
[INFO] Event listener registered
[INFO] Sent resource pack to player Spieler123
[INFO] Player Spieler123 successfully downloaded the resource pack
[INFO] Connecting Spieler123 to lobby
```

## 🛠️ Technical Details

- **Velocity API**: 3.4.0-SNAPSHOT
- **Kotlin**: 2.0.20-Beta1
- **SQLite**: 3.45.0.0
- **SnakeYAML**: 2.2
- **Shadow Plugin**: For dependency bundling

## 🏗️ Build

```bash
# Full build with all dependencies
./gradlew clean shadowJar

# Compile only (without Shadow)
./gradlew build
```

The final JAR is approximately **16 MB** in size and contains all necessary dependencies.

## 📝 Important Notes

- **Resource Pack URL**: Must be a direct `.zip` download URL
- **No Server Switch Blocks**: Players can freely switch between backend servers
- **Session Cleanup**: Data is automatically cleaned up on disconnect
- **Database Backup**: SQLite file can be easily copied/backed up

## 🐛 Troubleshooting

### Plugin doesn't load
- Make sure Velocity 3.4.0-SNAPSHOT is running
- Check if all dependencies are correctly bundled

### Resource pack is not sent
- Check the `resourcePackUrl` in the configuration
- Make sure the URL is accessible

### Players are not forwarded
- Check the server logs for errors
- Make sure backend servers are available

## 📜 License

Created by **thelion**

This project is free for private and commercial use.

## 🤝 Support

For questions or problems:
- Create an issue in the GitHub repository
- Check the logs for error messages
- Make sure all dependencies are correctly installed
