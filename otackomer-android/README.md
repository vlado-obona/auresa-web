# Otáčkomer – otáčky jednovalca z mikrofónu telefónu

Natívna Android aplikácia (Kotlin, Jetpack Compose), ktorá meria otáčky
motora jednovalca zo zvuku. Vznikla na nastavenie voľnobehu KTM 500 EXC‑F
2018 v garáži: veľké číslo čitateľné na diaľku, zelené pásmo predpísaného
voľnobehu 1 800 – 1 900 ot./min a červený strop 7 000 ot./min pre prvú
motohodinu zábehu (obe hodnoty podľa manuálu KTM).

Funguje kompletne offline, nič nenahráva ani neodosiela.

## Zostavenie

Požiadavky: JDK 17 alebo novší, Android SDK s platformou 35 (Android Studio
si ho stiahne samo). Gradle sa stiahne cez wrapper.

```bash
cd otackomer-android
./gradlew assembleDebug          # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # unit testy algoritmu
./gradlew installDebug           # inštalácia na pripojený telefón
```

V Android Studiu: *File → Open…* → priečinok `otackomer-android`.

| | |
|---|---|
| Package | `sk.rallysupport.otackomer` |
| minSdk / targetSdk / compileSdk | 24 / 35 / 35 |
| Gradle / AGP / Kotlin | 8.14.3 / 8.9.1 / 2.1.10 |
| UI | Jetpack Compose, Material 3 |
| Nastavenia | DataStore Preferences |

## Ako to funguje

Telefónne mikrofóny frekvencie pod ~100 Hz väčšinou odrežú, takže základná
frekvencia zážihov (voľnobeh 4‑taktu ≈ 15 Hz) v zázname priamo nie je.
Zážihy sú tam ale stále ako **modulácia hlasitosti** – každý výfukový pulz
je krátky výbuch hluku. Preto sa nemeria spektrum zvuku, ale **perióda
obálky**.

### 1. Snímanie zvuku (`audio/AudioCapture.kt`)

* `AudioRecord`, 48 000 Hz, mono, PCM 16 bit.
* Zdroj `UNPROCESSED`, ak ho zariadenie deklaruje cez
  `AudioManager.getProperty(PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED)`;
  inak `VOICE_RECOGNITION`, až potom `MIC`.
* Pri fallbacku sa explicitne vypnú `AutomaticGainControl`,
  `NoiseSuppressor` a `AcousticEchoCanceler` (ak sú dostupné). AGC a
  potlačenie šumu by zničili obálku, na ktorej meranie stojí.
* Čítanie beží v coroutine na `Dispatchers.IO` v 50 ms blokoch; mikrofón sa
  uvoľní pri zastavení aj pri odchode aplikácie na pozadie (`onStop`).

### 2. Obálka signálu (`dsp/EnvelopeExtractor.kt`)

1. každá vzorka sa usmerní (`abs`),
2. jednopólový dolnopriepustný filter 120 Hz:
   `alpha = 1 − exp(−2π·120/fs)`, `lp += alpha·(|x| − lp)`,
3. decimácia na 1 000 Hz (každá 48. hodnota),
4. kruhový buffer na ~4 s (4 000 vzoriek obálky).

### 3. Autokorelácia obálky – NSDF (`dsp/RpmEstimator.kt`)

Každých 200 ms zvuku sa vezme posledných 2 600 vzoriek obálky (2,6 s),
odčíta sa priemer a pre každé oneskorenie τ sa spočíta normalizovaná
autokorelácia podľa McLeodovej metódy:

```
nsdf[τ] = 2·Σ x[i]·x[i+τ] / Σ (x[i]² + x[i+τ]²)
```

Rozsah τ vychádza z rozsahu otáčok 600 – 12 000 ot./min:
`τ = envRate·60·revsPerFiring / rpm`, kde `revsPerFiring = 2` pre 4‑takt
(jeden zážih na dve otáčky) a `1` pre 2‑takt.

Vyberie sa **prvý lokálny vrchol nad 0,85 × globálne maximum**, nie
globálne maximum. Autokorelácia periodického signálu má vrcholy aj pri
dvojnásobku, trojnásobku… periódy a tie bývajú rovnako vysoké; keby sa bral
najvyšší, appka by občas hlásila polovičné otáčky. Poloha vrcholu sa spresní
parabolickou interpoláciou cez tri susedné body.

### 4. Prepočet a vyhladenie (`dsp/RpmSmoother.kt`, `dsp/TachometerEngine.kt`)

* `rpm = envRate·60·revsPerFiring / perióda`
* medián z posledných 7 odhadov, potom exponenciálne vyhladenie
  s koeficientom 0,4, zobrazenie zaokrúhlené na desiatky.
* Ak je hodnota NSDF vo vrchole pod **0,40** („zhoda signálu“ pod 40 %)
  alebo je špičková úroveň vstupu pod 0,4 % plného rozsahu, nová hodnota sa
  **neprijme** – posledná ostane zamrazená a stavový riadok ukáže dôvod
  („slabý zvuk“ vs. „priveľa hluku“).

### Presnosť na syntetickom signáli

Unit testy kŕmia algoritmus pulzným sledom (tlmené sínusové výbuchy 1,4 kHz
s časovou konštantou 4 ms) s prekryvom bieleho šumu. Výsledky pre 4‑takt:

| ot./min | 700 | 1 000 | 1 850 | 3 000 | 5 000 | 6 500 | 8 000 | 9 500 | 11 000 |
|---|---|---|---|---|---|---|---|---|---|
| chyba | 0 | 0 | 0 | 0 | 0 | +10 | 0 | −20 | −20 |

Známe obmedzenie: pri 2‑takte nad ~7 000 ot./min pripadá na periódu
zážihov menej než 8 vzoriek obálky (1 kHz), vrchol autokorelácie padne
medzi dve mriežkové hodnoty a detektor môže skočiť na dvojnásobnú periódu.
Pre cieľový 4‑taktný motor to nevadí (perióda je dvojnásobná).

## Používateľské rozhranie

* Obrovské číslo otáčok s tabulárnymi číslicami; v pásme 1 800 – 1 900
  zozelenie.
* Vodorovná stupnica 0 – 10 000 s ukazovateľom, sýtym pásmom voľnobehu,
  svetlým pracovným pásmom a červenou čiarou na 7 000.
* Pracovné pásmo (predvolene 3 500 – 7 000) si používateľ nastaví sám;
  KTM pre tento motor žiadne odporúčané pásmo nezverejňuje, preto sa v UI
  neprezentuje ako údaj od výrobcu. Ukladá sa cez DataStore.
* Priebehový graf posledných 60 s, indikátor vstupnej úrovne, zhoda signálu
  v percentách, maximum, použitý zdroj zvuku.
* Prepínač 4T / 2T, jedno veľké tlačidlo Spustiť / Zastaviť (64 dp).
* Počas merania drží obrazovku zapnutú (`FLAG_KEEP_SCREEN_ON`).
* Tmavá paleta je primárna, svetlý režim sleduje systém. Ak má používateľ
  v systéme vypnuté animácie, ukazovateľ sa neanimuje.
* Slovenské texty, popisky pre čítačku obrazovky.

## Štruktúra

```
app/src/main/kotlin/sk/rallysupport/otackomer/
  MainActivity.kt            jedna Activity, keep-screen-on, stop na pozadí
  audio/AudioCapture.kt      AudioRecord + výber zdroja + vypnutie efektov
  dsp/EnvelopeExtractor.kt   usmernenie, LP 120 Hz, decimácia, kruhový buffer
  dsp/RpmEstimator.kt        NSDF, prvý vrchol nad prahom, parabola
  dsp/RpmSmoother.kt         medián 7 + EMA 0,4
  dsp/TachometerEngine.kt    spojenie, prahy, zamrazenie hodnoty
  data/SettingsRepository.kt DataStore (pracovné pásmo, typ motora)
  ui/TachometerViewModel.kt  stav obrazovky, snímanie v coroutine
  ui/TachometerApp.kt        oprávnenie RECORD_AUDIO, keep-screen-on
  ui/TachometerScreen.kt     Compose UI (číslo, stupnica, graf, ovládanie)
  ui/theme/Theme.kt          paleta, Material 3
app/src/test/kotlin/…/dsp/   unit testy so syntetickým signálom
```

Balík `dsp` nemá žiadne závislosti na Androide – dá sa testovať a ladiť na
bežnom JVM.

## Tipy na meranie

Telefón drž asi pol metra od koncovky výfuku, nie priamo v prúde plynov.
Meranie je najpresnejšie na ustálených otáčkach; pri prudkej zmene sa
hodnota ustáli asi za sekundu.
