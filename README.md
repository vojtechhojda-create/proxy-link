# ProxyLink

ProxyLink is a high-performance, secure local bridge connecting Windows PC and Android devices over Wi-Fi/LAN with ultra-low latency.

---
> [!IMPORTANT]
> **Distribution & Licensing Update (v1.0.0):**
> Newer versions of ProxyLink are distributed exclusively as compiled commercial applications (`.exe` / `.app`) under a proprietary closed-source license.
>
> Usage of v1.0.0+ requires an active subscription or a lifetime license key. Legacy releases (v0.x) remain open-source under their original permissive license.

---

## Licensing & Tiers

ProxyLink is distributed as a single executable application (`.exe`). The core network capabilities are completely free for personal use, while advanced power-user, developer, and team features are unlocked via a license key or active subscription.

### License Overview

* **Core App (Free Tier):** Provided free of charge for personal P2P transfers and local synchronization.
* **Paid Tiers (Pro / Developer / Teams):** Feature modules are governed by a **Proprietary End User License Agreement (EULA)** and activated upon key verification.

| Tier | Price | Included Features |
| :--- | :--- | :--- |
| **ProxyLink Free** | **$0** / Free | Basic file transfer, shared clipboard, LAN transfer |
| **ProxyLink Pro** | **$7** one-time <br> *or* **$1** / month | Transfer history, custom visual themes, desktop widgets, download resuming |
| **ProxyLink Developer** | **$12** one-time <br> *or* **$2** / month | CLI access, Local API, webhooks, automation scripts |
| **ProxyLink Teams** | **$4** / user / month | User roles & management permissions, audit logs, shared team clipboard *(includes Pro)* |

---

## v1.0 — MVP (Minimum Viable Product)
*Goal: Establish core P2P connection, secure pairing, and basic audio/control streaming.*

- [x] **Core Networking & Discovery**
  - [x] mDNS local discovery (`_proxylink._tcp.local.`)
  - [x] Secure WebSocket server on Windows backend (Node.js/TypeScript) — wss, self-signed cert pinned via qr fingerprint
  - [x] Android client connection handling
- [x] **Secure Pairing (QR Code)**
  - [x] QR code generation on PC with one-time pairing token
  - [x] Android camera QR scanner & pairing request flow
  - [x] Windows approval dialog (whitelist)
- [x] **Audio Streaming**
  - [x] Windows system-wide audio capture via WASAPI Loopback
  - [x] Real-time audio transmission and playback on Android (`AudioTrack`)
- [x] **Remote Control & System Integration**
  - [x] Media control (Play/Pause, Volume Up/Down/Mute)
  - [x] Android Quick Settings Tile (connection toggle & status indicator)
  - [x] Android Share Sheet integration (send text/links/files directly to PC)
- [x] **Cross-Platform UI**
  - [x] Windows System Tray Menu (status, QR code popup, disconnect)
  - [x] Android Jetpack Compose main dashboard UI

---

## v1.1 — Reliability & Advanced Sharing
*Goal: Make the connection bulletproof and enhance productivity features.*

- [x] **Auto-Reconnect & State Recovery**
  - [x] Silent auto-pair on startup using stored cryptographic keys (ECDSA P-256 in Android Keystore, Ed25519 is not available in Keystore below API 33)
  - [x] Network switch and sleep/wake recovery handling (retries for about 80 s)
- [ ] **Clipboard & File Bridge
