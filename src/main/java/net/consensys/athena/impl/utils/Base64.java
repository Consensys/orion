package net.consensys.athena.impl.utils;

import java.nio.charset.StandardCharsets;

public class Base64 {

  public static String encode(byte[] bytes) {
    return new String(java.util.Base64.getEncoder().encode(bytes), StandardCharsets.UTF_8);
  }

  public static byte[] decode(String b64) {
    return java.util.Base64.getDecoder().decode(b64.getBytes(StandardCharsets.UTF_8));
  }
}
