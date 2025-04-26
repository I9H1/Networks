package org.pds.server.response;

import org.pds.util.DnsException;

public record QueryDnsResponse(String ip, String address) implements DnsResponse {
    
    public QueryDnsResponse {
        if (ip == null) {
            throw new DnsException("address not found");
        }
    }
    
    @Override
    public byte[] bytes() {
        return address.getBytes();
    }
}
