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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.api.enclave.KeyConfig;
import net.consensys.orion.api.enclave.KeyStore;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Base64;
import net.consensys.orion.impl.utils.Serializer;

import java.nio.file.Path;
import java.security.PublicKey;
import java.util.Optional;

import com.muquit.libsodiumjna.SodiumLibrary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class SodiumPublicKeyTest {

  private KeyConfig keyConfig;
  private KeyStore memoryKeyStore;

  @BeforeEach
  void setUp(@TempDirectory Path tempDir) {
    keyConfig = new KeyConfig(tempDir.resolve("ignore"), Optional.empty());
    SodiumLibrary.setLibraryPath(LibSodiumSettings.defaultLibSodiumPath());
    memoryKeyStore = new SodiumMemoryKeyStore();
  }

  @Test
  void roundTripSerialization() {
    SodiumPublicKey key = new SodiumPublicKey("fake encoded".getBytes(UTF_8));
    byte[] bytes = Serializer.serialize(HttpContentType.JSON, key);
    assertEquals(key, Serializer.deserialize(HttpContentType.JSON, SodiumPublicKey.class, bytes));
    bytes = Serializer.serialize(HttpContentType.CBOR, key);
    assertEquals(key, Serializer.deserialize(HttpContentType.CBOR, SodiumPublicKey.class, bytes));
  }

  @Test
  void keyFromB64EqualsOriginal() {
    // generate key
    PublicKey fakePK = memoryKeyStore.generateKeyPair(keyConfig);

    // b64 representation of key
    String b64 = Base64.encode(fakePK.getEncoded());

    // create new object from decoded key
    PublicKey rebuiltKey = new SodiumPublicKey(Base64.decode(b64));

    assertEquals(fakePK, rebuiltKey);
  }
}
