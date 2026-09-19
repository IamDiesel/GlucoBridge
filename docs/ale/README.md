# ALE-Vorlage (`docs/ale/`)

Dieser Ordner enthält **ausschließlich eine Dummy-/Template-Fassung** der JNI-Glue für die
verschlüsselte App-Schnittstelle (ALE). Er dient der **Dokumentation der Architektur** – nicht
dem Betrieb.

## Was hier liegt

- **`glucoale_dummy.c`** – strukturerhaltende Dummy-Fassung der JNI-Glue. Sie zeigt den Aufbau
  der ALE-Anbindung (Sektionen, Funktionen, JNI-Signaturen), enthält aber **keinerlei reale
  Werte oder Algorithmen** und liefert nur Fehlschläge zurück. Läuft die App ohne funktionierende
  ALE-Umsetzung, fällt sie automatisch auf die offene REST-Quelle zurück.

## Was **nicht** enthalten ist (Dummy-Artefakte)

Alle sicherheits-/herstellerrelevanten Bestandteile sind bewusst durch Platzhalter ersetzt und im
Code mit `[DUMMY]` bzw. `[DUMMY-STUB]` markiert:

| Artefakt | Status im Template |
|---|---|
| VMA-Offsets (`OFF_*`) | Platzhalter `0x000000` |
| Erfolgs-Returncode (`SKB_OK`) | Platzhalter `0` |
| Key-Ladder-Parameter (`U1_K*`, `U2_K*`) | Platzhalter `0` |
| Name des exportierten White-Box-Symbols | Platzhalter-String |
| ABI-Signaturen der White-Box-Funktionen | auf generische Zeiger neutralisiert |
| kp-Bündel-Parser (Formatlogik) | als Stub entfernt |
| Provisioning-Ablauf (Key-Ladder-Sequenz) | als Stub entfernt |

Die native Bibliothek (`*.so`) und die statischen Blobs (`cdfe1.bin` / `cdfe2.bin`) sind **nicht
Teil dieses Repos** und liegen ausschließlich in der git-ignorierten Zone `data/source-ale/`.

## Selbst beschaffen und eintragen

Für den **eigenen, persönlichen** ALE-Betrieb müssen die oben genannten Artefakte für die
**eigene Installation** selbst ermittelt und eingetragen werden:

1. `glucoale_dummy.c` nach `data/source-ale/src/main/cpp/glucoale.c` kopieren/umbenennen
   (diese Zone ist git-ignoriert und gelangt nicht ins Repo).
2. Die mit `[DUMMY]` / `[DUMMY-STUB]` markierten Stellen mit den selbst ermittelten Werten bzw.
   einer eigenen Implementierung füllen.
3. Die native Bibliothek und die Blobs an den in der Haupt-`README.md` (Abschnitt „ALE-Zone")
   genannten Ablageorten bereitstellen.

Diese Artefakte sind an eine App-/Bibliotheksversion gebunden und müssen nach einem brechenden
Update erneut ermittelt werden.

## Grundsatz

> Es wird **bewusst keine Anleitung** zur Beschaffung, Ermittlung oder Erzeugung dieser Artefakte
> bereitgestellt – weder in diesem Repository noch im Forschungsbericht. Ziel des Projekts sind
> **persönliche Interoperabilität** (Zugriff auf die **eigenen** Glukosewerte) sowie
> **Forschung und Wissensaufbau**. Keine Weitergabe herstellerspezifischer Artefakte.
