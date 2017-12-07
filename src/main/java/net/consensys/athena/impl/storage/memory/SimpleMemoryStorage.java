package net.consensys.athena.impl.storage.memory;

import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageKey;

import java.util.Hashtable;

public class SimpleMemoryStorage implements Storage {

    private Hashtable<StorageData, String> data
            = new Hashtable<StorageData, String>();

    @Override
    public StorageKey store(StorageData data) {
        return null;
    }

    @Override
    public StorageData retrieve(StorageKey key) {
        return null;
    }
}
