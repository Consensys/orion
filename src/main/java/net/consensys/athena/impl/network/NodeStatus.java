package net.consensys.athena.impl.network;

import java.net.URL;
import java.util.Date;

public class NodeStatus {

    private Date _lastSeen;
    private URL _url;

    public URL getURL() {
        return _url;
    }

    public void setURL(URL url) {
        _url = url;
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
