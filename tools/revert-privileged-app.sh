#!/usr/bin/env bash
#
# Undoes install-as-privileged-app.sh: removes SmartAAOS from /system/priv-app
# and deletes its privileged permission allowlist, returning the app to an
# ordinary third-party install.
#
# Worth knowing why a plain `adb uninstall` is not enough: once the package has
# been scanned from /system, installing a new APK over it only adds a /data
# copy and marks the package an *updated system app*. It keeps its privileged
# permissions, and uninstalling merely reverts to the system APK underneath.
# The system copy has to be deleted and the device rebooted for PackageManager
# to forget it.
#
# Requires an AVD booted with -writable-system, same as the install script.
#
set -euo pipefail

PKG=com.swapnil.smart.aaos
PRIV_DIR=/system/priv-app/SmartAAOS
ALLOWLIST_NAME=privapp-permissions-${PKG}.xml

step() { printf '\n== %s\n' "$1"; }

step "Waiting for device"
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done

step "Making /system writable"
adb root
adb wait-for-device
adb remount 2>&1 | sed 's/^/  /' || true
if ! adb shell 'touch /system/.rw_probe 2>/dev/null && rm /system/.rw_probe && echo ok' | grep -q ok; then
    echo "FAILED: /system is read-only. Reboot the AVD with -writable-system." >&2
    exit 1
fi

step "Removing the system copy and its allowlist"
adb shell rm -rf "$PRIV_DIR"
adb shell rm -f "/system/etc/permissions/$ALLOWLIST_NAME"

step "Removing any /data update of the package"
adb uninstall "$PKG" 2>/dev/null || echo "  (nothing in /data)"

step "Rebooting so PackageManager forgets the system package"
adb reboot
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done

step "State after revert"
adb shell pm path --user 10 "$PKG" || echo "  package not installed (expected)"

cat <<'DONE'

Reverted. Reinstall normally with:

  ./gradlew :automotive:installDebug

Speed, fuel and battery will still work - they are runtime permissions, not
privileged ones. RPM, odometer and VIN will fall back to simulated values.
DONE
