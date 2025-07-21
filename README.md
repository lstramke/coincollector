# CoinCollector

A JavaFX application for collecting and managing coins with a web-based frontend.

## Requirements

- Java 21 or higher
- Maven 3.6 or higher

## Build & Run

```zsh
# Build the project
mvn clean package

# Run the application
mvn javafx:run
```

## Project Structure

- `coincollector/` - Java backend (JavaFX application)
- `frontend/` - Web frontend (Vite + TypeScript)

The application uses a JavaFX WebView to display a web interface built with Vite.