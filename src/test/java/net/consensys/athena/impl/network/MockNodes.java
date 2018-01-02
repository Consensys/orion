package net.consensys.athena.impl.network;

import net.consensys.athena.api.network.NetworkNodes;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.HashSet;

public class MockNodes implements NetworkNodes {
    @Override
    public URL url() {
        try {
            return new URL("http://localhost:9001");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public HashSet<URL> nodeURLs() {
        return null;
    }

    @Override
    public URL urlForRecipient(PublicKey recipient) {
        return null;
    }

    @Override
    public HashMap<PublicKey, URL> nodePKs() {
        return null;
    }
}
