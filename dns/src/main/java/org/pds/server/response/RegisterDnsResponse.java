package org.pds.server.response;

public record RegisterDnsResponse(
        String domainName,
        String address,
        int port
) implements DnsResponse {
    @Override
    public byte[] bytes() {
        return "REGISTERED %s -> %s:%d".formatted(domainName, address, port).getBytes();
    }
}
