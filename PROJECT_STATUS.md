# project status

soubor vznikl až v tomto kroku; dřívější PROJECT_STATUS.md ani FEATURE_PLAN.md v repozitáři (main, 1 commit) neexistovaly.

## přenosy: kvalita obrázků (celek 5, první část)
hotovo v kódu (build neproběhl): nastavení přenosů má volbu originál (výchozí) / vysoká (delší strana max 2560 px, jpeg 90) / úsporná (1600 px, jpeg 80). platí jen pro "send image" (Link.sendImage), "send file" posílá vždy originál. zpracování jen pro jpeg; menší obrázky se nezvětšují a posílají se beze změny, png/gif/webp také; orientace se aplikuje do pixelů, překódování odstraní metadata včetně polohy (u originálu zůstanou; vysvětleno v textu); původní soubor se nemění, kopie je v cache; chyba nebo málo paměti = pošle se originál; běží mimo hlavní vlákno
NENÍ: volba "vlastní" a "vždy se zeptat", samostatné odstranění metadat u originálu, heic, konflikt názvu, limit auto-příjmu, náhledy (thumbnail) odesílatele i příjemce, stavy přenosu, otevření po přijetí. opětovné odeslání z historie u zmenšeného obrázku závisí na tom, že kopie v cache ještě existuje

## schránka (celek 4, první část)
před úpravou: jeden přepínač autoSync pro obě strany, historie schránky se ukládala vždy, žádná pauza, žádná kontrola citlivého obsahu; duplicity a smyčky řeší hash (clipHash) a to zůstává.
hotovo v kódu (build neproběhl):
- odesílání a příjem nezávisle (Prefs.clipSend / clipReceive; výchozí hodnota se přebírá ze starého autosync)
- pozastavení 15 min / 1 h / do ručního obnovení, uloženo v Prefs (přežije restart); ruční odeslání pauzu neblokuje
- automaticky se neposílá text označený zdrojovou aplikací jako citlivý (ClipDescription.EXTRA_IS_SENSITIVE, android 13); ruční odeslání ano
- historie schránky volitelná, VÝCHOZÍ VYPNUTO (změna chování: nově odeslané a přijaté texty se do historie neukládají), tlačítko vymazat jen schránku (soubory zůstanou)
- stav: poslední odeslání a přijetí (jen za běh), důvod nečinnosti: nepřipojeno / hostovské spojení / pozastaveno / obojí vypnuto; obsah schránky se nikam neloguje ani se nezobrazuje v diagnostice
- diagnostika ukazuje směry a pauzu
- texty cs/en
NENÍ hotovo: výběr cílových zařízení (app má jedno spojení s pc), limit počtu položek a doba uchování historie, přehled citlivosti nad rámec flagu; rychlé odeslání přes sdílecí nabídku už existovalo (ClipSendActivity)

## qr / nfc (celek 6, první část)
stav před úpravou (z kódu): app je jen klient pc serveru (okhttp websocket), telefony si soubory posílají přes pc. nfc je jen systémové otevření `proxylink://` štítku, který se hned připojil bez potvrzení. pc umí jen vygenerovat qr (`pair`, `guest`), nemá žádnou čtečku qr ani zprávu pro qr z telefonu.
hotovo v kódu (build neproběhl):
- skener qr: po správném kódu zůstane obrazovka a ukáže výsledek (připojování / připojeno / selhalo). soubor se pošle až výslovným tlačítkem "send a file" (existující SendActions / Link.sendFile); nic se neodesílá samo
- dole na skeneru tlačítko "generate qr code": DISABLED s vysvětlením, protože pc aplikace neumí číst qr z telefonu (konstanta PHONE_QR_SUPPORTED = false ve ScannerScreen.kt; změnit společně s pc)
- odkaz `proxylink://` z nfc štítku nebo jiné aplikace už se nepřipojí sám: potvrzovací dialog s názvem a adresou (PairLinkDialog). Link: parseLink / connectTo / pairFromQr (chování qr skeneru stejné)
- texty cs/en (21 nových klíčů), skener převeden na resources
- nfc obrazovka (ui/screens/NfcScreen.kt, route "nfc", vstup z řádku nfc na nástěnce bez připojení, bez nové hlavní karty): reader mode jen dokud čeká na štítek, čte NDEF uri `proxylink://pair|guest`, stejně validuje jako qr (Link.parseLink). stavy: nepodporováno, vypnuto (tlačítko do systémového nastavení), připraveno, potvrzení, připojování, připojeno, selhalo, timeout 60 s, špatný štítek. po přečtení se nic nepřipojí bez potvrzení; "připojeno" jen podle skutečného stavu Link; tlačítko "použít qr kód"; soubor se pošle až výslovným tlačítkem
- nfc jen zahajuje párování; soubory jdou dál přes pc. text na obrazovce to říká a odkazuje na hotspot v nastavení
NENÍ hotovo: posílání souborů přímo telefon–telefon přes nfc + wi-fi/hotspot (telefon nemá server ani protokol pro přímé spojení, pc se nemění); telefon jako nfc štítek (hce) pro opačný směr; nfc obrazovka nebyla vůbec spuštěna ani testována (build ani telefon)

## ověření buildu
github actions `gradle :app:assembleDebug` prošel (hlášeno uživatelem po opravě setPreferredDevice; log jsem neviděl). stav před touto změnou: build ano, telefon ne.

## aktuální úkol
4. skener qr: výsledek, výslovné odeslání souboru, potvrzení odkazů (hotovo v kódu, build neproběhl)
3. nastavení: texty do resources cs/en (hotovo v kódu, build po této změně neproběhl)
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

## nastavení: resources cs/en (celek 3, pokračování)
- nové `res/values/strings.xml` (en, výchozí) a `res/values-cs/strings.xml`; 82 klíčů, stejné v obou souborech, všechna použití R.string v kódu mají klíč
- převedeno: SettingsScreen.kt a SettingsSections.kt (všechny viditelné texty podstránek, dialogů, titulků)
- záměrně nepřevedeno: souhrn diagnostiky (zůstává anglicky pro hlášení chyb), texty chyb z Link.kt (lastError) a Hotspot, názvy palet, text "proxylink"
- jazyk následuje jazyk systému; volba jazyka přímo v aplikaci není (plánováno)
- zbytek aplikace (nástěnka, sdílení, oznámení, dialogy mimo nastavení) má texty stále natvrdo anglicky
- celek 3 zůstává ČÁSTEČNÝ: chybí výjimky zařízení, skrývání vypnutých funkcí z nástěnky a rychlých akcí, reset s výběrem rozsahu, licence knihoven

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
po zelené sestavě této změny a testu na telefonu (reconnect, tile, ikona, čeština): zbytek celku 3 (skrývání vypnutých funkcí, výjimky zařízení), potom celek 4 (schránka).
