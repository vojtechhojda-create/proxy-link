# project status

soubor vznikl až v tomto kroku; dřívější PROJECT_STATUS.md ani FEATURE_PLAN.md v repozitáři (main, 1 commit) neexistovaly.

## aktuální úkol
0. reconnect známých zařízení (implementováno v kódu, neověřeno buildem ani na telefonu)
1. launcher ikona podle palety + systémová monochromatická ikona (hotovo v kódu, neověřeno na zařízení)
2. oprava dlaždice rychlého nastavení (hotovo v kódu, neověřeno na zařízení)

## změněno (větev feature/palette-launcher-icon)
- branding/proxylink-icon-source.svg: přiložený originál, beze změny
- manifest: launcher vstup přesunut na dva activity-alias (LauncherBrand zapnutý, LauncherNavyOrange vypnutý)
- core/LauncherIcon.kt: zapne cílový alias, potom vypne ostatní
- Prefs.paletteIcon (výchozí vypnuto), přepínač ve vzhledu, sync při startu
- ic_launcher_foreground / ic_launcher_mono: zaoblení lišty (rx 1 z originálu), jinak geometrie beze změny
- nové: ic_launcher_navy_orange, ic_launcher_foreground_navy_orange, color ic_bg_navy_orange

## nastavení (celek 3, částečně)
- top-level: devices / connection / appearance / clipboard / transfers / notifications and control / privacy and security / diagnostics / about
- stránka behavior rozdělena na clipboard, transfers, notifications; nové jsou privacy (stav oprávnění, zapomenout všechna zařízení s potvrzením) a diagnostics (souhrn bez jmen, adres, tajemství a obsahu schránky, kopie označená jako citlivá)
- jen skutečné funkce, žádné nové přepínače bez implementace; všechno se ukládá v Prefs (SharedPreferences)
- ODLOŽENO: texty do resources cs/en (celý kód má texty natvrdo anglicky), skrývání vypnutých funkcí z nástěnky, výjimky na zařízení, limity schránky/přenosů, preferované zařízení, reset s výběrem rozsahu, licence knihoven (nelze bez ověření verzí)

## reconnect (celek 2)
skutečný stav před úpravou (z kódu): id zařízení, tajemství a pin certifikátu jsou v SharedPreferences pod id zařízení, ne pod IP, nový qr kód při reconnectu není potřeba. chyby: smyčka na starou IP každé 3 s bez limitu, možné souběžné pokusy, "odpojeno uživatelem" jen v paměti (po restartu se auto connect vrátil), žádná reakce na návrat sítě, žádné preferované zařízení.

změněno:
- jeden retry časovač (nový pokus ruší čekající), backoff 3/6/12/24/30 s, max 8 pokusů (celkem asi 2,5 min; dost na krátký výpadek, ne na nekonečné vybíjení baterie). potom převezme discovery nebo ruční "try again"
- adresa se bere z aktuální discovery; nové oznámení adresy během pokusu přepne na ni
- návrat sítě (ConnectivityManager callback, debounce 1,5 s): vynuluje počítadlo, vyčistí zastaralé peers, restartuje discovery a jednou zkusí poslední zařízení. nové oprávnění ACCESS_NETWORK_STATE (běžné, bez dotazu)
- ručně odpojeno se ukládá (Prefs.userDisconnected), po restartu aplikace nebo telefonu se auto connect nevrátí, dokud uživatel nepřipojí znovu. zapomenutí zařízení to už nenastaví
- preferované zařízení (Prefs.preferredId, přepínač v detailu zařízení): auto connect a dlaždice míří na něj, ostatní se připojují ručně; zapomenutí ho zruší
- Link.lastAttempt, lastAttemptId, lastError, retries; detail zařízení ukazuje poslední pokus jen u zařízení, kterého se týkal; "try again" po chybě
- diagnostika: poslední pokus a chyba, poslední úspěšné připojení, počet neúspěchů v řadě, běh služby, odpojeno uživatelem (bez jmen, adres a obsahu)

ověřeno: xml well-formed; statický průchod: restart služby/aplikace, výpadek a návrat sítě, ruční pokus, vícenásobné spuštění (jeden časovač, jeden debounce), vypnutí uživatelem. build ani test na telefonu neproběhl.
nejisté: že nsd po návratu wi-fi opravdu znovu najde zařízení (restart discovery to má pokrýt, bez testu nevím); chování na androidu 13 při úsporném režimu; foreground služba se po zabití systémem znovu nespustí sama.
nepoužito: změna pc (nebyla potřeba)

## dlaždice (zjištěno z kódu, ne z testu)
- zapnutí z vypnutého stavu: `Link.toggle()` -> `connectPaired()` jen nastavilo `reconnecting` a stav zůstal IDLE, dlaždice proto dál ukazovala "off". navíc dlaždice nespouštěla `LinkService`, takže proces mohl zaniknout dřív, než discovery zařízení našla. skutečnou příčinu na telefonu nemám potvrzenou.
- LinkTile: při kliknutí zkusí spustit LinkService (selhání nespadne), stavy: connected / connecting / searching / pair in the app / no paired device / off
- Link: reconnect a cancelReconnect upozorní posluchače, hledání po 30 s vyprší, druhé klepnutí během hledání hledání zruší

## ověřeno
- ProxyLogo.kt, launcher foreground/mono a ic_tile odpovídají geometrii originálu (přepočet souřadnic)
- xml je well-formed

## oprava buildu 1
github actions: platform declaration clash v Link.kt (setPreferred). property `preferred` generuje JVM setter setPreferred(String), ručně psaná funkce měla stejný podpis. přejmenováno na setPreferredDevice (Link.kt ×2 volání, SettingsSections.kt ×1). jiná kolize set*/get* funkce s property není (zkontrolováno grepem přes celý kód). chování beze změny. build po opravě zatím neproběhl.

## příprava na build
zkontrolováno staticky, bez překladače:
- gradle: agp 8.5.2, kotlin 2.0.20 + compose plugin, compileSdk 34, minSdk/targetSdk 33, compose bom 2024.09.00, okhttp 4.12.0, camerax 1.3.4, mlkit barcode 17.3.0; repozitáře google + mavenCentral. žádné nové závislosti nepřibyly
- gradle wrapper v repozitáři NENÍ; nedoplňoval jsem ho (jar nelze stáhnout z prostředí). workflow používá gradle/actions/setup-gradle s gradle 8.9 a příkazem `gradle`, což wrapper nepotřebuje. v android studiu se použije jeho vlastní gradle
- android.yml: spouští `gradle :app:assembleDebug`, nahrává app-debug.apk jako artefakt. upraveno: běží i na pull_request, přidáno --stacktrace, if-no-files-found: error, permissions: contents: read. build se spustí na každé větvi při změně proxylink-android/**
- manifest a všechny změněné xml jsou well-formed; aliasy jsou po `<activity>` MainActivity, ikony i barva existují
- všechny změněné kotlin soubory mají vyvážené závorky; importy v SettingsSections jsou pokryté (wildcardy layout, material3, runtime, components); použité symboly v Prefs, History, Link a Common.kt existují s odpovídajícími podpisy
- parser tree-sitter-kotlin hlásí chyby v Link.kt (19, původní verze 1) a v SettingsSections.kt (2, stejně jako původní). u Link.kt jsem příčinu nenašel (zkoušeno odebráním všech nových bloků); původní soubor už jedno hlášení měl, takže jde nejspíš o omezení gramatiky, ale jistotu bez kompilátoru nemám

neověřeno:
- samotný gradle build (v prostředí není android sdk ani přístup k google maven)
- chování aliasů ikony a reconnectu na zařízení s android 13

## dále
po zelené sestavě a testu reconnectu na telefonu: dokončit celek 3 (resources cs/en, výjimky zařízení, skrývání vypnutých funkcí), potom celek 4 (schránka).
