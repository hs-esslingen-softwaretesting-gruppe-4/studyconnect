# Use Case: Assign Task

### Name: 

    Assign Task

### Summary: 

    Dieser Anwendungsfall ermöglicht es dem Gruppenadministrator, eine bestehende Aufgabe innerhalb einer StudyConnect-Gruppe einem oder mehreren Gruppenmitgliedern zuzuweisen. Dies dient der Koordination von Verantwortlichkeiten und der klaren Verteilung von Aufgaben innerhalb der Lerngruppe, was die Zusammenarbeit strukturiert.

### Actor:

    Gruppenadministrator (Goup adminstrator)

### Triggering Event:

    Der Gruppenadministrator wählt in der Detailsicht einer Gruppenaufgabe die Option zum "Zuweisen" oder "Verantwortlicheen festlegen".

### Inputs:

    - Task-ID: Identifikation der Aufgabe, die zugewiesen werden soll
    - Member-IDs: Identifikation eines oder mehrerer Gruppenmitglieder, denen die Aufgabe zugewiesen werden soll

### Pre-Conditions:

    1. Der Benutzer muss erfolgreich als Gruppenadministrator in der StudyConnect-Anwendung eingeloggt sein.
    2. Die ausgewählte Aufgabe muss bereits existieren und mit der betreffenden Gruppe verknüpft sein. 
    3. Die zu wählenden Mitglieder müssen aktive Mitglieder der Gruppe sein.

### Process Description:

    1. Dder Gruppenadministrator wählt eine bestehende Gruppenaufgabe aus, daraufhin zeigt das System die Detailsicht der Aufgabe an und bietet die Funktion "Zuweisen" an.
    2. Der Gruppenadministrator wählt die Zuweisungsfunktion, woraufhin das System eine Liste aller aktiven Gruppenmitglieder zur Auswahl anzeigt.
    3. Der Gruppenadministrator wählt ein oder mehrere Mitglieder aus der Liste aus und bestätigt die Zuweisung, das System validiert die Eingabe.
    4. Das System aktualisiert den Aufgabaendatensatz und speichert die IDs der zugewiesenen Mitglieder.
    5. Das System sendet eine automatische Benachrichtigung an die neu zugewiesenen Mitglieder über die Notification Service. 
    6. Das System bestätigt die erfolgreiche Zuweisung und aktualisiert die Detailsicht der Aufgabe.  

### Exceptions:

    E1: Nicht-Administrator versucht Zuweisung
    Wenn ein reguläres Gruppenmitglied versucht die Zuweisungsfunktion zu nutzen, bricht das System den Vorgang ab und zeigt eine Fehlermeldung.

    E2: Gewähltes Mitglied ist nicht in der Gruppe
    Versucht der Administrator, einem Benutzer außerhalb der Gruppe zuzuweisen, zeigt das System eine Fehlermeldung.

    E3: Aufgaben-Datenabnkfehler
    Das System kann die Aufgabe nicht aktualisieren. Das System protokolliert den Fehler und benachrichtigt den Administrator: "Die Zuweisung konnte nicht gespeichert werden".

### Outputs and Post-Conditions:

    Outputs:
    - Bestätigungsmeldung (z.B. "Aufgabe wurde erfolgreich an xy zugewiesen)
    - Eine Benachrichtigung wird an das zugewiesene Mitglied gesendet
    - Die Detailsicht der Aufgabe zeigt die Namen der nun verantwortlichen Mitglieder an.

    Post-Conditions:
    - Der Aufgabendatensatz in der Datenbank enthält die Verknüpfung zu dem/den zugewiesenen Gruppenmitglied(ern)
    - Die zugewiesenen Mitglieder sind nun in der Lage, den Fortschritt der Aufgabe zu aktualisieren (Update Progress)