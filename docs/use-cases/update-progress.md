# Use Case: Update Progress

### Name: 

    Update Progress

### Summary: 

    Dieser Anwendungsfall ermöglicht es einem eingeloggten Studenten oder einem Gruppenadministrator, den aktuellen Zusatand einer Aufgabe zu ändern. Dies ist essentiell, um die "Awareness of Progress" zu gewährleisten, die Zeitplanung effektiv zu unterstützen.

### Actor:

    Student, Gruppenadministrator

### Triggering Event:

    Der Aktuer öffnet eine bestehende Aufgabe und wählt eine Option zur Statusänderung.

### Inputs:

    - Task ID (Identifikation der Aufgabe)
    - New Progress State ( Neuer Fortschrittstatus)


### Pre-Conditions:

    1. Der Benutzer muss erfolgreich in der StudyConnect-Anwendung eingeloggt sein.
    2. Die Aufgabe muss bereits existieren.
    3. Der Benutzer muss die Berechtigung haben, den Status dieser spezifischen Aufgabe zu ändern (persönliche Aufgabe oder ihm zugewiesene Gruppenaufgabe)

### Process Description:

    1. Der Aktuer navigiert sich zur Detailsicht der Aufgabe, das system zeigt den aktuellen Status der Aufgabe an.
    2. Der Aktuer ändert den Fortschrittsstatus auf den gewünschten neuen Wert und das System validiert den neuen Status.
    3.Der Aktuer bestätigt die Statusänderung, das System aktualisiert den Fortschrittsstatus der Aufgabe im Aufgabendatensatz.
    4. Das Systemm bestätigt die erfolgreiche Aktualisierung und zeigt den neuen Status an.

### Exceptions:

    E1: Fehlende Berechtigung 
    Wenn ein nicht autorisierter Student versucht, den Status einer Aufgabe zu ändern (z. B. eine fremde persönliche Aufgabe), bricht das System den Vorgang ab und zeigt eine Fehlermeldung an.

    E2: Ungültiges Status
    Der Akteur versucht, einen nicht definierten Status einzugeben (z. B. Freitext). Das System lehnt die Änderung ab und zeigt eine Fehlermeldung an, die nur die gültigen Zustände auflistet..

    E3: Datenbankfehler
    Das System kann den Datensatz aufgrund eines Fehlers nicht aktualisieren. Das System protokolliert den Fehler und informiert den Akteur: „Der Fortschritt konnte nicht gespeichert werden. Bitte versuchen Sie es erneut.

### Outputs and Post-Conditions:

    Outputs: 
    - Bestätigungsmeldung
    - Die Detailsicht der Aufgabe zeigt den neuen Fortschrittsstatus an.
    
    Post-Conditions:
    - Der Fortschrittsstatus der Aufgabe im Datenbankeintrag ist auf den neuen Wert gesetzt
    - (Bei Gruppenaufgaben): Alle relevanten Gruppenmitglieder (z. B. der Administrator) haben eine Benachrichtigung über die Statusänderung erhalten.