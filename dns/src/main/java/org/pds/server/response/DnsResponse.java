package org.pds.server.response;

public sealed interface DnsResponse permits
        DiscoverDnsResponse,
        ErrorDnsResponse,
        QueryDnsResponse,
        RegisterDnsResponse
{
    byte[] bytes();
}
