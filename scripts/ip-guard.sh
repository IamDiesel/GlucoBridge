#!/usr/bin/env bash
# IP-Guard (IP-5): verhindert, dass Libre/Abbott-Artefakte versioniert werden.
# Aufruf: pre-commit (nur gestagte Dateien) oder CI mit "--all" (alle getrackten).
set -euo pipefail

if [ "${1:-}" = "--all" ] || ! git rev-parse --verify HEAD >/dev/null 2>&1; then
  files=$(git ls-files)
else
  files=$(git diff --cached --name-only --diff-filter=ACM)
fi

bad=0
content_re='BEGIN (RSA )?PRIVATE KEY|ghost_aes_key|adcskb_gcm|0x8b0[0-9a-fA-F]{3}'

for f in $files; do
  case "$f" in
    *.so|data/source-ale/*|*libb11bb8*)
      echo "IP-GUARD: verbotener Pfad/Datei -> $f"; bad=1;;
  esac
  [ -f "$f" ] || continue
  case "$f" in
    scripts/ip-guard.sh|.gitignore) continue;;   # benennen die Muster selbst als Text
  esac
  if grep -EiIl "$content_re" "$f" >/dev/null 2>&1; then
    echo "IP-GUARD: verdaechtiger Inhalt -> $f"; bad=1
  fi
done

if [ "$bad" -ne 0 ]; then
  echo "IP-GUARD: ABBRUCH — IP-Artefakt erkannt. Nichts committen/pushen."
  exit 1
fi
echo "IP-GUARD: ok"
