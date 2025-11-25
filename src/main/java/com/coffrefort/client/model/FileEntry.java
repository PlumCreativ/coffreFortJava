package com.coffrefort.client.model;

import java.time.Instant;

public class FileEntry {
    private final String name;
    private final long size;
    private final Instant updatedAt;

    private FileEntry(String name, long size, Instant updatedAt) {
        this.name = name;
        this.size = size;
        this.updatedAt = updatedAt;
    }

    public static FileEntry of(String name, long size, Instant updatedAt) {
        return new FileEntry(name, size, updatedAt);
    }

    public String getName() { return name; }
    public long getSize() { return size; }
    public Instant getUpdatedAt() { return updatedAt; }
}
