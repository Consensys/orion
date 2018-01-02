package net.consensys.athena.impl.network;
import net.consensys.athena.api.config.Node;
import net.consensys.athena.api.network.NetworkNodes;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class NetworkDiscovery extends BaseNetworkDiscovery implements net.consensys.athena.api.network.NetworkDiscovery {

    public NetworkDiscovery() {
        nodeStatuses = new ArrayList<NodeStatus>();
    }

    public NetworkDiscovery(Collection<NodeStatus> nodeStatuses) {
        this.nodeStatuses = nodeStatuses;
    }

    public NetworkDiscovery(NetworkNodes nodes) {

        nodeStatuses = new ArrayList<NodeStatus>();
        HashSet<URL> urls = nodes.nodeURLs();

        for (URL url : urls) {
            NodeStatus nodeStatus = new NodeStatus();
            nodeStatus.setURL(url);
            nodeStatuses.add(nodeStatus);
        }
    }

    @Override
    public void doDiscover() throws IOException {
        doDiscover(timeout);
    }

    @Override
    public void doDiscover(long timeout) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .build();

        for(NodeStatus nodeStatus : nodeStatuses) {

            Request request = new Request.Builder()
                    .url(nodeStatus.getURL() + "/upcheck")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    nodeStatus.Update();
                }
                response.body().close();
            }
        }
    }
}
