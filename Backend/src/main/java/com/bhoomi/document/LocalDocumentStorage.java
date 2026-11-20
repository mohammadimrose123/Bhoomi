package com.bhoomi.document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalDocumentStorage implements DocumentStorage {

    private final Path root;

    public LocalDocumentStorage(@Value("${bhoomi.documents.storage-path:./var/private-documents}") String path) {
        this.root = Path.of(path).toAbsolutePath().normalize();
    }

    @Override
    public void store(String storageKey, byte[] content) {
        Path target = resolve(storageKey);
        try {
            Files.createDirectories(root);
            Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new DocumentStorageException("Private document could not be stored", exception);
        }
    }

    @Override
    public byte[] read(String storageKey) {
        try {
            return Files.readAllBytes(resolve(storageKey));
        } catch (IOException exception) {
            throw new DocumentStorageException("Private document could not be read", exception);
        }
    }

    private Path resolve(String storageKey) {
        if (storageKey == null || !storageKey.matches("[0-9a-fA-F-]{36}\\.(pdf|png|jpg)")) {
            throw new IllegalArgumentException("Invalid private document storage identifier");
        }
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root) || target.getParent() == null || !target.getParent().equals(root)) {
            throw new IllegalArgumentException("Invalid private document storage identifier");
        }
        return target;
    }
}
