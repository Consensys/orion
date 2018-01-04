package net.consensys.athena.impl.network;

import net.consensys.athena.api.cmd.AthenaRouter;
import net.consensys.athena.api.network.NetworkDiscovery;
import net.consensys.athena.impl.config.MemoryConfig;
import net.consensys.athena.impl.http.data.Serializer;
import net.consensys.athena.impl.http.server.netty.DefaultNettyServer;
import net.consensys.athena.impl.http.server.netty.NettyServer;
import net.consensys.athena.impl.http.server.netty.NettySettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.*;

public class NetworkDiscoveryTest {

    final Serializer serializer = new Serializer();
    private NettyServer[] server = new NettyServer[10];
    private MemoryNetworkNodes mocks = new MemoryNetworkNodes();

    @Before
    public void Setup() throws Exception {

        for (int i = 0; i < 10; i++) {
            int port = 9000 + i;
            mocks.addNodeURL(new URL("http://localhost:" + port));

            NettySettings settings =
                    new NettySettings(
                            empty(),
                            of(port),
                            empty(),
                            new AthenaRouter(new MemoryNetworkNodes(), new MemoryConfig(), serializer),
                            serializer);

            server[i] = new DefaultNettyServer(settings);
            server[i].start();
        }
    }

    @After
    public void TearDown()
    {
        for (int i = 0; i < server.length; i++) {
            server[i].stop();
        }
    }

    @Test
    public void shouldDiscoverLocalNodeInParallel() throws Exception {

        NetworkDiscovery networkDiscovery = new ParallelNetworkDiscovery(mocks);
        networkDiscovery.doDiscover();

        assertNotNull(networkDiscovery.getNetworkNodeStatuses());

        for(URL node : mocks.nodeURLs()) {
            long delta = mocks.getLastSeen(node);
            assertTrue(delta >= 0);
        }
    }
}