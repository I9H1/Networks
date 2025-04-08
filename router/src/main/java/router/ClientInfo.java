package router;

import java.net.InetSocketAddress;

public class ClientInfo {
    public String ip;
    public InetSocketAddress adress;

    public ClientInfo(String ip, InetSocketAddress adress) {
        this.ip = ip;
        this.adress = adress;
    }
}
