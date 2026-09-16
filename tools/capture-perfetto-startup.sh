#!/usr/bin/env bash
#
# Captures a Perfetto trace of SmartAAOS starting up on an AAOS emulator.
#
# Why the ordering below matters: to trace a *startup* you must already be
# recording before the process exists. So perfetto starts first, in the
# background, then the app is force-stopped and launched inside the trace
# window. Launching first and tracing after gives you a trace of a warm,
# already-running app, which is a different and much less interesting thing.
#
# Open the result at https://ui.perfetto.dev (drag the file in - it is parsed
# locally in the browser, nothing is uploaded).
#
set -euo pipefail

PKG=com.swapnil.smart.aaos
ACTIVITY=androidx.car.app.activity.CarAppActivity
CAR_USER=${CAR_USER:-10}
CONFIG=tools/perfetto-startup.cfg
DEVICE_OUT=/data/misc/perfetto-traces/smartaaos-startup.pftrace
LOCAL_OUT=${1:-/tmp/smartaaos-startup.pftrace}

step() { printf '\n== %s\n' "$1"; }

[ -f "$CONFIG" ] || { echo "missing $CONFIG"; exit 1; }

step "Waiting for device"
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done

step "Checking perfetto is present and tracing is enabled"
adb shell 'which perfetto' >/dev/null || { echo "perfetto not on device"; exit 1; }
# On user builds this can be off; userdebug has it on.
enabled=$(adb shell getprop persist.traced.enable | tr -d '\r')
[ "$enabled" = "1" ] || adb shell setprop persist.traced.enable 1

step "Making sure the app is cold"
adb shell am force-stop --user "$CAR_USER" "$PKG" || true

step "Starting the trace in the background"
# --background returns immediately and leaves perfetto recording.
adb shell "mkdir -p /data/misc/perfetto-traces"
adb shell "perfetto --txt -c - -o $DEVICE_OUT --background" < "$CONFIG"
sleep 2

step "Launching the app inside the trace window"
adb shell am start --user "$CAR_USER" -n "$PKG/$ACTIVITY" >/dev/null
echo "  launched; letting it settle and connect to the VHAL"

# The config records for 20s. Wait for it to finish on its own rather than
# killing perfetto, so the trailing buffer is flushed cleanly.
step "Waiting for the trace to complete"
for _ in $(seq 1 40); do
    if ! adb shell 'pgrep -x perfetto >/dev/null' 2>/dev/null; then break; fi
    sleep 1
done

step "Pulling the trace"
adb pull "$DEVICE_OUT" "$LOCAL_OUT"
ls -la "$LOCAL_OUT"

cat <<DONE

Open it:  https://ui.perfetto.dev   (drag the file in)

What to look for, in order:

 1. Find the com.swapnil.smart.aaos process track. Expand it.
    The main thread is the one named after the process.

 2. Look for a long 'bindApplication' / 'activityStart' slice. That is your
    cold start. Anything blocking the main thread inside it delays first frame.

 3. Turn on the binder slices. Every call into CarService or the VHAL is a
    separate process, so it shows as a binder transaction pair - client
    'binder transaction' and server 'binder reply'. Their width is the latency
    you are actually paying for a vehicle property read.

 4. Compare that against the app's own log lines, which appear on the same
    timeline from the android.log data source. This is the thing logcat alone
    cannot do: see a log message and the stall that caused it side by side.

 5. In the query box (bottom left) you can run SQL over the trace, e.g.
    the slowest binder calls:

      select s.name, s.dur/1e6 as ms, t.name as thread
      from slice s join thread_track tt on s.track_id = tt.id
      join thread t using(utid)
      where s.name like '%binder%' order by s.dur desc limit 20;
DONE
