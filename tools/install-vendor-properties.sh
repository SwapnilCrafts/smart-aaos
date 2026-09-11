#!/usr/bin/env bash
#
# Adds this project's custom vendor properties to the emulator's VHAL.
#
# No C++ and no AOSP build are needed. The reference VHAL
# (android.hardware.automotive.vehicle@V3-emulator-service) loads every .json
# file in /vendor/etc/automotive/vhalconfig/ when it starts, so a new property
# is a config change. C++ is only required when a property needs behaviour -
# computing a value, or talking to real hardware or a CAN bus.
#
# Requirements
#   - AVD booted with -writable-system (so /vendor can be remounted)
#   - The app installed as a privileged system app, because vendor properties
#     are gated by CAR_VENDOR_EXTENSION (signature|privileged).
#     Run tools/install-as-privileged-app.sh first.
#
set -euo pipefail

CONFIG=tools/SmartAaosVendorProperties.json
VHAL_CONFIG_DIR=/vendor/etc/automotive/vhalconfig
VHAL_SERVICE=vendor.vehicle-hal-emulator

# property id -> label, for the verification step
PROPS=(
    "557842433:VENDOR_DRIVE_MODE     0x21400001 INT32  READ_WRITE"
    "559939586:VENDOR_SERVICE_DUE_KM 0x21600002 FLOAT  READ"
    "554696707:VENDOR_BATTERY_HEALTH 0x21100003 STRING READ"
)

step() { printf '\n== %s\n' "$1"; }

[ -f "$CONFIG" ] || { echo "config not found: $CONFIG"; exit 1; }
python3 -c "import json,sys; json.load(open('$CONFIG'))" \
    || { echo "$CONFIG is not valid JSON"; exit 1; }

step "Waiting for device"
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done

step "Making /vendor writable"
adb root
adb wait-for-device
adb remount 2>&1 | sed 's/^/  /' || true
if ! adb shell 'touch /vendor/etc/.rw_probe 2>/dev/null && rm /vendor/etc/.rw_probe && echo ok' | grep -q ok; then
    echo "FAILED: /vendor is read-only. Reboot the AVD with -writable-system." >&2
    exit 1
fi
echo "  /vendor is writable"

step "Installing the property config"
adb push "$CONFIG" "$VHAL_CONFIG_DIR/"
adb shell chmod 644 "$VHAL_CONFIG_DIR/$(basename "$CONFIG")"

step "Restarting the VHAL so it reloads its configs"
# Only the HAL needs restarting, not the whole device - the config directory is
# read at service start. CarService picks the new properties up when it
# reconnects to the HAL.
adb shell setprop ctl.restart "$VHAL_SERVICE"
sleep 6

step "Confirming the VHAL loaded the file"
adb logcat -d 2>/dev/null | grep -F "$(basename "$CONFIG")" | tail -2 || \
    echo "  (no log line; check 'adb logcat -d | grep FakeVehicleHardware')"

step "Reading each property back"
fail=0
for entry in "${PROPS[@]}"; do
    id=${entry%%:*}
    label=${entry#*:}
    out=$(adb shell cmd car_service get-property-value "$id" 2>&1 | head -1)
    if echo "$out" | grep -q 'AVAILABLE'; then
        value=$(echo "$out" | sed 's/.*Value: //; s/}.*//')
        printf '  %-45s = %s\n' "$label" "$value"
    else
        printf '  %-45s FAILED: %s\n' "$label" "$out"
        fail=1
    fi
done

[ "$fail" = 0 ] || { echo; echo "Some properties did not load."; exit 1; }

cat <<'DONE'

Done. The VHAL must be restarted after any edit to the JSON.

IMPORTANT: this does not survive closing the emulator. `adb remount` could not
allocate scratch space on /data and fell back to free space on super, so the
overlay holding these files is lost when the emulator process exits. It does
survive `adb reboot` within a running emulator. Re-run this script (and
install-as-privileged-app.sh) after each emulator start.

In the app: Info -> Vendor Properties. Drive mode is writable, so tapping it
writes to the VHAL and the new value arrives back as a subscription event.

To remove:
  adb root && adb remount
  adb shell rm /vendor/etc/automotive/vhalconfig/SmartAaosVendorProperties.json
  adb shell setprop ctl.restart vendor.vehicle-hal-emulator
DONE
