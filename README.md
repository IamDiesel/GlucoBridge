# GlucoBridge

Schlanke **Android- & Wear-OS-App**, die den **eigenen** Glukosewert anzeigt und auf die Pixel Watch bringt.

> Forschungs- & Lernprojekt zu Interoperabilität und Kryptographie. **Kein Medizinprodukt.**
> Nutzung nur mit **eigenen** Daten. Weder dieses Repo noch der Forschungsbericht liefern eine
> Anleitung zur Nachbildung herstellerspezifischer Komponenten.

## Motivation

Der eigene Glukosewert ist heute über eine **offene REST-/Follower-API** abrufbar (von vielen Apps
und öffentlichen Repos genutzt) — die aber jederzeit eingeschränkt oder abgeschaltet werden kann.
Damit der Zugriff auf die **eigenen** Werte erhalten bleibt, nutzt GlucoBridge zusätzlich die
**verschlüsselte App-Schnittstelle (ALE)** als robusten Standardpfad.

Als reiner **Cloud-Follower** greift die App **weder den Sensor noch die Original-App** an — der
klinische LibreLinkUp-/LibreView-Fluss (z. B. für die behandelnde Ärztin/den Arzt) bleibt intakt.
Genau diese Lücke — Interoperabilität **ohne** Entkopplung von der offiziellen App — schließt das Projekt.

## Hinweise (bitte lesen)

- **Kein Medizinprodukt** — nicht für Diagnose, Therapie oder Dosierung. Maßgeblich sind das
  offizielle Messsystem und ärztlicher Rat.
- **Nur eigene Daten / eigenes Konto.** Alles bleibt **lokal** (Session Keystore-verschlüsselt,
  kein Upload; keine Passwörter/Token in Logs).
- **Keine herstellerspezifischen Artefakte im Repo** (git-ignoriert), **keine** Reproduktions-Anleitung.
- **Marken** gehören ihren Inhabern; keine Verbindung/Billigung. Nutzung „wie besehen" (as-is), auf eigenes Risiko.

## Funktionen

- Login (verschlüsselt & persistent), aktueller Wert + Trendpfeil + Zonenfarbe + Alter.
- Interaktiver Verlaufsgraph (Cursor, Verschieben, Zoom, Grid, Zielband); persistenter Verlauf (Room).
- Einstellungen: Pollingintervall, Zielbereich, Einheit (mg/dL ⇄ mmol/L), Datenquelle,
  eigener Export-Tab (siehe „Export & Interoperabilität").
- Hintergrund-Service mit dauerhafter Benachrichtigung.
- Pixel Watch: App-Screen mit Verlaufs-Sparkline (Cursor-Auswahl) und vergrößertem Graph,
  Tile (Kachel), Complication (Zifferblatt) — via Wearable Data Layer;
  Tile & Complication öffnen beim Antippen die Watch-App.
- Export der eigenen Werte: CSV, FHIR-JSON, Nightscout (Datei + Upload) und
  Health Connect (Google Health) — pro Ziel opt-in.

## Datenquellen

- **REST (offen/community):** funktioniert heute, potenziell abschaltbar.
- **ALE (verschlüsselt):** robuster Standardpfad; Implementierung in der **git-ignorierten** Zone
  `:data:source-ale` (nicht im Repo). Der Build ist **absent-safe** → ohne die Zone läuft die App
  über REST. Die Quelle ist in den Einstellungen wählbar.

## Export & Interoperabilität

Optionaler Export der eigenen Werte an weitere Ziele — je Ziel in einem eigenen
Einstellungen-Tab an-/abschaltbar, opt-in und lokal ausgelöst.

- **Health Connect (Google Health):** schreibt `BloodGlucoseRecord` automatisch im Poll-Takt
  (`specimenSource = interstitial fluid`, lokaler `zoneOffset` für sauberes Tages-Bucketing).
  Andere Health-Connect-Apps können die Werte danach lesen. Ersetzt den offiziellen
  LibreLinkUp-/LibreView-Fluss nicht.
- **Nightscout:** Datei-Export und Upload (automatisch + „Zeitraum jetzt hochladen").
- **CSV:** Datei-Export für Tabellen/Auswertung.
- **FHIR-JSON:** Datei-Export im FHIR-Format für Interop mit Gesundheits-Ökosystemen.

Alle Exporte betreffen ausschließlich die eigenen Daten und bleiben unter eigener Kontrolle.

## ALE-Zone: lokal bereitzustellende Dateien

Für den ALE-Betrieb müssen folgende herstellerspezifische Artefakte lokal in der git-ignorierten
Zone liegen (bewusst **nicht** Teil des Repos):

| Datei | Beschreibung | Ablageort in der App |
|---|---|---|
| `libb11bb8.so` | native White-Box-Krypto-Bibliothek (arm64-v8a) | `data/source-ale/src/main/jniLibs/arm64-v8a/` |
| `libc++_shared.so` | Laufzeit-Abhängigkeit von `libb11bb8.so` | `data/source-ale/src/main/jniLibs/arm64-v8a/` |
| `cdfe1.bin` | statischer, exportierter Blob (Quellmaterial „data1") | `data/source-ale/src/main/assets/ale/` |
| `cdfe2.bin` | statischer, exportierter Blob (Quellmaterial „data2") | `data/source-ale/src/main/assets/ale/` |
| `glucoale.c` | JNI-Glue zur White-Box (build-spezifische **Offsets**, Key-Ladder, kp-Parser). Als Vorlage liegt `docs/ale/glucoale_dummy.c` im Repo – **nach `glucoale.c` umbenennen/kopieren** und die selbst ermittelten Werte eintragen. | `data/source-ale/src/main/cpp/` |

Alle genannten Artefakte sind an eine App-/Bibliotheksversion gebunden und müssen nach einem
brechenden Update für die **eigene** Installation erneut ermittelt und eingetragen werden.
*Es wird bewusst keine Anleitung zur Beschaffung/Erzeugung dieser Artefakte bereitgestellt –
weder hier noch im Forschungsbericht.*

### Vorlage `docs/ale/glucoale_dummy.c`

Im Repo liegt nur eine **strukturerhaltende Dummy-Fassung** der JNI-Glue. Sie zeigt den Aufbau
der ALE-Anbindung, enthält aber **keinerlei reale Werte oder Algorithmen** und liefert nur
Fehlschläge (die App fällt dann auf REST zurück). Für den eigenen ALE-Betrieb die Datei nach
`data/source-ale/src/main/cpp/glucoale.c` kopieren/umbenennen und die mit `[DUMMY]` bzw.
`[DUMMY-STUB]` markierten Stellen selbst füllen. Als **Dummy** hinterlegt sind:

- **VMA-Offsets** (`OFF_*`) – Platzhalter `0x000000`
- **Erfolgs-Returncode** (`SKB_OK`) – Platzhalter
- **Key-Ladder-Parameter** (`U1_K*`, `U2_K*`) – Platzhalter `0`
- **Name des exportierten White-Box-Symbols** – Platzhalter-String
- **ABI-Signaturen der White-Box-Funktionen** – auf generische Zeiger neutralisiert
- **kp-Bündel-Parser** (Formatlogik) – als Stub entfernt
- **Provisioning-Ablauf** (Key-Ladder-Sequenz) – als Stub entfernt

Die native Bibliothek (`*.so`) und die statischen Blobs (`cdfe1/cdfe2`) sind ohnehin nicht Teil
des Repos (siehe Tabelle oben).

## IP-Trennung

Die stabile SPI (`:core:glucose-api`) ist IP-frei. Schutz: `.gitignore` deckt die IP-Zone ab; ein
Pre-Commit-Guard bricht bei verbotenen Pfaden/Inhalten ab. Aktivierung:

```bash
git config core.hooksPath .githooks
```

## Architektur

Multi-Modul, Clean Architecture / MVVM, manuelle Constructor-DI, Coroutines/Flow.

```
:app              App (UI/Compose, DI, Room, verschl. Session, Wear-Push, Service, Export/Health-Connect)
:wear             Wear-OS-App (Compose, Tile, Complication, Data-Layer-Empfang)
:core:model       Domänenmodelle
:core:glucose-api SPI: GlucoseSource (stabile, IP-freie Grenze)
:domain           Ports (Repository/Session/History/Settings), Zustandsmodell
:data:source-rest offene REST-Quelle
:data:repository  Quellenauswahl + Repository-Implementierung
:data:source-ale  (git-ignoriert) — NICHT im Repo
```

## Build & Ausführung

- **Android Studio** (aktuell), JDK 11, Android SDK (compileSdk 37), minSdk 26 (App) / 30 (Wear).
- Ohne die git-ignorierte ALE-Zone baut die App ohne NDK über REST.
- **Handy:** Run-Konfiguration `app`. **Uhr:** Run-Konfiguration `wear` (gekoppelte/WLAN-Debug-Uhr).

## Forschungsbericht

Öffentlich, in einem **separaten Repository** geführt — bewusste Trennung von Code und Bericht;
ebenfalls ohne Anleitungscharakter.

> 🔗 **Forschungsbericht:** [GitHub](https://github.com/IamDiesel/librelinkup-ale-writeup.git)

## Lizenz / rechtlicher Rahmen

Persönliches Forschungs- & Lernprojekt. Keine Weitergabe herstellerspezifischer Artefakte. Nutzung
im Rahmen der geltenden Rechtslage und der jeweiligen Nutzungsbedingungen.
