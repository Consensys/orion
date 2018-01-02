package net.consensys.athena.impl.network;

import java.util.Collection;

public abstract class BaseNetworkDiscovery {
    protected final int timeout = 1000;
    protected Collection<NodeStatus> nodeStatuses;

    public Collection<NodeStatus> getNodeStatuses() {
        return nodeStatuses;
    }
}

