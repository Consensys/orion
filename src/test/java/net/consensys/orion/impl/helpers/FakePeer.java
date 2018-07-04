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

package net.consensys.orion.impl.helpers;

import java.io.IOException;
import java.net.URL;
import java.security.PublicKey;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class FakePeer {
  public final MockWebServer server;
  public final PublicKey publicKey;

  public FakePeer(MockResponse response, PublicKey publicKey) throws IOException {
    server = new MockWebServer();
    this.publicKey = publicKey;
    server.enqueue(response);
    server.start();
  }

  public URL getURL() {
    return server.url("").url();
  }
}
