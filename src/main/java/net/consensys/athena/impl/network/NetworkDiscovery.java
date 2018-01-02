package net.consensys.athena.impl.network;
import net.consensys.athena.api.network.NetworkNodes;
import org.apache.http.impl.client.*;
import org.apache.http.client.methods.*;

import java.io.IOException;
import java.util.*;

public class NetworkDiscovery {
    public Collection<NodeStatus> nodeStatuses;

    public NetworkDiscovery() {
        nodeStatuses = new ArrayList<NodeStatus>();
    }

    public NetworkDiscovery(Collection<NodeStatus> nodeStatuses) {
        this.nodeStatuses = nodeStatuses;
    }

    public NetworkDiscovery(NetworkNodes nodes) {
        nodes.nodeURLs();
    }

    public void doDiscover() {
        doDiscover(1000);
    }

    public void doDiscover(int timeout) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();

        for(NodeStatus nodeStatus : nodeStatuses) {
            HttpPost httpPost = new HttpPost(nodeStatus.getURL());
            CloseableHttpResponse response = client.execute(httpPost);

            if (response.getStatusLine().getStatusCode() == 200) {
                nodeStatus.Update();
            }
        }

        client.close();
    }
}
