package net.consensys.athena.impl.network;

import com.fasterxml.jackson.databind.ser.Serializers;
import net.consensys.athena.api.config.Node;
import net.consensys.athena.api.network.NetworkDiscovery;
import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.api.network.NetworkNodesStatus;
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

public class ParallelNetworkDiscovery implements NetworkDiscovery {
    private final long timeout = 1000;
    private NetworkNodes nodes;

    @Override
    public NetworkNodesStatus getNetworkNodeStatuses() {
        return this.nodes;
    }

    public ParallelNetworkDiscovery(NetworkNodes nodes) {
        this.nodes = nodes;
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

        nodes.nodeURLs().parallelStream().forEach(node -> {
            Request request = new Request.Builder()
                    .url(node + "/upcheck")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    nodes.Update(node);
                }
                response.body().close();
            } catch (IOException ex) {
                //
            }
        });
    }
}
