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
