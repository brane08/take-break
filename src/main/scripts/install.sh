#!/bin/bash
INSTALL_DIR="$HOME/.local/share/take-break"
mkdir -p "$INSTALL_DIR"
mkdir -p "$HOME/.local/share/applications"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cp "$SCRIPT_DIR/take-break-app.jar" "$INSTALL_DIR/"
cp "$SCRIPT_DIR/icon.png" "$INSTALL_DIR/"
cp "$SCRIPT_DIR/run.sh" "$INSTALL_DIR/"
chmod +x "$INSTALL_DIR/run.sh"
sed "s|/opt/take-break|$INSTALL_DIR|g" "$SCRIPT_DIR/take-break.desktop" \
    > "$HOME/.local/share/applications/take-break.desktop"
echo "Installed. Launch from your application menu or run:"
echo "  $INSTALL_DIR/run.sh"
