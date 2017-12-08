package net.consensys.athena.impl.http;

import net.consensys.athena.impl.http.server.Serializer;
import net.consensys.athena.impl.http.server.Serializer.Protocol;

import java.io.Serializable;
import java.util.Objects;

import org.junit.Test;

public class SerializerTest {

  @Test
  public void testJavaSerialization() throws Exception {
    DummyObject dummyObjectOriginal = new DummyObject();
    byte[] bytes = Serializer.serialize(dummyObjectOriginal, Protocol.JAVA);
    DummyObject dummyObject = Serializer.deserialize(bytes, Protocol.JAVA, DummyObject.class);
    assert (dummyObject.equals(dummyObjectOriginal));
  }

  @Test
  public void testJsonSerialization() throws Exception {
    DummyObject dummyObjectOriginal = new DummyObject();
    byte[] bytes = Serializer.serialize(dummyObjectOriginal, Protocol.JSON);
    DummyObject dummyObject = Serializer.deserialize(bytes, Protocol.JSON, DummyObject.class);
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
