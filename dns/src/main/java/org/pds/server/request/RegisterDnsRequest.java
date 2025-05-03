package org.pds.server.request;

public record RegisterDnsRequest(
        String domainName,
        String address,
        int port
) implements DnsRequest {
}
