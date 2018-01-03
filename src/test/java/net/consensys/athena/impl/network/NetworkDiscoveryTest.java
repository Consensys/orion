package net.consensys.athena.impl.network;

import net.consensys.athena.api.cmd.AthenaRouter;
import net.consensys.athena.api.config.Node;
import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.impl.config.MemoryConfig;
import net.consensys.athena.impl.http.data.Serializer;
import net.consensys.athena.impl.http.server.netty.DefaultNettyServer;
import net.consensys.athena.impl.http.server.netty.NettyServer;
import net.consensys.athena.impl.http.server.netty.NettySettings;
import org.junit.Before;
import org.junit.Test;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.*;

public class NetworkDiscoveryTest {

    final Serializer serializer = new Serializer();
    private NettyServer server = null;

    @Before
    public void Setup() throws Exception {
        int port = 9001;
        NettySettings settings =
                new NettySettings(
                        empty(),
                        of(port),
                        empty(),
                        new AthenaRouter(new MemoryNetworkNodes(), new MemoryConfig(), serializer),
                        serializer);

        server = new DefaultNettyServer(settings);
        server.start();
    }

    @Test
    public void shouldDiscoverLocalNode() throws Exception {
        NetworkNodes mocks = new MockNodes();
        net.consensys.athena.api.network.NetworkDiscovery networkDiscovery = new NetworkDiscovery(mocks);

        networkDiscovery.doDiscover();

        assertNotNull(networkDiscovery.getNodeStatuses());

        for(NodeStatus node : networkDiscovery.getNodeStatuses()) {
            long delta = node.getLastSeen();
            assertTrue(delta >= 0);
        }
    }

    @Test
    public void shouldDiscoverLocalNodeInParallel() throws Exception {
        NetworkNodes mocks = new MockNodes();
        net.consensys.athena.api.network.NetworkDiscovery networkDiscovery = new ParallelNetworkDiscovery(mocks);

        networkDiscovery.doDiscover();

        assertNotNull(networkDiscovery.getNodeStatuses());

        for(NodeStatus node : networkDiscovery.getNodeStatuses()) {
            long delta = node.getLastSeen();
            assertTrue(delta >= 0);
        }
    }
}