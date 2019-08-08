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

import java.util.HashMap;
import java.util.Map;
import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.crypto.sodium.Box.PublicKey;
import net.consensys.cava.io.Base64;
import net.consensys.orion.http.handler.receive.ReceiveRequest;
import net.consensys.orion.http.handler.send.SendRequest;
import net.consensys.orion.http.server.HttpContentType;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Responsible for providing access to a running Orion instance via its HTTP interface such that
 * payloads can be submitted and extracted.
 *
 * It also encapsulates all aspects of the Orion Instance - such as its datapath and maintained
 * keys.
 */
public class OrionNode {
  private static final Logger LOG = LogManager.getLogger();

  private static final String configFileName = "config.toml";
  private static final String networkInterface = "127.0.0.1";

  private static final String urlPattern = "http://" + networkInterface + ":%d";

  private final String nodeName;
  private final List<KeyDefinition> keys;
  private final Path workPath;
  private final String libSodiumPath;
  private final List<String> bootnodeClientUrl;
  private OkHttpClient httpClient;

  private OrionProcessRunner runner = null;

  public OrionNode(
      final String nodeName,
      final List<KeyDefinition> keys,
      final Path workPath,
      final String libSodiumPath,
      final List<String> bootnodeClientUrl) {
    this.nodeName = nodeName;
    this.keys = keys;
    this.workPath = workPath;
    this.libSodiumPath = libSodiumPath;
    this.bootnodeClientUrl = bootnodeClientUrl;
  }

  public void start() throws IOException {
    generateConfigFile();
    runner = new OrionProcessRunner(configFileName, workPath);
    runner.start(nodeName);
    httpClient = new OkHttpClient();

  }

  public int peerCount() throws IOException {
        final Request request =
        new Request.Builder().url(clientAddress() + "/peercount").get().build();
    final Response response = httpClient.newCall(request).execute();

    LOG.info("Peers = " + response.body());

    return Integer.parseInt(response.body().string());
  }

  public String sendData(final byte[] data, final Box.PublicKey sender,
      final Collection<PublicKey> recipients)
      throws IOException {

    final JsonObject responseJson = createSendRequest(data, sender, recipients);

    return responseJson.getString("key");

  }

  public byte[] extractDataItem(final String dataKey, final Box.PublicKey identity)
      throws IOException {
    final ReceiveRequest rxReqeust =
        new ReceiveRequest(dataKey, Base64.encodeBytes(identity.bytesArray()));

    final JsonObject responseJson = sendRequestToOrion(rxReqeust);
    return Base64.decodeBytes(responseJson.getString("payload"));
  }

  public Box.PublicKey getPublicKey(final int index) {
    return keys.get(index).getKeys().publicKey();
  }


  public String clientAddress() {
    return String.format(urlPattern, runner.clientPort());
  }

  public String nodeAddress() {
    return String.format(urlPattern, runner.nodePort());
  }

  private void generateConfigFile() throws IOException {
    final String pubKeys =
        keys.stream().map(k -> "\"" + k.getPublicKeyPath().getFileName().toString() + "\"").collect(
            Collectors.joining(","));
    final String privKeys =
        keys.stream().map(k -> "\"" + k.getPrivateKeyPath().getFileName().toString() + "\"")
            .collect(
                Collectors.joining(","));

    final String otherNodes =
        bootnodeClientUrl.stream().map(k -> "\"" + k + "\"").collect(Collectors.joining(","));

    // 0 means Vertx will find a suitable port.
    String configContent = "workdir = \"" + workPath.toString() + "\"\n";
    configContent += "nodeUrl = \"" + String.format(urlPattern, 0) + "\"\n";
    configContent += "clientUrl = \"" + String.format(urlPattern, 0) + "\"\n";
    configContent += "nodenetworkinterface = \"" + networkInterface + "\"\n";
    configContent += "clientnetworkinterface = \"" + networkInterface + "\"\n";
    configContent += "libsodiumPath = \"" + libSodiumPath + "\"\n";
    configContent += "nodeport = 0\n";
    configContent += "clientport = 0\n";

    configContent += "publickeys = [" + pubKeys + "]\n";
    configContent += "privatekeys = [" + privKeys + "]\n";
    configContent += "othernodes  = [" + otherNodes + "]\n";

    final File configFile = new File(workPath.toFile(), configFileName);
    Files.write(configFile.toPath(), configContent.getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE);

  }

  private <T> JsonObject sendRequestToOrion(final T serialisableObject) throws IOException {
    final JsonObject payload = JsonObject.mapFrom(serialisableObject);
    final RequestBody requestBody =
        RequestBody.create(MediaType.parse(HttpContentType.JSON.toString()), payload.encode());

    final Request request =
        new Request.Builder().url(clientAddress() + "/receive").post(requestBody).build();
    final Response response = httpClient.newCall(request).execute();

    return new JsonObject(response.body().string());
  }

  private JsonObject createSendRequest(
      byte[] payload,
      final Box.PublicKey sender,
      final Collection<Box.PublicKey> to) throws IOException {
    Map<String, Object> map = new HashMap<>();
    map.put("payload", payload);
    map.put("from", Base64.encodeBytes(sender.bytesArray()));
    map.put("to", to.stream().map(t -> Base64.encodeBytes(t.bytesArray())).toArray());

    final JsonObject jsonToSend = new JsonObject(map);
    final RequestBody requestBody =
        RequestBody.create(MediaType.parse(HttpContentType.JSON.toString()), jsonToSend.encode());
    final Request request =
        new Request.Builder().url(clientAddress() + "/send").post(requestBody).build();
    final Response response = httpClient.newCall(request).execute();

    return new JsonObject(response.body().string());
  }
}
