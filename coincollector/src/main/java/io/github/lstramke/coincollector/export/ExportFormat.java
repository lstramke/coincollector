package io.github.lstramke.coincollector.export;

import java.util.Optional;

public enum ExportFormat {

    CSV("text/csv", "csv"),
    JSON("application/json", "json");

    private final String mimeType;
    private final String fileExtension;

    ExportFormat(String mimeType, String fileExtension) {
        this.mimeType = mimeType;
        this.fileExtension = fileExtension;
    }

    public String mimeType() {
        return mimeType;
    }

    public String fileExtension() {
        return fileExtension;
    }

    public static Optional<ExportFormat> from(String value) {
        if(value == null) {
            return Optional.empty();
        }

        return switch(value.toLowerCase()){
            case "csv" -> Optional.of(CSV);
            case "json" -> Optional.of(JSON);
            default -> Optional.empty();
        };
    }
}