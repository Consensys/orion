/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package net.consensys.orion.impl.utils;

import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.impl.http.server.HttpContentType;

import java.io.IOException;
import java.io.NotSerializableException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

public class Serializer {
  private static final ObjectMapper jsonObjectMapper = setupObjectMapper(new ObjectMapper());
  private static final ObjectMapper cborObjectMapper = setupObjectMapper(new ObjectMapper(new CBORFactory()));

  private Serializer() {}

  private static ObjectMapper setupObjectMapper(ObjectMapper objectMapper) {
    objectMapper.setSerializationInclusion(Include.NON_NULL);
    objectMapper.registerModule(new Jdk8Module());
    return objectMapper;
  }

  public static byte[] serialize(HttpContentType contentType, Object obj) {
    try {
      switch (contentType) {
        case JSON:
          return jsonObjectMapper.writeValueAsString(obj).getBytes(StandardCharsets.UTF_8);
        case TEXT:
          return obj.toString().getBytes(StandardCharsets.UTF_8);
        case CBOR:
          return cborObjectMapper.writeValueAsBytes(obj);
        default:
          throw new SerializationException(OrionErrorCode.OBJECT_JSON_SERIALIZATION, new NotSerializableException());
      }
    } catch (final IOException io) {
      throw new SerializationException(OrionErrorCode.OBJECT_JSON_SERIALIZATION, io);
    }
  }

  public static <T> T deserialize(HttpContentType contentType, Class<T> valueType, byte[] bytes) {
    try {
      switch (contentType) {
        case JSON:
          return jsonObjectMapper.readValue(bytes, valueType);
        case CBOR:
          return cborObjectMapper.readValue(bytes, valueType);
        case TEXT:
          return valueType.cast(new String(bytes, StandardCharsets.UTF_8));
        default:
          throw new SerializationException(OrionErrorCode.OBJECT_JSON_DESERIALIZATION, new NotSerializableException());
      }
    } catch (final Exception e) {
      throw new SerializationException(OrionErrorCode.OBJECT_JSON_DESERIALIZATION, e);
    }
  }

  public static void writeFile(HttpContentType contentType, Path file, Object obj) {
    try {
      getMapperOrThrows(contentType).writeValue(file.toFile(), obj);
    } catch (final IOException io) {
      throw new SerializationException(OrionErrorCode.OBJECT_WRITE, io);
    }
  }

  public static <T> T readFile(HttpContentType contentType, Path file, Class<T> valueType) {
    try {
      return getMapperOrThrows(contentType).readValue(file.toFile(), valueType);
    } catch (final IOException io) {
      throw new SerializationException(OrionErrorCode.OBJECT_READ, io);
    }
  }

  public static <T> T roundTrip(HttpContentType contentType, Class<T> valueType, Object obj) {
    return deserialize(contentType, valueType, serialize(contentType, obj));
  }

  private static ObjectMapper getMapperOrThrows(HttpContentType contentType) {
    switch (contentType) {
      case JSON:
        return jsonObjectMapper;
      case CBOR:
        return cborObjectMapper;
      default:
        throw new SerializationException(OrionErrorCode.OBJECT_UNSUPPORTED_TYPE, new NotSerializableException());
    }
  }
}
