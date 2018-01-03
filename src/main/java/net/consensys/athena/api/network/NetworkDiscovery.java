package net.consensys.athena.api.network;

import net.consensys.athena.impl.network.NodeStatus;

import java.io.IOException;
import java.util.Collection;

public interface NetworkDiscovery {
    Iterable<NodeStatus> getNodeStatuses();

    void doDiscover() throws IOException;

    void doDiscover(long timeout) throws IOException;
}
