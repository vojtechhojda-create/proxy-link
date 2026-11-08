# proxylink v2

- proxylink-pc: electron tray app (tls websocket server + mdns), .exe on windows, .app/.dmg on mac
- proxylink-android: kotlin/compose app, android 13 (api 33)

## pc
    cd proxylink-pc
    npm install
    npm run start        # tray app
    npm run dev          # terminal version (commands: s, f, l, c)
    npm run selftest     # protocol test (pairing, guest, files, relay, clipboard)
    npm run dist:win     # installer + portable exe in release/
    npm run dist:mac     # dmg + zip in release/

## pairing and sharing
- pair: the pc window shows a qr code and a 6 digit code. scan it in the app or type the code.
- guest (no pairing): the pc window has a second qr code. a phone that scans it can send one text, link or file to the pc (valid 5 minutes, one use, no clipboard access).
- other phones: all phones paired with the same pc see each other under "other devices via pc" and can send text and files through the pc.
- connection is tls (wss), the app pins the sha-256 fingerprint of the pc certificate.

## no shared wi-fi
settings -> start hotspot on the phone, join the pc to that wi-fi, pick the hotspot address in the "network" box of the pc window, scan the pair qr code.

## mac signing (optional)
without secrets the mac build is unsigned (right click -> open on first launch). to sign and notarize, you need an apple developer account and these repository secrets (settings -> secrets and variables -> actions):
- MAC_CERT_P12_BASE64: "developer id application" certificate exported as .p12, then base64 encoded
- MAC_CERT_PASSWORD: password of that .p12
- APPLE_ID: apple id email
- APPLE_APP_PASSWORD: app-specific password from appleid.apple.com
- APPLE_TEAM_ID: 10 character team id

## android (version 2, android 13+)
- tabs: dashboard, share, settings (devices, connection, appearance, behavior, about).
- not connected screen: scan qr (own scanner, camerax + ml kit), nfc, reconnect, forget. paired devices connect automatically.
- palettes: brand (#111827 + #22C55E), navy orange, system colors (material you). fonts: Inter, Space Grotesk, Inconsolata, bundled.
- everything is free and local: no cloud, no ads, no telemetry, no account.
- share sheet targets, notifications with accept / deny, quick settings tile, home screen widget, start after reboot.

## android
build comes from github actions (artifact proxylink-debug-apk), or `gradle :app:assembleDebug`.
