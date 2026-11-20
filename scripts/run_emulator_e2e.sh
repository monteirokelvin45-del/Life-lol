#!/usr/bin/env bash
set -euo pipefail

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
OUT_DIR="docs/emulator-screenshots"
mkdir -p "$OUT_DIR"

echo "=== [1/6] Preparing Android 14 Emulator Window & Animations ==="
adb wait-for-device
adb shell settings put global window_animation_scale 0.0 || true
adb shell settings put global transition_animation_scale 0.0 || true
adb shell settings put global animator_duration_scale 0.0 || true
adb shell wm dismiss-keyguard || true
adb shell input keyevent 82 || true

echo "=== [2/6] Installing APK with Runtime Permissions (-g) ==="
adb install -r -g "$APK_PATH"

check_no_crash() {
  local stage="$1"
  local pid
  pid="$(adb shell pidof com.lifeos.app | tr -d '\r' || true)"
  if [[ -z "$pid" ]]; then
    local crash_log
    crash_log="$(adb logcat -d -v brief *:E | grep -E "AndroidRuntime|SQLCipher|Room|LifeOs|FATAL" | tail -n 65 || true)"
    local encoded="${crash_log//$'\n'/%0A}"
    echo "::error title=Crash during ${stage}::Process com.lifeos.app died during ${stage}:%0A${encoded}"
    exit 1
  fi
  if adb logcat -d | grep -q "FATAL EXCEPTION"; then
    local crash_log
    crash_log="$(adb logcat -d | awk '/FATAL EXCEPTION/,0' | head -n 65)"
    local encoded="${crash_log//$'\n'/%0A}"
    echo "::error title=Fatal Exception during ${stage}::${encoded}"
    exit 1
  fi
  echo "  [OK] com.lifeos.app alive (PID=${pid}) at stage: ${stage}"
}

tap_by_label_or_fallback() {
  local label="$1"
  local fallback_x="$2"
  local fallback_y="$3"

  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 || true
  adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1 || true

  local coords
  coords="$(python3 - "$label" <<'PY'
import re, sys, xml.etree.ElementTree as ET
target = sys.argv[1].lower()
try:
    tree = ET.parse("/tmp/ui.xml")
    matches = []
    for elem in tree.iter("node"):
        txt = (elem.attrib.get("text") or "").lower()
        desc = (elem.attrib.get("content-desc") or "").lower()
        bounds = elem.attrib.get("bounds") or ""
        if target in txt or target in desc:
            m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
            if m:
                x1, y1, x2, y2 = map(int, m.groups())
                matches.append(((x1 + x2) // 2, (y1 + y2) // 2))
    if matches:
        # Prefer bottom-most match for bottom navigation tabs
        cx, cy = sorted(matches, key=lambda p: p[1])[-1]
        print(f"{cx} {cy}")
except Exception:
    pass
PY
)"
  if [[ -n "$coords" ]]; then
    echo "  -> Tapping '$label' at UIAutomator coords: $coords"
    # shellcheck disable=SC2086
    adb shell input tap $coords
  else
    echo "  -> Tapping '$label' at fallback coords: $fallback_x $fallback_y"
    adb shell input tap "$fallback_x" "$fallback_y"
  fi
  sleep 2
}

capture_screen() {
  local filename="$1"
  adb exec-out screencap -p > "${OUT_DIR}/${filename}"
  echo "  [SCREENSHOT] Saved ${OUT_DIR}/${filename} ($(wc -c < "${OUT_DIR}/${filename}") bytes)"
}

echo "=== [3/6] Launching com.lifeos.app/.MainActivity ==="
adb logcat -c
adb shell am start -W -n com.lifeos.app/.MainActivity
sleep 6
check_no_crash "Initial Cold Boot + SQLCipher + Keystore + Seed"

echo "=== [4/6] Navigating All 5 Tabs & Capturing Real OLED Screenshots ==="
# 1. Aba Hoje (Topo + Roda da Vida)
capture_screen "01-hoje-dashboard.png"
adb shell input swipe 540 1750 540 650 350
sleep 2
capture_screen "02-hoje-roda-da-vida.png"
check_no_crash "Home Tab & Balance Wheel"

# 2. Aba Hábitos + ModalBottomSheet de Detalhes
tap_by_label_or_fallback "Hábitos" 324 2240
capture_screen "03-habitos-heatmaps.png"
tap_by_label_or_fallback "Barra Fixa" 540 850
sleep 2
capture_screen "04-habito-detalhe-bottomsheet.png"
check_no_crash "Habits Tab & Detail BottomSheet"
adb shell input keyevent 4
sleep 1

# 3. Aba Treino (Execução + Seu Progresso Fisiológico)
tap_by_label_or_fallback "Treino" 540 2240
capture_screen "05-treino-execucao.png"
adb shell input swipe 540 1850 540 450 350
sleep 2
capture_screen "06-treino-progresso-kalo.png"
check_no_crash "Workout Tab & Physiology Charts"

# 4. Aba Estudos (Curva Bezier + Ebbinghaus + Leitura Ativa + OCR)
tap_by_label_or_fallback "Estudos" 756 2240
capture_screen "07-estudos-curva-ebbinghaus.png"
adb shell input swipe 540 1800 540 500 350
sleep 2
capture_screen "08-estudos-leitura-ocr.png"
check_no_crash "Study Tab & Active Reading"

# 5. Aba Carteira (Wallet Metálico + 2s NLP + Burn Matrix)
tap_by_label_or_fallback "Carteira" 972 2240
capture_screen "09-carteira-wallet.png"
adb shell input swipe 540 1850 540 450 350
sleep 2
capture_screen "10-carteira-burn-matrix.png"
check_no_crash "Finance Tab & Burn Matrix"

echo "=== [5/6] Verifying Encrypted SQLCipher Database File on Device ==="
DB_LS="$(adb shell run-as com.lifeos.app ls -la databases/ || true)"
echo "$DB_LS"
if ! echo "$DB_LS" | grep -q "life_os_encrypted.db"; then
  echo "::error title=Missing Encrypted DB::life_os_encrypted.db was not created in app sandbox!"
  exit 1
fi

echo "=== [6/6] E2E Runtime Verification Passed with 0 Crashes & 10 Screenshots! ==="
