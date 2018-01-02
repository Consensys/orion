package net.consensys.athena.impl.network;

import net.consensys.athena.api.config.Node;
import net.consensys.athena.api.network.NetworkDiscovery;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ParallelNetworkDiscovery implements NetworkDiscovery {
    private final int timeout = 1000;
    private Set<NodeStatus> nodes;

    @Override
    public Collection<NodeStatus> getNodeStatuses() {
        return nodes;
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

        nodes.parallelStream().forEach(nodeStatus -> {
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
