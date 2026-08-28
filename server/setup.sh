#!/usr/bin/env bash
# One-time installer for the local EzClient Community Server.
set -Eeuo pipefail

ROOT_DIR="/opt/ezclient-cape-community"
UNIT_NAME="ezclient-community.service"
ENV_FILE="/etc/ezclient-community.env"

fail() {
  echo
  echo "Setup fehlgeschlagen in Zeile $1. Es wurde nichts still beendet."
  echo "Prüfe: sudo journalctl -u ezclient-community -n 80 --no-pager"
  exit 1
}
trap 'fail $LINENO' ERR

if [[ "${EUID}" -ne 0 ]]; then
  echo "Bitte mit: sudo bash setup.sh starten"
  exit 1
fi
if [[ ! -f "$ROOT_DIR/server.py" ]]; then
  echo "server.py fehlt: $ROOT_DIR/server.py"
  exit 1
fi
if ! /usr/bin/python3 -m py_compile "$ROOT_DIR/server.py"; then
  echo "server.py enthält einen Python-Fehler. Der Dienst wurde nicht geändert."
  exit 1
fi

echo "Stoppe ausschließlich bekannte alte EzClient-Dienste …"
systemctl disable --now ezclient-community.service 2>/dev/null || true
systemctl disable --now ezclient-cape-community.service 2>/dev/null || true
rm -f /etc/systemd/system/ezclient-community.service
rm -f /etc/systemd/system/ezclient-cape-community.service
systemctl daemon-reload

id -u ezclient >/dev/null 2>&1 || useradd --system --home "$ROOT_DIR" --shell /usr/sbin/nologin ezclient
install -d -o ezclient -g ezclient -m 750 "$ROOT_DIR/cape_community_data"
chown -R ezclient:ezclient "$ROOT_DIR"
chmod 750 "$ROOT_DIR"
chmod 640 "$ROOT_DIR/server.py"

# The report panel is intentionally public as requested.
sed -i 's/^ADMIN_HOST = .*/ADMIN_HOST = "0.0.0.0"  # public admin panel/' "$ROOT_DIR/server.py"

if [[ ! -s "$ENV_FILE" ]]; then
  read -r -s -p "Passwort für das öffentliche Report-Panel: " ADMIN_PASSWORD
  echo
  [[ -n "$ADMIN_PASSWORD" ]] || { echo "Ein Passwort ist erforderlich."; exit 1; }
  printf 'EZCLIENT_ADMIN_PASSWORD=%s\n' "$ADMIN_PASSWORD" > "$ENV_FILE"
  unset ADMIN_PASSWORD
fi
chmod 600 "$ENV_FILE"

cat > "/etc/systemd/system/$UNIT_NAME" <<'UNIT'
[Unit]
Description=EzClient Community Server
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=ezclient
Group=ezclient
WorkingDirectory=/opt/ezclient-cape-community
EnvironmentFile=-/etc/ezclient-community.env
ExecStart=/usr/bin/python3 /opt/ezclient-cape-community/server.py
Restart=always
RestartSec=3
StartLimitIntervalSec=0
UMask=027

[Install]
WantedBy=multi-user.target
UNIT

systemctl daemon-reload
systemctl enable --now "$UNIT_NAME"
sleep 1
systemctl is-active --quiet "$UNIT_NAME"

if command -v ufw >/dev/null 2>&1; then
  ufw allow 18765/tcp
  ufw allow 18766/tcp
fi

echo
echo "Fertig. Cape-API:     http://SERVER-IP:18765/api/capes"
echo "Report-Panel:  http://SERVER-IP:18766"
echo "Status:        systemctl status ezclient-community"
