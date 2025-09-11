package io.github.nety.integrationcheck.batch.adapter;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamReader;

public final class ReaderAdapters {
    private ReaderAdapters() {}

    public static <T> ItemStreamReader<T> asNoopItemStreamReader(ItemReader<T> reader) {
        return new ItemStreamReader<>() {
            @Override public T read() throws Exception { return reader.read(); }
            @Override public void open(ExecutionContext ec) { /* no-op */ }
            @Override public void update(ExecutionContext ec) { /* no-op */ }
            @Override public void close() { /* no-op */ }
        };
    }
}
