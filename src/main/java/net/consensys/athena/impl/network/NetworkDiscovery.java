package net.consensys.athena.impl.network;
import org.apache.commons.*;
import java.util.*;

public class NetworkDiscovery {
    private Collection<Node> nodes;

    public NetworkDiscovery() {
        nodes = new ArrayList<Node>();
    }

    public void doDiscover() {
        doDiscover(1000);
    }

    public void doDiscover(int timeout) {
        CloseableHttpClient client = HttpClients.createDefault();



        HttpPost httpPost = new HttpPost("http://www.example.com");
     
//        List<NameValuePair> params = new ArrayList<NameValuePair>();
//        params.add(new BasicNameValuePair("username", "John"));
//        params.add(new BasicNameValuePair("password", "pass"));
//        httpPost.setEntity(new UrlEncodedFormEntity(params));
     
        CloseableHttpResponse response = client.execute(httpPost);
        //assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        client.close();
    }
}
