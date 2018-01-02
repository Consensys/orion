package net.consensys.athena.impl.network;

import java.util.Date;

public class NodeStatus {

    private Date _lastSeen;
    private String _ip;

    public String getURL() {
        return _ip;
    }

    public void setURL(String ip) {
        this._ip = ip;
    }

    public long getLatency() {
        return 0;
    }

    public Date getLastSeen() {
        return _lastSeen;
    }

    public void Update() {
        this._lastSeen = new Date();
    }
}
