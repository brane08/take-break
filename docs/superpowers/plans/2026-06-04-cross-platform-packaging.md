# Cross-Platform Packaging Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the single generic zip with three platform-specific zips (macOS `.app` bundle, Linux `.desktop` + installer, Windows VBScript silent launcher) produced by a single `mvn package` run.

**Architecture:** Three maven-assembly-plugin executions each read a platform descriptor from `assembly/`. Static macOS app bundle files live under `src/main/assembly/macos/`. Platform scripts live under `src/main/scripts/`. The existing fat JAR is referenced as the project artifact in every descriptor. No Java code changes.

**Tech Stack:** Maven assembly plugin (already in use), bash, cmd/bat, VBScript, macOS `sips`+`iconutil` (built-in), XML.

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

### Task 1: Generate coffee.icns and create macOS app bundle static files

**Files:**
- Create: `src/main/assembly/macos/Contents/Info.plist`
- Create: `src/main/assembly/macos/Contents/MacOS/TakeBreak`
- Create: `src/main/assembly/macos/Contents/Resources/coffee.icns`

- [ ] **Step 1: Create directory structure**

```bash
mkdir -p src/main/assembly/macos/Contents/MacOS
mkdir -p src/main/assembly/macos/Contents/Resources
```

- [ ] **Step 2: Generate coffee.icns from coffee.png**

Run from the project root:

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
cd ../../..
```

Expected: `src/main/assembly/macos/Contents/Resources/coffee.icns` now exists.

```bash
ls -lh src/main/assembly/macos/Contents/Resources/coffee.icns
```

Expected: file present, size > 0.

- [ ] **Step 3: Create Info.plist**

Create `src/main/assembly/macos/Contents/Info.plist`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
    "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleName</key>             <string>TakeBreak</string>
    <key>CFBundleIdentifier</key>       <string>com.github.brane08.fx.takebreak</string>
    <key>CFBundleVersion</key>          <string>1.0</string>
    <key>CFBundleExecutable</key>       <string>TakeBreak</string>
    <key>CFBundleIconFile</key>         <string>coffee</string>
    <key>CFBundlePackageType</key>      <string>APPL</string>
    <key>LSUIElement</key>              <true/>
    <key>NSHighResolutionCapable</key>  <true/>
</dict>
</plist>
```

Note: `LSUIElement=true` hides the app from the Dock — essential for a tray-only app. `CFBundleIconFile` references the icon **without** the `.icns` extension.

- [ ] **Step 4: Create TakeBreak launcher script**

Create `src/main/assembly/macos/Contents/MacOS/TakeBreak`:

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

Then make it executable:

```bash
chmod +x src/main/assembly/macos/Contents/MacOS/TakeBreak
```

- [ ] **Step 5: Verify directory structure**

```bash
find src/main/assembly/macos -type f | sort
```

Expected:
```
src/main/assembly/macos/Contents/Info.plist
src/main/assembly/macos/Contents/MacOS/TakeBreak
src/main/assembly/macos/Contents/Resources/coffee.icns
```

- [ ] **Step 6: Commit**

```bash
git add src/main/assembly/
git commit -m "build: add macOS .app bundle static files (Info.plist, launcher, coffee.icns)"
```

---

### Task 2: Create Linux scripts

**Files:**
- Modify: `src/main/scripts/run.sh`
- Create: `src/main/scripts/take-break.desktop`
- Create: `src/main/scripts/install.sh`

- [ ] **Step 1: Replace run.sh with version-checking version**

Overwrite `src/main/scripts/run.sh` with:

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
java -Dglass.gtk.uiScale=auto -Dprism.allowhidpi=true \
    -jar "$DIR/take-break-app.jar" >> "$DIR/take-break.log" 2>&1
```

- [ ] **Step 2: Create take-break.desktop**

Create `src/main/scripts/take-break.desktop`:

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

Note: `/opt/take-break` is a placeholder. `install.sh` (next step) rewrites it to the actual install location using `sed`.

- [ ] **Step 3: Create install.sh**

Create `src/main/scripts/install.sh`:

```bash
#!/bin/bash
INSTALL_DIR="$HOME/.local/share/take-break"
mkdir -p "$INSTALL_DIR"
mkdir -p "$HOME/.local/share/applications"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cp "$SCRIPT_DIR/take-break-app.jar" "$INSTALL_DIR/"
cp "$SCRIPT_DIR/icon.png" "$INSTALL_DIR/"
sed "s|/opt/take-break|$INSTALL_DIR|g" "$SCRIPT_DIR/take-break.desktop" \
    > "$HOME/.local/share/applications/take-break.desktop"
echo "Installed. Launch from your application menu or run:"
echo "  java -jar $INSTALL_DIR/take-break-app.jar"
```

- [ ] **Step 4: Commit**

```bash
git add src/main/scripts/run.sh \
        src/main/scripts/take-break.desktop \
        src/main/scripts/install.sh
git commit -m "build: Linux scripts — run.sh version check, .desktop entry, install.sh"
```

---

### Task 3: Create Windows scripts

**Files:**
- Modify: `src/main/scripts/run.cmd`
- Create: `src/main/scripts/take-break.vbs`

- [ ] **Step 1: Replace run.cmd with version-checking version**

Overwrite `src/main/scripts/run.cmd` with:

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

Note: `%~dp0` expands to the directory of `run.cmd` so the jar is found regardless of where the user runs the script from. `javaw` suppresses the console window.

- [ ] **Step 2: Create take-break.vbs**

Create `src/main/scripts/take-break.vbs`:

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

`shell.Run(..., 0, True)` — window style 0 = hidden, True = wait for exit. If `run.cmd` exits non-zero (Java missing or wrong version), a MsgBox appears.

- [ ] **Step 3: Commit**

```bash
git add src/main/scripts/run.cmd \
        src/main/scripts/take-break.vbs
git commit -m "build: Windows scripts — run.cmd version check, take-break.vbs silent launcher"
```

---

### Task 4: Create assembly descriptors

**Files:**
- Create: `assembly/macos-dist.xml`
- Create: `assembly/linux-dist.xml`
- Create: `assembly/windows-dist.xml`

- [ ] **Step 1: Create macos-dist.xml**

Create `assembly/macos-dist.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<assembly xmlns="http://maven.apache.org/ASSEMBLY/2.1.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/ASSEMBLY/2.1.0 http://maven.apache.org/xsd/assembly-2.1.0.xsd">
    <id>macos-dist</id>
    <formats>
        <format>zip</format>
    </formats>
    <includeBaseDirectory>false</includeBaseDirectory>
    <files>
        <file>
            <source>${project.build.directory}/${project.artifactId}-app.jar</source>
            <outputDirectory>TakeBreak.app/Contents/Java</outputDirectory>
            <destName>take-break-app.jar</destName>
        </file>
    </files>
    <fileSets>
        <!-- Info.plist and Resources/coffee.icns — standard permissions -->
        <fileSet>
            <directory>src/main/assembly/macos/Contents</directory>
            <outputDirectory>TakeBreak.app/Contents</outputDirectory>
            <excludes>
                <exclude>MacOS/**</exclude>
            </excludes>
        </fileSet>
        <!-- TakeBreak launcher — must be executable (0755) -->
        <fileSet>
            <directory>src/main/assembly/macos/Contents/MacOS</directory>
            <outputDirectory>TakeBreak.app/Contents/MacOS</outputDirectory>
            <fileMode>0755</fileMode>
        </fileSet>
    </fileSets>
</assembly>
```

- [ ] **Step 2: Create linux-dist.xml**

Create `assembly/linux-dist.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<assembly xmlns="http://maven.apache.org/ASSEMBLY/2.1.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/ASSEMBLY/2.1.0 http://maven.apache.org/xsd/assembly-2.1.0.xsd">
    <id>linux-dist</id>
    <formats>
        <format>zip</format>
    </formats>
    <includeBaseDirectory>false</includeBaseDirectory>
    <files>
        <file>
            <source>${project.build.directory}/${project.artifactId}-app.jar</source>
            <outputDirectory>take-break-linux</outputDirectory>
            <destName>take-break-app.jar</destName>
        </file>
        <file>
            <source>src/main/resources/coffee.png</source>
            <outputDirectory>take-break-linux</outputDirectory>
            <destName>icon.png</destName>
        </file>
    </files>
    <fileSets>
        <fileSet>
            <directory>src/main/scripts</directory>
            <outputDirectory>take-break-linux</outputDirectory>
            <includes>
                <include>run.sh</include>
                <include>install.sh</include>
                <include>take-break.desktop</include>
            </includes>
            <fileMode>0755</fileMode>
        </fileSet>
    </fileSets>
</assembly>
```

- [ ] **Step 3: Create windows-dist.xml**

Create `assembly/windows-dist.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<assembly xmlns="http://maven.apache.org/ASSEMBLY/2.1.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/ASSEMBLY/2.1.0 http://maven.apache.org/xsd/assembly-2.1.0.xsd">
    <id>windows-dist</id>
    <formats>
        <format>zip</format>
    </formats>
    <includeBaseDirectory>false</includeBaseDirectory>
    <files>
        <file>
            <source>${project.build.directory}/${project.artifactId}-app.jar</source>
            <outputDirectory>take-break-windows</outputDirectory>
            <destName>take-break-app.jar</destName>
        </file>
    </files>
    <fileSets>
        <fileSet>
            <directory>src/main/scripts</directory>
            <outputDirectory>take-break-windows</outputDirectory>
            <includes>
                <include>run.cmd</include>
                <include>take-break.vbs</include>
            </includes>
        </fileSet>
    </fileSets>
</assembly>
```

- [ ] **Step 4: Commit**

```bash
git add assembly/macos-dist.xml assembly/linux-dist.xml assembly/windows-dist.xml
git commit -m "build: add macos, linux, windows assembly descriptors"
```

---

### Task 5: Update pom.xml and verify build

**Files:**
- Modify: `pom.xml`
- Delete: `assembly/dist.xml`

- [ ] **Step 1: Replace the dist execution in pom.xml**

In `pom.xml`, find the `<execution>` block with `<id>dist</id>`:

```xml
			<execution>
				<id>dist</id>
				<phase>package</phase>
				<goals>
					<goal>single</goal>
				</goals>
				<configuration>
					<descriptors>
						<descriptor>${project.basedir}/assembly/dist.xml</descriptor>
					</descriptors>
				</configuration>
			</execution>
```

Replace it with three executions:

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

- [ ] **Step 2: Delete dist.xml**

```bash
rm assembly/dist.xml
```

- [ ] **Step 3: Run mvn package**

```bash
mvn package 2>&1 | tail -15
```

Expected: `BUILD SUCCESS`. All 10 tests pass, then assembly runs.

- [ ] **Step 4: Verify three zips exist**

```bash
ls -lh target/take-break-macos.zip target/take-break-linux.zip target/take-break-windows.zip
```

Expected: all three files present, each > 1 MB.

- [ ] **Step 5: Inspect macOS zip contents**

```bash
unzip -l target/take-break-macos.zip
```

Expected output includes:
```
TakeBreak.app/Contents/Info.plist
TakeBreak.app/Contents/MacOS/TakeBreak
TakeBreak.app/Contents/Resources/coffee.icns
TakeBreak.app/Contents/Java/take-break-app.jar
```

- [ ] **Step 6: Inspect Linux zip contents**

```bash
unzip -l target/take-break-linux.zip
```

Expected output includes:
```
take-break-linux/take-break-app.jar
take-break-linux/run.sh
take-break-linux/icon.png
take-break-linux/take-break.desktop
take-break-linux/install.sh
```

- [ ] **Step 7: Inspect Windows zip contents**

```bash
unzip -l target/take-break-windows.zip
```

Expected output includes:
```
take-break-windows/take-break-app.jar
take-break-windows/run.cmd
take-break-windows/take-break.vbs
```

- [ ] **Step 8: Verify macOS launcher is executable in the zip**

```bash
unzip -v target/take-break-macos.zip | grep TakeBreak
```

Expected: the `TakeBreak` entry shows permissions `-rwxr-xr-x` or `0755` in the attributes column.

- [ ] **Step 9: Smoke-test the macOS app on macOS**

```bash
cd /tmp
unzip -q ~/incubator/source/take-break/target/take-break-macos.zip
ls TakeBreak.app/Contents/MacOS/TakeBreak
# Launcher should already be executable (set by zip); if not:
chmod +x TakeBreak.app/Contents/MacOS/TakeBreak
# Run it to test version check works:
TakeBreak.app/Contents/MacOS/TakeBreak
cd -
```

Expected: app launches (system tray icon appears) OR if Java path differs, an osascript alert appears. Either confirms the launcher script executes correctly.

- [ ] **Step 10: Commit**

```bash
git add pom.xml
git rm assembly/dist.xml
git commit -m "build: replace dist zip with macos/linux/windows platform zips"
```
