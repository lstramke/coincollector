package io.github.lstramke.coincollector.export;

import java.util.UUID;

import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupGetAllException;

public interface ExportService {

    public ExportResult export(UUID userId, ExportFormat exportFormat) throws EuroCoinCollectionGroupGetAllException;
}
