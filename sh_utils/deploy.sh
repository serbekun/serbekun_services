#!/bin/bash
set -e

# Get the directory where this script resides, then go up one level to the project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/.."

echo "=== [deploy] Building serbekun-service (gradle shadowjar) ==="
export PATH=/opt/gradle/gradle-8.6/bin:$PATH
gradle shadowjar

# Pick up whatever shadowJar actually produced (name follows the version in
# build.gradle), and deploy it under a stable name so the run command never
# has to change when the version bumps.
JAR="$(ls -t build/libs/*-all.jar | head -n1)"
if [ -z "$JAR" ] || [ ! -f "$JAR" ]; then
  echo "[deploy] ERROR: no *-all.jar found in build/libs/" >&2
  exit 1
fi
echo "=== [deploy] Deploying JAR ($JAR) to DMZ ==="
scp "$JAR" dmz:/home/sergei/serbekun_services/serbekun-service.jar


echo "=== [deploy] Provisioning DMZ (yt-dlp + Deno) ==="
scp sh_utils/provision_dmz.sh dmz:/tmp/
ssh dmz "bash /tmp/provision_dmz.sh"

echo "=== [deploy] Restarting service on DMZ ==="
ssh dmz "bash -c '
  tmux send-keys -t ss_serbekun_com C-c
  sleep 3
  tmux send-keys -t ss_serbekun_com \"java -jar /home/sergei/serbekun_services/serbekun-service.jar\" Enter
'"
echo "=== [deploy] Deployment complete! ==="
echo "Check logs: ssh dmz \"tmux attach -t ss_serbekun_com\""
