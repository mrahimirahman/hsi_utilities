#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
APP_USER="hsiapp"
APP_GROUP="hsiapp"
APP_HOME="/opt/hsi-utilities"
APP_ETC="/etc/hsi-utilities"
APP_WWW="/var/www/hsi-utilities"
APP_LOG="/var/log/hsi-utilities"

apt-get update
apt-get install -y nginx unzip curl || true
if ! command -v java >/dev/null 2>&1; then
  apt-get install -y openjdk-8-jre-headless || apt-get install -y openjdk-11-jre-headless
fi

id -u "$APP_USER" >/dev/null 2>&1 || useradd --system --home "$APP_HOME" --shell /usr/sbin/nologin "$APP_USER"

mkdir -p "$APP_HOME" "$APP_ETC" "$APP_WWW" "$APP_LOG"
chown -R "$APP_USER":"$APP_GROUP" "$APP_HOME" "$APP_LOG"
chown -R www-data:www-data "$APP_WWW"

install -m 644 "$BASE_DIR/nginx/hsi-utilities.conf" /etc/nginx/sites-available/hsi-utilities.conf
ln -sf /etc/nginx/sites-available/hsi-utilities.conf /etc/nginx/sites-enabled/hsi-utilities.conf
rm -f /etc/nginx/sites-enabled/default

install -m 644 "$BASE_DIR/systemd/hsi-utilities-backend.service" /etc/systemd/system/hsi-utilities-backend.service

systemctl daemon-reload
nginx -t
systemctl enable nginx
systemctl enable hsi-utilities-backend || true

echo "Runtime installation completed."
