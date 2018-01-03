package net.consensys.athena.impl.network;

import java.net.URL;
import java.util.Date;
import java.time.*;

public class NodeStatus {

    //private Date _lastSeen;
    private Instant _instant;
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

    public long getLastSeen() {
        return Instant.now().toEpochMilli() - _instant.toEpochMilli();
    }


    public void Update() {
        _instant = Instant.now();
    }
}
