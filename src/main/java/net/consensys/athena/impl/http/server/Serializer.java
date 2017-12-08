package net.consensys.athena.impl.http.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Serializer {
  static final ObjectMapper jsonObjectMapper = new ObjectMapper();

  public enum Protocol {
    JAVA,
    JSON,
    HASKELL,
    RLPX
  }

  public static byte[] serialize(Object obj, Protocol protocol) throws IOException {
    switch (protocol) {
      case JAVA:
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(byteOutputStream);
        outputStream.writeObject(obj);
        return byteOutputStream.toByteArray();
      case JSON:
        return jsonObjectMapper.writeValueAsString(obj).getBytes(Charset.forName("UTF-8"));
      default:
        throw new NotSerializableException();
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> T deserialize(byte[] bytes, Protocol protocol, Class<T> valueType)
      throws IOException, ClassNotFoundException, ClassCastException {
    switch (protocol) {
      case JAVA:
        return (T) new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
      case JSON:
        return jsonObjectMapper.readValue(bytes, valueType);
      default:
        throw new NotSerializableException();
    }
  }
}
