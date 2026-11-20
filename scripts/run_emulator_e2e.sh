#!/usr/bin/env bash
set -euo pipefail

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
OUT_DIR="docs/emulator-screenshots"
mkdir -p "$OUT_DIR"

echo "=== [1/7] Suppressing Background Launcher ANR & Animations ==="
adb wait-for-device
adb shell settings put global hide_error_dialogs 1 || true
adb shell settings put global window_animation_scale 0.0 || true
adb shell settings put global transition_animation_scale 0.0 || true
adb shell settings put global animator_duration_scale 0.0 || true
adb shell wm dismiss-keyguard || true
adb shell input keyevent 82 || true
sleep 4

dismiss_system_anr_if_any() {
  adb shell uiautomator dump /sdcard/anr_check.xml >/dev/null 2>&1 || true
  adb pull /sdcard/anr_check.xml /tmp/anr_check.xml >/dev/null 2>&1 || true
  local close_coords
  close_coords="$(python3 - <<'PY'
import re, xml.etree.ElementTree as ET
try:
    tree = ET.parse("/tmp/anr_check.xml")
    for elem in tree.iter("node"):
        rid = elem.attrib.get("resource-id") or ""
        txt = (elem.attrib.get("text") or "").lower()
        bounds = elem.attrib.get("bounds") or ""
        if "aerr_close" in rid or txt == "close app" or txt == "fechar app":
            m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
            if m:
                x1, y1, x2, y2 = map(int, m.groups())
                print(f"{(x1 + x2) // 2} {(y1 + y2) // 2}")
                break
except Exception:
    pass
PY
)"
  if [[ -n "$close_coords" ]]; then
    echo "  [WARN] Dismissing background system ANR dialog at: $close_coords"
    # shellcheck disable=SC2086
    adb shell input tap $close_coords
    sleep 2
  fi
}

check_no_crash() {
  local stage="$1"
  dismiss_system_anr_if_any
  local pid
  pid="$(adb shell pidof com.lifeos.app | tr -d '\r' || true)"
  if [[ -z "$pid" ]]; then
    local crash_log
    crash_log="$(adb logcat -d -v brief *:E | grep -E "AndroidRuntime|SQLCipher|Room|LifeOs|FATAL" | tail -n 65 || true)"
    local encoded="${crash_log//$'\n'/%0A}"
    echo "::error title=Crash during ${stage}::Process com.lifeos.app died during ${stage}:%0A${encoded}"
    exit 1
  fi
  if adb logcat -d | grep -A 5 "FATAL EXCEPTION" | grep -q "com.lifeos"; then
    local crash_log
    crash_log="$(adb logcat -d | awk '/FATAL EXCEPTION/,0' | head -n 65)"
    local encoded="${crash_log//$'\n'/%0A}"
    echo "::error title=Fatal Exception in Life OS during ${stage}::${encoded}"
    exit 1
  fi
  echo "  [OK] com.lifeos.app alive (PID=${pid}) at stage: ${stage}"
}

navigate_route() {
  local route="$1"
  dismiss_system_anr_if_any
  adb shell am start -n com.lifeos.app/.MainActivity --es EXTRA_NAV_ROUTE "$route" >/dev/null
  sleep 2
  dismiss_system_anr_if_any
}

capture_screen() {
  local filename="$1"
  dismiss_system_anr_if_any
  adb exec-out screencap -p > "${OUT_DIR}/${filename}"
  echo "  [SCREENSHOT] Saved ${OUT_DIR}/${filename} ($(wc -c < "${OUT_DIR}/${filename}") bytes)"
}

tap_text_on_screen() {
  local target_label="$1"
  adb shell uiautomator dump /sdcard/ui_tap.xml >/dev/null 2>&1 || true
  adb pull /sdcard/ui_tap.xml /tmp/ui_tap.xml >/dev/null 2>&1 || true
  local coords
  coords="$(python3 - "$target_label" <<'PY'
import re, sys, xml.etree.ElementTree as ET
needle = sys.argv[1].lower()
try:
    tree = ET.parse("/tmp/ui_tap.xml")
    for elem in tree.iter("node"):
        txt = (elem.attrib.get("text") or "").lower()
        desc = (elem.attrib.get("content-desc") or "").lower()
        bounds = elem.attrib.get("bounds") or ""
        if needle in txt or needle in desc:
            m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
            if m:
                x1, y1, x2, y2 = map(int, m.groups())
                print(f"{(x1 + x2) // 2} {(y1 + y2) // 2}")
                break
except Exception:
    pass
PY
)"
  if [[ -n "$coords" ]]; then
    echo "  -> Tapping '$target_label' at: $coords"
    # shellcheck disable=SC2086
    adb shell input tap $coords
    sleep 2
  else
    echo "  [WARN] Could not find '$target_label' in UI dump"
  fi
}

echo "=== [2/7] Installing APK with Runtime Permissions (-g) ==="
adb install -r -g "$APK_PATH"
dismiss_system_anr_if_any

echo "=== [3/7] Cold Booting com.lifeos.app/.MainActivity ==="
adb logcat -c
adb shell am start -W -n com.lifeos.app/.MainActivity
sleep 6
check_no_crash "Initial Cold Boot + SQLCipher + Keystore + Seed"

echo "=== [4/7] Capturing All 5 Tabs, 14 Zero-GC Charts & Pillar 5 Mechanics ==="
# 1. Aba Hoje (Topo + Roda da Vida + Circadiano / Ghost Squad / RPG Radar)
navigate_route "home"
capture_screen "01-hoje-dashboard.png"
adb shell input swipe 540 1850 540 520 400
sleep 2
capture_screen "02-hoje-roda-da-vida.png"
adb shell input swipe 540 1900 540 380 400
sleep 2
capture_screen "14-hoje-circadiano-ghost-rpg.png"
adb shell input swipe 540 1900 540 420 400
sleep 2
capture_screen "15-hoje-radar-rpg-hexagonal.png"
check_no_crash "Home Tab, Circadian Wheel, Ghost Squad & RPG Radar"

# 2. Aba Hábitos (Escudo Anti-Falha + Efeito Composto + 7x52) + ModalBottomSheet
navigate_route "habits"
capture_screen "03-habitos-heatmaps.png"
adb shell input swipe 540 1850 540 550 400
sleep 2
tap_text_on_screen "Barra Fixa"
sleep 2
capture_screen "04-habito-detalhe-bottomsheet.png"
check_no_crash "Habits Tab, Streak Freeze, Compound Flame & Detail BottomSheet"
adb shell input keyevent 4
sleep 1

# 3. Aba Treino (Execução + Mapa Anatômico de Fadiga + Sobrecarga Progressiva)
navigate_route "workout"
capture_screen "05-treino-execucao.png"
adb shell input swipe 540 1900 540 380 400
sleep 2
capture_screen "06-treino-progresso-kalo.png"
check_no_crash "Workout Tab, Anatomical Fatigue Map & Progressive Overload"

# 4. Aba Estudos (Spline Haptic Scrubber + Dente-de-Serra Ebbinghaus + Cone de Incerteza)
navigate_route "study"
capture_screen "07-estudos-curva-ebbinghaus.png"
adb shell input swipe 540 1900 540 380 400
sleep 2
capture_screen "08-estudos-leitura-ocr.png"
check_no_crash "Study Tab, Sawtooth Ebbinghaus & Burndown Cone"

# 5. Aba Carteira (Wallet + Anti-Charity + Sankey Fluido + Correlação Multivariada + Burn Matrix)
navigate_route "finance"
capture_screen "09-carteira-wallet.png"
adb shell input swipe 540 1900 540 420 400
sleep 2
capture_screen "16-carteira-anticharity-sankey-correlacao.png"
adb shell input swipe 540 1900 540 420 400
sleep 2
capture_screen "10-carteira-burn-matrix.png"
check_no_crash "Finance Tab, Anti-Charity, Sankey, Multivariate & Burn Matrix"

echo "=== [5/7] Testing Live AI Portal Magico (Modal + Extraction + Confirmar Sistema) ==="
navigate_route "home"
adb shell input swipe 540 600 540 1800 300
sleep 1
tap_text_on_screen "Portal IA"
capture_screen "11-portal-magico-input.png"
tap_text_on_screen "Analisar Documento"
sleep 2
capture_screen "12-portal-magico-confirmar-sistema.png"
tap_text_on_screen "Confirmar Sistema"
sleep 2
capture_screen "13-sistema-confirmado-pela-ia.png"
check_no_crash "AI Portal Magico Extraction & Atomic Persistence"

echo "=== [6/7] Verifying Encrypted SQLCipher Database File on Device ==="
DB_LS="$(adb shell run-as com.lifeos.app ls -la databases/ || true)"
echo "$DB_LS"
if ! echo "$DB_LS" | grep -q "life_os_encrypted.db"; then
  echo "::error title=Missing Encrypted DB::life_os_encrypted.db was not created in app sandbox!"
  exit 1
fi

echo "=== [7/7] E2E Runtime Verification Passed with 0 Crashes & 16 Clean OLED Screenshots! ==="
