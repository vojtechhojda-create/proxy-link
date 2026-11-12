# feature plan

backlog a stav požadavků (verze 2.0.0-alpha.4). stavy: HOTOVO-OVĚŘENO (build i telefon) / NEOVĚŘENO (je v kódu a v ui, bez překladu a testu na telefonu) / ČÁSTEČNĚ / CHYBÍ / ODLOŽENO. nic z této verze není HOTOVO-OVĚŘENO: poslední build prošel před celkem 4 (schránka); všechno novější je jen napsané a staticky zkontrolované (závorky, klíče řetězců), což není překlad ani test.

## stav podle oblastí
- HOTOVO-OVĚŘENO (jen uživatelem hlášené, starší): schránka (odeslání a příjem, rychlá); build po opravě setPreferredDevice
- NEOVĚŘENO, v kódu a v ui: widgety (akce soubor/fotka/schránka, stav připojení, schránka, barvy podle palety); přenosy: náhled sdíleného obrázku, obrázek v oznámení, uložený název při shodě, otevření po přijetí; dlaždice rychlého nastavení; reconnect známých zařízení; podstránky nastavení + cs/en; migrace Prefs; hlavní přepínač schránky se skrytím tlačítek; limity a expirace historie schránky; limit auto-příjmu; Capabilities (preference vs schopnosti pc); kvalita obrázků; skener qr s potvrzením a zavřením po připojení; nfc obrazovka (štítky, stavy, timeout, potvrzení); ikona podle palety; 6 palet + systémové barvy; výběr písma; animace a ohraničení panelů; potvrzovací okno sdílení androidu s nfc/qr a přímé cíle pro spárovaná pc; o aplikaci: zabezpečení a soukromí, licence písem; allowBackup vypnuto
- ČÁSTEČNĚ: nastavení jako celek (chybí výjimky na zařízení, volba jazyka, texty oznámení a zbytku aplikace cs/en, reset s výběrem rozsahu, licence knihoven); oznámení a ovládání (bez vlastních voleb druhů oznámení, záměrně); ikony launcheru (jen brand a navyOrange, ostatní palety používají brand a nastavení to říká); přenosy (kvalita, limit, náhledy po přijetí a při sdílení ano; náhled před přijetím ne)
- CHYBÍ: náhled příchozího obrázku před přijetím (vyžaduje miniaturu v pc protokolu), volby při shodě názvu přepsat / zeptat se, volby vlastní a vždy se zeptat, odstranění metadat u originálu, heic; cílová zařízení schránky; licence knihoven; README
- ODLOŽENO / BLOKOVÁNO (potřebuje návrh, souhlas nebo změnu pc, pc se nemění): nfc telefon-telefon (emulace karty nebo nový transport; texty "přiložte telefony k sobě" jsou v resources připravené, zapne je konstanta NFC_PHONE_TO_PHONE); qr kód telefonu pro opačný směr (čtečka v pc); pole "caps" v hello pc; živé sdílení obrazovky (níže); kontrola prvního párování přes mdns na straně pc

## podrobnosti k blokům a odložené body
- vzhled: implementováno v kódu, neověřeno buildem ani na telefonu: animace, ohraničení panelů, palety graphite / forest / plum / paper (paper je světlá), výběr písma (space grotesk + inter výchozí, inter, ibm plex sans, atkinson hyperlegible, systémové) s náhledem a uložením. hotovo v kódu (neověřeno): kompaktní stránka vzhledu se skupinami, panel rychlých akcí na nástěnce (soubor / fotka / schránka, "další": nfc a sdílení obrazovky jako informace), pravdivé stavy přenosů, odstraněn mezikrok po připojení; volba zaoblení (hranatější / výchozí / zaoblené) přes theme, obnovit výchozí vzhled, kontrast loga paper 5,8:1. zbývá: ikony launcheru pro nové palety (dnes fallback na brand), splash/windowBackground ve světlé paletě (themes.xml je tmavé, při startu může krátce svítit tmavé pozadí), stávající palety brand a navyOrange mají slabý kontrast okrajů (outline ~1,5:1, doporučeno 3:1; nezměněno kvůli zachování vzhledu)
- sdílení v systémové nabídce: implementováno v kódu (neověřeno), viz PROJECT_STATUS.md: potvrzovací obrazovka s obsahem a cílem, přímé cíle pro spárovaná pc a zařízení přes pc, odstranění zkratky při zapomenutí, stav cíle na kartě sdílení. zbývá: ověřit na telefonu (pořadí zkratek, přenos po zavření aktivity). nfc ze sdílení (blok 5): implementováno jako volba v okně sdílení (neověřeno)
- obrazovka po připojení: implementováno v kódu (neověřeno), skener i nfc obrazovka se po skutečném připojení zavřou; zachování sdíleného obsahu řešit v bloku sdílení
- nfc ze sdílení: ShareActivity už má obrazovku s volbou cíle (ShareConfirmScreen), nfc by se nabídlo právě tam jako alternativa k připojení, až bude jasné řešení rozporu telefon-telefon u nfc obrazovky. nfc dál jen zahajuje párování, soubory přes pc
- o aplikaci: zabezpečení a soukromí; nutné ověřit v kódu (Link.kt tls, pin otisku certifikátu, hmac párování, žádný cloud) a body k ověření uvést v předání; nepoužít slovo end-to-end
- závislost na pc: pole "caps" v hello (telefon ho už umí číst), qr čtečka v pc, přímé spojení telefon-telefon. pc se nemění
- NÁHLED OBRÁZKU PŘI PŘÍJMU PŘES NFC: nfc nepřenáší soubory (jen zahajuje párování, soubory jdou přes pc), takže příchozí obrázek dostane náhled v dialogu příchozí žádosti a v seznamu až po funkci "náhledy" z bodu 5. nic se nesimuluje; stav: plánováno
- o aplikaci / zabezpečení a soukromí: implementováno v kódu (neověřeno), viz PROJECT_STATUS.md
- widgety (tři akce, stav připojení, motivy) navážou samostatným úkolem AŽ PO nfc obrazovce

## další samostatný blok: nfc obrazovka (vzhled hotový v kódu, neověřeno; texty podle skutečné funkce, telefon-telefon BLOKOVÁNO)
pořadí: po ověření aktuálního celku nejdřív doladit nfc obrazovku, potom widgety.
požadovaný vzhled:
- uprostřed obsahové plochy výrazná jednoduchá nfc ikona
- pod ní hlavní text "přiložte telefony k sobě"
- menší doplnění "přibližte zadní strany telefonů k sobě"
- barvy podle aktuální palety, bez nové hlavní karty
- případná animace jemná a respektující vypnuté systémové animace
- dostupné zrušení a alternativa (zadání v tomto místě končí; předpokládám alternativu qr kód, k potvrzení)
omezení k vyřešení před implementací: existující NfcScreen čte štítek (reader mode) a nfc jen zahajuje párování; "přiložení telefonů k sobě" (telefon-telefon) vyžaduje u druhého telefonu emulaci karty (hce) nebo android beam, který android 13 nemá. to je v bodě 6 BLOKOVÁNO jako nový transport a potřebuje souhlas; vzhled obrazovky lze udělat hned, funkčnost telefon-telefon ne. nic se nesmí tvářit funkčně, dokud to neumí.
stav: vzhled hotový podle zadání v NfcScreen.kt (ikona, hlavní a vedlejší text, paleta, jemná animace respektující systémové nastavení, zrušit + qr). rozpor textu a funkce viz PROJECT_STATUS.md, zatím vysvětleno poznámkou na obrazovce. nfc a qr jsou dostupné i v okně sdílení androidu (implementováno, neověřeno). přepnutí textů na "přiložte telefony k sobě" je jedna konstanta NFC_PHONE_TO_PHONE, až bude spojení telefon-telefon existovat. widgety navazují až po ověření této obrazovky

## lokální živé sdílení obrazovky telefon -> telefon

stav: plánováno, zatím neimplementovat. žádný kód, závislost, oprávnění ani změna transportu.

účel: uživatel v reálném čase zobrazí obrazovku svého telefonu na druhém spárovaném telefonu. nejde o posílání screenshotů.

první rozsah:
- android -> android, obraz bez zvuku, pouze sledování bez vzdáleného ovládání
- lokální síť, bez cloudu, backendu a relay serveru
- jeden odesílatel, jeden příjemce
- žádné automatické nahrávání ani ukládání obrazu
- bez změn pc aplikace a pc protokolu

plánovaný průchod:
1. detail spárovaného a připojeného zařízení
2. "sdílet obrazovku"
3. příjemce výslovně přijme nabídku sledování
4. odesílatel udělí systémový souhlas se zachytáváním
5. teprve po obou souhlasech začne přenos obrazu
6. kterákoli strana může relaci ukončit

pořadí dialogů ověřit při návrhu podle pravidel androidu. před oběma souhlasy se nepřenáší žádný obraz.

plánované ovládání: samostatná obrazovka sledování, celý displej, správný poměr stran, reakce na otočení, kvalita úsporná / vyvážená / vysoká, viditelný stav relace, ukončení dostupné na obou stranách, srozumitelné chyby (odmítnutí, ztráta spojení, ukončení zachytávání systémem).

neslibuje se rozlišení, fps, latence ani kodek; určí se z návrhu a měření.

začlenění do ui:
- bez čtvrté hlavní karty (zůstává nástěnka / sdílení / nastavení)
- vstup v detailu připojeného zařízení, případně volitelná rychlá akce
- nová podstránka nastavení "sdílení obrazovky": povolit funkci (výchozí vypnuto), výchozí kvalita, zobrazovat rychlou akci, povolená spárovaná zařízení
- vypnutá funkce nezobrazuje rychlé akce ani panely mimo nastavení
- výběr důvěryhodného zařízení nenahrazuje souhlas s konkrétní relací
- do současného ui se nepřidává nic, co by slibovalo nehotovou funkci

podmínky implementace:
- veřejná android API, MediaProjection; ověřit požadavky foreground služby a souhlasů pro podporované verze
- android 13 zůstává minimum; novější možnosti podmínit verzí systému
- neobcházet chráněný obsah, systémová omezení ani odmítnutý souhlas; žádný root ani accessibility jako obejití
- obraz se neukládá do logů, diagnostiky ani historie
- identita protistrany přes existující párování; stejná síť není důvěra
- při odpojení nebo ukončení uvolnit zdroje a zastavit zachytávání
- podporu vyjednat přes capabilities, starší klienti zůstanou funkční
- před implementací ověřit, zda stávající mobilní transport a zabezpečení stačí; případné rozšíření navrhnout zvlášť, bez zásahu do pc
- nabídka se zobrazí jen u protistrany, která funkci podporuje

## mimo rozsah
streaming pc zvuku, cloud, reklamy, telemetrie, billing, placené funkce, google play. pc kód a pc protokol se nemění.
