package net.consensys.athena.impl.network;

import java.util.Date;

public class Node {

    private Date _lastSeen;
    private String _ip;

    public String getIP() {
        return _ip;
    }

    public long getLatency() {

    }

    public boolean getAlive() {

    }

    public Date getLastSeen() {
        return _lastSeen;
    }
}
