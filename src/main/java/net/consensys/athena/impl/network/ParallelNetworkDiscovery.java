package net.consensys.athena.impl.network;

import net.consensys.athena.api.config.Node;
import net.consensys.athena.api.network.NetworkDiscovery;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

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
    public void doDiscover(int timeout) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();

        nodes.parallelStream().forEach(nodeStatus -> {
            HttpGet httpGet = new HttpGet(nodeStatus.getURL() + "/upcheck");

            try {
                CloseableHttpResponse response = client.execute(httpGet);

                if (response.getStatusLine().getStatusCode() == 200) {
                    nodeStatus.Update();
                }
            } catch (Exception ex) {
                //Unhandle
            }
        });

        client.close();
    }
}
