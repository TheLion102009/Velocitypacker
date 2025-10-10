# Velocitypacker

Ein Velocity-Plugin, das automatisch Resourcepacks an Spieler sendet und deren Status in einer SQLite-Datenbank speichert.

## Features

- ✅ Automatisches Senden von Resourcepacks beim Join
- ✅ SQLite-Datenbank zur persistenten Speicherung des Akzeptanz-Status
- ✅ Intelligentes Tracking: Pack wird nicht erneut gesendet beim Wechsel zwischen Backend-Servern
- ✅ Konfigurierbar: Spieler kicken bei Ablehnung oder fehlgeschlagenem Download
- ✅ SHA-1 Hash-Unterstützung für Resourcepack-Verifikation
- ✅ YAML-Konfiguration

## Installation

1. Kompiliere das Plugin mit `./gradlew build`
2. Die JAR-Datei findest du in `build/libs/`
3. Kopiere die JAR in den `plugins` Ordner deines Velocity-Servers
4. Starte den Server - die Konfigurationsdatei wird automatisch erstellt

## Konfiguration

Die `config.yml` wird automatisch im `plugins/velocitypacker/` Ordner erstellt:

```yaml
# URL zum Resourcepack (ERFORDERLICH)
resourcePackUrl: https://example.com/resourcepack.zip

# SHA-1 Hash des Resourcepacks (OPTIONAL)
# Format: 40 Zeichen Hexadezimal
resourcePackSha1: ''

# Nachricht die dem Spieler angezeigt wird
resourcePackPrompt: §aBitte akzeptiere das Resourcepack um zu spielen!

# Spieler kicken wenn das Resourcepack abgelehnt wird
kickOnDecline: true

# Spieler kicken wenn der Download fehlschlägt
kickOnFailedDownload: true

# Kick-Nachricht
kickMessage: §cDu musst das Resourcepack akzeptieren um zu spielen!

# Nur beim ersten Join senden
onlyOnFirstJoin: true
```

### SHA-1 Hash generieren

**Windows:**
```powershell
certutil -hashfile resourcepack.zip SHA1
```

**Linux/Mac:**
```bash
sha1sum resourcepack.zip
```

## Funktionsweise

1. **Erster Join**: Spieler joint auf den Velocity-Proxy
   - Plugin prüft die SQLite-Datenbank
   - Wenn noch nicht akzeptiert → Resourcepack wird gesendet
   
2. **Resourcepack-Status**:
   - ✅ **Akzeptiert & Heruntergeladen**: Status wird in DB gespeichert
   - ❌ **Abgelehnt**: Spieler wird gekickt (wenn `kickOnDecline: true`)
   - ⚠️ **Download fehlgeschlagen**: Spieler wird gekickt (wenn `kickOnFailedDownload: true`)

3. **Server-Wechsel**: Spieler wechselt zwischen Backend-Servern
   - Plugin prüft DB → Pack bereits akzeptiert
   - **Kein erneuter Download nötig!**

4. **Rejoin**: Spieler joint erneut auf den Proxy
   - Wenn `onlyOnFirstJoin: true` → Kein erneuter Download
   - Wenn `onlyOnFirstJoin: false` → Pack wird erneut gesendet

## Datenbank

Das Plugin verwendet SQLite zur Speicherung:
- **Speicherort**: `plugins/velocitypacker/resourcepack.db`
- **Gespeicherte Daten**: 
  - UUID des Spielers
  - Akzeptanz-Status (ja/nein)
  - Zeitstempel der letzten Aktualisierung

## Technische Details

- **Velocity API**: 3.4.0-SNAPSHOT
- **Kotlin**: 2.0.20-Beta1
- **SQLite**: 3.45.0.0
- **SnakeYAML**: 2.2

## Build

```bash
./gradlew clean build
```

Die kompilierte JAR findest du in `build/libs/Velocitypacker-0.1.jar`

**Wichtig**: Das Plugin verwendet das Shadow-Plugin, um alle Dependencies (Kotlin, SQLite, SnakeYAML) in die JAR zu bundlen. Die finale JAR ist ca. 15 MB groß und enthält alles was benötigt wird.

## Lizenz

Erstellt von thelion
