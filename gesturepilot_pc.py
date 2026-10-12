"""
GesturePilot PC receiver for Windows.
Install: py -m pip install pyautogui
Run: py gesturepilot_pc.py
Keep watch and PC on the same Wi-Fi. UDP port 50123. Ctrl+C stops it.
"""
import json
import socket
import pyautogui

HOST = "0.0.0.0"
PORT = 50123
pyautogui.FAILSAFE = True
pyautogui.PAUSE = 0

def main():
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind((HOST, PORT))
    print(f"GesturePilot listening on UDP {PORT}. Ctrl+C stops it.")
    print("Allow Python through Windows Firewall on Private networks only if prompted.")
    try:
        while True:
            data, addr = sock.recvfrom(2048)
            try:
                msg = json.loads(data.decode("utf-8"))
                if msg.get("type") == "move":
                    dx = max(-18, min(18, float(msg.get("x", 0))))
                    dy = max(-18, min(18, float(msg.get("y", 0))))
                    pyautogui.moveRel(dx, dy, duration=0)
                elif msg.get("type") == "click":
                    pyautogui.click()
            except (ValueError, TypeError, json.JSONDecodeError):
                print("Ignored malformed packet from", addr)
    except KeyboardInterrupt:
        print("\nGesturePilot stopped.")
    finally:
        sock.close()

if __name__ == "__main__":
    main()
