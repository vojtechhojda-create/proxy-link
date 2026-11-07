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

## android integration
- share sheet: proxylink appears as a direct share target (pc and other connected devices in the top row); also in the text selection menu.
- notifications for incoming items, with accept / deny buttons for requests that need approval (only while the app is not in the foreground).
- starts after reboot and after an update (setting "start after reboot"), quick settings tile, foreground service with a wi-fi lock while connected.
- background: the app asks for the battery optimization exemption. on xiaomi / samsung also allow autostart in the system app settings.

## android
build comes from github actions (artifact proxylink-debug-apk), or `gradle :app:assembleDebug`.
