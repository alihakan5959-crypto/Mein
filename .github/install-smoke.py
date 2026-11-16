"""Install and launch the real APK on Android; no screenshots or user media."""
import json
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

PKG = "com.example.netpulse.screen"
REPORT = pathlib.Path("install-report")
REPORT.mkdir(exist_ok=True)

def command(*args, check=True):
    result = subprocess.run(list(args), capture_output=True, text=True)
    if check and result.returncode:
        raise RuntimeError("Command failed: " + " ".join(args) + "\n" +
                           result.stdout + result.stderr)
    return result

def adb(*args, check=True):
    return command("adb", *args, check=check)

def ui():
    adb("shell", "uiautomator", "dump", "/sdcard/netpulse-ui.xml")
    result = adb("exec-out", "cat", "/sdcard/netpulse-ui.xml")
    return ET.fromstring(result.stdout)

def tap_text(names, timeout=25):
    deadline = time.monotonic() + timeout
    seen = []
    while time.monotonic() < deadline:
        root = ui()
        seen = [node.get("text", "") for node in root.iter("node")]
        for node in root.iter("node"):
            if node.get("text", "").casefold() not in names:
                continue
            if node.get("enabled") == "false":
                continue
            numbers = list(map(int, re.findall(r"\d+", node.get("bounds", ""))))
            if len(numbers) != 4:
                continue
            x1, y1, x2, y2 = numbers
            if x2 <= x1 or y2 <= y1:
                continue
            adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
            return
        time.sleep(1)
    raise AssertionError("Button not found. Seen: " + repr(seen))

report = {}
try:
    report["android"] = adb("shell", "getprop", "ro.build.version.release").stdout.strip()
    report["sdk"] = adb("shell", "getprop", "ro.build.version.sdk").stdout.strip()
    report["old_install"] = adb("install", "previous-v01/NetPulse-Android.apk").stdout.strip()
    upgrade = adb("install", "-r", "previous-v02/NetPulse-Android-0.2.apk", check=False)
    combined = upgrade.stdout + upgrade.stderr
    if upgrade.returncode == 0:
        report["old_upgrade"] = "success; signature conflict was not reproduced"
    elif "INSTALL_FAILED_UPDATE_INCOMPATIBLE" in combined:
        report["old_upgrade"] = "INSTALL_FAILED_UPDATE_INCOMPATIBLE; signature conflict reproduced"
    else:
        raise AssertionError("Unexpected original upgrade failure: " + combined)

    candidate = "dist/NetPulse-Ekran-0.2.1.apk"
    report["new_install"] = adb("install", candidate).stdout.strip()
    assert "Success" in report["new_install"], report["new_install"]
    report["package"] = adb("shell", "pm", "path", PKG).stdout.strip()
    assert report["package"].startswith("package:"), report["package"]
    adb("shell", "appops", "set", PKG, "SYSTEM_ALERT_WINDOW", "allow")
    adb("shell", "pm", "grant", PKG, "android.permission.POST_NOTIFICATIONS")
    adb("logcat", "-c")
    adb("shell", "am", "start", "-W", "-n", PKG + "/com.example.netpulse.MainActivity")
    tap_text({"ölçümü başlat"})
    time.sleep(2)
    service = adb("shell", "dumpsys", "activity", "services", PKG).stdout
    assert "com.example.netpulse.MeterService" in service, service
    report["floating_service"] = "started"
    adb("shell", "am", "force-stop", PKG)

    adb("shell", "am", "start", "-W", "-n", PKG + "/com.example.netpulse.MainActivity",
        "--ez", "requestCapture", "true")
    tap_text({"start now", "start", "share screen", "share"})
    deadline = time.monotonic() + 20
    projection = ""
    while time.monotonic() < deadline:
        projection = adb("shell", "dumpsys", "media_projection").stdout
        if PKG in projection:
            break
        time.sleep(1)
    assert PKG in projection, projection
    report["screen_projection"] = "consent granted and projection active"
    time.sleep(2)
    crashes = adb("logcat", "-b", "crash", "-d").stdout
    (REPORT / "crash-log.txt").write_text(crashes)
    assert "Process: " + PKG not in crashes, crashes
    report["crashes"] = "none"
    report["result"] = "PASS"
except Exception as error:
    report["result"] = "FAIL"
    report["error"] = str(error)
    raise
finally:
    (REPORT / "install-smoke.json").write_text(
        json.dumps(report, indent=2, ensure_ascii=False))
    print(json.dumps(report, indent=2, ensure_ascii=False))
    adb("shell", "am", "force-stop", PKG, check=False)
