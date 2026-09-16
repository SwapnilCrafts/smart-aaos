#!/usr/bin/env bash
#
# Builds, platform-signs and installs the Car System UI overlay (RRO).
#
# This is how an OEM rebrands the car without forking AOSP: a separate APK
# replaces CarSystemUI's resources at runtime. No platform build required -
# which is the point, because it means System UI customisation can be done and
# demonstrated on a laptop.
#
# Why platform signing is needed
#   An overlay may only replace another package's resources if one of these is
#   true:
#     1. it is signed with the same certificate as the target, or
#     2. the target declares <overlayable name="..."> and the overlay names it
#        with android:targetName, or
#     3. it is preinstalled in a trusted partition by the system image.
#
#   CarSystemUI declares no <overlayable> blocks (check with
#   `aapt2 dump overlayable CarSystemUI.apk` - it prints nothing), so route 2
#   is closed. Without signing, the install fails with:
#
#     Overlay ... and target com.android.systemui signed with different
#     certificates, and the overlay lacks <overlay android:targetName>
#
#   The emulator is a `test-keys` build (`getprop ro.build.tags`), so its
#   platform certificate is the public AOSP one. Signing with that key makes
#   route 1 true. On a real production car this key is the OEM's secret and
#   the overlay would ship inside the system image instead.
#
# Requirements
#   - AAOS emulator running (no -writable-system needed for this)
#   - The AOSP signing keys, fetched by this script if absent (~1 MB)
#
set -euo pipefail

PKG=com.swapnil.smartaaos.systemui.overlay
TARGET=com.android.systemui
MODULE=:systemui-overlay
APK_IN=systemui-overlay/build/outputs/apk/debug/systemui-overlay-debug.apk
APK_SIGNED=/tmp/systemui-overlay-platform.apk

KEYDIR=${KEYDIR:-$HOME/Documents/AndroidProjects/aosp-reference/aosp-build-keys/target/product/security}
KEYREPO=$(dirname "$(dirname "$(dirname "$KEYDIR")")")

# CarSystemUI runs as user 0, NOT the driver user 10. An overlay has to be
# enabled for the user its target runs as, or OverlayManager reports
# "Unable to retrieve overlay information".
TARGET_USER=${TARGET_USER:-0}

step() { printf '\n== %s\n' "$1"; }

sdk=${ANDROID_HOME:-$HOME/Library/Android/sdk}
APKSIGNER=$(ls "$sdk"/build-tools/*/apksigner 2>/dev/null | sort | tail -1)
AAPT2=$(ls "$sdk"/build-tools/*/aapt2 2>/dev/null | sort | tail -1)
[ -n "$APKSIGNER" ] || { echo "apksigner not found under $sdk/build-tools"; exit 1; }

step "Waiting for device"
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done

tags=$(adb shell getprop ro.build.tags | tr -d '\r')
echo "  build tags: $tags"
if [ "$tags" != "test-keys" ]; then
    echo "  WARNING: not a test-keys build. The AOSP platform key will not match" >&2
    echo "  this image, and the install will be rejected." >&2
fi

step "Fetching the AOSP platform key if needed"
if [ ! -f "$KEYDIR/platform.pk8" ]; then
    mkdir -p "$(dirname "$KEYREPO")"
    git clone --depth=1 --filter=blob:none --sparse \
        -b android15-automotiveos-release \
        https://android.googlesource.com/platform/build "$KEYREPO"
    git -C "$KEYREPO" sparse-checkout set target/product/security
fi
ls -la "$KEYDIR/platform.pk8" "$KEYDIR/platform.x509.pem"

step "Building the overlay"
./gradlew "$MODULE:assembleDebug" -q
[ -f "$APK_IN" ] || { echo "build produced no APK at $APK_IN"; exit 1; }
"$AAPT2" dump badging "$APK_IN" | grep '^overlay:' || {
    echo "not an overlay APK - is <overlay> missing from the manifest?"; exit 1; }

step "Signing with the platform key"
cp "$APK_IN" "$APK_SIGNED"
"$APKSIGNER" sign --key "$KEYDIR/platform.pk8" --cert "$KEYDIR/platform.x509.pem" "$APK_SIGNED"
"$APKSIGNER" verify --print-certs "$APK_SIGNED" | grep 'certificate DN' | head -1

step "Installing"
adb install -r "$APK_SIGNED"

step "Enabling for user $TARGET_USER"
adb shell cmd overlay enable --user "$TARGET_USER" "$PKG"
adb shell cmd overlay list --user "$TARGET_USER" "$TARGET" | grep -E "$PKG|^\[" | head -5

step "Restarting SystemUI so it reloads resources"
# SystemUI caches its resources; it is restarted automatically after being killed.
adb shell pkill -f "$TARGET" || true
sleep 10

cat <<DONE

Done. The system bar accent should now be the colour set in
systemui-overlay/src/main/res/values/colors.xml.

Screenshot it (AAOS has several displays, so pick the real id):
  adb shell dumpsys SurfaceFlinger --display-id
  adb exec-out screencap -p -d <id> > after.png

To turn it off / back on without reinstalling:
  adb shell cmd overlay disable --user $TARGET_USER $PKG
  adb shell cmd overlay enable  --user $TARGET_USER $PKG

To find other resources you can override, read an overlay that already ships:
  adb pull /product/overlay/googlecarui.theme.orange-com-android-systemui.apk
  aapt2 dump resources googlecarui.theme.orange-com-android-systemui.apk
DONE
