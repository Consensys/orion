package net.consensys.orion.impl.network;

import static java.nio.charset.StandardCharsets.UTF_8;
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
    pks.put(new SodiumPublicKey("bytes".getBytes(UTF_8)), u);
    ConcurrentNetworkNodes nodes = new ConcurrentNetworkNodes(new URL("http://some.server:8080/"), urls, pks);
    byte[] bytes = Serializer.serialize(HttpContentType.JSON, nodes);
    assertEquals(nodes, Serializer.deserialize(HttpContentType.JSON, ConcurrentNetworkNodes.class, bytes));
    bytes = Serializer.serialize(HttpContentType.CBOR, nodes);
    assertEquals(nodes, Serializer.deserialize(HttpContentType.CBOR, ConcurrentNetworkNodes.class, bytes));
  }
}
