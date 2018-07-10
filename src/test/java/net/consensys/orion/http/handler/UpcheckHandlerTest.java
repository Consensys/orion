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

package net.consensys.orion.http.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

class UpcheckHandlerTest extends HandlerTest {

  @Test
  void publicUpcheck() throws Exception {
    Request request = new Request.Builder().get().url(nodeBaseUrl + "/upcheck").build();

    Response resp = httpClient.newCall(request).execute();
    assertEquals(200, resp.code());
    assertEquals("I'm up!", resp.body().string());
  }

  @Test
  void privateUpcheck() throws Exception {
    Request request = new Request.Builder().get().url(clientBaseUrl + "/upcheck").build();

    Response resp = httpClient.newCall(request).execute();
    assertEquals(200, resp.code());
    assertEquals("I'm up!", resp.body().string());
  }
}
