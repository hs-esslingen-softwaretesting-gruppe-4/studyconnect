# User Story: Fortschritt aktualisieren (Update Progress)

Kurzbeschreibung:

Als Nutzer (Student oder Gruppenadministrator) möchte ich den Fortschritt bzw. Status einer Aufgabe ändern können, damit der aktuelle Bearbeitungsstand sichtbar und nachvollziehbar ist.

Akzeptanzkriterien:

- AC1 — Status ändern (Happy Path): Ein berechtigter Benutzer kann den Status einer ihm zugewiesenen oder eigenen Aufgabe ändern; die Änderung wird persistent gespeichert und in der Detailansicht angezeigt.
- AC2 — Rückmeldung: Nach erfolgreicher Statusänderung erhält der Benutzer eine Bestätigungsmeldung und kann den neuen Status einsehen.
- AC3 — Ungültige Eingabe: Gibt der Benutzer einen nicht definierten Status ein, wird die Änderung abgelehnt und eine Fehlermeldung mit verfügbaren Statusoptionen angezeigt.
- AC4 — Persistenzfehler: Wenn die Datenbank die neue Statusänderung nicht speichert, erhält der Benutzer eine Fehlermeldung mit weiteren Anweisungen und ein Fehlerprotokoll wird erzeugt; der vorherige Status bleibt erhalten.
- AC5 — Berechtigungen: Nur berechtigte Benutzer (z. B. Eigentümer oder zugewiesene Benutzer bzw. Gruppenadmin) können den Status ändern; andernfalls soll die Aktion failen.

Kurzszenarien (abgedeckt durch `updateProgress.feature`):

- Change status: Benutzer ändert Status → Bestätigung und Ansicht des neuen Status.
- Undefined status: Benutzer gibt invaliden Status ein → Fehlermeldung mit definierten Statuswerten.
- Error when changing status: Benutzer ändert Status, DB speichert nicht → Fehlerhinweis, Log‑Eintrag, vorheriger Status bleibt.