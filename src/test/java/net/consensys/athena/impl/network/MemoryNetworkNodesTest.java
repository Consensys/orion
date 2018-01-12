package net.consensys.athena.impl.network;

import static org.junit.Assert.*;

import net.consensys.athena.impl.enclave.sodium.SodiumPublicKey;
import net.consensys.athena.impl.http.server.HttpContentType;
import net.consensys.athena.impl.utils.Serializer;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.junit.Test;

public class MemoryNetworkNodesTest {
  @Test
  public void testRoundTripSerialization() throws MalformedURLException {
    CopyOnWriteArraySet<URL> urls = new CopyOnWriteArraySet<>();
    URL u = new URL("http://nowhere:9090/");
    urls.add(u);
    ConcurrentHashMap<SodiumPublicKey, URL> pks = new ConcurrentHashMap<>();
    pks.put(new SodiumPublicKey("bytes".getBytes()), u);
    MemoryNetworkNodes nodes =
        new MemoryNetworkNodes(new URL("http://some.server:8080/"), urls, pks);
    Serializer serializer = new Serializer();
    byte[] bytes = serializer.serialize(HttpContentType.JSON, nodes);
    assertEquals(
        nodes, serializer.deserialize(HttpContentType.JSON, MemoryNetworkNodes.class, bytes));
    bytes = serializer.serialize(HttpContentType.CBOR, nodes);
    assertEquals(
        nodes, serializer.deserialize(HttpContentType.CBOR, MemoryNetworkNodes.class, bytes));
  }
}
