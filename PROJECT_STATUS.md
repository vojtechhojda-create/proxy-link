# project status

## celek: nastavení + vzhled + panel rychlých akcí + pravdivé stavy (verze v kódu pořád 2.0.0-alpha.4, versionCode 4) - aktuální úkol
build neproběhl (v prostředí není android sdk ani gradle, pokus se neopakoval), na telefonu netestováno. ověřeno jen skriptem: všechny R.string klíče v kódu existují, cs a en mají shodných 291 klíčů, závorky ve zdrojích sedí (to není build).
hotovo v kódu:
- nastavení: hlavní stránka už byla stručný rozcestník devíti podstránek (zařízení, připojení, vzhled, schránka, přenosy, oznámení, soukromí, diagnostika, o aplikaci); nově u schránky ukazuje zapnuto / vypnuto podle skutečného stavu. nic dalšího nepřidáno, žádné nové přepínače
- vzhled: skupiny motiv a barvy / písmo / zaoblení / ikona aplikace; palety jsou jednořádkové karty s malými vzorky, zaoblení je jedna řada tří dlaždic s náhledem panelu, tlačítka a dialogu, karty menší (padding 12 dp); reset vzhledu a kontrast loga paper z minula zachovány
- nástěnka: jediný kompaktní panel rychlých akcí (QuickActionsPanel, ui/components/QuickActions.kt) pod připojeným zařízením: řádek "odeslat na: cíl · stav" (stav z Link.state: připojeno s ms / hostovské spojení / připojování / nutné párování / nepřipojeno; zařízení za pc se uvádí jako "X přes pc" a o samotném zařízení se nic netvrdí), čipy cíle, tlačítka soubor / fotka / schránka (schránka se skryje, když ji uživatel vypnul; při chybějící podpoře pc nebo hostovském spojení je tlačítko neaktivní a řádek pod ním říká proč), sbalené "další": poslat přes nfc (jen informace: nfc dnes čte pouze párovací štítky, mezi telefony neposílá) a sdílení obrazovky (jen informace: připravujeme, vyžaduje aktualizaci pc). nic se neposílá otevřením panelu, existující výběry a Link.sendFile / sendImage se nezměnily. při velkém systémovém písmu se tři tlačítka řadí pod sebe
- pravdivé stavy: řádky přenosů místo anglických "active / done" říkají čeká na přijetí příjemcem / odesílání / příjem / příjemce potvrdil (jen odchozí, po share.ok od pc) / přijato / příjemce odmítl / přerušeno nebo selhalo; odeslání schránky ohlásí úspěch jen když se opravdu něco předalo spojení (prázdná schránka dřív ukázala "odeslána"), text říká že pc příjem nepotvrzuje; toast u sdílení říká "odesílání na X" místo "odesláno"
- oprava buildu: ShareConfirmScreen.kt měl lokální proměnnou dev, která překrývala balíček dev.proxylink v odkazu na LinkService (oprava z dřívějška nebyla v kódu); nově import dev.proxylink.LinkService a LinkService::class.java. ověřeno jen čtením, ne překladem
- oprava buildu: ShareConfirmScreen.kt měl lokální proměnnou dev, která překrývala balíček dev.proxylink v odkazu na LinkService (oprava z dřívějška nebyla v kódu); nově import dev.proxylink.LinkService a LinkService::class.java. ověřeno jen čtením, ne překladem
- mezikrok po připojení: skener a nfc obrazovka už po připojení nenabízí "odeslat soubor" ani text "nic se neodešle..."; zůstává zavření a bezpečnostní potvrzení před připojením
- změněno: QuickActions.kt (nový), Common.kt, DashboardScreen.kt, SettingsScreen.kt, SettingsSections.kt, ScannerScreen.kt, NfcScreen.kt, SendActions.kt, ClipSendActivity.kt, Link.kt (sendClipboardNow, pushClipboard beze změny chování), strings cs/en
NENÍ ověřeno: překlad kódu, vzhled panelu a stránky vzhledu na zařízení, velké písmo, chování "další" při otáčení, toasty schránky na telefonu, přepis stavů přenosů na zařízení
NENÍ hotovo / záměrně odloženo: poslat přes nfc telefon-telefon, přenos obrazovky (potřebuje pc), kontrola release podpisu, nové widgety, interaktivní oznámení; zjištěno a nezměněno: existující bezpečnostní kontroly (pinning certifikátu, potvrzení při párování) jsem v tomto celku nerozšiřoval a nevytvářel jsem varování k nim; potvrzení doručení u schránky protokol nemá
další krok: blok a (verze 2.2.2 a versionCode, ověření opravy dev vs. dev.proxylink v ShareConfirmScreen, texty oznámení a chyb z Link.kt v angličtině), potom oznámení

## blok 3: branding a kompaktní ui, část 1 (verze v kódu pořád 2.0.0-alpha.4, versionCode 4) - aktuální úkol
ověřeno čtením kódu a přepočtem skriptem (ne buildem): ProxyLogo.kt, ic_launcher_foreground(.xml / _navy_orange) a ic_launcher_mono odpovídají branding/proxylink-icon-source.svg (cesty, oblouky, stroke 5,5, spojka, rx 30; launcher a mono jsou zmenšené 0,72 kolem středu, glyf v bezpečné zóně). launcher ikona, systémová monochromatická ikona a logo v aplikaci jsou oddělené vrstvy; palety mění jen barvy.
hotovo v kódu (build neproběhl, na telefonu netestováno):
- kontrast loga paleta paper: logoLine 1D4ED8 na dlaždici 1E293B měl 2,18:1, nově 60A5FA = 5,8:1 (jen barva, symbol beze změny; kontrast počítán skriptem)
- společné zaoblení hranatější / výchozí / zaoblené: ShapeStyle + ShapeStore (Shape.kt), použito jen přes ProxylinkTheme (Theme.kt), uloženo v Prefs.shape, načteno v MainActivity i ShareActivity; výchozí = dosavadní hodnoty. spodní list ve ShareConfirmScreen bere horní rohy z theme (dřív pevných 20 dp)
- vzhled: sekce "zaoblení" s malým náhledem panelu, tlačítka a dialogu u každé volby (kreslen vlastními poloměry volby)
- "obnovit výchozí vzhled" s potvrzením: vrací jen paletu brand, písmo, zaoblení a ikonu podle palety; zařízení, historii, schránku ani zabezpečení nemění
- texty cs/en (271 klíčů shodných)
- změněno: Palette.kt, Shape.kt, Theme.kt, Prefs.kt, MainActivity.kt, ShareActivity.kt, ShareConfirmScreen.kt, SettingsSections.kt, strings cs/en
NENÍ ověřeno: překlad (sdk ani gradle v prostředí nejsou), vzhled na zařízení, chování velkého systémového písma u nových karet, to že všechny komponenty berou MaterialTheme.shapes (ověřen jen jediný pevný RoundedCornerShape mimo Shape.kt, ten je opraven)
NENÍ hotovo z bloku 3: kompaktnější mezery, ikony a dialogy po obrazovkách, sjednocené prázdné / chybové / načítací stavy, kontrola kontrastu okrajů paper vs. brand a navyOrange (outline ~1,5:1), tmavé okno při startu ve světlé paletě, ikony launcheru pro další palety
další krok: zbytek bloku 3 (kompaktní mezery a dialogy), potom blok a (verze 2.2.2, ověření ShareConfirmScreen dev vs. dev.proxylink) a panel rychlých akcí

## přenosy: náhledy, konflikt názvu, otevření po přijetí (verze 2.0.0-alpha.4) - aktuální úkol
zjištěno z kódu: miniatury přijatých a odeslaných obrázků v historii už existovaly (HistoryRow, loadThumbnail). hotovo v kódu (build neproběhl, na telefonu netestováno):
- náhled sdíleného obrázku v potvrzovacím okně sdílení androidu (ShareConfirmScreen, UriThumb): miniatura prvního obrázku z uri, dokud platí oprávnění; nic se neukládá
- náhled přijatého obrázku v oznámení (BigPicture přes systémovou miniaturu), ostatní oznámení beze změny
- konflikt názvu: soubor se ukládá do Download/proxylink přes MediaStore, systém při shodě názvu zachová oba ("název (1).ext"), nic se nepřepíše; nově historie a oznámení ukazují název skutečně uložený (Link: InFile.name). volba "přepsat" nebo "zeptat se" NENÍ (přepis cizího souboru v MediaStore není bezpečně možný bez oprávnění). vysvětleno v nastavení přenosů
- otevření po přijetí: přepínač v přenosech (Prefs.openAfterReceive, výchozí vypnuto); soubor se otevře sám jen když je aplikace na obrazovce (Link.foreground), jinak ho otevře klepnutí na oznámení (android nedovolí aplikaci na pozadí otevírat okna); jen u úspěšně přijatých souborů (kontrolní součet sedí)
- texty cs/en, změněno: Link.kt, Notifier.kt, Prefs.kt, SettingsSections.kt, ShareConfirmScreen.kt, strings
NENÍ hotovo / odloženo (závisí na pc): náhled obrázku PŘED přijetím v dialogu žádosti. pc posílá v share.request jen název, velikost a mime; náhled by vyžadoval malou miniaturu v žádosti = změna protokolu pc (pc se nemění). nfc soubory nepřenáší, takže náhled u nfc znamená náhled po přijetí přes pc, ten je v oznámení a historii
NENÍ ověřeno: překlad, loadThumbnail pro uri ze sdílení (část aplikací dává uri, pro které miniatura selže; použije se zmenšené dekódování, jinak se náhled nezobrazí), otevření souboru za běhu, název s číslem na zařízení


## widgety (verze 2.0.0-alpha.3) - aktuální úkol
hotovo v kódu (build neproběhl, na telefonu netestováno; nové widgety se musí na plochu přidat ručně přes seznam widgetů):
- widget "ProxyLink akce" (3 buňky): soubor / fotka / schránka. soubor a fotka otevřou jen systémový výběr (WidgetSendActivity); odešle se to, co uživatel vybere, bez připojení se výběr vůbec neotevře a ukáže se důvod; fotka jde přes stávající Link.sendImage (respektuje kvalitu obrázků), soubor přes Link.sendFile, schránka přes stávající ClipSendActivity. tlačítko schránky z widgetu zmizí, když je schránka v nastavení vypnutá, a vrátí se po zapnutí (widgety se překreslí při změně přepínače)
- widget "ProxyLink stav": název pc a stav (připojeno / hostovské spojení / připojování / nepřipojeno), klepnutí otevře aplikaci. stav se čte přímo z Link; překresluje se při každé změně stavu spojení a nejvýš po 30 min systémem. proces zabitý systémem se projeví jako "nepřipojeno", nikdy jako odhad "připojeno"
- existující widget "odeslat schránku" zůstal, jen má nový vzhled a text v resources cs/en
- motivy: všechny widgety berou barvy z aktuální palety aplikace (surfaceContainer, onSurface, onSurfaceVariant, primary), případně z barev systému; překreslí se při změně palety. zaoblení rohů dělá systém (root @android:id/background). písmo widgetů je systémové (RemoteViews nemá vlastní písma); světlá paleta paper funguje
- nové: Widgets.kt (ClipWidget, ActionsWidget, StatusWidget a vykreslování; starý ClipWidget.kt odstraněn), WidgetSendActivity.kt, layouty widget_actions / widget_status / widget_clip, 3 ikony, info xml, manifest (2 receivery, aktivita), texty cs/en (258 klíčů)
NENÍ ověřeno: překlad, zobrazení v launcheru, barvy ve světlé paletě, chování po zabití procesu, to že photo picker drží oprávnění k obrázku po zavření průhledné aktivity (dokumentace androidu říká, že platí do restartu zařízení nebo zastavení aplikace; nezkoušeno)
NENÍ hotovo: konfigurační obrazovka widgetu, výběr cílového zařízení ve widgetu (odesílá se na aktuální spojení), aktualizace barev dynamických barev po změně světlý/tmavý systém bez další změny


## sjednocená verze 2.0.0-alpha.2 (versionCode 2) - aktuální stav
- předáváno jako jediný kořen proxy-link/ (proxylink-android, proxylink-pc beze změny, .github, .gitignore, README, proxylink-android/branding/proxylink-icon-source.svg, tyto dva soubory). žádné vnořené ani staré kopie ve výsledku; verzi jsem navýšil, aby šla v "o aplikaci" poznat nová instalace od staré
- zkontrolováno ve zdrojích: všechny dosud popsané změny jsou v kódu a každá je z ui nebo z manifestu skutečně volána (palety v seznamu vzhledu, FontStore v Theme, Capabilities v tlačítkách, limity historie v nastavení, ShareConfirm v ShareActivity, zkratky z Link, nfc a skener ve sdílení a navigaci). zkontrolováno jen čtením a skriptem, ne překladem
- build: android sdk ani gradle v prostředí nejsou, pokusy se neopakovaly; apk nový není. postup na windows:
  1) jdk 17, android sdk (ANDROID_HOME nebo local.properties se sdk.dir), gradle 8.9 (wrapper v repozitáři není)
  2) powershell: cd proxy-link\proxylink-android ; gradle :app:assembleDebug --stacktrace
  3) apk: proxy-link\proxylink-android\app\build\outputs\apk\debug\app-debug.apk (stejný příkaz běží i v .github/workflows/android.yml po pushi, artefakt app-debug.apk)
- nfc texty: obrazovka říká "přiložte telefon k NFC štítku" + qr alternativa; telefon-telefon není implementováno (konstanta NFC_PHONE_TO_PHONE = false)
- sdílení a velké soubory: Link.sendFile otevře stream souboru hned při sdílení (ještě v okně sdílení, dokud platí oprávnění k uri) a drží ho do přenosu, takže pozdější zánik oprávnění čtení už nepřeruší. nově okno před odesláním spustí LinkService (popředí), aby proces nezanikl po zavření okna. NEVYŘEŠENO / neověřeno: čekající žádost, kterou protistrana dlouho nepřijme, drží otevřený soubor; zabití procesu systémem přenos přeruší; nic z toho nebylo zkoušeno na telefonu. odesílá se jen tlačítkem "odeslat"


soubor vznikl až v tomto kroku; dřívější PROJECT_STATUS.md ani FEATURE_PLAN.md v repozitáři (main, 1 commit) neexistovaly.

## nfc obrazovka (blok 8) dodělána + nfc/qr v okně sdílení androidu (blok 5) - aktuální úkol
hotovo v kódu (build neproběhl, na telefonu netestováno):
- rozpor textu a funkce vyřešen konstantou NFC_PHONE_TO_PHONE = false v NfcScreen.kt (jako PHONE_QR_SUPPORTED ve skeneru). dokud aplikace neumí spojení dvou telefonů přes nfc (nemá emulaci karty ani transport telefon-telefon), obrazovka říká pravdu: "přiložte telefon ke štítku" / "přibližte zadní stranu telefonu ke štítku" (en: hold the phone to a pairing tag). požadované znění "přiložte telefony k sobě" / "přibližte zadní strany telefonů k sobě" je v resources připravené a zobrazí se po přepnutí konstanty na true spolu s podporou. poznámka o omezení pod tlačítky se pak skryje
- okno sdílení androidu (ShareConfirm): pokud cílové pc není připojeno, nabízí se v panelu cíle "připojit", "připojit přes nfc" (jen když telefon má nfc čip; zapnutí nfc řeší samotná obrazovka) a "naskenovat qr kód". nfc obrazovka i skener se otevřou přímo v tomto okně nad sdíleným obsahem, takže obsah, vybraný cíl a souhlas s odesláním zůstávají; po připojení se vrátí list s tlačítkem "odeslat". zpětné gesto z nfc/qr vrací na list, ne ke zrušení sdílení. nfc dál jen zahajuje párování, soubory jdou přes síť a pc; pc se nemění
- změněno: NfcScreen.kt, ShareConfirmScreen.kt, strings cs/en (244 klíčů shodných)
NENÍ ověřeno: překlad, reader mode nfc uvnitř průsvitné ShareActivity (reader mode se zapíná na aktivitě; v průsvitné by měl fungovat, ale nemám test), kamera ve ShareActivity, návrat do listu po připojení
NENÍ hotovo: skutečné spojení telefon-telefon přes nfc (hce nebo nový transport, potřebuje souhlas a návrh), qr kód telefonu pro opačný směr (vyžaduje čtečku v pc)

## sdílení v systémové nabídce androidu (blok 3)
hotovo v kódu (build neproběhl, na telefonu netestováno):
- běžný cíl ProxyLink pro text, obrázky a soubory zůstal (intent filtry beze změny)
- ShareActivity už nic neodesílá sama a neztrácí obsah: je z ní ComponentActivity s potvrzovací obrazovkou (ShareConfirmScreen.kt, dolní list v barvách palety). ukazuje obsah (název souboru / počet souborů / text), cílové zařízení, jeho stav a změnu cíle (čipy). odeslání jen výslovným tlačítkem "odeslat", zrušení vždy dostupné
- nepřipojený cíl: tlačítko "připojit" (Link.reconnect) a obrazovka počká, obsah zůstává; stav se mění podle skutečného Link.state. je-li telefon připojený k jinému pc, nabídne se "přepnout spojení" (Link.disconnect + reconnect); spojení je pořád jedno, nic se nepředstírá. cíl "jiné zařízení přes pc" je dostupný jen při spojení s pc a přítomnosti zařízení v seznamu
- přímé cíle (Sharing Shortcuts): pro každé spárované pc "pc:<id pc>" se skutečným názvem a stabilním id, pro zařízení viděná přes pc "dev:<id>"; počet omezen systémovým limitem (getMaxShortcutCountPerActivity); pořadí a zobrazení řídí android. po zapomenutí zařízení se odstraní dynamická i uložená zkratka (Shortcuts.remove); stará zkratka "pc" se odstraní; staré id zařízení bez předpony ShareActivity ještě rozumí
- karta sdílení v aplikaci ukazuje "odeslat na: <cíl> · připojeno / nepřipojeno"; změna cíle zůstává přes stávající čipy (TargetPicker)
- jak jsem vyložil rozpor ze zadání: "spárovaná zařízení" jsou pc ze seznamu spárovaných (jen ta app zná), "ostatní zařízení" jsou telefony viděné přes pc. mezi telefony není přímé spojení
- texty cs/en (241 klíčů shodných v obou souborech). změněno: ShareActivity.kt, ShareConfirmScreen.kt (nový), Shortcuts.kt, Link.kt, Common.kt, ShareScreen.kt, strings
NENÍ ověřeno: překlad, zobrazení a pořadí zkratek v systémové nabídce, přenos souboru po opuštění aktivity (oprávnění k uri ze sdílení platí jen po dobu aktivity; odeslání se spouští před jejím zavřením jako dřív, ale u velkého souboru nemám potvrzeno, že čtení doběhne), přepnutí spojení na telefonu
NENÍ hotovo: nfc ze sdílení (blok 5), pořadí směrování ShareConfirm k preferovanému zařízení, zachování obsahu při úplném zabití procesu

## nfc obrazovka: vzhled (blok 8)
hotovo v kódu (build neproběhl, na telefonu netestováno): uprostřed obsahové plochy jednoduchá nfc ikona na kulatém povrchu v barvách aktuální palety (primaryContainer), pod ní hlavní text "přiložte telefony k sobě" a menší "přibližte zadní strany telefonů k sobě" (cs; en: "hold the phones together" / "bring the backs of the phones close together"). dva tenké kruhy se při čekání jemně rozšiřují (2,2 s); bez pohybu, když jsou vypnuté systémové animace (ANIMATOR_DURATION_SCALE 0) nebo se na nic nečeká. dole vždy zrušit a alternativa "použít qr kód"; ostatní stavy (potvrzení, připojování, chyba, timeout, špatný štítek, nfc vypnuto/nepodporováno) mají stejné rozvržení; chování čtečky, potvrzení a zavření po připojení beze změny. bez nové hlavní karty. změněno: NfcScreen.kt, strings cs/en
UPOZORNĚNÍ NA ROZPOR: text "přiložte telefony k sobě" slibuje spojení telefon-telefon, které aplikace neumí (čte jen nfc štítky proxylink://, viz FEATURE_PLAN bod 6). proto je pod tlačítky viditelná poznámka, že zatím fungují jen štítky a pro druhý telefon je třeba qr kód. buď se text upraví na "přiložte telefon ke štítku", nebo se doplní hce/nový transport (potřebuje souhlas); rozhodnutí je na uživateli
NENÍ ověřeno: překlad, vzhled na zařízení (velikost 200 dp ikony při velkém písmu a na malém displeji), chování animace při vypnutých animacích

## kontrola úplnosti nastavení (stav kódu, ne zařízení)
legenda: [N] implementováno, neověřeno buildem ani na telefonu; [Č] částečně; [CH] chybí nebo odloženo. nic zde není "implementováno a ověřeno": žádná změna z těchto bloků neprošla překladem ani testem na telefonu (build prošel naposledy před celkem 4). celé nastavení proto NENÍ dokončené.
- zařízení: [N] seznam spárovaných, detail, zapomenout s potvrzením, preferované zařízení, reconnect, nové párování (qr). [CH] výjimky na zařízení, přejmenování zařízení
- připojení: [N] název telefonu (posílá se v hello), auto connect (Link ho opravdu čte). [Č] texty hotspotu a chyb z Link.kt zůstávají anglicky
- schránka: [N] hlavní přepínač, odesílání a příjem zvlášť, pauza (přežije restart), citlivý obsah, volitelná historie (výchozí vypnuto), limit počtu a doba uchování, vymazání. expirace se vyhodnocuje při startu, při uložení položky a při změně limitu, ne časovačem. [CH] cílová zařízení (jedno spojení s pc), přehled citlivosti nad rámec systémového příznaku
- přenosy: [N] auto-příjem s limitem MB, kvalita obrázků, vymazání seznamu položek. [CH] náhledy (včetně náhledu přijímaného obrázku, viz níže), konflikt názvu, otevření po přijetí, volby vlastní a vždy se zeptat, odstranění metadat u originálu, heic
- vzhled: [N] 6 palet + systémové barvy, výběr písma s náhledem, ikona podle palety (nové palety a systémové barvy dostanou ikonu brand a nastavení to říká), animace. [CH] vlastní ikony pro nové palety, tmavý splash při světlé paletě
- oznámení a ovládání: [N] start po restartu (BootReceiver ho čte), haptika (Haptics.kt ho čte), stav oznámení a odkaz do systému, výjimka z úspory baterie, dlaždice. [Č] texty oznámení a názvy kanálů jsou anglicky. [CH] vlastní volby druhů oznámení (nejsou, aby nebyly falešné přepínače)
- soukromí: [N] stav oprávnění, zapomenout všechna zařízení s potvrzením. [CH] reset s výběrem rozsahu
- diagnostika: [N] souhrn bez jmen, adres, tajemství a obsahu schránky, kopie označená jako citlivá, stav funkcí, limitů a verze protokolu pc. [Č] souhrn je anglicky záměrně
- o aplikaci: [N] verze, autor, nová sekce zabezpečení a soukromí (cs/en) s odkazy na existující správu, licence písem (OFL, texty v proxylink-android/licenses). [CH] licence knihoven (nelze bez ověření verzí a textů), kontrola překladu
kontroly ze zadání:
- vypnuté funkce: schránka po vypnutí zmizí z tlačítek nástěnky a sdílení, v nastavení zůstává přepínač a mazání; widget nelze skrýt, jen oznámí důvod. jiné volitelné funkce s panelem na nástěnce dnes neexistují
- vypnutí nemaže data: ano (historie zůstává, dokud ji uživatel nesmaže)
- nepřipojení a oprávnění vs. vypnutí: tlačítka se při chybějící podpoře pc nebo hostovském spojení jen deaktivují; nfc řádek rozlišuje nepodporováno / vypnuto v systému; stav oprávnění je zvlášť
- falešné přepínače pro budoucí pc funkce: nenalezeny; Capabilities jen čte, co pc ohlásí
- nežádoucí obrazovka po připojení: skener i nfc obrazovka se po skutečném připojení zavřou (neověřeno na zařízení)
- obnova po restartu: všechny hodnoty jsou v Prefs a načítají se při startu (palety, písmo, clipEnabled, pauza, limity); neověřeno na telefonu
drobnosti dokončené při kontrole: texty obrazovky nepřipojeno v resources cs/en, sekce zabezpečení v o aplikaci, android:allowBackup="false" (tajemství párování nesmí do automatické zálohy; po přeinstalaci se zařízení párují znovu)
body k ověření zabezpečení (nepotvrzeno kódem nebo mimo rozsah): (1) při prvním párování přes mdns pochází otisk certifikátu z oznámení v síti, ne z qr; chrání ho jednorázový kód, ale potřebuje zkontrolovat serverová strana pc; (2) kontrola názvu hostitele je vypnutá, identitu určuje jen pin; (3) tajemství a pin leží v SharedPreferences bez šifrování aplikací; (4) knihovna ML Kit má vlastní chování, které jsem neověřil; (5) nikdo nezkoumal, jak pc ukládá tajemství. certifikáty a nová kryptografie nejsou součástí tohoto celku

## palety a písma
hotovo v kódu (build neproběhl, na telefonu netestováno):
- palety (schválené): graphite, forest, plum (tmavé) a paper (světlá) v Palettes + PaletteStore; Palette má nové pole dark, motiv podle něj nastaví barvy stavových lišt. všechny material role vyplněné; kontrast WCAG spočítán skriptem: text 4,5:1, výrazné prvky a okraje 3:1, bez výjimek u nových palet
- text a ikony nad kamerou skeneru jsou vždy bílé (dřív inverseSurface, ve světlé paletě by na černém pozadí zmizely)
- launcher ikona: alias existuje jen pro brand a navyOrange; nové palety a systémové barvy používají brand, nastavení to u přepínače říká (icon_fallback). žádný duplicitní launcher vstup
- písmo: AppFont (default = space grotesk + inter beze změny, inter, ibm plex sans, atkinson hyperlegible, systémové), FontStore, Prefs.font, načtení v MainActivity, náhled každého písma v nastavení vzhledu, aplikuje se hned. monospace pro technické údaje zůstává (MonoStyle), není volitelné
- nová písma: ibm plex sans (variabilní, wght+wdth, 525 kB), atkinson hyperlegible (regular + bold, bez středního řezu); obě ověřena fonttools: česká diakritika kompletní, licence SIL OFL vložená v souboru. texty OFL pro všech pět písem v proxylink-android/licenses/ (z google/fonts), v aplikaci odkaz v textu o písmech. apk bude asi o 0,65 MB větší
- změněno: Palette.kt, Theme.kt, Type.kt, Prefs.kt, MainActivity.kt, LauncherIcon.kt, ScannerScreen.kt, SettingsSections.kt, strings cs/en (193 klíčů), res/font, licenses/
NENÍ ověřeno: překlad (např. FontVariation u plex se dvěma osami, AppFont.entries), skutečný vzhled paper na zařízení, čitelnost všech obrazovek ve světlé paletě (jen kontrasty rolí, ne obrazovky), splash při startu ve světlé paletě

## animace a detaily panelů (jemné a rychlé)
hotovo v kódu (build neproběhl, na telefonu netestováno): přechody obrazovek 180/120 ms (fade + malý posun, včetně zpětného přechodu), nástěnka plynule přechází mezi nepřipojeno a připojeno (Crossfade 200 ms), animovaný ukazatel průběhu přenosu (150 ms), položky seznamu ve sdílení se při změně přesouvají (animateItem), tlačítka se při stisku na 90 ms zmenší na 97 %, panely mají jemné ohraničení (outlineVariant, 50 % průhlednost). přepínače a čipy už animuje material 3. animace respektují systémové "odstranit animace". změněno: Common.kt, ProxylinkApp.kt, DashboardScreen.kt, ShareScreen.kt
NENÍ: animace průběhu párování/chyby ve skeneru, vlastní animace loga, výběr písma a nové palety (schváleny uživatelem, další blok). doladění mezer a zaoblení naslepo nedělám: čekám na konkrétní místa ze screenshotů
dále po ověření: nové palety (graphite, forest, plum, paper) s fallbackem launcher ikony na brand + výběr písma s náhledem

## odstranění obrazovky po připojení (blok 4)
hotovo v kódu (build neproběhl, na telefonu netestováno): screenshot odpovídá ScanResult ve ScannerScreen.kt (stejné texty má i NfcScreen). po skutečném přechodu Link.state na CONNECTED se skener i nfc obrazovka zavřou, ukáže se krátký toast "připojeno k ..." a standardní UI (nástěnka nebo stránka, odkud se přišlo). počítá se jen změna stavu pozorovaná po skenu, aby starší spojení nezavřelo obrazovku dřív než nové. párování, reconnect, potvrzení odkazů (PairLinkDialog), chyby a čekání zůstaly; tlačítka "odeslat soubor" / "hotovo" zůstávají jen jako záloha, když se změna stavu nepozoruje (např. sken při již aktivním spojení se stejným zařízením). na nfc obrazovce tlačítko odeslání souboru po připojení už nikdo neuvidí; odeslání je běžná akce na nástěnce
sdílený obsah: dnes žádný průchod neexistuje (ShareActivity aplikaci neotevírá), proto nebylo co zachovat; navážat bude blok sdílení v systémové nabídce
změněno: ScannerScreen.kt, NfcScreen.kt

## nastavení: dokončení (celek 3, druhá část)
hotovo v kódu (build NEPROBĚHL: v prostředí není android sdk ani gradle; na telefonu netestováno):
- schránka jako celek: nový hlavní přepínač Prefs.clipEnabled (výchozí zapnuto; Link.changeClipEnabled). vypnuto = žádná automatická synchronizace, žádné tlačítko schránky na nástěnce ani ve sdílení, widget jen oznámí důvod, nic se nezaznamenává. uložená historie zůstává, dokud ji uživatel nesmaže (vypnutí nic nemaže). v nastavení zůstane přepínač a smazání historie
- historie schránky: limit počtu (20/50/100/bez limitu, výchozí 100) a doba uchování (1/7/30 dní/do smazání, výchozí 30), jen pro položky schránky, soubory se nemažou. vyhodnocuje se při startu, při uložení nové položky a při změně limitu (History.enforceClipLimits), NE časovačem: položka může přežít svůj čas, když aplikace stojí. uživatelé, kteří už historii měli zapnutou, dostanou při migraci bez limitů (nic se jim nesmaže)
- migrace Prefs: klíč schema (2); autosync se zapíše do clipsend/cliprecv, neplatná kvalita obrázků se vrátí na originál; staré klíče zůstávají (downgrade funguje)
- přenosy: auto-příjem má volitelný limit velikosti souboru (10/100/1000 MB/bez limitu, výchozí bez limitu = dosavadní chování); větší soubor čeká na potvrzení jako při vypnutém auto-příjmu. texty a odkazy se tím nemění
- preference vs schopnosti protistrany: nový core/Capabilities.kt (Feature TEXT/FILES/CLIPBOARD). pc dnes v hello posílá jen v=2, což má všechny tři funkce; pokud pc jednou přidá pole "caps", telefon ho přečte a rozhoduje. dostupné = uživatel chce (Prefs) A protistrana umí (Capabilities). chybějící podpora se ukáže jako nedostupná (tlačítko neaktivní, důvod ve stavu schránky), nikdy jako "vypnuto uživatelem"; hostovské spojení schránku neposílá (tlačítko neaktivní). pc se nemění
- cs/en: dokončena nástěnka, karta sdílení (záhlaví, filtry), názvy hlavních karet, tlačítka odeslání, dialog příchozí žádosti, toasty widgetu, nové texty nastavení (187 klíčů, shodné v obou souborech, všechna R.string v kódu mají klíč; zkontrolováno skriptem)
- přístupnost: volby palet a kvality obrázků jsou selectable (čtečka hlásí vybráno), popis tlačítka zpět je přeložený, tlačítka pauzy se při zvětšeném písmu (fontScale > 1.3) řadí pod sebe, limity jsou čipy s posunem do strany
- diagnostika: přibyl stav funkce schránky, limity historie, limit auto-příjmu, verze protokolu pc (bez jmen, adres a obsahu)
ověřeno staticky: závorky všech kotlin souborů, xml cs/en well-formed, klíče; písma: inter, space grotesk i inconsolata jsou variabilní ttf s vloženou licencí SIL OFL (z tabulky name), obsahují celou českou diakritiku a uvozovky „“ (kontrola fonttools). ověřeno beze změn: UI = space grotesk (nadpisy) + inter (text), monospace jen pro technické údaje (MonoStyle)
NENÍ ověřeno: překlad (kompilace), chování na telefonu, vzhled přepínačů při velkém písmu, migrace na zařízení s dřívější instalací
stále chybí v celku 3: výjimky na zařízení, volba jazyka v aplikaci, texty zbytku aplikace v resources (NotConnectedScreen, ShareActivity, oznámení, HistoryRow, TransferRow stavy), reset s výběrem rozsahu, licence knihoven. celkový text "soubory" i widget: widget nelze za běhu skrýt bez zásahu do uživatelových widgetů, proto jen oznámí důvod
rozpory přiložených podkladů: přiložené PROJECT_STATUS.md a FEATURE_PLAN.md se z disku nedaly porovnat bajt po bajtu (jsou jen v zadání); obsah po sekcích odpovídá verzím v zipu (13200 a 5074 B). zip obsahuje app/build s apk; nepoužito jako důkaz, zdroj je kód

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

## starší seznam úkolů
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

## dále (předchozí stav)
po zelené sestavě této změny a testu na telefonu (reconnect, tile, ikona, čeština): zbytek celku 3 (skrývání vypnutých funkcí, výjimky zařízení), potom celek 4 (schránka).

## dále
1. build v github actions nebo android studiu + test na telefonu (schránka vyp/zap, limity historie, migrace, limit auto-příjmu, čeština, velké písmo)
2. po zelené sestavě: blok 4 (odstranění obrazovky po připojení), potom sdílení v systémové nabídce, nfc ze sdílení, o aplikaci - zabezpečení a soukromí, vzhled (palety, písma)
