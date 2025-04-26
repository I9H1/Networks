package org.pds.server.response;

import org.pds.util.DnsException;

public record ErrorDnsResponse(DnsException exception) implements DnsResponse {

    @Override
    public byte[] bytes() {
        return exception.getMessage().getBytes();
    }
}
