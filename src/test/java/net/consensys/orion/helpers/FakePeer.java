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
package net.consensys.orion.helpers;

import net.consensys.orion.enclave.sodium.MemoryKeyStore;

import java.io.IOException;
import java.net.URI;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.tuweni.crypto.sodium.Box;
import org.apache.tuweni.crypto.sodium.Box.PublicKey;

public class FakePeer {
  public final MockWebServer server;
  public final Box.PublicKey publicKey;

  public FakePeer(final MockResponse response, final MemoryKeyStore memoryKeyStore) throws IOException {
    server = new MockWebServer();
    publicKey = memoryKeyStore.generateKeyPair();
    server.enqueue(response);
    server.start();
  }

  public FakePeer(final MockResponse response, final Box.PublicKey publicKey) throws IOException {
    server = new MockWebServer();
    this.publicKey = publicKey;
    server.enqueue(response);
    server.start();
  }

  public FakePeer(final PublicKey publicKey) throws IOException {
    this.server = new MockWebServer();
    this.publicKey = publicKey;
    server.start();
  }

  public void addResponse(final MockResponse response) {
    server.enqueue(response);
  }

  public URI getURI() {
    return server.url("").uri();
  }
}
