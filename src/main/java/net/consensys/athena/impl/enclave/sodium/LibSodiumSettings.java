package net.consensys.athena.impl.enclave.sodium;

import java.io.File;

import com.sun.jna.Platform;

public class LibSodiumSettings {

  private LibSodiumSettings() {}

  public static String defaultLibSodiumPath() {
    if (Platform.isMac()) {
      return "/usr/local/lib/libsodium.dylib";
    } else if (Platform.isWindows()) {
      return "C:/libsodium/libsodium.dll";
    } else if (new File("/usr/lib/x86_64-linux-gnu/libsodium.so").exists()) {
      //Ubuntu trusty location.
      return "/usr/lib/x86_64-linux-gnu/libsodium.so";
    } else {
      return "/usr/local/lib/libsodium.so";
    }
  }
}
