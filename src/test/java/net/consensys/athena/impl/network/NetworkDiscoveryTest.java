package net.consensys.athena.impl.network;

import net.consensys.athena.api.cmd.AthenaRouter;
import net.consensys.athena.api.config.Node;
import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.impl.config.MemoryConfig;
import net.consensys.athena.impl.http.data.Serializer;
import net.consensys.athena.impl.http.server.netty.DefaultNettyServer;
import net.consensys.athena.impl.http.server.netty.NettyServer;
import net.consensys.athena.impl.http.server.netty.NettySettings;
import org.junit.Test;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.*;

public class NetworkDiscoveryTest {

    final Serializer serializer = new Serializer();

    @Test
    public void shouldDiscoverLocalNode() throws Exception {
        NetworkNodes mocks = new MockNodes();
        NetworkDiscovery networkDiscovery = new NetworkDiscovery(mocks);

        int port = 9001;
        NettySettings settings =
                new NettySettings(
                        empty(),
                        of(port),
                        empty(),
                        new AthenaRouter(new MemoryNetworkNodes(), new MemoryConfig(), serializer),
                        serializer);

        NettyServer server = new DefaultNettyServer(settings);
        server.start();

        networkDiscovery.doDiscover();

        assertFalse(networkDiscovery.getNodeStatuses().isEmpty());
    }
}