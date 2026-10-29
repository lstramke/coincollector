package io.github.lstramke.coincollector.export;

public record ExportResult(
    byte[] content,
    ExportFormat contentType
) {}
