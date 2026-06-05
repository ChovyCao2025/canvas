package org.chovy.canvas.domain.bi.storage;

public interface BiFileStorage {

    String provider();

    BiStoredFile write(String storageKey, byte[] bytes);

    byte[] read(String storageKey);

    boolean delete(String storageKey);
}
