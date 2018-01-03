package net.consensys.athena.impl.network;

import com.fasterxml.jackson.databind.ser.Serializers;
import net.consensys.athena.api.config.Node;
import net.consensys.athena.api.network.NetworkDiscovery;
import net.consensys.athena.api.network.NetworkNodes;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ParallelNetworkDiscovery extends BaseNetworkDiscovery implements NetworkDiscovery {

    @Override
    public Iterable<NodeStatus> getNodeStatuses() {
        return nodeStatuses;
    }

    public ParallelNetworkDiscovery(Iterable<NodeStatus> nodeStatuses) {
        this.nodeStatuses = new ArrayList<NodeStatus>();

        for(NodeStatus node : nodeStatuses) {
            this.nodeStatuses.add(node);
        }
    }

    public ParallelNetworkDiscovery(NetworkNodes nodes) {

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

        nodeStatuses.parallelStream().forEach(nodeStatus -> {
            Request request = new Request.Builder()
                    .url(nodeStatus.getURL() + "/upcheck")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    nodeStatus.Update();
                }
                response.body().close();
            } catch (IOException ex) {
                //
            }
        });
    }
}
