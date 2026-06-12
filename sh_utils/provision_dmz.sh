#!/bin/bash
set -e

echo "--- Installing yt-dlp ---"
pip3 install --break-system-packages --quiet yt-dlp yt-dlp-ejs 2>&1 | tail -2

echo "--- Installing Deno ---"
if ! which deno >/dev/null 2>&1 && ! test -f "$HOME/.deno/bin/deno"; then
    mkdir -p "$HOME/.deno/bin"
    curl -fsSL https://github.com/denoland/deno/releases/latest/download/deno-x86_64-unknown-linux-gnu.zip -o /tmp/deno.zip
    python3 << 'PYEOF'
import zipfile
zipfile.ZipFile('/tmp/deno.zip').extract('deno', '/home/sergei/.deno/bin/')
PYEOF
    chmod +x "$HOME/.deno/bin/deno"
    rm /tmp/deno.zip
fi

echo "Deno: $($HOME/.deno/bin/deno --version 2>&1 | head -1)"
echo "yt-dlp: $(yt-dlp --version 2>&1)"
