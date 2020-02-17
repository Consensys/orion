package net.consensys.orion.storage;

import net.consensys.orion.cmd.Orion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.crypto.sodium.Box;
import org.apache.tuweni.kv.KeyValueStore;
import org.apache.tuweni.kv.ProxyKeyValueStore;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/** Utility functions used to manipulate key-value store to expose higher order functions. **/
public class StorageUtils {

    private static final Logger log = LogManager.getLogger(StorageUtils.class);

    private StorageUtils() {
    }

    public static KeyValueStore<Box.PublicKey, URI> convertToPubKeyStore(KeyValueStore<Bytes, Bytes> store) {
        return ProxyKeyValueStore
                .open(store, Box.PublicKey::fromBytes, Box.PublicKey::bytes, StorageUtils::bytesToURI, StorageUtils::uriToBytes);
    }

    private static Bytes uriToBytes(Box.PublicKey key, URI uri) {
        return Bytes.wrap(uri.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static URI bytesToURI(Bytes v) {
        try {
            return URI.create(new String(v.toArray(), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            log.warn("Error reading URI", e);
        }
        return null;
    }
}
