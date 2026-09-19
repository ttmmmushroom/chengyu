#!/usr/bin/env bash
# 把网页资源同步进 Android 工程的 assets 目录（用于本地构建）。
# 用法：bash scripts/copy-assets.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/app"
DST="$ROOT/android/app/src/main/assets"

mkdir -p "$DST"
cp "$SRC/index.html" \
   "$SRC/pinyin-pro.js" \
   "$SRC/idiom.json" \
   "$SRC/manifest.webmanifest" \
   "$SRC/sw.js" \
   "$SRC/icon-192.png" \
   "$SRC/icon-512.png" \
   "$DST/"

echo "已复制网页资源到: $DST"
echo "下一步：cd android && ./gradlew assembleDebug"
