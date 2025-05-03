package org.pds.server.response;

import org.pds.server.request.DiscoverDnsRequest;
import org.pds.server.request.DnsRequest;
import org.pds.server.request.QueryDnsRequest;
import org.pds.server.request.RegisterDnsRequest;
import org.pds.util.DnsException;

import java.net.InetAddress;
import java.util.HashMap;

public class DnsResponseGenerator {

    private final InetAddress dnsServerAddress;
    private final HashMap<String, DomainInfo> domainTable;

    private record DomainInfo(String address, int port) {}

    public DnsResponseGenerator(
            InetAddress dnsServerAddress,
            HashMap<String, DomainInfo> domainTable
    ) {
        this.dnsServerAddress = dnsServerAddress;
        this.domainTable = domainTable;
    }

    public DnsResponse getDnsResponse(DnsRequest request) {
        if (request instanceof DiscoverDnsRequest) {
            return new DiscoverDnsResponse(dnsServerAddress);
        } else if (request instanceof RegisterDnsRequest registerRequest) {
            domainTable.put(registerRequest.domainName(),
                    new DomainInfo(registerRequest.address(), registerRequest.port()));
            return new RegisterDnsResponse(registerRequest.domainName(),
                    registerRequest.address(),
                    registerRequest.port());
        } else if (request instanceof QueryDnsRequest queryRequest) {
            DomainInfo info = domainTable.get(queryRequest.domainName());
            if (info == null) {
                throw new DnsException("Domain not found: " + queryRequest.domainName());
            }
            return new QueryDnsResponse(queryRequest.domainName(), info.address(), info.port());
        }
        throw new DnsException("Unsupported request type");
    }
}
