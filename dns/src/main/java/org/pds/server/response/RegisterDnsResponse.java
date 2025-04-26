package org.pds.server.response;

public record RegisterDnsResponse(
        String domainName,
        String address
) implements DnsResponse {
    @Override
    public byte[] bytes() {
        return "REGISTERED %s -> %s".formatted(domainName, address).getBytes();
    }
}
