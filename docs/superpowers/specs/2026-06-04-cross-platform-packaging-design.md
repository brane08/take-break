# Cross-Platform Packaging — Design Spec

## Goal

Produce three platform-specific zip distributions from a single `mvn package` run. Each zip is self-contained for its platform (requires Java 21+ pre-installed) and includes a native-feel launcher: a double-clickable `.app` bundle on macOS, an XDG `.desktop` entry on Linux, and a silent VBScript launcher on Windows. All launchers validate Java presence and version before starting.

## Scope

**In scope:** macOS `.app` bundle in zip, Linux zip with `.desktop` + `install.sh`, Windows zip with `.vbs` silent launcher. Java version validation in all launchers. `coffee.icns` generated once from existing `coffee.png` and committed.

**Out of scope:** Bundling a JRE (users must have Java 21+), OS-level installers (.dmg, .deb, .rpm, .msi), code signing / notarization, CI automation.

---

## Architecture

`mvn package` runs three `maven-assembly-plugin` executions (replacing the existing `dist` execution), each producing a platform zip from its own descriptor. Platform-specific static files live under `src/main/assembly/<platform>/` and scripts under `src/main/scripts/`. The fat JAR (`take-break-app.jar`) is referenced from each descriptor as the project artifact.

---

## File Map

| Action | File |
|--------|------|
| Delete | `assembly/dist.xml` |
| Create | `assembly/macos-dist.xml` |
| Create | `assembly/linux-dist.xml` |
| Create | `assembly/windows-dist.xml` |
| Create | `src/main/assembly/macos/Contents/Info.plist` |
| Create | `src/main/assembly/macos/Contents/MacOS/TakeBreak` |
| Create | `src/main/assembly/macos/Contents/Resources/coffee.icns` |
| Modify | `src/main/scripts/run.sh` |
| Modify | `src/main/scripts/run.cmd` |
| Create | `src/main/scripts/take-break.vbs` |
| Create | `src/main/scripts/take-break.desktop` |
| Create | `src/main/scripts/install.sh` |
| Modify | `pom.xml` |

---

## Components

### macOS zip — `take-break-macos.zip`

Unpacks to:
```
TakeBreak.app/
  Contents/
    Info.plist
    MacOS/
      TakeBreak          ← executable launcher script
    Resources/
      coffee.icns
    Java/
      take-break-app.jar
```

Users drag `TakeBreak.app` to `/Applications` and double-click.

**Info.plist:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
    "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleName</key>         <string>TakeBreak</string>
    <key>CFBundleIdentifier</key>   <string>com.github.brane08.fx.takebreak</string>
    <key>CFBundleVersion</key>      <string>1.0</string>
    <key>CFBundleExecutable</key>   <string>TakeBreak</string>
    <key>CFBundleIconFile</key>     <string>coffee</string>
    <key>CFBundlePackageType</key>  <string>APPL</string>
    <key>LSUIElement</key>          <true/>
    <key>NSHighResolutionCapable</key> <true/>
</dict>
</plist>
```

- `LSUIElement=true` — hides from Dock (tray-only app)
- `NSHighResolutionCapable=true` — enables Retina rendering

**TakeBreak launcher script:**
```bash
#!/bin/bash
if ! command -v java &>/dev/null; then
    osascript -e 'display alert "Take Break" message "Java not found. Install Java 21+ from adoptium.net"'
    exit 1
fi
VERSION=$(java -version 2>&1 | awk -F'"' '/version/{print $2}' | cut -d. -f1)
if [ "$VERSION" -lt 21 ] 2>/dev/null; then
    osascript -e 'display alert "Take Break" message "Java 21+ required. Found version: '"$VERSION"'"'
    exit 1
fi
DIR="$(cd "$(dirname "$0")" && pwd)"
exec java -Dprism.allowhidpi=true -jar "$DIR/../Java/take-break-app.jar"
```

**coffee.icns generation** (run once on macOS, result committed):
```bash
cd src/main/resources
mkdir -p TakeBreak.iconset
sips -z 16 16   coffee.png --out TakeBreak.iconset/icon_16x16.png
sips -z 32 32   coffee.png --out TakeBreak.iconset/icon_16x16@2x.png
sips -z 32 32   coffee.png --out TakeBreak.iconset/icon_32x32.png
sips -z 64 64   coffee.png --out TakeBreak.iconset/icon_32x32@2x.png
sips -z 128 128 coffee.png --out TakeBreak.iconset/icon_128x128.png
sips -z 256 256 coffee.png --out TakeBreak.iconset/icon_128x128@2x.png
sips -z 256 256 coffee.png --out TakeBreak.iconset/icon_256x256.png
sips -z 512 512 coffee.png --out TakeBreak.iconset/icon_256x256@2x.png
sips -z 512 512 coffee.png --out TakeBreak.iconset/icon_512x512.png
iconutil -c icns TakeBreak.iconset -o ../assembly/macos/Contents/Resources/coffee.icns
rm -rf TakeBreak.iconset
```

---

### Linux zip — `take-break-linux.zip`

Unpacks to:
```
take-break-linux/
  take-break-app.jar
  run.sh
  icon.png
  take-break.desktop
  install.sh
```

**run.sh** (updated with version check):
```bash
#!/bin/bash
if ! command -v java &>/dev/null; then
    echo "ERROR: Java not found. Install Java 21+ from adoptium.net" >&2
    exit 1
fi
VERSION=$(java -version 2>&1 | awk -F'"' '/version/{print $2}' | cut -d. -f1)
if [ "$VERSION" -lt 21 ] 2>/dev/null; then
    echo "ERROR: Java 21+ required. Found: $VERSION" >&2
    exit 1
fi
DIR="$(cd "$(dirname "$0")" && pwd)"
java -Dglass.gtk.uiScale=auto -Dprism.allowhidpi=true -jar "$DIR/take-break-app.jar" \
    >> "$DIR/take-break.log" 2>&1
```

**take-break.desktop:**
```ini
[Desktop Entry]
Type=Application
Name=Take Break
Comment=Reminder to take regular breaks
Exec=java -jar /opt/take-break/take-break-app.jar
Icon=/opt/take-break/icon.png
Terminal=false
Categories=Utility;
StartupNotify=false
```

**install.sh** (rewrites paths to wherever the user unpacked):
```bash
#!/bin/bash
INSTALL_DIR="$HOME/.local/share/take-break"
mkdir -p "$INSTALL_DIR"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cp "$SCRIPT_DIR/take-break-app.jar" "$INSTALL_DIR/"
cp "$SCRIPT_DIR/icon.png" "$INSTALL_DIR/"
sed "s|/opt/take-break|$INSTALL_DIR|g" "$SCRIPT_DIR/take-break.desktop" \
    > "$HOME/.local/share/applications/take-break.desktop"
echo "Installed. Launch from your application menu or run: java -jar $INSTALL_DIR/take-break-app.jar"
```

---

### Windows zip — `take-break-windows.zip`

Unpacks to:
```
take-break-windows/
  take-break-app.jar
  run.cmd
  take-break.vbs
```

**run.cmd** (updated with version check):
```cmd
@echo off
where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ERROR: Java not found. Install Java 21+ from adoptium.net
    pause
    exit /b 1
)
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VER=%%v
)
set JAVA_VER=%JAVA_VER:"=%
for /f "delims=." %%m in ("%JAVA_VER%") do set JAVA_MAJOR=%%m
if %JAVA_MAJOR% LSS 21 (
    echo ERROR: Java 21+ required. Found: %JAVA_VER%
    pause
    exit /b 1
)
set JAVA_PATH=javaw
if defined JAVA_HOME set JAVA_PATH=%JAVA_HOME%\bin\javaw
%JAVA_PATH% -Dprism.allowhidpi=true -jar "%~dp0take-break-app.jar"
```

**take-break.vbs** (silent launcher — no console window):
```vbs
Set fso = CreateObject("Scripting.FileSystemObject")
Set shell = CreateObject("WScript.Shell")
dir = fso.GetParentFolderName(WScript.ScriptFullName)
code = shell.Run("cmd /c """ & dir & "\run.cmd""", 0, True)
If code <> 0 Then
    MsgBox "Take Break failed to start. Run run.cmd in a terminal for details.", _
           vbExclamation, "Take Break"
End If
```

Users double-click `take-break.vbs` for silent launch. `run.cmd` remains available for debugging.

---

## pom.xml Changes

Remove the `dist` execution. Add three executions:

```xml
<execution>
    <id>macos-dist</id>
    <phase>package</phase>
    <goals><goal>single</goal></goals>
    <configuration>
        <finalName>take-break-macos</finalName>
        <appendAssemblyId>false</appendAssemblyId>
        <descriptors>
            <descriptor>${project.basedir}/assembly/macos-dist.xml</descriptor>
        </descriptors>
    </configuration>
</execution>
<execution>
    <id>linux-dist</id>
    <phase>package</phase>
    <goals><goal>single</goal></goals>
    <configuration>
        <finalName>take-break-linux</finalName>
        <appendAssemblyId>false</appendAssemblyId>
        <descriptors>
            <descriptor>${project.basedir}/assembly/linux-dist.xml</descriptor>
        </descriptors>
    </configuration>
</execution>
<execution>
    <id>windows-dist</id>
    <phase>package</phase>
    <goals><goal>single</goal></goals>
    <configuration>
        <finalName>take-break-windows</finalName>
        <appendAssemblyId>false</appendAssemblyId>
        <descriptors>
            <descriptor>${project.basedir}/assembly/windows-dist.xml</descriptor>
        </descriptors>
    </configuration>
</execution>
```

---

## Assembly Descriptors

Each descriptor includes:
1. The project artifact (fat JAR) placed at its platform-specific path
2. A `fileSet` picking up platform scripts from `src/main/scripts/`
3. For macOS: a `fileSet` for `src/main/assembly/macos/` with the `TakeBreak` script marked executable (`0755`)

**macos-dist.xml** structure:
- Artifact → `TakeBreak.app/Contents/Java/take-break-app.jar`
- FileSet `src/main/assembly/macos/` → `TakeBreak.app/Contents/` (preserves subdirs; sets `fileMode=0755` for `Contents/MacOS/TakeBreak` via a `<file>` entry)

**linux-dist.xml** structure:
- Artifact → `take-break-linux/take-break-app.jar`
- FileSet `src/main/scripts/` includes `run.sh`, `take-break.desktop`, `install.sh` → `take-break-linux/` with `run.sh` and `install.sh` at `fileMode=0755`
- File `src/main/resources/coffee.png` → `take-break-linux/icon.png`

**windows-dist.xml** structure:
- Artifact → `take-break-windows/take-break-app.jar`
- FileSet `src/main/scripts/` includes `run.cmd`, `take-break.vbs` → `take-break-windows/`

---

## Test Notes

No automated tests — packaging is verified manually:
- `mvn package` produces `take-break-macos.zip`, `take-break-linux.zip`, `take-break-windows.zip` in `target/`
- Unzip macOS artifact: verify `TakeBreak.app/` structure, `chmod +x Contents/MacOS/TakeBreak`, double-click on macOS
- Unzip Linux artifact: verify `run.sh` is executable, `.desktop` file has correct structure
- Unzip Windows artifact: verify `take-break.vbs` exists alongside `run.cmd`
