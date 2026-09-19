# GlucoBridge

Schlanke, architektonisch saubere Android-App (Kotlin/Jetpack Compose), die den **eigenen**
Glukosewert anzeigt und an eine **Pixel Watch** weiterreicht.

> **Reines Forschungs- und Lernprojekt** zu Interoperabilität, Protokollen und Kryptographie.
> **Kein Produkt, kein Medizinprodukt.** Weder dieses Repository noch der begleitende
> Forschungsbericht enthalten eine Anleitung zur Nachbildung herstellerspezifischer Komponenten.

---

## Motivation: persönliche Interoperabilität

Der eigene Glukosewert lässt sich heute über eine **offene, community-dokumentierte REST-/
Follower-Schnittstelle** abrufen — genutzt von zahlreichen Drittanwendungen und öffentlichen
GitHub-Projekten. Diese offene Schnittstelle kann jedoch jederzeit **eingeschränkt oder
abgeschaltet** werden.

Damit der Zugriff auf die **eigenen** Werte erhalten bleibt, untersucht dieses Projekt zusätzlich
die **aktuelle, verschlüsselte App-Schnittstelle (ALE)** und nutzt sie als **Rückfall- bzw.
Standardpfad** für die persönliche Interoperabilität. Zielsetzung ist damit zweigeteilt:

- **Persönliche Interoperabilität** — dauerhafter, eigenständiger Zugriff auf die **eigenen** Daten,
  robust gegen eine Abschaltung der offenen API.
- **Globaler Forschungszweck** — Wissensaufbau und -vermittlung zu Protokoll- und
  Kryptographie-Interoperabilität.

---

## Zweck & Rahmen (bitte lesen)

- **Reine Forschung & Wissensvermittlung.** Es geht um das Verstehen und Dokumentieren von
  Interoperabilität — nicht um ein einsatzfertiges Werkzeug für Dritte.
- **Keine Anleitung / keine Reproduktion.** Weder in diesem Repository noch im öffentlichen
  Forschungsbericht wird eine Schritt-für-Schritt-Anleitung, ein Rezept oder ausführbarer Code
  zur **Nachbildung** der herstellerspezifischen (verschlüsselten) Komponenten bereitgestellt.
  Die dafür nötige Zone ist bewusst **nicht Teil** dieses Repos (siehe *IP-Trennung*).
- **Nur eigene Daten.** Nutzung ausschließlich mit **eigenen** Zugangsdaten und dem **eigenen**
  Konto (bzw. mit ausdrücklicher Zustimmung der betroffenen Person).

---

## ⚠️ Disclaimer

- **Kein Medizinprodukt.** Nicht für Diagnose, Therapie oder Dosierungsentscheidungen. Triff
  **keine** medizinischen Entscheidungen auf Basis dieser App; es gilt stets das offizielle
  Messsystem und ärztlicher Rat.
- **Datenschutz.** Zugangsdaten, Token und Glukosewerte bleiben **lokal auf dem Gerät** (Session
  verschlüsselt via Android Keystore). **Kein** Upload an Dritte. Logs enthalten bewusst **kein**
  Passwort und **keinen** Token.
- **Keine herstellerspezifischen Artefakte im Repo.** Native Bibliotheken, statische Krypto-Blobs,
  Offsets, Schlüssel- und Signaturmaterial sind **nicht** enthalten und werden **nicht** verteilt.
- **Marken.** „LibreLinkUp", „Libre", „Abbott", „Pixel", „Wear OS" u. a. gehören ihren jeweiligen
  Inhabern. Dieses Projekt steht in **keiner** Verbindung zu diesen Unternehmen und wird von ihnen
  weder unterstützt noch gebilligt.
- **Rechtsrahmen.** Interoperabilität mit den **eigenen** Daten. Örtliche Rechtslage und die
  jeweiligen Nutzungsbedingungen bitte eigenverantwortlich beachten.
- **Ohne Gewähr.** Bereitstellung „wie besehen" (as-is), Nutzung auf eigenes Risiko, keine Haftung.

---

## Funktionen

- **Login** mit eigenen Zugangsdaten; Session verschlüsselt & persistent.
- **Aktueller Wert** mit Trendpfeil, Zielbereichs-Färbung, Alter.
- **Interaktiver Verlaufsgraph** (Cursor, horizontales Verschieben, Zoom, Grid, Achsen, Zielband).
- **Persistenter Verlauf** (Room).
- **Einstellungen**: Pollingintervall, Zielbereich, Einheit (mg/dL ⇄ mmol/L), Datenquelle.
- **Hintergrund-Betrieb**: Foreground-Service mit dauerhafter Benachrichtigung.
- **Pixel Watch**: App-Screen, **Tile** (Kachel) und **Complication** (Zifferblatt).

---

## Datenquellen

- **REST (offen/community):** die dokumentierte Follower-Schnittstelle — funktioniert heute, ist
  aber potenziell von einer Abschaltung betroffen.
- **ALE (verschlüsselte App-Schnittstelle):** Standardpfad für die persönliche Interoperabilität;
  robuster gegen eine Abschaltung der offenen API. Die zugehörige Implementierung liegt in einer
  **git-ignorierten Zone** und ist **nicht Teil dieses Repos**.

Der Build ist **absent-safe**: Fehlt die git-ignorierte Zone, läuft die App über den offenen
REST-Pfad; die Quelle ist in den Einstellungen wählbar.

---

## IP-Trennung

Die stabile Schnittstelle (`:core:glucose-api`) enthält **kein** herstellerspezifisches Wissen.
Herstellerspezifische Komponenten sind konsequent aus der Versionsverwaltung ausgeschlossen:

- `.gitignore` deckt die gesamte IP-Zone ab (Modul, native Bibliotheken, Krypto-Blobs, Schlüssel).
- Ein **Pre-Commit-Guard** (`scripts/ip-guard.sh`) bricht Commits ab, sobald ein verbotener Pfad
  oder verdächtiger Inhalt erkannt wird. Aktivierung:

```bash
git config core.hooksPath .githooks
```

---

## Architektur (Kurzüberblick)

Multi-Modul, Clean Architecture / MVVM, manuelle Constructor-DI, Coroutines/Flow.

```
:app              App (UI/Compose, DI, Room, verschl. Session, Wear-Push, Service)
:wear             Wear-OS-App (Compose, Tile, Complication, Data-Layer-Empfang)
:core:model       Domänenmodelle
:core:glucose-api SPI: GlucoseSource (stabile, IP-freie Grenze)
:domain           Ports (Repository/Session/History/Settings), Zustandsmodell
:data:source-rest offene REST-Quelle
:data:repository  Quellenauswahl + Repository-Implementierung
:data:source-ale  (git-ignoriert) — NICHT im Repo
```

---

## Build & Ausführung

- **Android Studio** (aktuell), JDK 11, Android SDK (compileSdk 37), minSdk 26 (App) / 30 (Wear).
- Ohne die git-ignorierte Zone baut die App ohne NDK über REST.
- **Handy:** Run-Konfiguration `app`. **Uhr:** Run-Konfiguration `wear` (gekoppelte/WLAN-Debug-Uhr).

---

## Forschungsbericht (öffentlich)

Die App ist der praktische Teil eines größeren Forschungs- und Lernprojekts. Der begleitende
**öffentliche Forschungsbericht** beschreibt Vorgehen und Erkenntnisse (Interoperabilität,
Protokoll, Kryptographie, App-Architektur) zu **Bildungs- und Forschungszwecken**.

Auch der Bericht verfolgt ausdrücklich **keinen Anleitungscharakter**: Er stellt **keine**
Schritt-für-Schritt-Reproduktion und **kein** herstellerspezifisches Material bereit, sondern
dient dem Wissensaufbau und der Wissensvermittlung. Die öffentliche Fassung wird bewusst in einem **separaten öffentlichen Repository** geführt — **Code und Bericht sind absichtlich getrennt**.

> 🔗 **Forschungsbericht:** wird bewusst in einem **separaten öffentlichen Repository** geführt — _Link hier eintragen_

---

## Lizenz / rechtlicher Rahmen

Persönliches Forschungs- und Lernprojekt. Keine Weitergabe herstellerspezifischer Artefakte.
Nutzung auf eigene Verantwortung im Rahmen der geltenden Rechtslage und der jeweiligen
Nutzungsbedingungen.
