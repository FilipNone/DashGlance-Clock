#!/usr/bin/env bash
# Starts the Android Auto Desktop Head Unit (DHU) and connects it to the phone on USB.
#
#   scripts/dhu.sh                 ADB tunnelling. Tap "Start head unit server" on the phone first.
#   scripts/dhu.sh --usb           Accessory mode, which works without the head unit server.
#   scripts/dhu.sh -c config/default_1080p.ini
#
# Arguments go straight to desktop-head-unit. Config paths are relative to
# $ANDROID_HOME/extras/google/auto.
set -euo pipefail

sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
dhu_dir="$sdk/extras/google/auto"
dhu="$dhu_dir/desktop-head-unit"

if [[ ! -f "$dhu" ]]; then
    echo "Can't find the DHU in $dhu_dir" >&2
    echo "Install it with: android sdk install extras/google/auto" >&2
    exit 1
fi
chmod +x "$dhu"

missing=$(ldd "$dhu" | awk '/not found/ { print $1 }' | sort -u)
if [[ -n "$missing" ]]; then
    echo "The DHU needs these libraries, which aren't installed:" >&2
    echo "$missing" | sed 's/^/  /' >&2
    echo "On Arch/CachyOS:  sudo pacman -S libc++" >&2
    echo "On Debian/Ubuntu: sudo apt-get install libc++1 libc++abi1" >&2
    exit 1
fi

if [[ " $* " == *" --usb"* ]]; then
    # A phone left in accessory mode by an earlier run makes the new DHU lose sync and quit
    # ("Out of sync with phone"). Resetting the phone's USB starts the handshake fresh.
    # resetUsbGadget needs Android 12 or newer, so a failure here is ignored.
    if adb shell svc usb resetUsbGadget >/dev/null 2>&1; then
        sleep 3
        adb wait-for-device
    fi
else
    adb forward tcp:5277 tcp:5277
fi

cd "$dhu_dir"
exec ./desktop-head-unit "$@"
