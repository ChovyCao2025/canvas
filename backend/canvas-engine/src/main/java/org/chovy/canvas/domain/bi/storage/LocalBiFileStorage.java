package org.chovy.canvas.domain.bi.storage;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalBiFileStorage implements BiFileStorage {

    public static final String PROVIDER = "LOCAL";

    private final Path root;

    public LocalBiFileStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public BiStoredFile write(String storageKey, byte[] bytes) {
        Path file = resolve(storageKey);
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, bytes == null ? new byte[0] : bytes);
            return new BiStoredFile(PROVIDER, storageKey, file.toString(), Files.size(file));
        } catch (IOException e) {
            throw new IllegalStateException("failed to write BI storage object: " + storageKey, e);
        }
    }

    @Override
    public BiStoredFile write(String storageKey, BiFileStorageWriter writer) {
        Path file = resolve(storageKey);
        try {
            Files.createDirectories(file.getParent());
            try (var output = Files.newOutputStream(file)) {
                if (writer != null) {
                    writer.write(output);
                }
            }
            return new BiStoredFile(PROVIDER, storageKey, file.toString(), Files.size(file));
        } catch (IOException e) {
            throw new IllegalStateException("failed to write BI storage object: " + storageKey, e);
        }
    }

    @Override
    public byte[] read(String storageKey) {
        try {
            return Files.readAllBytes(resolve(storageKey));
        } catch (IOException e) {
            throw new IllegalStateException("BI storage object is not available: " + storageKey, e);
        }
    }

    @Override
    public boolean delete(String storageKey) {
        Path file = resolve(storageKey);
        try {
            boolean deleted = Files.deleteIfExists(file);
            deleteEmptyParents(file.getParent());
            return deleted;
        } catch (IOException e) {
            throw new IllegalStateException("failed to delete BI storage object: " + storageKey, e);
        }
    }

    private Path resolve(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("storageKey is required");
        }
        Path file = root.resolve(storageKey).toAbsolutePath().normalize();
        if (!file.startsWith(root)) {
            throw new IllegalArgumentException("storageKey escapes BI storage root: " + storageKey);
        }
        return file;
    }

    private void deleteEmptyParents(Path parent) throws IOException {
        Path cursor = parent;
        while (cursor != null && cursor.startsWith(root) && !cursor.equals(root)) {
            try {
                Files.deleteIfExists(cursor);
            } catch (DirectoryNotEmptyException e) {
                return;
            }
            cursor = cursor.getParent();
        }
    }
}
