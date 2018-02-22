package net.consensys.orion.impl.utils;

import net.consensys.orion.impl.http.server.HttpContentType;

import java.io.File;
import java.io.IOException;
import java.io.NotSerializableException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

public class Serializer {
  private final ObjectMapper jsonObjectMapper;
  private final ObjectMapper cborObjectMapper;

  public Serializer() {
    cborObjectMapper = setupObjectMapper(new ObjectMapper(new CBORFactory()));
    jsonObjectMapper = setupObjectMapper(new ObjectMapper());
  }

  private ObjectMapper setupObjectMapper(ObjectMapper objectMapper) {
    objectMapper.setSerializationInclusion(Include.NON_NULL);
    objectMapper.registerModule(new Jdk8Module());
    return objectMapper;
  }

  public byte[] serialize(HttpContentType contentType, Object obj) {
    try {
      switch (contentType) {
        case JSON:
          return jsonObjectMapper.writeValueAsString(obj).getBytes(StandardCharsets.UTF_8);
        case TEXT:
          return obj.toString().getBytes(StandardCharsets.UTF_8);
        case CBOR:
          return cborObjectMapper.writeValueAsBytes(obj);
        default:
          throw new SerializationException(new NotSerializableException());
      }
    } catch (IOException io) {
      throw new SerializationException(io);
    }
  }

  public <T> T deserialize(HttpContentType contentType, Class<T> valueType, byte[] bytes) {
    try {
      switch (contentType) {
        case JSON:
          return jsonObjectMapper.readValue(bytes, valueType);
        case CBOR:
          return cborObjectMapper.readValue(bytes, valueType);
        case TEXT:
          return valueType.cast(new String(bytes, StandardCharsets.UTF_8));
        default:
          throw new SerializationException(new NotSerializableException());
      }
    } catch (Exception e) {
      throw new SerializationException(e);
    }
  }

  public void writeFile(HttpContentType contentType, File file, Object obj) {
    try {
      getMapperOrThrows(contentType).writeValue(file, obj);
    } catch (IOException io) {
      throw new SerializationException(io);
    }
  }

  public <T> T readFile(HttpContentType contentType, File file, Class<T> valueType) {
    try {
      return getMapperOrThrows(contentType).readValue(file, valueType);
    } catch (IOException io) {
      throw new SerializationException(io);
    }
  }

  public <T> T roundTrip(HttpContentType contentType, Class<T> valueType, Object obj) {
    return deserialize(contentType, valueType, serialize(contentType, obj));
  }

  private ObjectMapper getMapperOrThrows(HttpContentType contentType) {
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
