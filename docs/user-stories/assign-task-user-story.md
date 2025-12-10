# User Story: Aufgabe zuweisen (Assign Task)

Kurzbeschreibung:

Als Gruppenadministrator möchte ich eine vorhandene Aufgabe einem oder mehreren Mitgliedern der Gruppe zuweisen können, damit Verantwortlichkeiten klar verteilt sind und die zugewiesenen Mitglieder benachrichtigt werden.

Akzeptanzkriterien:

- AC1 — Zuweisung erfolgreich: Ein angemeldeter Gruppenadministrator kann ein oder mehrere gültige Gruppenmitglieder auswählen und die Zuweisung bestätigen. Die Auswahl wird in der Aufgabe persistent gespeichert und ist in der Detailansicht sichtbar.
- AC2 — Benachrichtigung: Nach erfolgreicher Zuweisung erhalten die neu zugewiesenen Mitglieder eine Benachrichtigung oder ein entsprechender Log‑Eintrag wird erzeugt.
- AC3 — Berechtigungsprüfung: Ein nicht‑administrativer Benutzer darf keine Zuweisung vornehmen; der Versuch liefert eine Fehlermeldung und es erfolgt keine Persistierung.
- AC4 — Mitgliedschaftsprüfung: Wenn ein ausgewählter Nutzer nicht zur Gruppe gehört, wird die Zuweisung abgebrochen und eine aussagekräftige Fehlermeldung angezeigt; es erfolgen keine Änderungen an der Aufgabe.
- AC5 — Fehlertoleranz: Bei Persistenzfehlern (z. B. Datenbankausfall) werden keine Teiländerungen übernommen; der vorherige Zustand bleibt erhalten und eine Fehlermeldung (z. B. "Die Zuweisung konnte nicht gespeichert werden") wird gezeigt.
- AC6 — Idempotenz: Wiederholte Zuweisungen derselben Mitglieder erzeugen keine Duplikate in der Assignee‑Liste.

Akzeptanztests / Beispiel‑Szenarien:

1) Happy Path
- Gegeben: Ein Gruppenadministrator ist angemeldet und wählt eine bestehende Gruppenaufgabe.
- Wenn: Der Administrator wählt Bob und Carol als Assignees und bestätigt.
- Dann: Bob und Carol sind in der Aufgaben‑Detailansicht als Assignees sichtbar und erhalten eine Benachrichtigung.

2) Nicht‑Admin versucht Zuweisung
- Gegeben: Ein normales Gruppenmitglied ist angemeldet.
- Wenn: Es versucht, eine Zuweisung vorzunehmen.
- Dann: Es erhält eine Fehlermeldung (z. B. "authorization") und die Aufgabe bleibt unverändert.

3) Externer Nutzer ausgewählt
- Gegeben: Admin wählt einen Nutzer, der nicht zur Gruppe gehört.
- Dann: Die Zuweisung bricht ab und die Meldung "Selected user is not a group member" wird angezeigt.

4) Persistenzfehler
- Gegeben: Simulierter Persistenzfehler (z. B. DB not available).
- Wenn: Admin versucht, Assignees zu speichern.
- Dann: Keine Änderung wird persistiert; es erscheint die Meldung "The assignment could not be saved" und vorherige Assignees bleiben bestehen.

Hinweise zur Implementierung:

- Autorisierung prüfen (nur Gruppenadmins dürfen zuweisen).
- Vor dem Persistieren prüfen, ob die ausgewählten Nutzer Mitglieder der Gruppe sind.
- Vor dem Speichern Snapshot der Assignees anlegen, um Rollback/Verifikation bei Fehlern zu ermöglichen.
- Benachrichtigungen können synchron oder asynchron erfolgen; für Tests ist ein protokollierter Hinweis ausreichend.
