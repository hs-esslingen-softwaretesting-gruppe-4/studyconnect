# Use Case: Create Task

### Name:

    Create Task

### Summary:

    Dieser Anwendungsfall ermöglicht es einem eingeloggten Studenten, eine neue persönliche oder gruppenbezogene Aufgabe in der StudyConnect-Anwendung zu definieren. Das System erfasst wesentliche Aufgabendetails wie Titel, Priorität, Fälligkeitsdatum und optionale Kategoerie/Notizen. Dadurch wird sichergestellt, dass alle Lernverpflichtungen formal strukturiert und verfolgt werden können.

### Actor:

    Student

### Triggering Event:

    Der Student navigiert zur Oberfläche für die Aufgabenstellung und initiiert den Prozess.

### Inputs:

    - Titel der Aufgabe (Obligatorisch)
    - Fälligkeitsdatum (Optional)
    - Priorität (Optional)
    - Kategorie (Optional)
    - Notizen (Optional)
    - Gruppenzugehörigkeit (Optional)


### Pre-Conditions:

    1. Der Benutzer muss erfolgreich in der StudyConnect-Anwendung eingeloggt sein.
    2. Der Benutzer muss die Zugriffsrechte für die Task-Management-Funktion besitzen.
    3. Das System muss betriebsbereit sein und mit der Datenbank verbunden sein.

### Process Description:

    1. Der Student wählt die Option zum Erstellen einer neuen Aufgabe. Das System zeigt das Formular zur Aufgabenerstellung an.
    2. Der Student gibt die erforlderlichen Aufgabendetails ein, daraufhin führt das System eine Formvalidierung der Eingabefelder durch.
    3. Der Student bestätigt das Erstellen der Aufgabe und das System prüft, dass die obligatorischen Felder ausgefüllt sind.
    4. Das System erstellt einen neuen Aufgabendatensatz in der Datenbank.
    5. Das system verknüpft die neue Aufgabe mit dem Profil des Studenten und gegebenenfalls der Gruppe
    6. Das System bestätigt die Erstellung und leitet den Studenten zur Aufgabenübersicht weiter.

### Exceptions:

    E1: Fehlende Pflichteingabe
    Versucht der Student, die Aufgabe ohne Titel zu speichern, zeigt das System eine Fehlermeldung anund hält das Formular zur Korrektur geöffnet

    E2: Ungültiges Eingabeformat
    Wird ein Datum/Uhrzeit im ungültigen Format für das Fälligkeitsdatum eingegeben, zeigt das System eine spezifische Fehlermeldung an und fordert zur Korrektur auf.

    E3: Datenbankfehler
    Kann das System die Aufgabe nicht in die Datenbank schreiben, protokolliert das System den fehler und informiert den Studenten.

### Outputs and Post-Conditions:

    Outputs:
    - Bestätigungsmeldung

    Post-Conditions:
    - Ein neuer Aufgabendatensatz existiert in der Datenbank
    - Die neue Aufgabe ist mit initiierenden Studenten verknüpft
