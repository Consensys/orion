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

package net.consensys.orion.impl.enclave.sodium;

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
