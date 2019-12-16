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
package net.consensys.orion.acceptance.dsl;

import net.consensys.orion.http.handler.receive.ReceiveRequest;
import net.consensys.orion.http.server.HttpContentType;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import io.vertx.core.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.tuweni.crypto.sodium.Box;
import org.apache.tuweni.crypto.sodium.Box.PublicKey;
import org.apache.tuweni.io.Base64;

/**
 * Responsible for providing access to a running Orion instance via its HTTP interface such that payloads can be
 * submitted and extracted.
 *
 * It also encapsulates all aspects of the Orion Instance - such as its datapath and maintained publicKeys.
 */
public class OrionNode {

  private final List<Box.PublicKey> publicKeys;
  private final OkHttpClient httpClient = new OkHttpClient();

  private final OrionProcessRunner runner;
  private final int bootNodeCount;

  public OrionNode(final List<Box.PublicKey> publicKeys, final OrionProcessRunner runner, final int bootNodeCount) {
    this.publicKeys = publicKeys;
    this.runner = runner;
    this.bootNodeCount = bootNodeCount;
  }

  public int peerCount() throws IOException {
    final Request request = new Request.Builder().url(clientUrl() + "/peercount").get().build();
    final Response response = httpClient.newCall(request).execute();

    final String peerCountStr = response.body().string();
    return Integer.parseInt(peerCountStr);
  }

  public int getBootnodeCount() {
    return bootNodeCount;
  }

  public int getPublicKeyCount() {
    return publicKeys.size();
  }

  public String sendData(final byte[] data, final Box.PublicKey sender, final PublicKey... recipients)
      throws IOException {

    final JsonObject responseJson = createSendRequest(data, sender, recipients);

    return responseJson.getString("key");
  }

  public byte[] extractDataItem(final String dataKey, final Box.PublicKey identity) throws IOException {
    final ReceiveRequest rxReqeust = new ReceiveRequest(dataKey, Base64.encodeBytes(identity.bytesArray()));

    final JsonObject responseJson = sendRequestToOrion(rxReqeust);
    return Base64.decodeBytes(responseJson.getString("payload"));
  }

  public Box.PublicKey getPublicKey(final int index) {
    return publicKeys.get(index);
  }

  private String clientUrl() {
    return runner.clientUrl();
  }

  public String nodeUrl() {
    return runner.nodeUrl();
  }

  private <T> JsonObject sendRequestToOrion(final T serialisableObject) throws IOException {
    final JsonObject payload = JsonObject.mapFrom(serialisableObject);
    final RequestBody requestBody =
        RequestBody.create(MediaType.parse(HttpContentType.JSON.toString()), payload.encode());

    final Request request = new Request.Builder().url(clientUrl() + "/receive").post(requestBody).build();
    final Response response = httpClient.newCall(request).execute();

    return new JsonObject(response.body().string());
  }

  private JsonObject createSendRequest(final byte[] payload, final Box.PublicKey sender, final PublicKey... recipients)
      throws IOException {
    final List<PublicKey> to = Lists.newArrayList(recipients);
    final Map<String, Object> map = new HashMap<>();
    map.put("payload", payload);
    map.put("from", Base64.encodeBytes(sender.bytesArray()));
    map.put("to", to.stream().map(t -> Base64.encodeBytes(t.bytesArray())).toArray());

    final JsonObject jsonToSend = new JsonObject(map);
    final RequestBody requestBody =
        RequestBody.create(MediaType.parse(HttpContentType.JSON.toString()), jsonToSend.encode());
    final Request request = new Request.Builder().url(clientUrl() + "/send").post(requestBody).build();
    final Response response = httpClient.newCall(request).execute();

    return new JsonObject(response.body().string());
  }
}
