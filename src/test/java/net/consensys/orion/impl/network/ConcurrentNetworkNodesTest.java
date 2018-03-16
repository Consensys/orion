package net.consensys.orion.impl.network;

import static org.junit.Assert.*;

import net.consensys.orion.impl.enclave.sodium.SodiumPublicKey;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Serializer;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.junit.Test;

public class ConcurrentNetworkNodesTest {
  @Test
  public void roundTripSerialization() throws MalformedURLException {
    CopyOnWriteArraySet<URL> urls = new CopyOnWriteArraySet<>();
    URL u = new URL("http://nowhere:9090/");
    urls.add(u);
    ConcurrentHashMap<SodiumPublicKey, URL> pks = new ConcurrentHashMap<>();
    pks.put(new SodiumPublicKey("bytes".getBytes()), u);
    ConcurrentNetworkNodes nodes =
        new ConcurrentNetworkNodes(new URL("http://some.server:8080/"), urls, pks);
    Serializer serializer = new Serializer();
    byte[] bytes = serializer.serialize(HttpContentType.JSON, nodes);
    assertEquals(
        nodes, serializer.deserialize(HttpContentType.JSON, ConcurrentNetworkNodes.class, bytes));
    bytes = serializer.serialize(HttpContentType.CBOR, nodes);
    assertEquals(
        nodes, serializer.deserialize(HttpContentType.CBOR, ConcurrentNetworkNodes.class, bytes));
  }
}
