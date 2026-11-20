package com.bhoomi.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalDocumentStorageTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesAndReadsOnlyGeneratedKeysInsidePrivateRoot() {
        LocalDocumentStorage storage = new LocalDocumentStorage(temporaryDirectory.resolve("private").toString());
        String key = "123e4567-e89b-12d3-a456-426614174000.pdf";
        byte[] content = "%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        storage.store(key, content);

        assertThat(storage.read(key)).containsExactly(content);
        assertThat(temporaryDirectory.resolve("private").resolve(key)).exists();
    }

    @Test
    void rejectsPathTraversalKeys() {
        LocalDocumentStorage storage = new LocalDocumentStorage(temporaryDirectory.toString());

        assertThatThrownBy(() -> storage.read("../private.pdf"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
