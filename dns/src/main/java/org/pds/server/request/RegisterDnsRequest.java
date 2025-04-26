package org.pds.server.request;

public record RegisterDnsRequest(
        String domainName,
        String address
) implements DnsRequest {
}
