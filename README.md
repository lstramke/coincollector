# CoinCollector

A Java application for collecting and managing coins with a web-based frontend.

## Build & Run

```zsh
# Build the project (includes frontend build)
mvn clean package

# Run the application
java -jar coincollector/target/coincollector-1.0.0.jar
```

The application will start a web server on `http://localhost:8080`. 

## Development

### Backend Only
```zsh
cd coincollector
mvn clean compile
```

### Frontend Only
```zsh
cd frontend
npm install
npm run dev
```

## Project Structure

- `coincollector/` - Java backend (embedded HTTP server)
  - `src/main/java/` - Java source code
  - `src/main/resources/` - Application resources
  - `target/` - Build output
- `frontend/` - Web frontend (Vite + TypeScript)
  - `src/` - Frontend source code
  - `dist/` - Built frontend (copied to backend during Maven build)

## Technology Stack

**Backend:**
- Java 21
- com.sun.net.httpserver (embedded HTTP server)
- SQLite
- SLF4J + Logback

**Frontend:**
- Svelte 5.43
- Vite 7.2
- TypeScript 5.9
- TailwindCSS 4.1
- Axios

The Maven build automatically compiles the frontend and packages it into the final JAR file.