# Gesture Pilot

First Wi-Fi cursor prototype for Galaxy Watch / Wear OS and Windows.

## What is implemented
- Watch app reads accelerometer movement and sends relative cursor movement over local Wi-Fi.
- On-watch start/stop, recalibration, and explicit left-click button.
- Windows receiver: `gesturepilot_pc.py`.

## Test on Windows
1. Install Python 3.
2. In PowerShell, run `py -m pip install pyautogui`.
3. Run `py gesturepilot_pc.py` from the repository folder.
4. If Windows Firewall asks, allow Python on **Private networks only**.
5. Run `ipconfig` and note the IPv4 address of the PC Wi-Fi adapter.
6. Open this project in Android Studio and build/install the watch app.
7. Ensure watch and PC use the same Wi-Fi. Enter the PC IPv4 address on the watch and tap **Spustit kurzor**.
8. Keep your wrist still briefly for calibration, then move gently. Tap **Klik** for a left click.

## Important
- This is an early prototype and has not yet been built or tested on a physical watch.
- UDP port 50123 is used on the local network. Do not expose it to the internet or forward it on your router.
- Cursor axis mapping and sensitivity may need tuning after real-world tests.
- Stop the PC receiver with Ctrl+C. PyAutoGUI has a failsafe: move the mouse to the top-left corner to trigger it.
