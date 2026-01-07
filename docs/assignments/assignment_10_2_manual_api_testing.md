# 10.2 Manual API Testing (Template)

Fill this document with your results (screenshots or CLI logs required).

## Tool Used
- Name/version: curl 8.x (PowerShell curl.exe)
- Why chosen: Schnell aus der Konsole ausführbar; einfache Roh-HTTP-Protokolle

## Setup Steps
1. Prepare backend dev environment
   - Copy backend/.env.example to backend/.env and keep `SPRING_PROFILES_ACTIVE=dev`.
   - Start Dev DB:
     ```powershell
     docker compose -f backend/dev.docker-compose.yaml up -d
     ```
   - Start backend (dev profile):
     ```powershell
     cd backend
     .\mvnw.cmd spring-boot:run
     ```
   - Backend base URL: http://localhost:8080
2. (Optional) Regenerate frontend API client if needed
   ```powershell
   cd frontend
   npm ci
   npm run api:generate
   ```

## Endpoints Under Test
- Base URL: http://localhost:8080
- Successful request: `GET /api/groups`
- Invalid request: `GET /api/groups/search?query=` (leerer Query-Parameter)

## Requests and Responses

### A) Successful Request (List Public Groups)
- Request
  - Method: GET
  - URL: http://localhost:8080/api/groups
- Evidence (screenshot/CLI log)
  - Siehe Run Logs unten
- Expected outcome
  - HTTP 200 OK
  - JSON-Array; bei leerer Datenbank: []

### B) Invalid Request (Validation Error)
- Request
  - Method: GET
  - URL: http://localhost:8080/api/groups/search?query=
- Evidence (screenshot/CLI log)
  - Siehe Run Logs unten
- Expected outcome
  - HTTP 400 Bad Request
  - Fehlermeldung zur Validierung (z. B. "Query must not be blank")

### Optional: Read Back
- `GET /api/groups` liefert die vollständige Liste öffentlicher Gruppen
- `GET /api/groups/search?query=math` sollte 200 liefern und je nach Datenlage Listen-Einträge oder ein leeres Array zurückgeben

## Raw CLI Examples (curl)
Use `curl.exe` in PowerShell to avoid aliasing:

- Health
  ```powershell
  curl.exe "http://localhost:8080/actuator/health" -i
  ```

- Success (Groups list)
  ```powershell
  curl.exe "http://localhost:8080/api/groups" -i
  ```

- Invalid (blank query)
  ```powershell
  curl.exe "http://localhost:8080/api/groups/search?query=" -i
  ```

## Observations & Summary
 ````

 ## Run Logs (executed on 2026-01-07)

 ### Health

 ```
 HTTP/1.1 200
 Vary: Origin
 Vary: Access-Control-Request-Method
 Vary: Access-Control-Request-Headers
 X-Content-Type-Options: nosniff
 X-XSS-Protection: 0
 Cache-Control: no-cache, no-store, max-age=0, must-revalidate
 Pragma: no-cache
 Expires: 0
 X-Frame-Options: SAMEORIGIN
 Content-Type: application/vnd.spring-boot.actuator.v3+json
 Transfer-Encoding: chunked
 Date: Wed, 07 Jan 2026 09:52:22 GMT

 {"status":"UP"}
 ```

 ### Successful Request – GET /api/groups

 ```
 HTTP/1.1 200
 Vary: Origin
 Vary: Access-Control-Request-Method
 Vary: Access-Control-Request-Headers
 X-Content-Type-Options: nosniff
 X-XSS-Protection: 0
 Cache-Control: no-cache, no-store, max-age=0, must-revalidate
 Pragma: no-cache
 Expires: 0
 X-Frame-Options: SAMEORIGIN
 Content-Type: application/json
 Transfer-Encoding: chunked
 Date: Wed, 07 Jan 2026 09:52:31 GMT

 []
 ```

 ### Invalid Request – GET /api/groups/search?query=

 ```
 HTTP/1.1 400
 Vary: Origin
 Vary: Access-Control-Request-Method
 Vary: Access-Control-Request-Headers
 X-Content-Type-Options: nosniff
 X-XSS-Protection: 0
 Cache-Control: no-cache, no-store, max-age=0, must-revalidate
 Pragma: no-cache
 Expires: 0
 X-Frame-Options: SAMEORIGIN
 Content-Type: application/json
 Transfer-Encoding: chunked
 Date: Wed, 07 Jan 2026 09:52:40 GMT
 Connection: close

 {"timestamp":"2026-01-07T09:52:40.643469700Z","status":400,"error":"Validation Failed","message":"searchPublicGroups.query: Query must not be blank","path":"/api/groups/search"}
 ```

 ### Summary
- Successful request: `GET /api/groups` returned 200 with an empty list (fresh DB).
- Invalid request: blank `query` returned 400 with a clear validation message.
- Tool: curl (PowerShell `curl.exe`).


Die Tests entsprachen den Erwartungen: Der Service war über den Healthcheck erreichbar, das Listing der Gruppen funktionierte fehlerfrei, und fehlende Eingaben wurden mit einem klaren 400-Fehler beantwortet. Die Durchführung mit curl war unkompliziert und die Roh-HTTP-Ausgaben ließen sich direkt als Nachweis in dieses Dokument übernehmen.
