package net.consensys.athena.impl.enclave;

import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageKey;
import net.consensys.athena.impl.storage.SimpleStorage;

public class MockKeyStorage implements Storage {
    @Override
    public StorageKey store(StorageData data) {
        return null;
    }

    @Override
    public StorageData retrieve(StorageKey key) {
        return new MockStorageData();
    }
}
