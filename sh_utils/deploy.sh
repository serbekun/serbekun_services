#!/bin/bash
set -e

cd "$(dirname "$0")"

echo "=== [deploy] Building serbekun-service (gradle shadowjar) ==="
export PATH=/opt/gradle/gradle-8.6/bin:$PATH
gradle shadowjar

JAR="build/libs/serbekun-service-1.0.0-all.jar"
echo "=== [deploy] Deploying JAR to DMZ ==="
scp "$JAR" dmz:/home/sergei/serbekun_service/

echo "=== [deploy] Provisioning DMZ (yt-dlp + Deno) ==="
scp provision_dmz.sh dmz:/tmp/
ssh dmz "bash /tmp/provision_dmz.sh"

echo "=== [deploy] Restarting service on DMZ ==="
ssh dmz "bash -c '
  tmux send-keys -t ss_serbekun_com C-c
  sleep 3
  tmux send-keys -t ss_serbekun_com \"java -jar /home/sergei/serbekun_service/serbekun-service-1.0.0-all.jar\" Enter
'"
echo "=== [deploy] Deployment complete! ==="
echo "Check logs: ssh dmz \"tmux attach -t ss_serbekun_com\""
