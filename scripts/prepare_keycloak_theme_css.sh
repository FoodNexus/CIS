#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONT_DIR="$ROOT_DIR/civic-platform-frontend"
DIST_DIR="$FRONT_DIR/dist/civic-platform-frontend"
THEME_DIR="${1:-$ROOT_DIR/keycloak/themes/cis-front/login}"
CSS_DEST_DIR="$THEME_DIR/resources/css"
THEME_PROPERTIES="$THEME_DIR/theme.properties"

if [ ! -d "$DIST_DIR" ]; then
  echo "Build output not found at $DIST_DIR"
  echo "Run: (cd \"$FRONT_DIR\" && npm run build)"
  exit 1
fi

STYLE_CSS="$(ls -1t "$DIST_DIR"/styles*.css 2>/dev/null | head -n 1 || true)"
if [ -z "$STYLE_CSS" ]; then
  echo "No styles*.css found in $DIST_DIR"
  exit 1
fi

mkdir -p "$CSS_DEST_DIR"
cp "$STYLE_CSS" "$CSS_DEST_DIR/login.css"

if [ ! -f "$THEME_PROPERTIES" ]; then
  cat > "$THEME_PROPERTIES" <<'EOF'
parent=keycloak
styles=css/login.css
EOF
fi

echo "Copied:"
echo "  $STYLE_CSS"
echo "to:"
echo "  $CSS_DEST_DIR/login.css"
echo
echo "Theme properties:"
echo "  $THEME_PROPERTIES"
