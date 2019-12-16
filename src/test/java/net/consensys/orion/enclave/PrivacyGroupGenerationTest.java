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
package net.consensys.orion.enclave;

import static org.apache.tuweni.io.Base64.encodeBytes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.consensys.orion.enclave.sodium.MemoryKeyStore;
import net.consensys.orion.enclave.sodium.SodiumEnclave;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Security;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tuweni.crypto.sodium.Box;
import org.junit.Before;
import org.junit.Test;

public class PrivacyGroupGenerationTest {


  private final MemoryKeyStore keyStore = new MemoryKeyStore();
  private SodiumEnclave enclave;

  static {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  @Before
  public void setUp() {
    enclave = new SodiumEnclave(keyStore);
  }

  @Test
  public void expectedPrivacyGroupIsGenerated() throws IOException {

    final URL url = getClass().getClassLoader().getResource("keySets.json");
    if (url == null) {
      throw new FileNotFoundException();
    }
    try (final Reader reader = Files.newBufferedReader(Paths.get(url.getPath()), Charset.defaultCharset())) {
      final ObjectMapper objectMapper = new ObjectMapper();

      final List<PrivacyGroupTest> privacyGroups =
          objectMapper.readValue(reader, new TypeReference<List<PrivacyGroupTest>>() {});

      assertTrue(true);
      for (final PrivacyGroupTest privacyGroup : privacyGroups) {
        final Box.PublicKey[] addresses =
            Arrays.stream(privacyGroup.getPrivacyGroup()).map(enclave::readKey).toArray(Box.PublicKey[]::new);
        assertEquals(
            privacyGroup.getPrivacyGroupId(),
            encodeBytes(enclave.generatePrivacyGroupId(addresses, null, PrivacyGroupPayload.Type.LEGACY)));

      }
    } catch (final IOException e) {
      throw e;
    }
  }
}


class PrivacyGroupTest {
  String privacyGroupId;
  String[] privacyGroup;

  public String getPrivacyGroupId() {
    return privacyGroupId;
  }

  public String[] getPrivacyGroup() {
    return privacyGroup;
  }

  public void setPrivacyGroupId(final String privacyGroupId) {
    this.privacyGroupId = privacyGroupId;
  }

  public void setPrivacyGroup(final String[] privacyGroup) {
    this.privacyGroup = privacyGroup;
  }
}
