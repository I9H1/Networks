package org.pds.server.request;

import org.pds.util.DnsException;

public record QueryDnsRequest(String domainName) implements DnsRequest {

    public QueryDnsRequest {
        if (domainName == null) {
            throw new DnsException("Domain name cannot be null");
        }
    }
}
