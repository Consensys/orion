package net.consensys.athena.impl.http.data;

import static net.consensys.athena.impl.http.data.ContentType.CBOR;
import static net.consensys.athena.impl.http.data.ContentType.JSON;

import java.io.File;
import java.io.IOException;
import java.io.NotSerializableException;
import java.nio.charset.Charset;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

public class Serializer {
  private final ObjectMapper jsonObjectMapper;
  private final ObjectMapper cborObjectMapper;

  public Serializer() {
    cborObjectMapper = new ObjectMapper(new CBORFactory());
    cborObjectMapper.setSerializationInclusion(Include.NON_NULL);

    jsonObjectMapper = new ObjectMapper();
    jsonObjectMapper.setSerializationInclusion(Include.NON_NULL);
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

  public void writeFile(Object obj, ContentType contentType, File file) {
    try {
      getMapperOrThrows(contentType).writeValue(file, obj);
    } catch (IOException io) {
      throw new SerializationException(io);
    }
  }

  public <T> T readFile(ContentType contentType, File file, Class<T> valueType) {
    try {
      return getMapperOrThrows(contentType).readValue(file, valueType);
    } catch (IOException io) {
      throw new SerializationException(io);
    }
  }

  private ObjectMapper getMapperOrThrows(ContentType contentType) throws SerializationException {
    switch (contentType) {
      case JSON:
        return jsonObjectMapper;
      case CBOR:
        return cborObjectMapper;
      default:
        throw new SerializationException(new NotSerializableException());
    }
  }
}
