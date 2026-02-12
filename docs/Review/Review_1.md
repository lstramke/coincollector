
# Code Review - CoinCollector

## Backend

  - schichten struktur der ordner auflösen und hinzu feature-orientierten ordnern

### Datenmodell
  - In den Interfaces Coin, CoinCollection, CollectionGroup das Werfen von Exceptions in den Methodenbeschreibungen ergänzen
  - Die Struktur der Coin -> Collection -> Group Struktur überdenken, Composite-Pattern wie am Anfang       bereits mal überlegt ==> eine Änderung hätte starke Auswirkungen (API, DB), lohnt wenn Flexibilität notwendig ist -> bei der nächsten DB Migration einfügbar
  - bei den Enums, CoinValue, CoinCountry, Mint, CoinDescription, deztlicher machen das es um Euromünzen geht, Tippfehler in Mint, displayname bei CoinCountry nur sinnvoll wenn man beim userprofil einen sprache wählen kann passiert nicht kann weg
  - user class kann zu record werden, generell möglichst alle Datenmodelklassen wenn möglich zu records
  - eigene Typen für die Id-Arten

### Datenbankmodell
- 

### Repository Layer
- 

### Service Layer
- 

### HTTP Handlers
- 

### Dokumentation
- 

### API Beschreibung
- 

## API Design
- 

## Frontend

### Darstellende Komponenten
- 

### Store
- 

### Service Layer
- 

### API Anschluss
- 

### Dokumentation
- 

