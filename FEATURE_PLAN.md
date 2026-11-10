# feature plan

backlog a stav požadavků. soubor vznikl v tomto kroku; dříve v repozitáři nebyl. stav = plánováno / rozpracováno / implementováno (neověřeno) / ověřeno. nic zde není "ověřeno", dokud neproběhl test na zařízení.

## stav podle priorit
1. dlaždice rychlého nastavení: implementováno, build prošel; na telefonu neověřeno, viz PROJECT_STATUS.md
2. reconnect známých zařízení: implementováno, build prošel; na telefonu neověřeno (restart, výpadek a návrat sítě)
3. hierarchie a trvalé ukládání nastavení: rozpracováno; podstránky a texty cs/en v nastavení hotové v kódu; chybí výjimky zařízení, skrývání vypnutých funkcí, volba jazyka v aplikaci, texty zbytku aplikace v resources
4. schránka: rozpracováno; implementováno (neověřeno): oddělené odesílání a příjem, pauza, citlivý obsah, volitelná historie (výchozí vypnuto), stav a důvod nečinnosti. chybí: cílová zařízení, limit a doba uchování historie
5. přenosy, náhledy, kvalita obrázků: rozpracováno; kvalita obrázků (originál / vysoká / úsporná) implementována (neověřeno). plánováno: náhledy, konflikt názvu, limit auto-příjmu, otevření po přijetí, volby vlastní a vždy se zeptat
6. nfc: rozpracováno; hotové jen potvrzení odkazů z nfc štítku a skener qr s explicitním odesláním souboru (build neproběhl). nfc obrazovka (reader mode, stavy, timeout, potvrzení) implementována (neověřeno). plánováno: hce pro opačný směr. BLOKOVÁNO: přímé posílání telefon–telefon přes nfc + wi-fi/hotspot vyžaduje serverovou část v telefonu (tls, párování, protokol) = nový transport, potřebuje návrh a souhlas; qr kód telefonu pro opačný směr vyžaduje čtečku qr v pc aplikaci (pc se nemění)
7. vzhled, ikony, oznámení, diagnostika: rozpracováno; ikona podle palety implementována (neověřeno), diagnostika základní
8. licence a dokumentace: plánováno; README zatím neaktualizováno

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
