#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: sudo bash deploy_frontend.sh /path/to/hospital-maintenance-frontend.zip"
  exit 1
fi

ZIP_FILE="$1"
TARGET_DIR="/var/www/hsi-utilities"
TMP_DIR="/tmp/hsi-utilities-frontend"

mkdir -p "$TARGET_DIR"
rm -rf "$TMP_DIR"
mkdir -p "$TMP_DIR"
unzip -o "$ZIP_FILE" -d "$TMP_DIR" >/dev/null

rm -rf "$TARGET_DIR"/*
if [ -d "$TMP_DIR/browser" ]; then
  cp -R "$TMP_DIR/browser"/* "$TARGET_DIR"/
else
  cp -R "$TMP_DIR"/* "$TARGET_DIR"/
fi

chown -R www-data:www-data "$TARGET_DIR"
find "$TARGET_DIR" -type d -exec chmod 755 {} \;
find "$TARGET_DIR" -type f -exec chmod 644 {} \;

nginx -t
systemctl reload nginx

echo "Frontend deployed to $TARGET_DIR"
