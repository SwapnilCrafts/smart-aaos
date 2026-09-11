#!/usr/bin/env bash
#
# Installs SmartAAOS as a privileged system app on an AAOS emulator, so it can
# hold the signature|privileged car permissions that no ordinary APK can get:
#
#   CAR_ENGINE_DETAILED -> ENGINE_RPM
#   CAR_MILEAGE         -> PERF_ODOMETER
#   CAR_IDENTIFICATION  -> INFO_VIN
#
# Everything else the app reads is already available to a normal install:
# CAR_INFO / CAR_POWERTRAIN are protectionLevel:normal, and CAR_SPEED /
# CAR_ENERGY are protectionLevel:dangerous, i.e. ordinary runtime permissions.
# See FINDINGS.md.
#
# Requirements
#   - The AVD must be booted with -writable-system, e.g.
#       emulator -avd AAOS_API35_UserDebug -writable-system -no-snapshot-load
#     Without it, `adb remount` fails with "Device must be bootloader unlocked".
#   - A userdebug (not user) system image, so `adb root` is permitted.
#   - Several GB of free disk: -writable-system creates a system.img.qcow2
#     overlay, and a full disk makes the emulator die mid-session.
#
# Rollback
#   Deleting the .qcow2 overlays in the AVD directory restores the pristine
#   images:  rm ~/.android/avd/<name>.avd/*.qcow2
#
# Persistence
#   On this AVD `adb remount` reports "Failed to allocate scratch on /data,
#   fallback to use free space on super". That overlay is lost when the
#   emulator process exits, so the install survives `adb reboot` but NOT
#   closing and reopening the emulator. Re-run this script after each start.
#
set -euo pipefail

PKG=com.swapnil.smart.aaos
APK=${1:-automotive/build/outputs/apk/debug/automotive-debug.apk}
ALLOWLIST=tools/privapp-permissions-${PKG}.xml
PRIV_DIR=/system/priv-app/SmartAAOS
# The driver on AAOS is user 10, not user 0.
CAR_USER=${CAR_USER:-10}

step() { printf '\n== %s\n' "$1"; }

[ -f "$APK" ]       || { echo "APK not found: $APK (run ./gradlew :automotive:assembleDebug)"; exit 1; }
[ -f "$ALLOWLIST" ] || { echo "allowlist not found: $ALLOWLIST"; exit 1; }

step "Waiting for device"
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done

step "Making /system writable"
adb root
adb wait-for-device
# adb remount's wording varies (it may report "Using overlayfs for /system"
# rather than success), so don't parse it - probe with an actual write.
adb remount 2>&1 | sed 's/^/  /' || true
if ! adb shell 'touch /system/.rw_probe 2>/dev/null && rm /system/.rw_probe && echo ok' | grep -q ok; then
    echo "FAILED: /system is read-only." >&2
    echo "Reboot the AVD with -writable-system and make sure the disk is not full:" >&2
    echo "  emulator -avd <name> -writable-system -no-snapshot-load" >&2
    exit 1
fi
echo "  /system is writable"

step "Removing any normal (data) install of $PKG"
# A /data install shadows the system one; both present means the old APK wins.
adb uninstall "$PKG" 2>/dev/null || echo "  (no data install, fine)"

step "Pushing the APK to $PRIV_DIR"
adb shell mkdir -p "$PRIV_DIR"
adb push "$APK" "$PRIV_DIR/SmartAAOS.apk"
# priv-app APKs must be world-readable or PackageManager skips them at scan.
adb shell chmod 644 "$PRIV_DIR/SmartAAOS.apk"
adb shell chmod 755 "$PRIV_DIR"

step "Installing the privileged permission allowlist"
adb push "$ALLOWLIST" "/system/etc/permissions/$(basename "$ALLOWLIST")"
adb shell chmod 644 "/system/etc/permissions/$(basename "$ALLOWLIST")"

step "Rebooting so PackageManager rescans /system/priv-app"
# The scan only happens at boot: pushing an APK into priv-app on a running
# system has no effect until the package database is rebuilt.
adb reboot
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done

step "Verifying the install landed in /system"
adb shell pm path --user "$CAR_USER" "$PKG" || { echo "package not installed"; exit 1; }

step "Granting the runtime (dangerous) car permissions to user $CAR_USER"
# These are NOT covered by the privileged allowlist; they are runtime grants.
# The app also asks for them itself via CarContext.requestPermissions().
for p in CAR_SPEED CAR_ENERGY; do
    adb shell pm grant --user "$CAR_USER" "$PKG" "android.car.permission.$p" \
        && echo "  granted $p"
done

step "Resulting privileged permission state"
adb shell dumpsys package "$PKG" | grep -E 'CAR_ENGINE_DETAILED|CAR_MILEAGE|CAR_IDENTIFICATION|CAR_SPEED|CAR_ENERGY' || true

cat <<'DONE'

Done. To confirm the properties are actually live, launch the app and look for
the availability probe:

  adb logcat -c
  adb shell am start --user 10 -n com.swapnil.smart.aaos/androidx.car.app.activity.CarAppActivity
  adb logcat -d | grep 'Availability:'

ENGINE_RPM, PERF_ODOMETER and INFO_VIN should now read "LIVE VHAL".
DONE
