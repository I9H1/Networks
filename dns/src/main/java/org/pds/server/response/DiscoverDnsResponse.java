package org.pds.server.response;

import java.net.InetAddress;

public record DiscoverDnsResponse(InetAddress inetAddress) implements DnsResponse {
    @Override
    public byte[] bytes() {
        return inetAddress.getHostAddress().getBytes();
    }
}
