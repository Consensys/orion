package net.consensys.athena.impl.http.data;

import java.io.IOException;
import java.io.NotSerializableException;
import java.nio.charset.Charset;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Serializer {
  private final ObjectMapper jsonObjectMapper;
  private final ObjectMapper cborObjectMapper;

  public Serializer(ObjectMapper jsonObjectMapper, ObjectMapper cborObjectMapper) {
    this.jsonObjectMapper = jsonObjectMapper;
    this.cborObjectMapper = cborObjectMapper;
  }

  public byte[] serialize(Object obj, ContentType contentType) {
    try {
      switch (contentType) {
        case JSON:
          return jsonObjectMapper.writeValueAsString(obj).getBytes(Charset.forName("UTF-8"));
        case TEXT:
          return obj.toString().getBytes(Charset.forName("UTF-8"));
        case CBOR:
          return cborObjectMapper.writeValueAsBytes(obj);
        default:
          throw new SerializationException(new NotSerializableException());
      }
    } catch (IOException io) {
      throw new SerializationException(io);
    }
  }

  @SuppressWarnings("unchecked")
  public <T> T deserialize(byte[] bytes, ContentType contentType, Class<T> valueType) {
    try {
      switch (contentType) {
        case JSON:
          return jsonObjectMapper.readValue(bytes, valueType);
        case CBOR:
          return cborObjectMapper.readValue(bytes, valueType);
        case TEXT:
          if (valueType != String.class) {
            throw new SerializationException(new NotSerializableException());
          }
          return (T) new String(bytes, Charset.forName("UTF-8"));
        default:
          throw new SerializationException(new NotSerializableException());
      }
    } catch (Exception e) {
      throw new SerializationException(e);
    }
  }
}
