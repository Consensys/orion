package net.consensys.athena.impl.enclave;

import org.junit.Test;

import static org.junit.Assert.*;

public class EncryptedPayloadTest {
    @Test
    public void getSender() throws Exception {
    }

    @Test
    public void getCipherText() throws Exception {
    }

    @Test
    public void should_get_new_Nonce() throws Exception {
        EncryptedPayload payload = new EncryptedPayload();
        long actual = payload.getNonce();
        assertTrue(actual > 0);
    }

    @Test
    public void getCombinedKeys() throws Exception {
    }

    @Test
    public void getCombinedKeyNonce() throws Exception {
    }

}