#!/usr/bin/env bash
set -euo pipefail

systemctl restart hsi-utilities-backend
systemctl reload nginx
systemctl status hsi-utilities-backend --no-pager
nginx -t
