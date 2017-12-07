package net.consensys.athena.impl.enclave;

import net.consensys.athena.api.storage.StorageData;
import org.bouncycastle.util.encoders.Hex;

public class MockStorageData implements StorageData {
    @Override
    public String getBase64Encoded() {


        java.math.BigInteger i = new java.math.BigInteger("3258612805645805793480074867989621323193038677294493313537981367693025155795");
        return i.toString(64);
    }

    @Override
    public byte[] getRaw() {
        //Test vectors from Section 10

        //byte[] alicesk = Hex.decode("0x77,0x07,0x6d,0x0a,0x73,0x18,0xa5,0x7d,0x3c,0x16,0xc1,0x72,0x51,0xb2,0x66,0x45,0xdf,0x4c,0x2f,0x87,0xeb,0xc0,0x99,0x2a,0xb1,0x77,0xfb,0xa5,0x1d,0xb9,0x2c,0x2a");
        byte[] alicesk = Hex.decode("77 07 6d 0a 73 18 a5 7d 3c 16 c1 72 51 b2 66 45 df 4c 2f 87 eb c0 99 2a b1 77 fb a5 1d b9 2c 2a");
        return alicesk;

//        java.math.BigInteger i = new java.math.BigInteger("3258612805645805793480074867989621323193038677294493313537981367693025155795");
//        return i.toByteArray();
    }
}
