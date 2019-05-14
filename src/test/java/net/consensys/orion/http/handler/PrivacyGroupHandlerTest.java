/*
 * Copyright 2019 ConsenSys AG.
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
package net.consensys.orion.http.handler;

import static net.consensys.cava.io.Base64.encodeBytes;
import static net.consensys.orion.http.server.HttpContentType.JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.orion.enclave.Enclave;
import net.consensys.orion.enclave.sodium.MemoryKeyStore;
import net.consensys.orion.enclave.sodium.SodiumEnclave;
import net.consensys.orion.http.handler.privacy.PrivacyGroupRequest;
import net.consensys.orion.http.handler.privacy.PrivacyGroups;
import net.consensys.orion.utils.Serializer;

import java.nio.file.Path;
import java.util.Arrays;

import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

public class PrivacyGroupHandlerTest extends HandlerTest {
  private MemoryKeyStore memoryKeyStore;

  @Override
  protected Enclave buildEnclave(Path tempDir) {
    memoryKeyStore = new MemoryKeyStore();
    Box.PublicKey defaultNodeKey = memoryKeyStore.generateKeyPair();
    memoryKeyStore.addNodeKey(defaultNodeKey);
    return new SodiumEnclave(memoryKeyStore);
  }

  @Test
  void expectedPrivacyGroupId() throws Exception {
    Box.PublicKey senderKey = memoryKeyStore.generateKeyPair();
    Box.PublicKey recipientKey = memoryKeyStore.generateKeyPair();

    String[] toEncrypt = new String[] {encodeBytes(senderKey.bytesArray()), encodeBytes(recipientKey.bytesArray())};
    Box.PublicKey[] addresses = Arrays.stream(toEncrypt).map(enclave::readKey).toArray(Box.PublicKey[]::new);
    PrivacyGroupRequest privacyGroupRequestExpected = buildPrivacyGroupRequest(toEncrypt);
    Request request = buildPrivateAPIRequest("/privacyGroupId", JSON, privacyGroupRequestExpected);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    assertEquals(200, resp.code());


    PrivacyGroups[] privacyGroups = Serializer.deserialize(JSON, PrivacyGroups[].class, resp.body().bytes());

    assertEquals(privacyGroups[0].getPrivacyGroupId(), encodeBytes(enclave.generatePrivacyGroupId(addresses)));
  }

  PrivacyGroupRequest buildPrivacyGroupRequest(String[] addresses) {
    return new PrivacyGroupRequest(addresses);
  }
}
