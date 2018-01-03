package net.consensys.athena.impl.network;

import java.util.ArrayList;

public abstract class BaseNetworkDiscovery {
    protected final int timeout = 1000;
    protected ArrayList<NodeStatus> nodeStatuses;

    public Iterable<NodeStatus> getNodeStatuses() {
        return nodeStatuses;
    }
}

