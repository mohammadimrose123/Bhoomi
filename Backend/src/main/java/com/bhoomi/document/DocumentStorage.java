package com.bhoomi.document;

public interface DocumentStorage {

    void store(String storageKey, byte[] content);

    byte[] read(String storageKey);
}
