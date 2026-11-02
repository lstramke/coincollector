package io.github.lstramke.coincollector.export;

import java.util.List;

import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;

public interface ExportWriter {
    public ExportFormat format();

    public byte[] write(List<EuroCoinCollectionGroup> wholeCollection);
}
