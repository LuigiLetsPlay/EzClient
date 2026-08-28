#!/usr/bin/env bash
# Safe in-place updater: validates code first, preserves all cape data.
set -Eeuo pipefail
ROOT_DIR="/opt/ezclient-cape-community"
SERVICE="ezclient-community"

if [[ "${EUID}" -ne 0 ]]; then
  echo "Bitte mit: sudo bash update.sh starten"
  exit 1
fi
if [[ ! -f "$ROOT_DIR/server.py" ]]; then
  echo "server.py fehlt in $ROOT_DIR"
  exit 1
fi

/usr/bin/python3 -m py_compile "$ROOT_DIR/server.py"
chown ezclient:ezclient "$ROOT_DIR/server.py"
chmod 640 "$ROOT_DIR/server.py"
systemctl daemon-reload
systemctl restart "$SERVICE"
sleep 1
systemctl is-active --quiet "$SERVICE"
echo "Update erfolgreich. API-Test:"
curl --fail --silent http://127.0.0.1:18765/api/capes
echo
