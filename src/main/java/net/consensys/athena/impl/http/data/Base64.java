package net.consensys.athena.impl.http.data;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class Base64 {

  public static String encode(byte[] bytes) {
    return new String(java.util.Base64.getEncoder().encode(bytes), Charset.forName("UTF-8"));
  }

  public static byte[] decode(String b64) {
    try {
      return java.util.Base64.getDecoder().decode(b64.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException io) {
      throw new RuntimeException(io);
    }
  }
}
