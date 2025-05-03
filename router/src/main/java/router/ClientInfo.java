package router;

import java.net.InetSocketAddress;

public class ClientInfo {
    public String ip;
    public InetSocketAddress address;

    public ClientInfo(String ip, InetSocketAddress address) {
        this.ip = ip;
        this.address = address;
    }
}
