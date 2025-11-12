# User Story: Aufgabe erstellen (Create Task)

Kurzbeschreibung:

Als eingelogger Student möchte ich neue Aufgaben mit Titel und optionalen Details (Fälligkeitsdatum, Priorität, Kategorie, Notizen, Gruppenzuordnung) erstellen können, damit ich meine Lernaufgaben strukturiert planen und nachverfolgen kann.

Akzeptanzkriterien:

- AC1 — Minimaler Erfolg: Der Benutzer kann eine Aufgabe mit mindestens einem Titel anlegen; die Aufgabe wird in der Datenbank persistiert und ist in der Aufgabenübersicht sichtbar.
- AC2 — Alle Details: Werden optionale Felder angegeben (due date, priority, category, notes, group), so werden diese korrekt gespeichert und sind beim Anzeigen der Aufgabe verfügbar.
- AC3 — Validierung: Fehlt der Titel, wird eine aussagekräftige Fehlermeldung angezeigt ("Title is required") und das Formular bleibt zur Korrektur geöffnet; es wird kein Eintrag in der DB erstellt.
- AC4 — Datumsformat: Ungültige Datumsangaben führen zu einer Fehlermeldung ("Invalid date format") und verhindern die Persistierung.
- AC5 — Default‑Werte: Wenn Priority nicht angegeben ist, wird ein Standardwert (z. B. "Medium") gesetzt; Status wird standardmäßig auf "Open" gesetzt.
- AC6 — Gruppenzuordnung: Wird eine Gruppe ausgewählt, wird die Aufgabe mit der Gruppe verknüpft und erscheint in der Gruppenliste.
- AC7 — Fehlertoleranz: Bei DB‑Fehlern (z. B. Verbindungsausfall) wird die Erstellung abgebrochen, ein Fehler protokolliert und eine technische Fehlermeldung angezeigt; es erfolgt keine teilweises Schreiben.
- AC8 — Edge Case: Sehr lange Notizen (z. B. 1000 Zeichen) werden korrekt gespeichert und wiedergegeben.
- AC9 — Zugriffsrechte: Benutzer ohne Task‑Management‑Recht erhalten beim Versuch, die Erstellungsseite zu öffnen, eine Zugriffsverweigerung und eine Meldung "You do not have permission to create tasks".

Beispielszenarien (abgedeckt durch `create-task.feature`):

- Happy Path (minimal): Titel eingeben → Absenden → Aufgabe wird erstellt und ist sichtbar.
- Happy Path (vollständig): Alle Felder ausgefüllt → Absenden → Eigenschaften stimmen mit den Eingaben überein.
- Validierungsfall: Kein Titel → Fehlermeldung, kein DB‑Eintrag.
- Ungültiges Datum: Fehlermeldung, kein DB‑Eintrag.
- Gruppenaufgabe: Aufgabe wird einer Gruppe zugeordnet und erscheint in der Gruppenliste.
- Persistenzfehler: Bei DB‑Ausfall erscheint eine technische Fehlermeldung und kein Eintrag wird erstellt.
