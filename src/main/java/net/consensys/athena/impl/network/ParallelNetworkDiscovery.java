package net.consensys.athena.impl.network;

import net.consensys.athena.api.network.NetworkDiscovery;
import net.consensys.athena.api.network.NetworkNodes;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ParallelNetworkDiscovery implements NetworkDiscovery {
  private final long timeout = 1000;
  private NetworkNodes nodes;

  public ParallelNetworkDiscovery(NetworkNodes nodes) {
    this.nodes = nodes;
  }

  @Override
  public NetworkNodes doDiscover() throws IOException {
    return doDiscover(timeout);
  }

  @Override
  public NetworkNodes doDiscover(long timeout) throws IOException {
    OkHttpClient client =
        new OkHttpClient.Builder().connectTimeout(timeout, TimeUnit.MILLISECONDS).build();

    MemoryNetworkNodes newNodes = new MemoryNetworkNodes();

    nodes
        .nodeURLs()
        .parallelStream()
        .forEach(
            node -> {
              Request request = new Request.Builder().url(node + "/partyinfo").build();

              try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                  URL newNode = new URL("http://localhost");
                  newNodes.addNodeURL(newNode);
                }
                response.body().close();
              } catch (IOException ex) {
                //
              }
            });

    return newNodes;
  }
}
