package org.chovy.canvas.domain.bi.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface BiFileStorage {

    String provider();

    BiStoredFile write(String storageKey, byte[] bytes);

    default BiStoredFile write(String storageKey, BiFileStorageWriter writer) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (writer != null) {
                writer.write(output);
            }
            return write(storageKey, output.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("failed to write BI storage object: " + storageKey, e);
        }
    }

    byte[] read(String storageKey);

    boolean delete(String storageKey);
}
