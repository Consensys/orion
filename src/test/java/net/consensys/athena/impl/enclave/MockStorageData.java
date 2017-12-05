package net.consensys.athena.impl.enclave;

import net.consensys.athena.api.storage.StorageData;

public class MockStorageData implements StorageData {
    @Override
    public String getBase64Encoded() {
        java.math.BigInteger i = new java.math.BigInteger("3258612805645805793480074867989621323193038677294493313537981367693025155795");
        return i.toString(64);
    }

    @Override
    public byte[] getRaw() {
        java.math.BigInteger i = new java.math.BigInteger("3258612805645805793480074867989621323193038677294493313537981367693025155795");
        return i.toByteArray();
    }
}
