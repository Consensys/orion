package net.consensys.athena.impl.network;
import net.consensys.athena.api.config.Node;
import net.consensys.athena.api.network.NetworkNodes;
import org.apache.http.impl.client.*;
import org.apache.http.client.methods.*;

import java.io.IOException;
import java.net.URL;
import java.util.*;

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
    public void doDiscover(int timeout) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();

        for(NodeStatus nodeStatus : nodeStatuses) {
            //maybe port is required?

            HttpGet httpGet = new HttpGet(nodeStatus.getURL() + "/upcheck");
            CloseableHttpResponse response = client.execute(httpGet);

            if (response.getStatusLine().getStatusCode() == 200) {
                nodeStatus.Update();
            }
        }

        client.close();
    }
}
