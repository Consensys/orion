package net.consensys.athena.impl.enclave.sodium;

import static org.junit.Assert.*;

import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.Serializer;

import org.junit.Test;

public class SodiumPublicKeyTest {

  @Test
  public void testRoundTripSerialization() {
    SodiumPublicKey key = new SodiumPublicKey("fake encoded".getBytes());
    Serializer serializer = new Serializer();
    byte[] bytes = serializer.serialize(ContentType.JSON, key);
    assertEquals(key, serializer.deserialize(ContentType.JSON, SodiumPublicKey.class, bytes));
    bytes = serializer.serialize(ContentType.CBOR, key);
    assertEquals(key, serializer.deserialize(ContentType.CBOR, SodiumPublicKey.class, bytes));
  }
}
