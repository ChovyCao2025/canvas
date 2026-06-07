package org.chovy.canvas.domain.bi.storage;

import java.io.IOException;
import java.io.OutputStream;

@FunctionalInterface
public interface BiFileStorageWriter {

    void write(OutputStream output) throws IOException;
}
