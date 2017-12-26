package net.consensys.athena.impl.http;

import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.Serializer;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import org.junit.Test;

public class SerializerTest {

  Serializer serializer = new Serializer(new ObjectMapper(), new ObjectMapper(new CBORFactory()));

  @Test
  public void testJsonSerialization() throws Exception {
    DummyObject dummyObjectOriginal = new DummyObject();
    byte[] bytes = serializer.serialize(dummyObjectOriginal, ContentType.JSON);
    DummyObject dummyObject = serializer.deserialize(bytes, ContentType.JSON, DummyObject.class);
    assert (dummyObject.equals(dummyObjectOriginal));
  }

  @Test
  public void testCBORSerialization() throws Exception {
    DummyObject dummyObjectOriginal = new DummyObject();
    byte[] bytes = serializer.serialize(dummyObjectOriginal, ContentType.CBOR);
    DummyObject dummyObject = serializer.deserialize(bytes, ContentType.CBOR, DummyObject.class);
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
