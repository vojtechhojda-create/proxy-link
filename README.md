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
- tabs: dashboard, share, settings (devices, connection, clipboard, transfers, appearance, notifications, privacy and security, diagnostics, about).
- not connected screen: scan qr (own scanner, camerax + ml kit), nfc, reconnect, forget. paired devices connect automatically.
- appearance (settings -> appearance, three subpages): theme (10 themes, 6 dark and 4 light, plus system colors, with a preview before use), font (ui font, text size, monospace), interface look (corners, density, element size). fonts are bundled: Inter, Space Grotesk, Inconsolata and others. animations follow the system setting "remove animations".
- everything is free and local: no cloud, no ads, no telemetry, no account.
- share sheet targets, notifications with accept / deny, quick settings tile, home screen widget, start after reboot.

## versions and compatibility
- the pc app (package.json) and the android app (versionName) have their own versions, shown in the pc window and in android settings -> about. the protocol number is 2 on both sides.
- both sides announce themselves in the hello message: protocol `v`, app version `app`, features `caps` (the pc also sends `min`, the lowest mobile protocol it serves). all fields are optional, an app that sends none is treated as older and keeps the basic transfers (text, files, clipboard).
- a feature is available only when the other side announced it. a protocol below the minimum is refused with "requires a pc update" or "requires a mobile update" and nothing is sent. screen sharing is not implemented and is only ever shown as "coming soon, requires a pc update".

## privacy and security (what is and is not checked)
- the connection is tls. the app pins the sha-256 fingerprint of the pc certificate and refuses a different one; the host name is not checked. paired devices prove themselves with an hmac of a per connection nonce.
- not checked: whether the pc app or this app was modified, and files you receive. the file hash tool (settings -> privacy and security) only compares the bytes of one local file with a hash you paste. a match is not proof that a file is harmless.
- app signature check: `res/values/release_cert.xml` takes the public sha-256 fingerprint(s) of the release certificate. empty (today) means "not verified" and nothing is blocked. with a fingerprint set, a release build whose signature does not match blocks sending and receiving (devices, history and settings are kept). it is a local check inside the app and can be bypassed by someone who modifies the app.
- notifications hide their content on the lock screen by default (the system lock screen setting can still show it). a running or waiting transfer can be cancelled; there is no pause.
- diagnostics are exported only on request, with a preview, without keys, pairing secrets or clipboard content.

## android
build comes from github actions (artifact proxylink-debug-apk), or `gradle :app:assembleDebug` (jdk 17, android sdk, gradle 8.9). builds are debug signed until a release signing config exists. the windows exe is built with `npm run dist:win` on windows (on linux electron-builder needs wine) and is not code signed.
