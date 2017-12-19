package net.consensys.athena.impl.http.server;

import net.consensys.athena.impl.http.data.ContentType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Serializer {
  private final ObjectMapper jsonObjectMapper;
  private final ObjectMapper cborObjectMapper;

  public Serializer(ObjectMapper jsonObjectMapper, ObjectMapper cborObjectMapper) {
    this.jsonObjectMapper = jsonObjectMapper;
    this.cborObjectMapper = cborObjectMapper;
  }

  public byte[] serialize(Object obj, ContentType contentType) throws IOException {
    switch (contentType) {
      case JAVA_ENCODED:
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(byteOutputStream);
        outputStream.writeObject(obj);
        return byteOutputStream.toByteArray();
      case JSON:
        return jsonObjectMapper.writeValueAsString(obj).getBytes(Charset.forName("UTF-8"));
      case RAW:
        return obj.toString().getBytes(Charset.forName("UTF-8"));
      case CBOR:
        return cborObjectMapper.writeValueAsBytes(obj);
      default:
        throw new NotSerializableException();
    }
  }

  @SuppressWarnings("unchecked")
  public <T> T deserialize(byte[] bytes, ContentType contentType, Class<T> valueType)
      throws IOException, ClassNotFoundException, ClassCastException {
    switch (contentType) {
      case JAVA_ENCODED:
        return (T) new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
      case JSON:
        return jsonObjectMapper.readValue(bytes, valueType);
      case CBOR:
        return cborObjectMapper.readValue(bytes, valueType);
      default:
        throw new NotSerializableException();
    }
  }
}
