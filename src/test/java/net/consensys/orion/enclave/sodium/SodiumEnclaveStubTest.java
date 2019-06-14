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
package net.consensys.orion.enclave.sodium;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import net.consensys.orion.enclave.EncryptedPayload;

import javax.xml.bind.DatatypeConverter;

import org.junit.jupiter.api.Test;

class SodiumEnclaveStubTest {

  @Test
  void roundTripEncryption() {
    byte[] message = "hello".getBytes(UTF_8);
    SodiumEnclaveStub enclave = new SodiumEnclaveStub();
    EncryptedPayload encryptedPayload = enclave.encrypt(message, null, null, null);
    byte[] bytes = enclave.decrypt(encryptedPayload, null);
    assertArrayEquals(message, bytes);
  }

  @Test
  void roundTripEncryptionWithFunkyBytes() {
    byte[] message = DatatypeConverter.parseHexBinary("0079FF00FF89");
    SodiumEnclaveStub enclave = new SodiumEnclaveStub();
    EncryptedPayload encryptedPayload = enclave.encrypt(message, null, null, null);
    byte[] bytes = enclave.decrypt(encryptedPayload, null);
    assertArrayEquals(message, bytes);
  }
}
