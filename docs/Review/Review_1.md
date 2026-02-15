# Selbst Review - CoinCollector

## Backend
  - Einen `core/`-Ordner einführen, in dem die grundlegenden, generischen Interfaces für alle Münzsorten, Collection- und Gruppentypen abgelegt werden.  
  Falls später auf das Composite-Pattern umgestellt wird, liegen hier die gemeinsamen Basistypen für alle Container-/Coin-Objekte.
  - Schichtenstruktur der Ordner auflösen und zu feature-orientierten Ordnern ändern
    - *Feature-Ordnerstruktur:*
      - `feature/domain/`: Interfaces & Datenimplementierungen  
      - `feature/dto/`: DTOs  
      - `feature/service/`: Service-Implementierungen  
      - `feature/infrastructure/`: Infrastruktur-/Repository-Implementierungen 
  - Logging klarer machen, routenaufruf -> service -> ... <- antwort, vielleicht bietet spring da auch schon was

### Datenmodell
  - In den Interfaces Coin, CoinCollection, CollectionGroup das Werfen von Exceptions in den Methodenbeschreibungen ergänzen
  - Die Struktur der Coin -> Collection -> Group Struktur überdenken, Generisches Composite-Pattern wie am Anfang bereits mal überlegt ==> eine Änderung hätte starke Auswirkungen (API, DB), lohnt wenn Flexibilität notwendig ist -> bei der nächsten DB Migration einfügbar
  - bei den Enums, CoinValue, CoinCountry, Mint, CoinDescription, deutlicher machen das es um Euromünzen geht, Tippfehler in Mint, displayname bei CoinCountry nur sinnvoll wenn man beim userprofil einen sprache wählen kann passiert nicht kann weg
  - user class kann zu record werden, generell alle Datenmodelklassen wenn möglich zu records
  - eigene Typen für die Id-Arten

### Datenbankmodell
  - Check Constraints für die Enum Wertebereiche ergänzen
  - Über Migrations gedanken machen: Wo? Womit? Wann?
  - Datenbankdatei nicht im Programmverzeichnis speichern:
  Die SQLite-Datenbank sollte betriebssystemabhängig im Benutzerprofil abgelegt werden (z. B. %LOCALAPPDATA%/CoinCollector/data/coincollector.db unter Windows, ~/.config/CoinCollector/coincollector.db unter Linux/Mac).
  Wichtig: Die Datenbank bleibt nur dann bei Updates/Neuinstallationen erhalten, wenn der Datenordner nicht vom Installer/Updater gelöscht wird. 
  Das Verhalten hängt von der Konfiguration des Installers ab und sollte explizit geprüft werden.

### Repository Layer
  - i.O.
  - trennen des user repos vom coin feature dann
  
### Service Layer
  - viele verschiedene exceptions, eventuell Zusammfassbare dabei, wenn der Fehlerfall nach außen nicht anderes behandelt wird ist er unnötig, exception Factory einführen
  - trennen der Session- und User-Elemente von den Eurocoin-Domain-Elementen

### HTTP Handlers
  - Namen anpassen an eurocoin domain
  - vieles wird durch spring dann sowieso anders

### Dokumentation
  - einmal ordentlich aufräumen
  - frontend design zu Erstentwurf machen und verdeutlichen das es eine solche Dokumentation für weitere Schritte nicht geben wird
  - Einzeldokumentationen in domains verschieben
  - außen nur Gesamtdokumentation behalten, dort auf Komponenten reduzieren, verweisen auf die anderen Dateien
  - JavaDoc HTML seite für spezifische technische Dokumentation über maven generieren lassen und in die domain docs
  - In Readme verweisen
  - Readme aktualisieren

### API Beschreibung
  - Api Spec pro domain anlegen, userApi.yaml und euroCoin.yaml, standardfehler responses in eine dritte datei
  - verlinken zu in der äußeren Gesamtdoku zu einem file CoinCollectorAPI.yaml

## API Design
  - i.O, muss an composite pattern angepasst werden

## Frontend
  - ebenfalls zu feature-orientierten Ordnern umstellen

### Darstellende Komponenten
- Basis‑UI‑Elemente schrittweise von manuellen Tailwind‑Klassen auf Skeleton (Svelte) migrieren (Buttons, Inputs, Modals, Toasts, Layout‑Primitives).
- Tailwind behalten für Design‑Tokens / Farben; Skeleton in `tailwind.config` als content aufnehmen, tailwind.config datei erstellen um farben als tailwind direkt zugreifbar zu machen anstatt css variablen
- Wrapper‑Components (`src/lib/ui/*`) anlegen, die Skeleton intern nutzen
- Migration inkrementell: zuerst Primitives (Button/Input/Toast), danach domain‑spezifische Komponenten ersetzen; keine Abhängigkeit zu SvelteKit erforderlich.

### Store
  - die Verschachtelung loswerden im groups store, für leichtere updates
  - nur ids und dann für den view zusammensetzen aus group und collection store, da ne klare trennung reinbekommen das ein collection update in der groupe ankommt ohne diese neu laden zu müssen

### Service Layer
  - an api interceptoren und auto extraktion anpassen 

### API Anschluss
  - API-Client (axios) sollte responses automatisch entpacken (response.data) und einen zentralen Error-Interceptor haben.
    - Fehler in eine einheitliche ApiError-Klasse mappen.
    - HTTP 401 (Unauthorized) im Interceptor abfangen und ein Logout-/Refresh-Event oder Callback auslösen. 

### Dokumentation
  - TypeDoc auch automatisch erstellen lassen und in docs legen
  - Komponetendiagramm hinzufügen
