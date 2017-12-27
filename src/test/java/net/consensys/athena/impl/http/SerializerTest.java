package net.consensys.athena.impl.http;

import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.Serializer;

import java.io.Serializable;
import java.util.Objects;

import org.junit.Test;

public class SerializerTest {

  final Serializer serializer = new Serializer();

  @Test
  public void testJsonSerialization() throws Exception {
    DummyObject dummyObjectOriginal = new DummyObject();
    byte[] bytes = serializer.serialize(ContentType.JSON, dummyObjectOriginal);
    DummyObject dummyObject = serializer.deserialize(ContentType.JSON, DummyObject.class, bytes);
    assert (dummyObject.equals(dummyObjectOriginal));
  }

  @Test
  public void testCBORSerialization() throws Exception {
    DummyObject dummyObjectOriginal = new DummyObject();
    byte[] bytes = serializer.serialize(ContentType.CBOR, dummyObjectOriginal);
    DummyObject dummyObject = serializer.deserialize(ContentType.CBOR, DummyObject.class, bytes);
    assert (dummyObject.equals(dummyObjectOriginal));
  }
}

class DummyObject implements Serializable {
  public String name;
  public int age;

  public DummyObject() {
    this.name = "john";
    this.age = 42;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DummyObject that = (DummyObject) o;
    return age == that.age && Objects.equals(name, that.name);
  }
}
